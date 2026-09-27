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
    var bounds = WindowBounds(0, 0, 0, 0)
        private set
    private var orientation = ScreenOrientation.PORTRAIT

    /** 現在の向きの保存値を読み込む。 */
    fun load() {
        orientation = currentOrientation()
        bounds = clamp(saved())
    }

    fun moveBy(
        dx: Float,
        dy: Float,
    ) {
        bounds = clamp(bounds.copy(x = bounds.x + dx.toInt(), y = bounds.y + dy.toInt()))
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
    }
}
