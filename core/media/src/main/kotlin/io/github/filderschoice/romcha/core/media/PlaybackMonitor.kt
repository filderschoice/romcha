package io.github.filderschoice.romcha.core.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import io.github.filderschoice.romcha.core.chat.resolve.TrackMetadata
import io.github.filderschoice.romcha.core.media.MediaMapping.MetadataValueType
import io.github.filderschoice.romcha.core.media.MediaMapping.MetadataValueType.LONG
import io.github.filderschoice.romcha.core.media.MediaMapping.MetadataValueType.OTHER
import io.github.filderschoice.romcha.core.media.MediaMapping.MetadataValueType.TEXT
import io.github.filderschoice.romcha.core.media.MediaMapping.MetadataValueType.UNKNOWN
import io.github.filderschoice.romcha.core.sync.PlaybackSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 公式アプリの再生状態。
 *
 * @property sessionFound 対象アプリの MediaSession が見つかっている
 * @property debugLines MediaSession の全キーと値（M0 の Q-02 確認用。画面表示のみでログへは出さない。N-07）
 */
data class NowPlaying(
    val snapshot: PlaybackSnapshot = PlaybackSnapshot.NONE,
    val metadata: TrackMetadata? = null,
    val sessionFound: Boolean = false,
    val debugLines: List<String> = emptyList(),
)

/**
 * 公式アプリの MediaSession から再生位置・状態・速度・メタデータを取得する（F-SYNC-01/02、PLAN 4.2）。
 *
 * 「通知へのアクセス」が許可されている必要がある。MediaSession は表示形態（全画面・PiP・バックグラウンド）に
 * 依存しないため、同じ方法で取得できる見込み（M0 で実機確認）。コールバックはメインスレッドで受ける。
 */
class PlaybackMonitor(
    private val context: Context,
    private val targetPackage: String = YOUTUBE_PACKAGE,
) {
    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow(NowPlaying())
    val state: StateFlow<NowPlaying> = mutableState.asStateFlow()

    private var controller: MediaController? = null
    private var started = false

    /** 型の分からない MediaMetadata のキーについて、読めた型（メインスレッドでのみ読み書きする） */
    private val learnedTypes = mutableMapOf<String, MetadataValueType>()

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers -> attach(controllers.orEmpty()) }

    private val controllerCallback =
        object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) = publish()

            override fun onMetadataChanged(metadata: MediaMetadata?) = publish()

            override fun onSessionDestroyed() = attach(emptyList())
        }

    /** 「通知へのアクセス」が許可されているか（F-APP-01 の権限案内で使う）。 */
    fun isAccessGranted(): Boolean {
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(context)
        return context.packageName in enabled
    }

    /** 監視を始める。許可が無い場合は何もせず false を返す。 */
    fun start(): Boolean {
        if (started) return true
        if (!isAccessGranted()) return false
        return try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, handler)
            attach(sessionManager.getActiveSessions(listenerComponent))
            started = true
            true
        } catch (ignored: SecurityException) {
            // 許可の取り消し直後などに発生する。未検出として扱う
            false
        }
    }

    fun stop() {
        if (!started) return
        sessionManager.removeOnActiveSessionsChangedListener(sessionsListener)
        controller?.unregisterCallback(controllerCallback)
        controller = null
        started = false
        mutableState.value = NowPlaying()
    }

    private fun attach(controllers: List<MediaController>) {
        val next = controllers.firstOrNull { it.packageName == targetPackage }
        if (next?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(controllerCallback)
            next?.registerCallback(controllerCallback, handler)
            controller = next
        }
        publish()
    }

    private fun publish() {
        val current = controller
        if (current == null) {
            mutableState.value = NowPlaying()
            return
        }
        val playbackState = current.playbackState
        val snapshot =
            if (playbackState == null) {
                PlaybackSnapshot.NONE
            } else {
                MediaMapping.toSnapshot(
                    state = playbackState.state,
                    positionMs = playbackState.position,
                    lastPositionUpdateTimeMs = playbackState.lastPositionUpdateTime,
                    playbackSpeed = playbackState.playbackSpeed,
                )
            }
        val metadata = current.metadata
        val entries = collectEntries(current, metadata, playbackState)
        mutableState.value =
            NowPlaying(
                snapshot = snapshot,
                metadata =
                    MediaMapping.toTrackMetadata(
                        title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                        durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0,
                        entries = entries,
                    ),
                sessionFound = true,
                debugLines = MediaMapping.debugLines(entries),
            )
    }

    /** MediaMetadata・MediaDescription・各 extras・キューの全キーを文字列で集める（PLAN 4.3 手順1、Q-02）。 */
    private fun collectEntries(
        controller: MediaController,
        metadata: MediaMetadata?,
        playbackState: PlaybackState?,
    ): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        metadata?.keySet()?.forEach { key ->
            val value = metadataValue(metadata, key)
            if (value != null) entries["metadata.$key"] = value
        }
        metadata?.description?.let { description ->
            description.mediaId?.let { entries["description.mediaId"] = it }
            description.mediaUri?.let { entries["description.mediaUri"] = it.toString() }
            putBundle(entries, "description.extras", description.extras)
        }
        putBundle(entries, "controller.extras", controller.extras)
        putBundle(entries, "state.extras", playbackState?.extras)
        controller.queue?.forEachIndexed { index, item ->
            item.description.mediaId?.let { entries["queue[$index].mediaId"] = it }
            item.description.mediaUri?.let { entries["queue[$index].mediaUri"] = it.toString() }
        }
        return entries
    }

    /**
     * キーの型に合った取得メソッドだけで読む（型違いの取得は Bundle の警告を logcat へ出すため。BL-028）。
     *
     * 型の分からないキーは文字列 → 数値の順に試し、読めた型を [learnedTypes] に覚えて次回からはその型だけで読む
     * （警告はキーごとに初回だけになる）。
     */
    private fun metadataValue(
        metadata: MediaMetadata,
        key: String,
    ): String? {
        val text = { metadata.getText(key)?.toString() }
        val long = { metadata.getLong(key).takeIf { it != 0L }?.toString() }
        val type = MediaMapping.metadataValueType(key).takeUnless { it == UNKNOWN } ?: learnedTypes[key] ?: UNKNOWN
        return when (type) {
            TEXT -> text()
            LONG -> long()
            OTHER -> null
            UNKNOWN ->
                text()?.also { learnedTypes[key] = TEXT } ?: long()?.also { learnedTypes[key] = LONG }
        }
    }

    private fun putBundle(
        entries: MutableMap<String, String>,
        prefix: String,
        bundle: Bundle?,
    ) {
        bundle ?: return
        for (key in bundle.keySet()) {
            @Suppress("DEPRECATION") // 型の分からない値を文字列化するため、型指定の無い get を使う
            val value = bundle.get(key) ?: continue
            entries["$prefix.$key"] = value.toString()
        }
    }

    companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }
}
