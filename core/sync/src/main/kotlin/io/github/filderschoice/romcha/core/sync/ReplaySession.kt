package io.github.filderschoice.romcha.core.sync

import io.github.filderschoice.romcha.core.chat.ChatMessage
import io.github.filderschoice.romcha.core.chat.ChatParseResult
import io.github.filderschoice.romcha.core.chat.FetchFailure
import io.github.filderschoice.romcha.core.chat.FetchResult
import io.github.filderschoice.romcha.core.chat.RetryListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** リプレイチャットの取得元。本番は `InnerTubeClient.fetchReplay`、テストは偽物を渡す。 */
fun interface ReplayChatSource {
    suspend fun fetch(
        continuation: String,
        playerOffsetMs: Long?,
        listener: RetryListener,
    ): FetchResult<ChatParseResult.Success>
}

/** 取得の状態。オーバーレイの状態表示に使う（F-CHAT-10、F-OVL-09）。 */
sealed interface FetchStatus {
    data object Idle : FetchStatus

    data object Loading : FetchStatus

    /** 再試行の待機中（[attempt] 回目の失敗後） */
    data class Retrying(
        val attempt: Int,
        val failure: FetchFailure,
    ) : FetchStatus

    /** 再試行しても失敗した。[SessionTiming.failureCooldownMs] 後に再度取得する */
    data class Failed(
        val failure: FetchFailure,
    ) : FetchStatus
}

/**
 * @property messages 表示すべきメッセージ（時刻順）
 * @property ended リプレイの終端まで取得済み
 */
data class ReplayState(
    val positionMs: Long = 0,
    val status: PlaybackStatus = PlaybackStatus.NONE,
    val messages: List<ChatMessage> = emptyList(),
    val fetchStatus: FetchStatus = FetchStatus.Idle,
    val ended: Boolean = false,
)

/**
 * 取得・表示更新の間隔。
 *
 * @property tickIntervalMs 表示の更新間隔（PLAN 4.2）
 * @property failureCooldownMs 再試行しても失敗した後、次に取得するまでの待ち時間
 * @property minFetchIntervalMs 続きの取得の最小間隔（K-04。シーク後の取り直しは対象外）
 */
data class SessionTiming(
    val tickIntervalMs: Long = 250,
    val failureCooldownMs: Long = 10_000,
    val minFetchIntervalMs: Long = 1_000,
)

/**
 * アーカイブのリプレイチャットを再生位置に同期させて取得・表示する（F-CHAT-01〜03、F-SYNC-03〜05）。
 *
 * [SessionTiming.tickIntervalMs] ごとに [SyncEngine] を進め、要求された範囲を [source] から取得する。
 * 初回・シーク後は [initialContinuation] と `playerOffsetMs` で取り直し、それ以外は応答の継続トークンで続きを取る。
 * [SyncEngine] はスレッドセーフではないため、[run] は単一スレッドのディスパッチャーで実行する。
 */
class ReplaySession(
    private val source: ReplayChatSource,
    private val initialContinuation: String,
    private val playback: () -> PlaybackSnapshot,
    private val clock: () -> Long,
    config: SyncConfig = SyncConfig(),
    private val timing: SessionTiming = SessionTiming(),
) {
    private val engine =
        SyncEngine<ChatMessage>(offsetOf = { it.videoOffsetMs ?: 0 }, keyOf = { it.id }, config = config)
    private val mutableState = MutableStateFlow(ReplayState())
    val state: StateFlow<ReplayState> = mutableState.asStateFlow()

    private var continuation: String? = null
    private var activeGeneration = -1
    private var fetchJob: Job? = null
    private var retryAfterMs = 0L
    private var lastFetchAtMs: Long? = null

    /** キャンセルされるまで同期を続ける。 */
    suspend fun run() =
        coroutineScope {
            while (isActive) {
                val now = clock()
                val frame = engine.tick(playback(), now)
                mutableState.update {
                    it.copy(
                        positionMs = frame.positionMs,
                        status = frame.status,
                        messages = if (frame.changed) frame.visible else it.messages,
                    )
                }
                val request = frame.fetchRequest
                if (request != null) {
                    if (shouldDefer(request, now)) {
                        engine.onFetchFailed(request.generation)
                    } else {
                        lastFetchAtMs = now
                        fetchJob?.takeIf { request.restart }?.cancel()
                        fetchJob = launch { fetch(request) }
                    }
                }
                delay(timing.tickIntervalMs)
            }
        }

    /**
     * 要求を見送り、後の tick で再要求させるか（K-04 の通信量抑制）。
     * 再試行しても失敗した直後の冷却期間中と、続きの取得が最小間隔より短い場合に見送る。シーク後の取り直しは間隔を問わない。
     */
    private fun shouldDefer(
        request: FetchRequest,
        now: Long,
    ): Boolean {
        if (now < retryAfterMs) return true
        val last = lastFetchAtMs ?: return false
        return !request.restart && now - last < timing.minFetchIntervalMs
    }

    private suspend fun fetch(request: FetchRequest) {
        activeGeneration = request.generation
        val token = if (request.restart) initialContinuation else continuation ?: initialContinuation
        val offset = if (request.restart || continuation == null) request.fromMs else null
        mutableState.update { it.copy(fetchStatus = FetchStatus.Loading, ended = false) }
        val listener =
            RetryListener { attempt, _, failure ->
                mutableState.update { it.copy(fetchStatus = FetchStatus.Retrying(attempt, failure)) }
            }
        val result = source.fetch(token, offset, listener)
        if (request.generation != activeGeneration) return
        when (result) {
            is FetchResult.Success -> onSuccess(request, result.value)
            is FetchResult.Failure -> {
                retryAfterMs = clock() + timing.failureCooldownMs
                engine.onFetchFailed(request.generation)
                mutableState.update { it.copy(fetchStatus = FetchStatus.Failed(result.failure)) }
            }
        }
    }

    private fun onSuccess(
        request: FetchRequest,
        chunk: ChatParseResult.Success,
    ) {
        continuation = chunk.continuation?.token
        val lastOffset = chunk.messages.maxOfOrNull { it.videoOffsetMs ?: 0 }
        // メッセージが無い区間でも先へ進めるよう、空の応答は一定幅を取得済みとみなす
        val coveredUntil = maxOf(lastOffset ?: (request.fromMs + EMPTY_CHUNK_ADVANCE_MS), request.fromMs)
        val hasMore = chunk.continuation != null
        engine.onFetched(request.generation, chunk.messages, coveredUntil, hasMore)
        mutableState.update { it.copy(fetchStatus = FetchStatus.Idle, ended = !hasMore) }
    }

    private companion object {
        const val EMPTY_CHUNK_ADVANCE_MS = 10_000L
    }
}
