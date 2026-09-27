package io.github.filderschoice.romcha

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import io.github.filderschoice.romcha.core.media.MediaListenerService
import io.github.filderschoice.romcha.core.media.PlaybackMonitor
import io.github.filderschoice.romcha.feature.overlay.DisplaySettingsStore
import io.github.filderschoice.romcha.feature.overlay.OverlayChannel
import io.github.filderschoice.romcha.feature.overlay.OverlayEvent
import io.github.filderschoice.romcha.feature.overlay.OverlayService
import io.github.filderschoice.romcha.ui.HomeActions
import io.github.filderschoice.romcha.ui.RomchaApp
import io.github.filderschoice.romcha.ui.UpdateState
import io.github.filderschoice.romcha.ui.UpdateUiModel
import io.github.filderschoice.romcha.update.UpdateChecker
import kotlinx.coroutines.launch

/**
 * 起動画面。権限案内（F-APP-01）、URL 入力（F-VID-05）、共有の受信（F-VID-04）、免責表示（F-APP-04）、
 * OSS ライセンス（F-APP-03）、更新の確認（F-APP-02）、MediaSession の診断表示（M0 の Q-02 確認用）を持つ。
 */
class MainActivity : ComponentActivity() {
    private val status =
        mutableStateOf(PermissionStatus(overlay = false, notificationAccess = false, postNotifications = false))
    private lateinit var monitor: PlaybackMonitor
    private val updateState = mutableStateOf<UpdateState>(UpdateState.Idle)
    private val updateChecker by lazy { UpdateChecker(userAgent = "Romcha/${BuildConfig.VERSION_NAME}") }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        monitor = PlaybackMonitor(this)
        DisplaySettingsStore.init(this)
        refreshStatus()
        setContent {
            RomchaApp(
                status = status.value,
                nowPlaying = monitor.state,
                update = UpdateUiModel(BuildConfig.VERSION_NAME, updateState.value),
                actions = actions,
            )
        }
        if (savedInstanceState == null) handleShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        monitor.start()
    }

    override fun onPause() {
        monitor.stop()
        super.onPause()
    }

    private fun refreshStatus() {
        status.value =
            PermissionStatus(
                overlay = Settings.canDrawOverlays(this),
                notificationAccess = packageName in NotificationManagerCompat.getEnabledListenerPackages(this),
                postNotifications =
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED,
            )
    }

    /** 公式アプリの「共有」から URL を受け取り、オーバーレイで開いて公式アプリへ戻る（F-VID-04）。 */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val videoId =
            SharedTextHandler.videoIdFrom(
                intent.getStringExtra(Intent.EXTRA_TEXT),
                intent.getStringExtra(Intent.EXTRA_SUBJECT),
            )
        if (videoId == null) {
            Toast.makeText(this, R.string.share_no_video, Toast.LENGTH_LONG).show()
            return
        }
        if (openVideo(videoId)) finish()
    }

    private fun openVideo(videoId: String): Boolean {
        OverlayChannel.requestVideo(videoId)
        val started = OverlayService.start(this)
        if (!started) Toast.makeText(this, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
        return started
    }

    private fun openSettings(
        primary: Intent,
        fallback: Intent,
    ) {
        try {
            startActivity(primary)
        } catch (ignored: ActivityNotFoundException) {
            startActivity(fallback)
        }
    }

    private val actions =
        object : HomeActions {
            override fun requestOverlay() =
                openSettings(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()),
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
                )

            override fun requestNotificationAccess() {
                val component = ComponentName(this@MainActivity, MediaListenerService::class.java).flattenToString()
                openSettings(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component),
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                )
            }

            override fun requestPostNotifications() =
                notificationPermission.launch(
                    Manifest.permission.POST_NOTIFICATIONS,
                )

            override fun startOverlay() {
                if (!OverlayService.start(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
                }
            }

            override fun stopOverlay() {
                OverlayChannel.send(OverlayEvent.StopRequested)
                OverlayService.stop(this@MainActivity)
            }

            override fun openVideo(videoId: String) {
                this@MainActivity.openVideo(videoId)
            }

            override fun checkForUpdate() {
                if (updateState.value == UpdateState.Checking) return
                updateState.value = UpdateState.Checking
                lifecycleScope.launch {
                    updateState.value = UpdateState.Done(updateChecker.check(BuildConfig.VERSION_NAME))
                }
            }

            override fun openReleasePage() {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, UpdateChecker.RELEASES_PAGE.toUri()))
                } catch (ignored: ActivityNotFoundException) {
                    Toast.makeText(this@MainActivity, R.string.update_no_browser, Toast.LENGTH_LONG).show()
                }
            }
        }
}
