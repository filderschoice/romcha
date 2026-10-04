package io.github.filderschoice.romcha.crash

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 真偽値 1 つを永続化する入れ物。実機では SharedPreferences、テストでは偽物を差し込む。 */
interface BooleanStore {
    fun read(default: Boolean): Boolean

    fun write(value: Boolean)
}

/**
 * クラッシュ情報の送信の設定（BL-089）。既定はオンで、利用者が設定画面でオフにできる。
 *
 * 値を変えるたびに [onChanged] を呼び、収集の有効・無効を実際の送信側へ反映する。
 */
class CrashReportingSettings(
    private val store: BooleanStore,
    private val onChanged: (Boolean) -> Unit,
) {
    private val _enabled = MutableStateFlow(store.read(DEFAULT_ENABLED))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /** 起動時に、保存済みの設定を送信側へ反映する。 */
    fun apply() = onChanged(_enabled.value)

    /** 設定を変える。永続化してから送信側へ反映する。 */
    fun setEnabled(value: Boolean) {
        store.write(value)
        _enabled.value = value
        onChanged(value)
    }

    companion object {
        const val DEFAULT_ENABLED = true
    }
}
