package io.github.filderschoice.romcha.feature.overlay

/**
 * フローティングウィンドウの位置と大きさ（F-OVL-02/06）。
 *
 * 画面の向きごとに [OverlayPrefs] へ保存し、向きが変わったらその向きの保存値へ切り替える。
 * 位置と大きさは常に画面内へ収める（[WindowBounds.clampTo]）。
 *
 * @param screen 現在の画面の幅と高さ（px）
 * @param density 画面密度（dp から px への換算）
 */
internal class WindowPlacement(
    private val prefs: OverlayPrefs,
    private val screen: () -> Pair<Int, Int>,
    private val density: () -> Float,
) {
    /** 通常表示の位置と大きさ（最小化中も保持し、復帰時に使う） */
    var bounds = WindowBounds(0, 0, 0, 0)
        private set
    private var orientation = ScreenOrientation.PORTRAIT

    /**
     * 最小化（バブル）中か（F-OVL-04）。バブルは通常表示の左上の位置に出し、バブルの移動は通常表示の位置にも反映する。
     * 復帰時は通常表示の大きさで画面内へ収め直す。
     */
    var minimized: Boolean = false
        set(value) {
            field = value
            if (!value) bounds = clamp(bounds)
        }

    /** 現在ウィンドウに適用する位置と大きさ（最小化中はバブルの大きさ）。 */
    val current: WindowBounds
        get() = if (minimized) bounds.copy(width = dp(BUBBLE_DP), height = dp(BUBBLE_DP)) else bounds

    /** 現在の向きの保存値を読み込む。 */
    fun load() {
        orientation = currentOrientation()
        bounds = if (minimized) saved() else clamp(saved())
    }

    fun moveBy(
        dx: Float,
        dy: Float,
    ) {
        if (minimized) {
            // バブルは自身の大きさで画面内へ収め、通常表示の位置だけを動かす（大きさは保持する）
            val (width, height) = screen()
            val bubble = current.copy(x = bounds.x + dx.toInt(), y = bounds.y + dy.toInt())
            val moved = bubble.clampTo(width, height, bubble.width, bubble.height)
            bounds = bounds.copy(x = moved.x, y = moved.y)
        } else {
            bounds = clamp(bounds.copy(x = bounds.x + dx.toInt(), y = bounds.y + dy.toInt()))
        }
    }

    fun resizeBy(
        dx: Float,
        dy: Float,
    ) {
        bounds = clamp(bounds.copy(width = bounds.width + dx.toInt(), height = bounds.height + dy.toInt()))
    }

    fun save() = prefs.saveBounds(orientation, bounds)

    /** 画面の向きが変わっていれば、その向きの保存値へ切り替えて true を返す。 */
    fun onConfigurationChanged(): Boolean {
        if (currentOrientation() == orientation) return false
        load()
        return true
    }

    private fun currentOrientation(): ScreenOrientation {
        val (width, height) = screen()
        return ScreenOrientation.of(width, height)
    }

    private fun saved(): WindowBounds = prefs.bounds(orientation, dp(DEFAULT_WIDTH_DP), dp(DEFAULT_HEIGHT_DP))

    private fun clamp(next: WindowBounds): WindowBounds {
        val (width, height) = screen()
        val minSize = dp(MIN_SIZE_DP)
        return next.clampTo(width, height, minSize, minSize)
    }

    private fun dp(value: Int): Int = (value * density()).toInt()

    private companion object {
        const val DEFAULT_WIDTH_DP = 280
        const val DEFAULT_HEIGHT_DP = 360
        const val MIN_SIZE_DP = 160
        const val BUBBLE_DP = 48
    }
}
