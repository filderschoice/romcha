package io.github.filderschoice.romcha.feature.overlay

/** 画面端への退避の向き（BL-049）。 */
enum class StashSide {
    LEFT,
    RIGHT,
}

/** フローティングウィンドウの表示状態。いずれも保存しない（サービスの起動ごとに通常表示から始める）。 */
sealed interface WindowMode {
    /** 通常表示 */
    data object Normal : WindowMode

    /** 丸いバブルへの最小化（F-OVL-04） */
    data object Minimized : WindowMode

    /** 画面の左右の端への退避。細いつまみだけを残す（BL-049。YouTube 公式アプリの PiP と同じ操作） */
    data class Stashed(
        val side: StashSide,
    ) : WindowMode
}

/** 画面端への退避と復帰の判定（BL-049。Android 非依存）。 */
object StashRule {
    private const val THRESHOLD_DIVISOR = 3

    /**
     * ドラッグ中の横位置。画面の左右の外へのはみ出しを許し（指に付いて動かす）、画面内に [keepVisible] だけは残す
     * （指がヘッダーから外れて操作を失わないため）。
     */
    fun dragX(
        x: Int,
        width: Int,
        screenWidth: Int,
        keepVisible: Int,
    ): Int {
        val keep = keepVisible.coerceAtMost(width)
        return x.coerceIn(-(width - keep), screenWidth - keep)
    }

    /** 画面の左右の外へはみ出している量（px。左が負、右が正、はみ出していなければ 0）。 */
    fun overshoot(
        x: Int,
        width: Int,
        screenWidth: Int,
    ): Int =
        when {
            x < 0 -> x
            x + width > screenWidth -> x + width - screenWidth
            else -> 0
        }

    /** 退避とみなすはみ出し量。ウィンドウ幅の 3 分の 1 と [minPx] の大きい方。 */
    fun threshold(
        width: Int,
        minPx: Int,
    ): Int = maxOf(minPx, width / THRESHOLD_DIVISOR)

    /**
     * ヘッダーのドラッグを離した時に退避する向き。退避しなければ null。
     *
     * @param overshootX 画面の左右の外へはみ出している量（px。左が負、右が正）
     * @param thresholdPx 退避とみなすはみ出し量
     */
    fun sideFor(
        overshootX: Int,
        thresholdPx: Int,
    ): StashSide? =
        when {
            overshootX <= -thresholdPx -> StashSide.LEFT
            overshootX >= thresholdPx -> StashSide.RIGHT
            else -> null
        }

    /** つまみを画面の内側へ [thresholdPx] 以上スワイプしたら復帰する。 */
    fun shouldRestore(
        side: StashSide,
        dragX: Float,
        thresholdPx: Float,
    ): Boolean =
        when (side) {
            StashSide.LEFT -> dragX >= thresholdPx
            StashSide.RIGHT -> dragX <= -thresholdPx
        }

    /**
     * 退避中のつまみの位置と大きさ。退避した側の画面端に付け、縦位置はウィンドウの位置のまま画面内へ収める。
     *
     * @param screen 画面の幅と高さ（px）
     * @param tab つまみの幅と高さ（px）
     */
    fun tabBounds(
        side: StashSide,
        windowY: Int,
        screen: Pair<Int, Int>,
        tab: Pair<Int, Int>,
    ): WindowBounds {
        val (screenWidth, screenHeight) = screen
        val (tabWidth, tabHeight) = tab
        return WindowBounds(
            x = if (side == StashSide.LEFT) 0 else (screenWidth - tabWidth).coerceAtLeast(0),
            y = windowY.coerceIn(0, (screenHeight - tabHeight).coerceAtLeast(0)),
            width = tabWidth,
            height = tabHeight,
        )
    }
}
