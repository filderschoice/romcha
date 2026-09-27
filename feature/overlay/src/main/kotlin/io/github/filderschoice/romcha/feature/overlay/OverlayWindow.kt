package io.github.filderschoice.romcha.feature.overlay

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * フローティングウィンドウ本体（`TYPE_APPLICATION_OVERLAY`）の追加・削除と、位置・大きさの管理（F-OVL-01/02/06）。
 *
 * 位置と大きさは画面の向きごとに [OverlayPrefs] へ保存し、向きが変わったらその向きの値へ切り替える。
 */
internal class OverlayWindow<T>(
    private val owner: T,
    private val prefs: OverlayPrefs,
) where T : LifecycleService, T : SavedStateRegistryOwner {
    private val windowManager = owner.getSystemService(WindowManager::class.java)
    private val density get() = owner.resources.displayMetrics.density
    private var view: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private var bounds = WindowBounds(0, 0, 0, 0)
    private var orientation = ScreenOrientation.PORTRAIT

    val isShown: Boolean get() = view != null

    fun show(content: @Composable () -> Unit) {
        if (view != null) return
        orientation = currentOrientation()
        bounds = clamp(savedBounds())
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
            ComposeView(owner).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent(content)
            }
        windowManager.addView(composeView, layoutParams)
        view = composeView
        params = layoutParams
    }

    fun hide() {
        view?.let { windowManager.removeView(it) }
        view = null
        params = null
    }

    fun moveBy(
        dx: Float,
        dy: Float,
    ) = apply(bounds.copy(x = bounds.x + dx.toInt(), y = bounds.y + dy.toInt()))

    fun resizeBy(
        dx: Float,
        dy: Float,
    ) = apply(bounds.copy(width = bounds.width + dx.toInt(), height = bounds.height + dy.toInt()))

    fun saveBounds() = prefs.saveBounds(orientation, bounds)

    /** 画面の向きが変わったら、その向きで記憶していた位置と大きさへ切り替える（F-OVL-06） */
    fun onConfigurationChanged() {
        if (view == null) return
        val next = currentOrientation()
        if (next == orientation) return
        orientation = next
        apply(savedBounds())
    }

    private fun currentOrientation(): ScreenOrientation {
        val metrics = windowManager.currentWindowMetrics.bounds
        return ScreenOrientation.of(metrics.width(), metrics.height())
    }

    private fun savedBounds(): WindowBounds =
        prefs.bounds(orientation, (DEFAULT_WIDTH_DP * density).toInt(), (DEFAULT_HEIGHT_DP * density).toInt())

    private fun clamp(next: WindowBounds): WindowBounds {
        val metrics = windowManager.currentWindowMetrics.bounds
        val minSize = (MIN_SIZE_DP * density).toInt()
        return next.clampTo(metrics.width(), metrics.height(), minSize, minSize)
    }

    private fun apply(next: WindowBounds) {
        bounds = clamp(next)
        val layoutParams = params ?: return
        layoutParams.x = bounds.x
        layoutParams.y = bounds.y
        layoutParams.width = bounds.width
        layoutParams.height = bounds.height
        view?.let { windowManager.updateViewLayout(it, layoutParams) }
    }

    private companion object {
        const val DEFAULT_WIDTH_DP = 280
        const val DEFAULT_HEIGHT_DP = 360
        const val MIN_SIZE_DP = 160
    }
}
