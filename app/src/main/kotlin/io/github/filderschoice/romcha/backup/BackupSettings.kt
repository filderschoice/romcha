package io.github.filderschoice.romcha.backup

import android.content.Context
import androidx.core.content.edit
import io.github.filderschoice.romcha.crash.BooleanStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 設定のバックアップ（再インストール時の復元）を使うかどうか（BL-097）。既定はオンで、利用者が HOME 画面でオフにできる。
 *
 * 自動バックアップの対象はマニフェストの規則で静的に決まるため、オフの間は [RomchaBackupAgent] がバックアップの実行自体を止める。
 * この値自身は、復元で上書きされないようバックアップの対象にしない（`data_extraction_rules.xml` に含めない）。
 */
class BackupSettings(
    private val store: BooleanStore,
) {
    private val _enabled = MutableStateFlow(store.read(DEFAULT_ENABLED))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun setEnabled(value: Boolean) {
        store.write(value)
        _enabled.value = value
    }

    companion object {
        const val DEFAULT_ENABLED = true
        private const val PREFS = "backup_control"
        private const val KEY_ENABLED = "enabled"

        private var shared: BackupSettings? = null

        /** プロセス内で 1 つを共有する（画面とバックアップ処理で同じ値を見る）。 */
        @Synchronized
        fun shared(context: Context): BackupSettings =
            shared ?: BackupSettings(sharedPrefsStore(context.applicationContext)).also { shared = it }

        /** バックアップ処理用。画面側で変更済みでも、保存済みの値を読み直す。 */
        fun isEnabledNow(context: Context): Boolean = sharedPrefsStore(context.applicationContext).read(DEFAULT_ENABLED)

        private fun sharedPrefsStore(context: Context): BooleanStore {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return object : BooleanStore {
                override fun read(default: Boolean) = prefs.getBoolean(KEY_ENABLED, default)

                override fun write(value: Boolean) = prefs.edit { putBoolean(KEY_ENABLED, value) }
            }
        }
    }
}
