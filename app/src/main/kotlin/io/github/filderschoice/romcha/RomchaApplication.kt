package io.github.filderschoice.romcha

import android.app.Application
import io.github.filderschoice.romcha.crash.CrashReporting

/** クラッシュ情報の送信設定を、どの入口（画面・サービス）より先に反映するための Application（BL-089）。 */
class RomchaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporting.init(this)
    }
}
