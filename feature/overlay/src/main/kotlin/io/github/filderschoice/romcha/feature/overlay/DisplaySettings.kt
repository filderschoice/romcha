package io.github.filderschoice.romcha.feature.overlay

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * チャットの表示設定（F-VIEW-01）。アプリ画面の「表示設定」で変え、フローティングウィンドウの表示に反映する。
 *
 * 既定値は従来の表示（投稿者名のみ。アイコン・時刻なし）と同じにする。
 *
 * @property showAuthorName 投稿者名を出す
 * @property showAuthorIcon 投稿者のアイコンを出す
 * @property showTime 時刻を出す（リプレイは動画内の位置、ライブ・プレミアは投稿時刻）
 */
data class DisplaySettings(
    val showAuthorName: Boolean = true,
    val showAuthorIcon: Boolean = false,
    val showTime: Boolean = false,
)

/**
 * 表示設定の保存と共有（端末内のみ。N-06）。
 *
 * アプリ画面とフローティングウィンドウ（同一プロセス）が同じ [state] を購読する。使う前に [init] を呼ぶ。
 */
object DisplaySettingsStore {
    private const val NAME = "display"
    private const val KEY_AUTHOR_NAME = "showAuthorName"
    private const val KEY_AUTHOR_ICON = "showAuthorIcon"
    private const val KEY_TIME = "showTime"

    private var prefs: SharedPreferences? = null
    private val mutableState = MutableStateFlow(DisplaySettings())
    val state: StateFlow<DisplaySettings> = mutableState.asStateFlow()

    /** 保存値を読み込む（2 回目以降は何もしない）。 */
    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val loaded = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        prefs = loaded
        val defaults = DisplaySettings()
        mutableState.value =
            DisplaySettings(
                showAuthorName = loaded.getBoolean(KEY_AUTHOR_NAME, defaults.showAuthorName),
                showAuthorIcon = loaded.getBoolean(KEY_AUTHOR_ICON, defaults.showAuthorIcon),
                showTime = loaded.getBoolean(KEY_TIME, defaults.showTime),
            )
    }

    /** 設定を変えて保存する。 */
    fun update(transform: (DisplaySettings) -> DisplaySettings) {
        val next = transform(mutableState.value)
        mutableState.value = next
        prefs?.edit {
            putBoolean(KEY_AUTHOR_NAME, next.showAuthorName)
            putBoolean(KEY_AUTHOR_ICON, next.showAuthorIcon)
            putBoolean(KEY_TIME, next.showTime)
        }
    }
}
