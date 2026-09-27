package io.github.filderschoice.romcha.feature.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * 常駐通知（F-OVL-08）。表示／非表示の切り替えと終了の操作を付け、本文のタップでアプリを開く。
 *
 * タッチ透過モード中（F-OVL-05）はウィンドウを操作できないため、解除の操作も付ける。
 */
internal class OverlayNotifications(
    private val context: Context,
    private val service: Class<out Service>,
) {
    fun build(
        visible: Boolean,
        title: String?,
        touchThrough: Boolean = false,
    ): Notification {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val name = context.getString(R.string.overlay_channel_name)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW))
        }
        val toggleLabel = if (visible) R.string.overlay_action_hide else R.string.overlay_action_show
        val builder =
            Notification
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_overlay_notification)
                .setContentTitle(context.getString(R.string.overlay_notification_title))
                .setContentText(title ?: context.getString(R.string.overlay_title_placeholder))
                .setOngoing(true)
                .addAction(action(context.getString(toggleLabel), ACTION_TOGGLE))
        if (touchThrough) {
            builder.addAction(action(context.getString(R.string.overlay_action_release_touch), ACTION_RELEASE_TOUCH))
        }
        builder.addAction(action(context.getString(R.string.overlay_action_stop), ACTION_STOP))
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            builder.setContentIntent(PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE))
        }
        return builder.build()
    }

    private fun action(
        label: String,
        action: String,
    ): Notification.Action {
        val intent =
            PendingIntent.getService(
                context,
                action.hashCode(),
                Intent(context, service).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        return Notification.Action.Builder(null, label, intent).build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val ACTION_TOGGLE = "io.github.filderschoice.romcha.overlay.TOGGLE"
        const val ACTION_STOP = "io.github.filderschoice.romcha.overlay.STOP"
        const val ACTION_RELEASE_TOUCH = "io.github.filderschoice.romcha.overlay.RELEASE_TOUCH"
        private const val CHANNEL_ID = "overlay"

        /**
         * 通知の本文に出すタイトルの変化（BL-033）。状態は表示の更新ごとに発行されるため、同じタイトルの間は流さない
         * （通知の出し直しを動画の切り替え時に限る）。
         */
        fun titleChanges(states: Flow<OverlayUiState>): Flow<String?> = states.map { it.title }.distinctUntilChanged()
    }
}
