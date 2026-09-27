package io.github.filderschoice.romcha.core.sync

/** 公式アプリの再生状態。MediaSession の PlaybackState を Android 非依存に写したもの。 */
enum class PlaybackStatus {
    PLAYING,
    PAUSED,
    BUFFERING,
    STOPPED,

    /** MediaSession が見つからない、または状態が取得できない。 */
    NONE,
}

/**
 * 再生状態のスナップショット（F-SYNC-01）。
 *
 * @property positionMs 最後に報告された再生位置（ミリ秒）
 * @property updatedAtElapsedMs [positionMs] が報告された時刻（`SystemClock.elapsedRealtime()` 基準）
 * @property speed 再生速度（1.0 が等速）
 */
data class PlaybackSnapshot(
    val status: PlaybackStatus,
    val positionMs: Long,
    val updatedAtElapsedMs: Long,
    val speed: Float,
) {
    val isPlaying: Boolean get() = status == PlaybackStatus.PLAYING

    companion object {
        val NONE = PlaybackSnapshot(PlaybackStatus.NONE, positionMs = 0, updatedAtElapsedMs = 0, speed = 1f)
    }
}

/** PLAN 4.2 の現在位置推定。再生中のみ経過時間×速度を加算する（F-SYNC-03 / F-SYNC-05）。 */
object PositionEstimator {
    fun estimate(
        snapshot: PlaybackSnapshot,
        nowElapsedMs: Long,
    ): Long {
        if (!snapshot.isPlaying) return snapshot.positionMs.coerceAtLeast(0)
        val elapsed = (nowElapsedMs - snapshot.updatedAtElapsedMs).coerceAtLeast(0)
        return (snapshot.positionMs + elapsed * snapshot.speed).toLong().coerceAtLeast(0)
    }
}
