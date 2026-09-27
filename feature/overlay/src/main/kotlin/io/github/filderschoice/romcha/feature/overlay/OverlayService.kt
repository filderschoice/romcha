package io.github.filderschoice.romcha.feature.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.filderschoice.romcha.feature.overlay.ui.ChatOverlay
import io.github.filderschoice.romcha.feature.overlay.ui.OverlayActions
import kotlinx.coroutines.launch

/**
 * 他アプリの上にチャットを表示するフォアグラウンドサービス（F-OVL-01/02/03/08、PLAN 4.6）。
 *
 * `TYPE_APPLICATION_OVERLAY` のウィンドウへ Compose の画面を載せる。通常時は `FLAG_NOT_FOCUSABLE` で
 * 公式アプリの操作を妨げない。常駐通知から表示／非表示の切り替えと終了ができる。
 */
class OverlayService :
    LifecycleService(),
    SavedStateRegistryOwner {
    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: OverlayPrefs
    private var view: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private var bounds = WindowBounds(0, 0, 0, 0)
    private val opacity = mutableFloatStateOf(OverlayPrefs.DEFAULT_OPACITY)
    private var visible = true

    override fun onCreate() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        prefs = OverlayPrefs(this)
        opacity.floatValue = prefs.opacity
        lifecycleScope.launch {
            OverlayChannel.events.collect { if (it is OverlayEvent.StopRequested) stopSelf() }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        when (intent?.action) {
            ACTION_TOGGLE -> setVisible(!visible)
            ACTION_STOP -> {
                OverlayChannel.send(OverlayEvent.StopRequested)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> setVisible(true)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeWindow()
        super.onDestroy()
    }

    private fun setVisible(show: Boolean) {
        if (show && !Settings.canDrawOverlays(this)) return
        visible = show
        if (show) addWindow() else removeWindow()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun addWindow() {
        if (view != null) return
        val metrics = windowManager.currentWindowMetrics.bounds
        val density = resources.displayMetrics.density
        val minSize = (MIN_SIZE_DP * density).toInt()
        val saved = prefs.bounds((DEFAULT_WIDTH_DP * density).toInt(), (DEFAULT_HEIGHT_DP * density).toInt())
        bounds = saved.clampTo(metrics.width(), metrics.height(), minSize, minSize)
        val layoutParams =
            WindowManager
                .LayoutParams(
                    bounds.width,
                    bounds.height,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = bounds.x
                    y = bounds.y
                }
        val composeView =
            ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    val state by OverlayChannel.state.collectAsState()
                    ChatOverlay(state = state, opacity = opacity.floatValue, actions = actions)
                }
            }
        windowManager.addView(composeView, layoutParams)
        view = composeView
        params = layoutParams
    }

    private fun removeWindow() {
        view?.let { windowManager.removeView(it) }
        view = null
        params = null
    }

    private fun applyBounds(next: WindowBounds) {
        val metrics = windowManager.currentWindowMetrics.bounds
        val minSize = (MIN_SIZE_DP * resources.displayMetrics.density).toInt()
        bounds = next.clampTo(metrics.width(), metrics.height(), minSize, minSize)
        val layoutParams = params ?: return
        layoutParams.x = bounds.x
        layoutParams.y = bounds.y
        layoutParams.width = bounds.width
        layoutParams.height = bounds.height
        view?.let { windowManager.updateViewLayout(it, layoutParams) }
    }

    private val actions =
        object : OverlayActions {
            override fun onMove(
                dx: Float,
                dy: Float,
            ) = applyBounds(bounds.copy(x = bounds.x + dx.toInt(), y = bounds.y + dy.toInt()))

            override fun onResize(
                dx: Float,
                dy: Float,
            ) = applyBounds(bounds.copy(width = bounds.width + dx.toInt(), height = bounds.height + dy.toInt()))

            override fun onGestureEnd() {
                prefs.saveBounds(bounds)
                prefs.opacity = opacity.floatValue
            }

            override fun onOpacityChange(opacity: Float) {
                this@OverlayService.opacity.floatValue = OverlayFormat.clampOpacity(opacity)
            }

            override fun onHide() = setVisible(false)

            override fun onCandidateSelected(videoId: String) {
                OverlayChannel.send(OverlayEvent.CandidateSelected(videoId))
            }
        }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.overlay_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                )
            manager.createNotificationChannel(channel)
        }
        val toggleLabel = if (visible) R.string.overlay_action_hide else R.string.overlay_action_show
        val builder =
            Notification
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_overlay_notification)
                .setContentTitle(getString(R.string.overlay_notification_title))
                .setContentText(OverlayChannel.state.value.title ?: getString(R.string.overlay_title_placeholder))
                .setOngoing(true)
                .addAction(action(getString(toggleLabel), ACTION_TOGGLE))
                .addAction(action(getString(R.string.overlay_action_stop), ACTION_STOP))
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            builder.setContentIntent(PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE))
        }
        return builder.build()
    }

    private fun action(
        label: String,
        action: String,
    ): Notification.Action = Notification.Action.Builder(null, label, servicePendingIntent(action)).build()

    private fun servicePendingIntent(action: String): PendingIntent =
        PendingIntent.getService(
            this,
            action.hashCode(),
            Intent(this, OverlayService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_TOGGLE = "io.github.filderschoice.romcha.overlay.TOGGLE"
        private const val ACTION_STOP = "io.github.filderschoice.romcha.overlay.STOP"
        private const val DEFAULT_WIDTH_DP = 280
        private const val DEFAULT_HEIGHT_DP = 360
        private const val MIN_SIZE_DP = 160

        /** オーバーレイを表示する（アプリが前面にある時に呼ぶ）。オーバーレイ権限が無い場合は false。 */
        fun start(context: Context): Boolean {
            if (!Settings.canDrawOverlays(context)) return false
            ContextCompat.startForegroundService(context, Intent(context, OverlayService::class.java))
            return true
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
