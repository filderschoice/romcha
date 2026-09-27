package io.github.filderschoice.romcha.feature.overlay

import io.github.filderschoice.romcha.core.chat.ChatMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/** 同期状態の表示（F-OVL-09）。 */
enum class SyncIndicator {
    /** 公式アプリの再生位置に同期している */
    SYNCING,

    /** 公式アプリが一時停止中 */
    PAUSED,

    /** 公式アプリの再生を検出できない */
    NOT_DETECTED,

    /** ライブ・プレミアの最新追従 */
    LIVE,
}

/** 動画の切り替え候補（F-VID-02）。 */
data class OverlayCandidate(
    val videoId: String,
    val title: String,
    val channelName: String,
)

/**
 * フローティングウィンドウの表示内容。
 *
 * @property notice 状態・エラーの説明文（チャット無効 F-VID-07、通信失敗 F-CHAT-10、解析失敗 N-08 など）。無ければ null
 * @property candidates 誤特定時にワンタップで切り替える候補、または確度が低い時の選択肢（F-VID-02）
 */
data class OverlayUiState(
    val title: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val positionMs: Long = 0,
    val indicator: SyncIndicator = SyncIndicator.NOT_DETECTED,
    val notice: String? = null,
    val candidates: List<OverlayCandidate> = emptyList(),
)

/** 利用者の操作（フローティングウィンドウ・常駐通知・アプリ画面から、セッションへ伝える）。 */
sealed interface OverlayEvent {
    data class CandidateSelected(
        val videoId: String,
    ) : OverlayEvent

    /** 常駐通知またはウィンドウから終了が選ばれた */
    data object StopRequested : OverlayEvent
}

/**
 * フローティングウィンドウと、表示内容を作る処理（セッション）の間の受け渡し口。
 *
 * 単一プロセスのアプリのため、プロセス内で共有するオブジェクトで受け渡す。
 */
object OverlayChannel {
    private val mutableState = MutableStateFlow(OverlayUiState())
    val state: StateFlow<OverlayUiState> = mutableState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<OverlayEvent>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<OverlayEvent> = mutableEvents.asSharedFlow()

    fun publish(state: OverlayUiState) {
        mutableState.value = state
    }

    fun update(transform: (OverlayUiState) -> OverlayUiState) {
        mutableState.value = transform(mutableState.value)
    }

    /** 購読者が居ない時に送った操作は捨てられる（ウィンドウ表示中の操作にだけ使う）。 */
    fun send(event: OverlayEvent) {
        mutableEvents.tryEmit(event)
    }

    private val mutableRequestedVideo = MutableStateFlow<String?>(null)

    /**
     * 共有・URL 入力で指定された動画ID（F-VID-04/05。自動特定より優先する）。
     *
     * セッションの開始前に指定されても失われないよう、取り出されるまで保持する。
     */
    val requestedVideo: StateFlow<String?> = mutableRequestedVideo.asStateFlow()

    fun requestVideo(videoId: String) {
        mutableRequestedVideo.value = videoId
    }

    /** 指定された動画IDを取り出し、保持をやめる。 */
    fun takeRequestedVideo(): String? = mutableRequestedVideo.getAndUpdate { null }

    private const val EVENT_BUFFER = 8
}
