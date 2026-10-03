package io.github.filderschoice.romcha.feature.overlay.session

import io.github.filderschoice.romcha.core.chat.FetchFailure
import io.github.filderschoice.romcha.core.sync.FetchStatus
import io.github.filderschoice.romcha.core.sync.PlaybackStatus
import io.github.filderschoice.romcha.feature.overlay.NoticeLevel
import io.github.filderschoice.romcha.feature.overlay.OverlayNotice
import io.github.filderschoice.romcha.feature.overlay.SyncIndicator

/**
 * セッションの状態をオーバーレイの表示文・表示状態へ変換する。
 *
 * 待ち状態・利用者の操作で進められる状態は案内（[NoticeLevel.INFO]）、失敗・チャットを出せない状態は [NoticeLevel.ERROR] にする。
 */
internal object SessionMessages {
    val NOT_DETECTED = info("公式アプリの再生を検出していません")
    val RESOLVING = info("再生中の動画を特定しています…")
    val AMBIGUOUS = info("動画を特定できませんでした。候補から選んでください")
    val NOT_FOUND = error("動画を特定できません。公式アプリの「共有」または URL 入力で指定してください")
    val LOADING_VIDEO = info("チャットを読み込んでいます…")
    val CHAT_UNAVAILABLE = error("この動画ではチャットを利用できません")
    val REPLAY_NOT_PROVIDED = error("配信は終了しました。チャットのリプレイは利用できません")
    val SCREEN_OFF = info("画面オフのためチャットの取得を停止しています")

    /** チャット無効の説明（応答の文言。無ければ既定文。F-VID-07）。 */
    fun chatUnavailable(message: String?): OverlayNotice = message?.let(::error) ?: CHAT_UNAVAILABLE

    /** ライブ・プレミアの終了後、リプレイの準備を待っている間の表示（F-CHAT-06）。 */
    fun liveEnded(attempt: Int): OverlayNotice = info("配信は終了しました。チャットのリプレイの準備を待っています（$attempt 回目）")

    /** 通信・解析の失敗を利用者向けの文にする（F-CHAT-10、N-08）。 */
    fun describe(failure: FetchFailure): OverlayNotice =
        when (failure) {
            is FetchFailure.Network -> error("通信できません。接続を確認してください")
            is FetchFailure.Http -> error("YouTube からエラーが返されました（HTTP ${failure.code}）")
            is FetchFailure.Parse -> error("チャットの解析に失敗しました（YouTube の仕様変更の可能性があります）")
        }

    fun describe(status: FetchStatus): OverlayNotice? =
        when (status) {
            FetchStatus.Idle, FetchStatus.Loading -> null
            is FetchStatus.Retrying -> info("再接続しています（${status.attempt} 回目）")
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

    private fun info(text: String) = OverlayNotice(text, NoticeLevel.INFO)

    private fun error(text: String) = OverlayNotice(text, NoticeLevel.ERROR)
}
