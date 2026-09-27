package io.github.filderschoice.romcha.feature.overlay.session

import io.github.filderschoice.romcha.core.chat.FetchFailure
import io.github.filderschoice.romcha.core.sync.FetchStatus
import io.github.filderschoice.romcha.core.sync.PlaybackStatus
import io.github.filderschoice.romcha.feature.overlay.SyncIndicator

/** セッションの状態をオーバーレイの表示文・表示状態へ変換する。 */
internal object SessionMessages {
    const val NOT_DETECTED = "公式アプリの再生を検出していません"
    const val RESOLVING = "再生中の動画を特定しています…"
    const val AMBIGUOUS = "動画を特定できませんでした。候補から選んでください"
    const val NOT_FOUND = "動画を特定できません。公式アプリの「共有」または URL 入力で指定してください"
    const val LOADING_VIDEO = "チャットを読み込んでいます…"
    const val CHAT_UNAVAILABLE = "この動画ではチャットを利用できません"
    const val REPLAY_NOT_PROVIDED = "配信は終了しました。チャットのリプレイは利用できません"
    const val SCREEN_OFF = "画面オフのためチャットの取得を停止しています"

    /** ライブ・プレミアの終了後、リプレイの準備を待っている間の表示（F-CHAT-06）。 */
    fun liveEnded(attempt: Int): String = "配信は終了しました。チャットのリプレイの準備を待っています（$attempt 回目）"

    /** 通信・解析の失敗を利用者向けの文にする（F-CHAT-10、N-08）。 */
    fun describe(failure: FetchFailure): String =
        when (failure) {
            is FetchFailure.Network -> "通信できません。接続を確認してください"
            is FetchFailure.Http -> "YouTube からエラーが返されました（HTTP ${failure.code}）"
            is FetchFailure.Parse -> "チャットの解析に失敗しました（YouTube の仕様変更の可能性があります）"
        }

    fun describe(status: FetchStatus): String? =
        when (status) {
            FetchStatus.Idle, FetchStatus.Loading -> null
            is FetchStatus.Retrying -> "再接続しています（${status.attempt} 回目）"
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
}
