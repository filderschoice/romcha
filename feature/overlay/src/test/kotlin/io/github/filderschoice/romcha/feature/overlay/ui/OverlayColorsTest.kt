package io.github.filderschoice.romcha.feature.overlay.ui

import io.github.filderschoice.romcha.feature.overlay.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayColorsTest {
    @Test
    fun システム追従とダークは従来の暗色でライトだけ明るい配色にする() {
        assertEquals(OverlayColors.Dark, OverlayColors.of(ThemeMode.SYSTEM))
        assertEquals(OverlayColors.Dark, OverlayColors.of(ThemeMode.DARK))
        assertEquals(OverlayColors.Light, OverlayColors.of(ThemeMode.LIGHT))
    }
}
