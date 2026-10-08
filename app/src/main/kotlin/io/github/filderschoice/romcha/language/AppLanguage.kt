package io.github.filderschoice.romcha.language

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.core.content.edit

/** アプリの表示言語（BL-104）。日本語と英語のみ。既定は日本語。 */
enum class AppLanguage(
    val tag: String,
) {
    JAPANESE("ja"),
    ENGLISH("en"),
    ;

    companion object {
        /** 言語タグ（`en-US` のような地域付きを含む）から対応する言語を選ぶ。対応外・空なら既定の日本語。 */
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { tag != null && (tag == it.tag || tag.startsWith(it.tag + "-")) } ?: JAPANESE
    }
}

/**
 * アプリの表示言語の読み書き。保存は OS のアプリ別言語（[LocaleManager]）に任せ、独自の保存値は持たない
 * （設定のバックアップ・初期化の対象外）。変更すると OS が画面を作り直す。
 */
object AppLanguageSettings {
    private const val PREFS = "language"
    private const val KEY_INITIALIZED = "initialized"

    fun current(context: Context): AppLanguage {
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        return AppLanguage.fromTag(if (locales.isEmpty) null else locales[0].toLanguageTag())
    }

    fun set(
        context: Context,
        language: AppLanguage,
    ) {
        val manager = context.getSystemService(LocaleManager::class.java)
        manager.applicationLocales = LocaleList.forLanguageTags(language.tag)
    }

    /**
     * 初回の起動時だけ、アプリ別言語が未設定なら日本語にする。これが無いと、英語の端末では最初から英語で表示される
     * （既定の言語は日本語とする仕様。2026-10-09 ユーザー指示）。利用者が OS の設定で選んだ言語は上書きしない。
     */
    fun initializeDefault(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_INITIALIZED, false)) return
        prefs.edit { putBoolean(KEY_INITIALIZED, true) }
        val manager = context.getSystemService(LocaleManager::class.java)
        if (manager.applicationLocales.isEmpty) set(context, AppLanguage.JAPANESE)
    }
}
