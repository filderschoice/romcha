package io.github.filderschoice.romcha

import io.github.filderschoice.romcha.core.chat.VideoUrlParser

/** 初回起動時に案内する権限（F-APP-01）。案内する順に並べる。 */
enum class PermissionStep {
    /** 他のアプリの上に重ねて表示（SYSTEM_ALERT_WINDOW） */
    OVERLAY,

    /** 通知へのアクセス（MediaSession の取得に使う。通知の内容は読まない） */
    NOTIFICATION_ACCESS,

    /** 通知の表示（常駐通知。POST_NOTIFICATIONS） */
    POST_NOTIFICATIONS,
}

data class PermissionStatus(
    val overlay: Boolean,
    val notificationAccess: Boolean,
    val postNotifications: Boolean,
) {
    fun isGranted(step: PermissionStep): Boolean =
        when (step) {
            PermissionStep.OVERLAY -> overlay
            PermissionStep.NOTIFICATION_ACCESS -> notificationAccess
            PermissionStep.POST_NOTIFICATIONS -> postNotifications
        }

    /** 次に案内すべき権限。すべて許可済みなら null。 */
    val nextStep: PermissionStep? get() = PermissionStep.entries.firstOrNull { !isGranted(it) }

    /** フローティング表示を始められるか。オーバーレイ権限は必須、他は無くても手動の動画指定で使える。 */
    val canStartOverlay: Boolean get() = overlay
}

/** 共有インテント（ACTION_SEND）の本文・件名から動画IDを取り出す（F-VID-04）。 */
object SharedTextHandler {
    fun videoIdFrom(vararg texts: String?): String? =
        texts.filterNotNull().firstNotNullOfOrNull(VideoUrlParser::extractVideoId)
}
