package io.github.filderschoice.romcha.feature.overlay

import android.content.Context
import androidx.core.content.edit
import io.github.filderschoice.romcha.core.sync.LiveTimeline

/** ウィンドウの位置・大きさ・不透明度の保存（端末内のみ。N-06）。 */
internal class OverlayPrefs(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var opacity: Float
        get() = prefs.getFloat(KEY_OPACITY, DEFAULT_OPACITY)
        set(value) = prefs.edit { putFloat(KEY_OPACITY, OverlayFormat.clampOpacity(value)) }

    /** ライブ・プレミア中の表示遅延（秒。F-SYNC-08） */
    var liveDelaySeconds: Int
        get() = prefs.getInt(KEY_LIVE_DELAY, LiveTimeline.DEFAULT_DELAY_SECONDS)
        set(value) =
            prefs.edit {
                putInt(KEY_LIVE_DELAY, value.coerceIn(LiveTimeline.MIN_DELAY_SECONDS, LiveTimeline.MAX_DELAY_SECONDS))
            }

    fun bounds(
        defaultWidth: Int,
        defaultHeight: Int,
    ): WindowBounds =
        WindowBounds(
            x = prefs.getInt(KEY_X, 0),
            y = prefs.getInt(KEY_Y, DEFAULT_Y),
            width = prefs.getInt(KEY_WIDTH, defaultWidth),
            height = prefs.getInt(KEY_HEIGHT, defaultHeight),
        )

    fun saveBounds(bounds: WindowBounds) =
        prefs.edit {
            putInt(KEY_X, bounds.x)
            putInt(KEY_Y, bounds.y)
            putInt(KEY_WIDTH, bounds.width)
            putInt(KEY_HEIGHT, bounds.height)
        }

    companion object {
        const val DEFAULT_OPACITY = 0.6f
        private const val DEFAULT_Y = 200
        private const val NAME = "overlay"
        private const val KEY_OPACITY = "opacity"
        private const val KEY_LIVE_DELAY = "liveDelaySeconds"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
    }
}
