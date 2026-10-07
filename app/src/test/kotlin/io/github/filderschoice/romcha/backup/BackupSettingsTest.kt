package io.github.filderschoice.romcha.backup

import io.github.filderschoice.romcha.crash.BooleanStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSettingsTest {
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
        assertTrue(BackupSettings(FakeStore()).enabled.value)
    }

    @Test
    fun 切り替えると保存され状態にも反映される() {
        val store = FakeStore()
        val settings = BackupSettings(store)
        settings.setEnabled(false)
        assertFalse(settings.enabled.value)
        assertEquals(false, store.value)
        assertFalse(BackupSettings(store).enabled.value)
    }
}
