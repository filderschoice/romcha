package io.github.filderschoice.romcha.crash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportingSettingsTest {
    private class FakeStore(
        var value: Boolean? = null,
    ) : BooleanStore {
        override fun read(default: Boolean) = value ?: default

        override fun write(value: Boolean) {
            this.value = value
        }
    }

    @Test
    fun 既定はオン() {
        val settings = CrashReportingSettings(FakeStore()) {}
        assertTrue(settings.enabled.value)
    }

    @Test
    fun 保存済みのオフを読み込み起動時に反映する() {
        val applied = mutableListOf<Boolean>()
        val settings = CrashReportingSettings(FakeStore(false)) { applied += it }
        settings.apply()
        assertFalse(settings.enabled.value)
        assertEquals(listOf(false), applied)
    }

    @Test
    fun 変更は永続化され送信側へ反映される() {
        val store = FakeStore()
        val applied = mutableListOf<Boolean>()
        val settings = CrashReportingSettings(store) { applied += it }
        settings.setEnabled(false)
        settings.setEnabled(true)
        assertEquals(true, store.value)
        assertEquals(listOf(false, true), applied)
        assertTrue(settings.enabled.value)
    }
}
