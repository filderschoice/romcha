package io.github.filderschoice.romcha.ui

/** アプリ画面の操作。 */
interface HomeActions {
    fun requestOverlay()

    fun requestNotificationAccess()

    fun requestPostNotifications()

    fun startOverlay()

    fun stopOverlay()

    fun openVideo(videoId: String)
}
