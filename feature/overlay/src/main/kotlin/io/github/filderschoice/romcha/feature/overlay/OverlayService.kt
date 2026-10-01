package io.github.filderschoice.romcha.feature.overlay

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import io.github.filderschoice.romcha.core.chat.resolve.VideoResolver
import io.github.filderschoice.romcha.core.media.PlaybackMonitor
import io.github.filderschoice.romcha.core.sync.PlaybackSnapshot
import io.github.filderschoice.romcha.feature.overlay.session.InnerTubeBackend
import io.github.filderschoice.romcha.feature.overlay.session.PersistentResolutionCache
import io.github.filderschoice.romcha.feature.overlay.session.SessionEnvironment
import io.github.filderschoice.romcha.feature.overlay.session.SessionIo
import io.github.filderschoice.romcha.feature.overlay.session.SessionSettings
import io.github.filderschoice.romcha.feature.overlay.session.WatchCoordinator
import io.github.filderschoice.romcha.feature.overlay.ui.ChatOverlay
import io.github.filderschoice.romcha.feature.overlay.ui.ManualCommand
import io.github.filderschoice.romcha.feature.overlay.ui.OverlayActions
import io.github.filderschoice.romcha.feature.overlay.ui.OverlayCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private lateinit var prefs: OverlayPrefs
    private lateinit var window: OverlayWindow<OverlayService>
    private val notifications by lazy { OverlayNotifications(this, OverlayService::class.java) }
    private val settings = MutableStateFlow(OverlaySettings())
    private val touchThrough = mutableStateOf(false)
    private val windowMode = mutableStateOf<WindowMode>(WindowMode.Normal)
    private val manualTimer = MutableStateFlow<PlaybackSnapshot?>(null)
    private var visible = true
    private val screenOn = MutableStateFlow(true)
    private lateinit var monitor: PlaybackMonitor

    /** 画面のオン・オフを受け、オフの間はチャットの取得を止める（N-03） */
    private val screenReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                screenOn.value = intent.action == Intent.ACTION_SCREEN_ON
            }
        }

    override fun onCreate() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        super.onCreate()
        prefs = OverlayPrefs(this)
        DisplaySettingsStore.init(this)
        window = OverlayWindow(this, prefs)
        settings.value = prefs.settings
        lifecycleScope.launch {
            OverlayChannel.events.collect { if (it is OverlayEvent.StopRequested) stopSelf() }
        }
        // 表示中の動画が変わったら常駐通知の本文も更新する（BL-033）
        lifecycleScope.launch {
            OverlayNotifications.titleChanges(OverlayChannel.state).collect { updateNotification() }
        }
        monitor = PlaybackMonitor(this)
        monitor.start()
        screenOn.value = getSystemService(PowerManager::class.java).isInteractive
        val filter =
            IntentFilter(Intent.ACTION_SCREEN_ON).apply { addAction(Intent.ACTION_SCREEN_OFF) }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        lifecycleScope.launch { createCoordinator().run() }
    }

    private fun createCoordinator(): WatchCoordinator {
        val backend = InnerTubeBackend()
        val resolver =
            VideoResolver(search = {
                    query,
                    live,
                ->
                backend.search(query, live)
            }, cache = PersistentResolutionCache.shared(this))
        val io =
            SessionIo(
                requestedVideo = OverlayChannel.requestedVideo,
                takeRequestedVideo = OverlayChannel::takeRequestedVideo,
                events = OverlayChannel.events,
                publish = OverlayChannel::publish,
            )
        val env =
            SessionEnvironment(
                nowPlaying = monitor.state,
                screenOn = screenOn,
                settings =
                    SessionSettings(
                        liveDelaySeconds = settings.part { it.liveDelaySeconds },
                        syncOffsetMs = settings.part { it.syncOffsetMs },
                        maxVisible = DisplaySettingsStore.state.part { it.maxVisible },
                        topChatOnly = DisplaySettingsStore.state.part { it.topChatOnly },
                    ),
                clock = SystemClock::elapsedRealtime,
                manualTimer = manualTimer,
            )
        return WatchCoordinator(backend = backend, resolver = resolver, env = env, io = io)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        // 通知へのアクセスが後から許可された場合に備え、起動のたびに監視の開始を試みる（開始済みなら何もしない）
        monitor.start()
        startForeground(
            NOTIFICATION_ID,
            notifications.build(visible, OverlayChannel.state.value.title, touchThrough.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        when (intent?.action) {
            ACTION_TOGGLE -> setVisible(!visible)
            ACTION_RELEASE_TOUCH -> setTouchThrough(false)
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
        window.hide()
        unregisterReceiver(screenReceiver)
        monitor.stop()
        OverlayChannel.publish(OverlayUiState())
        super.onDestroy()
    }

    private fun setVisible(show: Boolean) {
        if (show && !Settings.canDrawOverlays(this)) return
        visible = show
        if (show) showWindow() else window.hide()
        updateNotification()
    }

    private fun updateNotification() {
        getSystemService(
            NotificationManager::class.java,
        ).notify(NOTIFICATION_ID, notifications.build(visible, OverlayChannel.state.value.title, touchThrough.value))
    }

    private fun setTouchThrough(on: Boolean) {
        touchThrough.value = on
        window.touchThrough = on
        updateNotification()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        window.onConfigurationChanged()
    }

    private fun showWindow() =
        window.show {
            val state by OverlayChannel.state.collectAsState()
            val current by settings.collectAsState()
            val timer by manualTimer.collectAsState()
            val display by DisplaySettingsStore.state.collectAsState()
            ChatOverlay(
                state = state,
                settings = current,
                display = display,
                touchThrough = touchThrough.value,
                mode = windowMode.value,
                manualTimer = timer,
                actions = actions,
            )
        }

    /** 設定の一部だけを流す（セッションへ渡す表示遅延・同期の補正・表示保持件数） */
    private fun <T, R> StateFlow<T>.part(select: (T) -> R): StateFlow<R> =
        map(select).stateIn(lifecycleScope, SharingStarted.Eagerly, select(value))

    private val actions =
        object : OverlayActions {
            override fun onMove(
                dx: Float,
                dy: Float,
            ) = window.moveBy(dx, dy)

            override fun onResize(
                dx: Float,
                dy: Float,
            ) = window.resizeBy(dx, dy)

            override fun onGestureEnd() {
                // 画面端を越えて押し込んで離したら退避する（BL-049）
                window.endGesture()?.let { onWindowModeChange(WindowMode.Stashed(it)) }
                prefs.settings = settings.value
            }

            override fun onSettingsChange(settings: OverlaySettings) {
                this@OverlayService.settings.value = settings.normalized()
            }

            override fun onWindowModeChange(mode: WindowMode) {
                windowMode.value = mode
                window.mode = mode
            }

            override fun onCommand(command: OverlayCommand) =
                when (command) {
                    OverlayCommand.TOUCH_THROUGH -> setTouchThrough(true)
                    OverlayCommand.HIDE -> setVisible(false)
                    // アプリ本体の画面を開く（BL-052）。オーバーレイを表示中のため、サービスからのアクティビティ起動が
                    // 認められる（バックグラウンドからの起動制限の例外）。起動用インテントが無い場合は何もしない
                    OverlayCommand.OPEN_APP ->
                        packageManager.getLaunchIntentForPackage(packageName)?.let {
                            startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } ?: Unit
                }

            override fun onCandidateSelected(videoId: String) {
                OverlayChannel.send(OverlayEvent.CandidateSelected(videoId))
            }

            override fun onManual(command: ManualCommand) {
                manualTimer.value =
                    ManualControl.apply(
                        timer = manualTimer.value,
                        command = command,
                        displayedPositionMs = OverlayChannel.state.value.positionMs - settings.value.syncOffsetMs,
                        nowElapsedMs = SystemClock.elapsedRealtime(),
                    )
            }

            override fun onInputFocus(focused: Boolean) {
                window.focusable = focused
            }
        }

    companion object {
        private const val NOTIFICATION_ID = OverlayNotifications.NOTIFICATION_ID
        private const val ACTION_TOGGLE = OverlayNotifications.ACTION_TOGGLE
        private const val ACTION_STOP = OverlayNotifications.ACTION_STOP
        private const val ACTION_RELEASE_TOUCH = OverlayNotifications.ACTION_RELEASE_TOUCH

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
