package io.github.filderschoice.romcha.ui

/** アプリ画面の操作。 */
interface HomeActions {
    fun requestOverlay()

    fun requestNotificationAccess()

    fun requestPostNotifications()

    fun startOverlay()

    fun stopOverlay()

    fun openVideo(videoId: String)

    /** 更新を確認する（F-APP-02。押した時だけ通信する） */
    fun checkForUpdate()

    /** 動画特定のキャッシュ（タイトル・チャンネル名・長さ → 動画ID）を消す。別の動画のチャットが出る時の対処 */
    fun clearResolutionCache()

    /** 最新リリースのダウンロードページを開く */
    fun openReleasePage()
}
