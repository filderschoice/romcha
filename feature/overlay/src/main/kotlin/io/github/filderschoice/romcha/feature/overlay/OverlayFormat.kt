package io.github.filderschoice.romcha.feature.overlay

import kotlin.math.roundToInt

/** 表示用の値の計算（Android 非依存の純粋関数。単体テストで検証する）。 */
object OverlayFormat {
    private const val MILLIS_PER_SECOND = 1_000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val MINUTES_PER_HOUR = 60L
    private const val CHANNEL_MAX = 255.0
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val BYTE_MASK = 0xFF
    private const val LUMA_RED = 0.299
    private const val LUMA_GREEN = 0.587
    private const val LUMA_BLUE = 0.114
    private const val LUMA_THRESHOLD = 0.6

    /** 再生位置を `m:ss` または `h:mm:ss` で表す。 */
    fun position(positionMs: Long): String {
        val totalSeconds = positionMs.coerceAtLeast(0) / MILLIS_PER_SECOND
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        val minutes = totalSeconds / SECONDS_PER_MINUTE % MINUTES_PER_HOUR
        val hours = totalSeconds / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR)
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /** 同期状態の表示文（F-OVL-09）。 */
    fun indicatorLabel(indicator: SyncIndicator): String =
        when (indicator) {
            SyncIndicator.SYNCING -> "同期中"
            SyncIndicator.PAUSED -> "一時停止"
            SyncIndicator.NOT_DETECTED -> "未検出"
            SyncIndicator.LIVE -> "ライブ"
        }

    /**
     * 背景色（ARGB）の上に置く文字色として、黒と白のどちらが読みやすいかを返す（スーパーチャットの色帯。F-VIEW-02）。
     *
     * @return 明るい背景なら true（黒文字）、暗い背景なら false（白文字）
     */
    fun prefersDarkText(argb: Int): Boolean {
        val red = (argb shr RED_SHIFT and BYTE_MASK) / CHANNEL_MAX
        val green = (argb shr GREEN_SHIFT and BYTE_MASK) / CHANNEL_MAX
        val blue = (argb and BYTE_MASK) / CHANNEL_MAX
        return LUMA_RED * red + LUMA_GREEN * green + LUMA_BLUE * blue > LUMA_THRESHOLD
    }

    /** 背景の不透明度（F-OVL-03）を 0.2〜1.0 に収める。完全に透明にすると操作できる場所が分からなくなるため下限を置く。 */
    fun clampOpacity(opacity: Float): Float = opacity.coerceIn(MIN_OPACITY, 1f)

    /** 不透明度をスライダー表示用の百分率にする。 */
    fun opacityPercent(opacity: Float): Int = (clampOpacity(opacity) * PERCENT).roundToInt()

    /**
     * チャットの文字サイズの倍率（F-VIEW-01）を 0.8〜1.5 に収め、0.1 刻みに丸める。
     *
     * 1.0（100%）が従来の大きさ（中）。スライダーの途中の値を保存しても刻みに揃うよう丸める。
     */
    fun clampFontScale(scale: Float): Float =
        (
            (scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE) * PERCENT / FONT_SCALE_STEP_PERCENT).roundToInt() *
                FONT_SCALE_STEP_PERCENT / PERCENT.toFloat()
        )

    /** 文字サイズの倍率をスライダー表示用の百分率にする。 */
    fun fontScalePercent(scale: Float): Int = (clampFontScale(scale) * PERCENT).roundToInt()

    const val MIN_OPACITY = 0.2f
    const val MIN_FONT_SCALE = 0.8f
    const val MAX_FONT_SCALE = 1.5f
    const val DEFAULT_FONT_SCALE = 1f

    /** スライダーの刻みの数（両端を除く）。0.8〜1.5 を 0.1 刻みにする */
    const val FONT_SCALE_STEPS = 6
    private const val FONT_SCALE_STEP_PERCENT = 10
    private const val PERCENT = 100
}

/** 画面内でのウィンドウの位置と大きさ（px）。 */
data class WindowBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    /**
     * 画面（[screenWidth] × [screenHeight]）からはみ出さないように位置と大きさを収める（F-OVL-02）。
     *
     * 大きさは [minWidth] / [minHeight] 以上、画面以下にし、位置は右端・下端が画面内に残るようにする。
     */
    fun clampTo(
        screenWidth: Int,
        screenHeight: Int,
        minWidth: Int,
        minHeight: Int,
    ): WindowBounds {
        val w = width.coerceIn(minOf(minWidth, screenWidth), screenWidth)
        val h = height.coerceIn(minOf(minHeight, screenHeight), screenHeight)
        return WindowBounds(
            x = x.coerceIn(0, screenWidth - w),
            y = y.coerceIn(0, screenHeight - h),
            width = w,
            height = h,
        )
    }
}

/**
 * 新着時の自動スクロールの判断（F-OVL-07）。
 *
 * 利用者が遡ってスクロールしたら自動スクロールを止めて「最新へ」ボタンを出し、最下部へ戻ったら再開する。
 */
class AutoScrollPolicy {
    var following: Boolean = true
        private set

    val showJumpToLatest: Boolean get() = !following

    /** 利用者が一覧をドラッグした。最下部に居なければ追従をやめる。 */
    fun onUserScrolled(atBottom: Boolean) {
        following = atBottom
    }

    /** 最下部に到達した（利用者の操作・「最新へ」ボタンのどちらでも）。 */
    fun onReachedBottom() {
        following = true
    }

    /** 表示の作り直し（シーク・動画の切り替え）では追従を再開する。 */
    fun onRebuilt() {
        following = true
    }
}
