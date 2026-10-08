package io.github.filderschoice.romcha.feature.overlay.ui

import io.github.filderschoice.romcha.feature.overlay.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayColorsTest {
    @Test
    fun システム追従とダークは従来の暗色でライトだけ明るい配色にする() {
        assertEquals(OverlayColors.Dark, OverlayColors.of(ThemeMode.SYSTEM))
        assertEquals(OverlayColors.Dark, OverlayColors.of(ThemeMode.DARK))
        assertEquals(OverlayColors.Light, OverlayColors.of(ThemeMode.LIGHT))
    }

    @Test
    fun ライトだけ文字の縁取りで暗い背景の上でも読めるようにしダークは従来のままにする() {
        assertNotNull(OverlayColors.Light.textShadow)
        assertNull(OverlayColors.Dark.textShadow)
    }
}
