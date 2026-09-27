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
 * フローティングウィンドウ本体（`TYPE_APPLICATION_OVERLAY`）の追加・削除と、`LayoutParams` の反映（F-OVL-01/02/04/05/06、BL-049）。
 *
 * 位置と大きさの計算・保存は [WindowPlacement] に任せる。
 */
internal class OverlayWindow<T>(
    private val owner: T,
    prefs: OverlayPrefs,
) where T : LifecycleService, T : SavedStateRegistryOwner {
    private val windowManager = owner.getSystemService(WindowManager::class.java)
    private val placement =
        WindowPlacement(
            prefs = prefs,
            screen = { windowManager.currentWindowMetrics.bounds.let { it.width() to it.height() } },
            density = { owner.resources.displayMetrics.density },
        )
    private var view: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null

    /**
     * タッチ透過モード（F-OVL-05）。`FLAG_NOT_TOUCHABLE` で下のアプリへタッチを通す。
     *
     * 他アプリのオーバーレイ越しのタッチは、ウィンドウの不透明度が [TOUCH_THROUGH_MAX_ALPHA] を超えると OS に遮断されるため、
     * 透過モード中はウィンドウ全体の不透明度をその値に下げる（PLAN 4.6）。
     */
    var touchThrough: Boolean = false
        set(value) {
            field = value
            updateLayout()
        }

    /** 表示状態（通常・最小化 F-OVL-04・画面端への退避 BL-049） */
    var mode: WindowMode
        get() = placement.mode
        set(value) {
            placement.mode = value
            updateLayout()
        }

    /** 文字入力中だけフォーカスを取れるようにする（通常は `FLAG_NOT_FOCUSABLE` で公式アプリの操作を妨げない） */
    var focusable: Boolean = false
        set(value) {
            field = value
            updateLayout()
        }

    fun show(content: @Composable () -> Unit) {
        if (view != null) return
        placement.load()
        val layoutParams =
            WindowManager
                .LayoutParams(
                    0,
                    0,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    // 画面外へのはみ出し（BL-050）を許すため FLAG_LAYOUT_NO_LIMITS を付ける。位置は WindowPlacement が制御する
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT,
                ).apply { gravity = Gravity.TOP or Gravity.START }
        val composeView =
            ComposeView(owner).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent(content)
            }
        params = layoutParams
        fill(layoutParams)
        windowManager.addView(composeView, layoutParams)
        view = composeView
    }

    fun hide() {
        view?.let { windowManager.removeView(it) }
        view = null
        params = null
    }

    fun moveBy(
        dx: Float,
        dy: Float,
    ) {
        placement.moveBy(dx, dy)
        updateLayout()
    }

    fun resizeBy(
        dx: Float,
        dy: Float,
    ) {
        placement.resizeBy(dx, dy)
        updateLayout()
    }

    /** 移動・サイズ変更の操作の終わり。位置を保存し、画面の外へ十分はみ出して離した場合は退避する向きを返す（はみ出しが少なければ画面内へ戻す） */
    fun endGesture(): StashSide? = placement.endGesture().also { updateLayout() }

    /** 画面の向きが変わったら、その向きで記憶していた位置と大きさへ切り替える（F-OVL-06） */
    fun onConfigurationChanged() {
        if (view != null && placement.onConfigurationChanged()) updateLayout()
    }

    private fun updateLayout() {
        val layoutParams = params ?: return
        fill(layoutParams)
        view?.let { windowManager.updateViewLayout(it, layoutParams) }
    }

    private fun fill(layoutParams: WindowManager.LayoutParams) {
        val bounds = placement.current
        layoutParams.x = bounds.x
        layoutParams.y = bounds.y
        layoutParams.width = bounds.width
        layoutParams.height = bounds.height
        layoutParams.flags = layoutParams.flags.with(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, touchThrough)
        layoutParams.flags = layoutParams.flags.with(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, !focusable)
        layoutParams.alpha = if (touchThrough) TOUCH_THROUGH_MAX_ALPHA else 1f
    }

    private fun Int.with(
        flag: Int,
        on: Boolean,
    ): Int = if (on) this or flag else this and flag.inv()

    companion object {
        /** タッチを下のアプリへ通せるウィンドウの不透明度の上限（Android 12 以降の制約） */
        const val TOUCH_THROUGH_MAX_ALPHA = 0.8f
    }
}
