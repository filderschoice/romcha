package io.github.filderschoice.romcha.feature.overlay.ui

import androidx.compose.ui.unit.dp
import io.github.filderschoice.romcha.feature.overlay.DisplaySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HeaderButtonSizeTest {
    @Test
    fun 既定は従来の36dpで大きくする設定では推奨の48dpにする() {
        assertFalse(DisplaySettings().largeHeaderButtons)
        assertEquals(36.dp, headerButtonSize(DisplaySettings().largeHeaderButtons))
        assertEquals(48.dp, headerButtonSize(large = true))
    }
}
