package io.github.filderschoice.romcha.feature.overlay.session

import androidx.annotation.StringRes
import io.github.filderschoice.romcha.core.chat.FetchFailure
import io.github.filderschoice.romcha.core.sync.FetchStatus
import io.github.filderschoice.romcha.core.sync.PlaybackStatus
import io.github.filderschoice.romcha.feature.overlay.NoticeLevel
import io.github.filderschoice.romcha.feature.overlay.NoticeMessage
import io.github.filderschoice.romcha.feature.overlay.OverlayNotice
import io.github.filderschoice.romcha.feature.overlay.R
import io.github.filderschoice.romcha.feature.overlay.SyncIndicator

/**
 * セッションの状態をオーバーレイの表示文・表示状態へ変換する。
 *
 * 待ち状態・利用者の操作で進められる状態は案内（[NoticeLevel.INFO]）、失敗・チャットを出せない状態は [NoticeLevel.ERROR] にする。
 */
internal object SessionMessages {
    val NOT_DETECTED = info(R.string.notice_not_detected)
    val RESOLVING = info(R.string.notice_resolving)
    val AMBIGUOUS = info(R.string.notice_ambiguous)
    val NOT_FOUND = error(R.string.notice_not_found)
    val LOADING_VIDEO = info(R.string.notice_loading_video)
    val CHAT_UNAVAILABLE = error(R.string.notice_chat_unavailable)
    val REPLAY_NOT_PROVIDED = error(R.string.notice_replay_not_provided)
    val SCREEN_OFF = info(R.string.notice_screen_off)

    /** チャット情報が無い理由を YouTube が示さない時。配信直後でリプレイが未生成の可能性を案内し、自動で再確認する（BL-106）。 */
    val CHAT_PENDING = info(R.string.notice_chat_pending)

    /** チャット無効の説明（応答の文言。無ければ既定文。F-VID-07）。応答の文言は翻訳できないためそのまま出す。 */
    fun chatUnavailable(message: String?): OverlayNotice =
        message?.let { OverlayNotice(NoticeMessage.Raw(it), NoticeLevel.ERROR) } ?: CHAT_UNAVAILABLE

    /** ライブ・プレミアの終了後、リプレイの準備を待っている間の表示（F-CHAT-06）。 */
    fun liveEnded(attempt: Int): OverlayNotice = info(R.string.notice_live_ended, attempt)

    /** 通信・解析の失敗を利用者向けの文にする（F-CHAT-10、N-08）。 */
    fun describe(failure: FetchFailure): OverlayNotice =
        when (failure) {
            is FetchFailure.Network -> error(R.string.notice_network)
            is FetchFailure.Http -> error(R.string.notice_http, failure.code)
            is FetchFailure.Parse -> error(R.string.notice_parse)
        }

    fun describe(status: FetchStatus): OverlayNotice? =
        when (status) {
            FetchStatus.Idle, FetchStatus.Loading -> null
            is FetchStatus.Retrying -> info(R.string.notice_retrying, status.attempt)
            is FetchStatus.Failed -> describe(status.failure)
        }

    fun indicator(
        status: PlaybackStatus,
        sessionFound: Boolean,
    ): SyncIndicator =
        when {
            !sessionFound -> SyncIndicator.NOT_DETECTED
            status == PlaybackStatus.PLAYING -> SyncIndicator.SYNCING
            status == PlaybackStatus.PAUSED || status == PlaybackStatus.BUFFERING -> SyncIndicator.PAUSED
            else -> SyncIndicator.NOT_DETECTED
        }

    private fun info(
        @StringRes id: Int,
        vararg args: Any,
    ) = OverlayNotice(NoticeMessage.Res(id, args.toList()), NoticeLevel.INFO)

    private fun error(
        @StringRes id: Int,
        vararg args: Any,
    ) = OverlayNotice(NoticeMessage.Res(id, args.toList()), NoticeLevel.ERROR)
}
