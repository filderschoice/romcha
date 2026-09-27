package io.github.filderschoice.romcha.feature.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StashRuleTest {
    private val screen = 1_080 to 2_400
    private val tab = 60 to 216

    @Test
    fun 画面端でさらに閾値以上押し込んだ側へ退避する() {
        assertEquals(StashSide.LEFT, StashRule.sideFor(overshootX = -150, thresholdPx = 144))
        assertEquals(StashSide.RIGHT, StashRule.sideFor(overshootX = 144, thresholdPx = 144))
        assertNull(StashRule.sideFor(overshootX = 100, thresholdPx = 144))
        assertNull(StashRule.sideFor(overshootX = 0, thresholdPx = 144))
    }

    @Test
    fun つまみを内側へスワイプしたら復帰する() {
        assertTrue(StashRule.shouldRestore(StashSide.LEFT, dragX = 80f, thresholdPx = 72f))
        assertFalse(StashRule.shouldRestore(StashSide.LEFT, dragX = -80f, thresholdPx = 72f))
        assertTrue(StashRule.shouldRestore(StashSide.RIGHT, dragX = -80f, thresholdPx = 72f))
        assertFalse(StashRule.shouldRestore(StashSide.RIGHT, dragX = 40f, thresholdPx = 72f))
    }

    @Test
    fun つまみは退避した側の画面端に付き縦位置を画面内に収める() {
        assertEquals(WindowBounds(0, 300, 60, 216), StashRule.tabBounds(StashSide.LEFT, 300, screen, tab))
        assertEquals(
            WindowBounds(1_020, 2_184, 60, 216),
            StashRule.tabBounds(StashSide.RIGHT, 2_300, screen, tab),
        )
        assertEquals(WindowBounds(0, 0, 60, 216), StashRule.tabBounds(StashSide.LEFT, -50, screen, tab))
    }
}
