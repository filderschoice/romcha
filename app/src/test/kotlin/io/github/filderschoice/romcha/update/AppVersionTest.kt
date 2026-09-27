package io.github.filderschoice.romcha.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {
    private fun v(text: String) = requireNotNull(AppVersion.parse(text))

    @Test
    fun タグ形式とビルドメタデータ付きを読む() {
        assertEquals(AppVersion(1, 2, 3), AppVersion.parse("v1.2.3"))
        assertEquals(AppVersion(1, 0, 0, "rc.1"), AppVersion.parse("V1.0.0-rc.1+build.5"))
        assertEquals(AppVersion(0, 1, 0), AppVersion.parse(" 0.1.0 "))
    }

    @Test
    fun 形式が違えばnull() {
        assertNull(AppVersion.parse("1.2"))
        assertNull(AppVersion.parse("latest"))
        assertNull(AppVersion.parse("1.2.3.4"))
        assertNull(AppVersion.parse(""))
    }

    @Test
    fun 番号を数値で比べる() {
        assertTrue(v("1.10.0") > v("1.9.9"))
        assertTrue(v("2.0.0") > v("1.99.99"))
        assertEquals(0, v("v1.0.0").compareTo(v("1.0.0+abc")))
    }

    @Test
    fun プレリリースは同じ番号の正式版より古い() {
        assertTrue(v("1.0.0-rc.1") < v("1.0.0"))
        assertTrue(v("1.0.0-rc.2") > v("1.0.0-rc.1"))
        assertTrue(v("1.0.1-beta") > v("1.0.0"))
    }

    @Test
    fun 文字列へ戻す() {
        assertEquals("1.0.0-rc.1", AppVersion(1, 0, 0, "rc.1").toString())
        assertEquals("0.1.0", AppVersion(0, 1, 0).toString())
    }
}
