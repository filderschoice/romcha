package io.github.filderschoice.romcha.feature.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayFormatTest {
    @Test
    fun 再生位置を時分秒で表す() {
        assertEquals("0:00", OverlayFormat.position(-1))
        assertEquals("1:05", OverlayFormat.position(65_999))
        assertEquals("1:00:00", OverlayFormat.position(3_600_000))
        assertEquals("10:02:03", OverlayFormat.position(36_123_000))
    }

    @Test
    fun 明るい色帯には黒文字暗い色帯には白文字を選ぶ() {
        assertTrue(OverlayFormat.prefersDarkText(0xFFFFCA28.toInt()))
        assertTrue(OverlayFormat.prefersDarkText(0xFF1DE9B6.toInt()))
        assertFalse(OverlayFormat.prefersDarkText(0xFF1E88E5.toInt()))
        assertFalse(OverlayFormat.prefersDarkText(0xFFD00000.toInt()))
    }

    @Test
    fun 不透明度は下限と上限に収める() {
        assertEquals(OverlayFormat.MIN_OPACITY, OverlayFormat.clampOpacity(0f))
        assertEquals(1f, OverlayFormat.clampOpacity(2f))
        assertEquals(60, OverlayFormat.opacityPercent(0.6f))
    }

    @Test
    fun 文字サイズの倍率は範囲内に収めて10パーセント刻みに丸める() {
        assertEquals(OverlayFormat.MIN_FONT_SCALE, OverlayFormat.clampFontScale(0.1f))
        assertEquals(OverlayFormat.MAX_FONT_SCALE, OverlayFormat.clampFontScale(3f))
        assertEquals(1.2f, OverlayFormat.clampFontScale(1.23f))
        assertEquals(100, OverlayFormat.fontScalePercent(OverlayFormat.DEFAULT_FONT_SCALE))
        assertEquals(130, OverlayFormat.fontScalePercent(1.27f))
    }

    @Test
    fun 同期オフセットは符号付きの秒で表す() {
        assertEquals("+1.5", OverlayFormat.offsetSeconds(1_500))
        assertEquals("-0.5", OverlayFormat.offsetSeconds(-500))
        assertEquals("0.0", OverlayFormat.offsetSeconds(0))
        assertEquals("-10.0", OverlayFormat.offsetSeconds(-10_000))
    }

    @Test
    fun 画面の向きは幅と高さで決める() {
        assertEquals(ScreenOrientation.LANDSCAPE, ScreenOrientation.of(2_000, 1_000))
        assertEquals(ScreenOrientation.PORTRAIT, ScreenOrientation.of(1_000, 2_000))
        assertEquals(ScreenOrientation.PORTRAIT, ScreenOrientation.of(1_000, 1_000))
    }

    @Test
    fun ウィンドウは画面からはみ出さず最小サイズ以上に収める() {
        val clamped = WindowBounds(x = 900, y = -50, width = 100, height = 5_000).clampTo(1_000, 2_000, 200, 200)
        assertEquals(WindowBounds(x = 800, y = 0, width = 200, height = 2_000), clamped)
    }

    @Test
    fun 画面が最小サイズより小さい場合は画面に合わせる() {
        val clamped = WindowBounds(0, 0, 50, 50).clampTo(100, 100, 200, 200)
        assertEquals(WindowBounds(0, 0, 100, 100), clamped)
    }

    @Test
    fun 遡ってスクロールしたら追従をやめ最下部に戻ったら再開する() {
        val policy = AutoScrollPolicy()
        assertTrue(policy.following)

        policy.onUserScrolled(atBottom = false)
        assertFalse(policy.following)
        assertTrue(policy.showJumpToLatest)

        policy.onReachedBottom()
        assertTrue(policy.following)

        policy.onUserScrolled(atBottom = true)
        assertTrue(policy.following)

        policy.onUserScrolled(atBottom = false)
        policy.onRebuilt()
        assertFalse(policy.showJumpToLatest)
    }

    @Test
    fun 同期状態の表示文() {
        assertEquals("同期中", OverlayFormat.indicatorLabel(SyncIndicator.SYNCING))
        assertEquals("未検出", OverlayFormat.indicatorLabel(SyncIndicator.NOT_DETECTED))
    }
}
