package io.github.filderschoice.romcha

import io.github.filderschoice.romcha.language.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun 言語タグから対応する言語を選ぶ() {
        assertEquals(AppLanguage.JAPANESE, AppLanguage.fromTag("ja"))
        assertEquals(AppLanguage.JAPANESE, AppLanguage.fromTag("ja-JP"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"))
    }

    @Test
    fun 対応外や未設定は既定の日本語にする() {
        assertEquals(AppLanguage.JAPANESE, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.JAPANESE, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.JAPANESE, AppLanguage.fromTag("fr"))
        assertEquals(AppLanguage.JAPANESE, AppLanguage.fromTag("english"))
    }
}
