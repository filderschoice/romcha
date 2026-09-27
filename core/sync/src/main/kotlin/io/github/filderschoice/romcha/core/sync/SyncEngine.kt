package io.github.filderschoice.romcha.core.sync

/**
 * SyncEngine の設定値（PLAN 4.5、N-04）。
 *
 * @property seekToleranceMs シークとみなすずれの閾値
 * @property rewindOnSeekMs シーク・初回表示時に遡って表示する範囲
 * @property prefetchAheadMs 推定位置より先に確保しておくバッファの長さ
 * @property maxVisible 表示保持件数の上限（F-VIEW-04 の既定値）
 */
data class SyncConfig(
    val seekToleranceMs: Long = SeekDetector.DEFAULT_TOLERANCE_MS,
    val rewindOnSeekMs: Long = 30_000,
    val prefetchAheadMs: Long = 60_000,
    val maxVisible: Int = 500,
)

/**
 * チャット取得の要求。
 *
 * @property fromMs 取得を始める動画内オフセット
 * @property restart true なら継続トークンを捨ててオフセット指定で取り直す（初回・シーク後）
 * @property generation 要求の世代。シークで増え、古い世代の応答は [SyncEngine.onFetched] で破棄される
 */
data class FetchRequest(
    val fromMs: Long,
    val restart: Boolean,
    val generation: Int,
)

/**
 * 1 回の [SyncEngine.tick] の結果。
 *
 * @property visible 表示すべきメッセージ（時刻順）。[changed] が false の時は空で、前回の表示を維持する
 * @property rebuilt 初回・シークで表示を作り直した
 */
data class SyncFrame<T>(
    val positionMs: Long,
    val status: PlaybackStatus,
    val visible: List<T>,
    val changed: Boolean,
    val rebuilt: Boolean,
    val fetchRequest: FetchRequest?,
)

/**
 * リプレイチャットを再生位置に同期させるエンジン（F-SYNC-03〜05、F-CHAT-02/03 の判断部分）。Android 非依存。
 *
 * 取得済みで未表示のメッセージを動画内オフセット順に保持し、推定位置以下になったものを表示へ流す。
 * 通信は行わず、取得が必要になったら [SyncFrame.fetchRequest] で要求する。呼び出し側は取得結果を
 * [onFetched] / [onFetchFailed] で返す。スレッドセーフではないため、同一スレッドから呼び出す。
 */
class SyncEngine<T>(
    private val offsetOf: (T) -> Long,
    private val keyOf: (T) -> String,
    private val config: SyncConfig = SyncConfig(),
) {
    private val seekDetector = SeekDetector(config.seekToleranceMs)
    private val pending = ArrayList<T>()
    private val visible = ArrayDeque<T>()
    private val keys = HashSet<String>()

    private var initialized = false
    private var windowStartMs = 0L
    private var shownUntilMs = 0L
    private var coveredUntilMs: Long? = null
    private var endReached = false
    private var inFlight = false
    private var generation = 0
    private var changedSinceTick = false

    /** 現在の再生状態から表示を進める。表示更新間隔（250ms）ごとに呼ぶ。 */
    fun tick(
        snapshot: PlaybackSnapshot,
        nowElapsedMs: Long,
    ): SyncFrame<T> {
        val position = PositionEstimator.estimate(snapshot, nowElapsedMs)
        val seeked = seekDetector.update(position, nowElapsedMs, snapshot.isPlaying, snapshot.speed)
        val rebuilt = !initialized || seeked
        if (rebuilt) rebuild(position)

        var changed = rebuilt || changedSinceTick
        changedSinceTick = false
        while (pending.isNotEmpty() && offsetOf(pending.first()) <= position) {
            addVisible(pending.removeAt(0))
            changed = true
        }
        if (position > shownUntilMs) shownUntilMs = position

        return SyncFrame(
            positionMs = position,
            status = snapshot.status,
            visible = if (changed) visible.toList() else emptyList(),
            changed = changed,
            rebuilt = rebuilt,
            fetchRequest = nextFetchRequest(position, snapshot.isPlaying),
        )
    }

    /**
     * 取得結果を受け取る。
     *
     * @param coveredUntilMs この応答で取得済みになった範囲の終端（動画内オフセット）
     * @param hasMore 続きがあるか。false ならリプレイの終端に達した
     */
    fun onFetched(
        generation: Int,
        items: List<T>,
        coveredUntilMs: Long,
        hasMore: Boolean,
    ) {
        if (generation != this.generation) return
        inFlight = false
        this.coveredUntilMs = maxOf(this.coveredUntilMs ?: coveredUntilMs, coveredUntilMs)
        endReached = !hasMore
        items.forEach(::accept)
    }

    /** 取得に失敗した。次の [tick] で同じ範囲を再要求する（再試行の間隔は呼び出し側が制御する）。 */
    fun onFetchFailed(generation: Int) {
        if (generation == this.generation) inFlight = false
    }

    /** 動画の切り替え時に呼び、状態を初期化する（F-VID-03）。 */
    fun reset() {
        initialized = false
        seekDetector.reset()
        clear()
        generation++
    }

    private fun rebuild(position: Long) {
        initialized = true
        clear()
        generation++
        windowStartMs = (position - config.rewindOnSeekMs).coerceAtLeast(0)
        shownUntilMs = position
    }

    private fun clear() {
        pending.clear()
        visible.clear()
        keys.clear()
        coveredUntilMs = null
        endReached = false
        inFlight = false
        changedSinceTick = false
    }

    private fun nextFetchRequest(
        position: Long,
        playing: Boolean,
    ): FetchRequest? {
        if (inFlight || endReached) return null
        val covered = coveredUntilMs
        val request =
            when {
                covered == null -> FetchRequest(windowStartMs, restart = true, generation = generation)
                // 一時停止中は先読みしない（N-03）
                playing && covered < position + config.prefetchAheadMs ->
                    FetchRequest(covered, restart = false, generation = generation)
                else -> null
            }
        if (request != null) inFlight = true
        return request
    }

    private fun accept(item: T) {
        val offset = offsetOf(item)
        if (offset < windowStartMs || !keys.add(keyOf(item))) return
        if (offset <= shownUntilMs) {
            // 表示位置より前の遅着分は、表示中のリストへ時刻順に差し込む
            val index = visible.indexOfFirst { offsetOf(it) > offset }.let { if (it < 0) visible.size else it }
            visible.add(index, item)
            trimVisible()
            changedSinceTick = true
        } else {
            val found = pending.binarySearchBy(offset) { offsetOf(it) }
            pending.add(if (found < 0) -found - 1 else found + 1, item)
        }
    }

    private fun addVisible(item: T) {
        visible.addLast(item)
        trimVisible()
    }

    private fun trimVisible() {
        while (visible.size > config.maxVisible) {
            keys.remove(keyOf(visible.removeFirst()))
        }
    }
}
