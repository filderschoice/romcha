package io.github.filderschoice.romcha.feature.overlay

/**
 * フローティングウィンドウの位置と大きさ（F-OVL-02/04/06、BL-049）。
 *
 * 画面の向きごとに [OverlayPrefs] へ保存し、向きが変わったらその向きの保存値へ切り替える。
 * 通常表示の位置と大きさは常に画面内へ収める（[WindowBounds.clampTo]）。
 *
 * @param screen 現在の画面の幅と高さ（px）
 * @param density 画面密度（dp から px への換算）
 */
internal class WindowPlacement(
    private val prefs: OverlayPrefs,
    private val screen: () -> Pair<Int, Int>,
    private val density: () -> Float,
) {
    /** 通常表示の位置と大きさ（最小化・退避中も保持し、復帰時に使う） */
    var bounds = WindowBounds(0, 0, 0, 0)
        private set
    private var orientation = ScreenOrientation.PORTRAIT

    /** 移動の操作中の、画面内へ収める前の横位置（画面端を越えた押し込み量を求める。操作の終わりで null に戻す） */
    private var rawX: Int? = null

    /**
     * 表示状態。
     *
     * - 最小化（F-OVL-04）: バブルは通常表示の左上の位置に出し、バブルの移動は通常表示の位置にも反映する。
     * - 退避（BL-049）: 通常表示を退避した側の画面端へ寄せ、つまみはその縦位置に出す。
     *
     * 通常表示へ戻す時は、通常表示の大きさで画面内へ収め直す。
     */
    var mode: WindowMode = WindowMode.Normal
        set(value) {
            field = value
            when (value) {
                WindowMode.Normal -> bounds = clamp(bounds)
                is WindowMode.Stashed -> bounds = clamp(bounds.copy(x = edgeX(value.side)))
                WindowMode.Minimized -> Unit
            }
        }

    /** 現在ウィンドウに適用する位置と大きさ。 */
    val current: WindowBounds
        get() =
            when (val mode = mode) {
                WindowMode.Normal -> bounds
                WindowMode.Minimized -> bounds.copy(width = dp(BUBBLE_DP), height = dp(BUBBLE_DP))
                is WindowMode.Stashed ->
                    StashRule.tabBounds(mode.side, bounds.y, screen(), dp(TAB_WIDTH_DP) to dp(TAB_HEIGHT_DP))
            }

    /** 現在の向きの保存値を読み込む。 */
    fun load() {
        orientation = currentOrientation()
        bounds = if (mode == WindowMode.Normal) clamp(saved()) else saved()
    }

    fun moveBy(
        dx: Float,
        dy: Float,
    ) {
        val (width, height) = screen()
        when (mode) {
            WindowMode.Normal -> {
                val x = (rawX ?: bounds.x) + dx.toInt()
                rawX = x
                bounds = clamp(bounds.copy(x = x, y = bounds.y + dy.toInt()))
            }
            // バブル・つまみは自身の大きさで画面内へ収め、通常表示の位置だけを動かす（大きさは保持する）
            WindowMode.Minimized -> {
                val bubble = current.copy(x = bounds.x + dx.toInt(), y = bounds.y + dy.toInt())
                val moved = bubble.clampTo(width, height, bubble.width, bubble.height)
                bounds = bounds.copy(x = moved.x, y = moved.y)
            }
            is WindowMode.Stashed -> {
                val tab = current
                bounds = bounds.copy(y = (bounds.y + dy.toInt()).coerceIn(0, (height - tab.height).coerceAtLeast(0)))
            }
        }
    }

    fun resizeBy(
        dx: Float,
        dy: Float,
    ) {
        bounds = clamp(bounds.copy(width = bounds.width + dx.toInt(), height = bounds.height + dy.toInt()))
    }

    /** 操作の終わりに位置を保存し、画面端を越えて押し込んで離した場合は退避する向きを返す（BL-049）。 */
    fun endGesture(): StashSide? {
        prefs.saveBounds(orientation, bounds)
        val overshoot = rawX?.let { it - bounds.x } ?: 0
        rawX = null
        return if (mode == WindowMode.Normal) StashRule.sideFor(overshoot, dp(STASH_THRESHOLD_DP)) else null
    }

    /** 画面の向きが変わっていれば、その向きの保存値へ切り替えて true を返す。 */
    fun onConfigurationChanged(): Boolean {
        if (currentOrientation() == orientation) return false
        load()
        (mode as? WindowMode.Stashed)?.let { bounds = clamp(bounds.copy(x = edgeX(it.side))) }
        return true
    }

    private fun currentOrientation(): ScreenOrientation {
        val (width, height) = screen()
        return ScreenOrientation.of(width, height)
    }

    private fun edgeX(side: StashSide): Int = if (side == StashSide.LEFT) 0 else screen().first - bounds.width

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
        const val TAB_WIDTH_DP = 20
        const val TAB_HEIGHT_DP = 72

        /** 画面端でさらにこれだけ押し込んで離したら退避する */
        const val STASH_THRESHOLD_DP = 48
    }
}
