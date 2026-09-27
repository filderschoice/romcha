package io.github.filderschoice.romcha.feature.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlaySettingsTest {
    @Test
    fun 設定値を範囲と刻みに収める() {
        val normalized =
            OverlaySettings(opacity = 0f, fontScale = 1.23f, liveDelaySeconds = 99, syncOffsetMs = 1_400).normalized()
        assertEquals(OverlayFormat.MIN_OPACITY, normalized.opacity)
        assertEquals(1.2f, normalized.fontScale)
        assertEquals(30, normalized.liveDelaySeconds)
        assertEquals(1_500L, normalized.syncOffsetMs)
    }

    @Test
    fun 既定値は従来の表示と同じ() {
        assertEquals(OverlaySettings(), OverlaySettings().normalized())
        assertEquals(0.6f, OverlaySettings().opacity)
        assertEquals(1f, OverlaySettings().fontScale)
    }
}
