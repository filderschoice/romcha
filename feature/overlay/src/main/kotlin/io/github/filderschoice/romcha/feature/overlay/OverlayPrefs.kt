package io.github.filderschoice.romcha.feature.overlay

import android.content.Context
import androidx.core.content.edit
import io.github.filderschoice.romcha.core.sync.LiveTimeline
import io.github.filderschoice.romcha.core.sync.SyncOffset

/** ウィンドウの位置・大きさ（画面の向きごと）・不透明度・文字サイズの保存（端末内のみ。N-06）。 */
internal class OverlayPrefs(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    /** 設定パネルの値（不透明度・文字サイズ・表示遅延・同期の補正） */
    var settings: OverlaySettings
        get() =
            OverlaySettings(
                opacity = prefs.getFloat(KEY_OPACITY, OverlaySettings.DEFAULT_OPACITY),
                fontScale = prefs.getFloat(KEY_FONT_SCALE, OverlayFormat.DEFAULT_FONT_SCALE),
                liveDelaySeconds = prefs.getInt(KEY_LIVE_DELAY, LiveTimeline.DEFAULT_DELAY_SECONDS),
                syncOffsetMs = prefs.getLong(KEY_SYNC_OFFSET, SyncOffset.DEFAULT_MS),
            ).normalized()
        set(value) {
            val normalized = value.normalized()
            prefs.edit {
                putFloat(KEY_OPACITY, normalized.opacity)
                putFloat(KEY_FONT_SCALE, normalized.fontScale)
                putInt(KEY_LIVE_DELAY, normalized.liveDelaySeconds)
                putLong(KEY_SYNC_OFFSET, normalized.syncOffsetMs)
            }
        }

    /** 画面の向きごとに保存した位置と大きさ（F-OVL-06）。未保存なら既定値。 */
    fun bounds(
        orientation: ScreenOrientation,
        defaultWidth: Int,
        defaultHeight: Int,
    ): WindowBounds {
        val prefix = prefix(orientation)
        return WindowBounds(
            x = prefs.getInt(prefix + KEY_X, 0),
            y = prefs.getInt(prefix + KEY_Y, DEFAULT_Y),
            width = prefs.getInt(prefix + KEY_WIDTH, defaultWidth),
            height = prefs.getInt(prefix + KEY_HEIGHT, defaultHeight),
        )
    }

    fun saveBounds(
        orientation: ScreenOrientation,
        bounds: WindowBounds,
    ) {
        val prefix = prefix(orientation)
        prefs.edit {
            putInt(prefix + KEY_X, bounds.x)
            putInt(prefix + KEY_Y, bounds.y)
            putInt(prefix + KEY_WIDTH, bounds.width)
            putInt(prefix + KEY_HEIGHT, bounds.height)
        }
    }

    /** 縦は従来のキーをそのまま使い、向き別の保存を導入する前の位置を引き継ぐ */
    private fun prefix(orientation: ScreenOrientation): String =
        when (orientation) {
            ScreenOrientation.PORTRAIT -> ""
            ScreenOrientation.LANDSCAPE -> LANDSCAPE_PREFIX
        }

    companion object {
        private const val DEFAULT_Y = 200
        private const val NAME = "overlay"
        private const val KEY_OPACITY = "opacity"
        private const val KEY_FONT_SCALE = "fontScale"
        private const val KEY_SYNC_OFFSET = "syncOffsetMs"
        private const val KEY_LIVE_DELAY = "liveDelaySeconds"
        private const val LANDSCAPE_PREFIX = "landscape."
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
    }
}
