package io.github.filderschoice.romcha.core.sync

import io.github.filderschoice.romcha.core.chat.ChatMessage
import io.github.filderschoice.romcha.core.chat.ChatParseResult
import io.github.filderschoice.romcha.core.chat.FetchResult
import io.github.filderschoice.romcha.core.chat.RetryListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** ライブチャットの取得元。本番は `InnerTubeClient.fetchLive`、テストは偽物または MockWebServer を使う。 */
fun interface LiveChatSource {
    suspend fun fetch(
        continuation: String,
        listener: RetryListener,
    ): FetchResult<ChatParseResult.Success>
}

/**
 * ポーリングの間隔（F-CHAT-04）。
 *
 * 応答の推奨間隔（`timeoutMs`）に従い、[minIntervalMs]〜[maxIntervalMs] に収める。推奨間隔が無い場合は [defaultIntervalMs]。
 *
 * @property pausedCheckIntervalMs 公式アプリが一時停止中に、再開を確かめる間隔（N-03。この間は取得しない）
 */
data class LivePolling(
    val defaultIntervalMs: Long = 5_000,
    val minIntervalMs: Long = 1_000,
    val maxIntervalMs: Long = 10_000,
    val failureCooldownMs: Long = 10_000,
    val pausedCheckIntervalMs: Long = 1_000,
    val maxMessages: Int = SyncConfig().maxVisible,
) {
    fun intervalFor(timeoutMs: Long?): Long = (timeoutMs ?: defaultIntervalMs).coerceIn(minIntervalMs, maxIntervalMs)
}

/**
 * @property receivedAtMs 受信時刻（`clock` 基準）。表示遅延（F-SYNC-08）の起点
 */
data class ReceivedMessage(
    val message: ChatMessage,
    val receivedAtMs: Long,
)

/**
 * @property received 受信したメッセージ（受信順、上限 [LivePolling.maxMessages] 件）
 * @property ended 継続トークンが無くなった（ライブ・プレミアの終了。F-CHAT-06）
 */
data class LiveState(
    val received: List<ReceivedMessage> = emptyList(),
    val fetchStatus: FetchStatus = FetchStatus.Idle,
    val ended: Boolean = false,
)

/**
 * プレミア公開中・待機中・通常ライブのチャットをポーリングで追従する（F-CHAT-04/05）。
 *
 * 応答の継続トークンを次の取得に使い、推奨間隔だけ待って繰り返す。継続トークンが無くなったら終了とみなして止まる。
 * 公式アプリが明示的に一時停止されている間は取得を止める（N-03）。プレミアの待機中は公式アプリが再生状態にならないため、
 * 一時停止以外（再生中・未検出・停止・バッファ中）では取得を続ける（F-CHAT-05）。
 */
class LiveChatSession(
    private val source: LiveChatSource,
    initialContinuation: String,
    private val playback: () -> PlaybackSnapshot,
    private val clock: () -> Long,
    private val polling: LivePolling = LivePolling(),
) {
    private val mutableState = MutableStateFlow(LiveState())
    val state: StateFlow<LiveState> = mutableState.asStateFlow()

    private var continuation: String? = initialContinuation
    private val seen = LinkedHashSet<String>()

    /** 終了（継続トークンが無くなる）かキャンセルまでポーリングを続ける。 */
    suspend fun run() {
        while (true) {
            val token = continuation ?: return
            if (playback().status == PlaybackStatus.PAUSED) {
                delay(polling.pausedCheckIntervalMs)
                continue
            }
            delay(poll(token))
        }
    }

    /** 1 回取得し、次の取得までの待ち時間を返す。 */
    private suspend fun poll(token: String): Long {
        mutableState.update { it.copy(fetchStatus = FetchStatus.Loading) }
        val listener =
            RetryListener { attempt, _, failure ->
                mutableState.update { it.copy(fetchStatus = FetchStatus.Retrying(attempt, failure)) }
            }
        return when (val result = source.fetch(token, listener)) {
            is FetchResult.Failure -> {
                mutableState.update { it.copy(fetchStatus = FetchStatus.Failed(result.failure)) }
                polling.failureCooldownMs
            }
            is FetchResult.Success -> {
                val chunk = result.value
                continuation = chunk.continuation?.token
                accept(chunk.messages)
                mutableState.update { it.copy(fetchStatus = FetchStatus.Idle, ended = continuation == null) }
                polling.intervalFor(chunk.continuation?.timeoutMs)
            }
        }
    }

    private fun accept(messages: List<ChatMessage>) {
        val now = clock()
        val fresh = messages.filter { seen.add(it.id) }.map { ReceivedMessage(it, now) }
        if (fresh.isEmpty()) return
        mutableState.update { it.copy(received = (it.received + fresh).takeLast(polling.maxMessages)) }
        while (seen.size > polling.maxMessages * SEEN_FACTOR) seen.remove(seen.first())
    }

    private companion object {
        /** 重複判定に覚えておく ID の数（表示上限の倍数）。古い ID が再送されることは無い前提で上限を置く（N-04） */
        const val SEEN_FACTOR = 4
    }
}
