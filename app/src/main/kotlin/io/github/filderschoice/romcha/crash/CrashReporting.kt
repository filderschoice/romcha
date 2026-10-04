package io.github.filderschoice.romcha.crash

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.github.filderschoice.romcha.BuildConfig

/**
 * Firebase Crashlytics によるクラッシュ情報の送信（BL-089）。
 *
 * 自動収集は Manifest で無効にしてあり、起動時に設定値で有効・無効を決める（設定がオフなら何も送らない）。
 * google-services.json が無いビルド（[BuildConfig.FIREBASE_ENABLED] が false）では何もせず、設定項目も出さない。
 */
object CrashReporting {
    private const val PREFS = "crash_reporting"
    private const val KEY_ENABLED = "enabled"

    val available: Boolean = BuildConfig.FIREBASE_ENABLED

    private var settings: CrashReportingSettings? = null

    /** 設定。[init] の前、または Firebase が無効なビルドでは null。 */
    fun settings(): CrashReportingSettings? = settings

    fun init(context: Context) {
        if (!available || settings != null) return
        val app = context.applicationContext
        FirebaseApp.initializeApp(app)
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val store =
            object : BooleanStore {
                override fun read(default: Boolean) = prefs.getBoolean(KEY_ENABLED, default)

                override fun write(value: Boolean) = prefs.edit { putBoolean(KEY_ENABLED, value) }
            }
        settings =
            CrashReportingSettings(store) { enabled ->
                FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled
            }.also { it.apply() }
    }
}
