package io.github.filderschoice.romcha.feature.overlay

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** テーマ（F-VIEW-05）。 */
enum class ThemeMode {
    /** アプリ画面はシステムの設定に合わせ、フローティングウィンドウは従来どおり暗色 */
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * チャットの表示設定（F-VIEW-01）。アプリ画面の「表示設定」で変え、フローティングウィンドウの表示に反映する。
 *
 * 既定値は従来の表示（投稿者名のみ。アイコン・時刻なし）と同じにする。
 *
 * @property showAuthorName 投稿者名を出す
 * @property showAuthorIcon 投稿者のアイコンを出す
 * @property showTime 時刻を出す（リプレイは動画内の位置、ライブ・プレミアは投稿時刻）
 * @property onlyPaid スーパーチャット・スーパーステッカーだけを出す（F-VIEW-03。以下 3 つはいずれかに当てはまれば出す）
 * @property onlyMembers メンバーの投稿とメンバー加入・ギフトだけを出す
 * @property onlyModerators モデレーター・配信者の投稿だけを出す
 * @property ngWords 本文に含むと出さない語（[ChatFilter]）
 * @property topChatOnly YouTube の「上位のチャット」だけを取得する（F-CHAT-07。既定は「すべてのチャット」）
 * @property theme テーマ（F-VIEW-05。既定はシステム追従で、従来の見た目と同じ）
 * @property maxVisible 表示保持件数の上限（F-VIEW-04。[MIN_VISIBLE]〜[MAX_VISIBLE]、[VISIBLE_STEP] 刻み。N-04）
 * @property largeHeaderButtons フローティングのヘッダーのボタンを推奨の 48dp にする（BL-081。既定は従来の 36dp）
 */
data class DisplaySettings(
    val showAuthorName: Boolean = true,
    val showAuthorIcon: Boolean = false,
    val showTime: Boolean = false,
    val onlyPaid: Boolean = false,
    val onlyMembers: Boolean = false,
    val onlyModerators: Boolean = false,
    val ngWords: List<String> = emptyList(),
    val maxVisible: Int = DEFAULT_VISIBLE,
    val topChatOnly: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val largeHeaderButtons: Boolean = false,
) {
    companion object {
        const val DEFAULT_VISIBLE = 500
        const val MIN_VISIBLE = 100
        const val MAX_VISIBLE = 1_000
        const val VISIBLE_STEP = 100

        /** 表示保持件数を範囲に収め、刻みに丸める。 */
        fun clampVisible(count: Int): Int =
            (count.coerceIn(MIN_VISIBLE, MAX_VISIBLE) + VISIBLE_STEP / 2) / VISIBLE_STEP * VISIBLE_STEP
    }
}

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
    private const val KEY_ONLY_PAID = "onlyPaid"
    private const val KEY_ONLY_MEMBERS = "onlyMembers"
    private const val KEY_ONLY_MODERATORS = "onlyModerators"
    private const val KEY_NG_WORDS = "ngWords"
    private const val KEY_MAX_VISIBLE = "maxVisible"
    private const val KEY_TOP_CHAT_ONLY = "topChatOnly"
    private const val KEY_THEME = "theme"
    private const val KEY_LARGE_HEADER_BUTTONS = "largeHeaderButtons"

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
                onlyPaid = loaded.getBoolean(KEY_ONLY_PAID, defaults.onlyPaid),
                onlyMembers = loaded.getBoolean(KEY_ONLY_MEMBERS, defaults.onlyMembers),
                onlyModerators = loaded.getBoolean(KEY_ONLY_MODERATORS, defaults.onlyModerators),
                ngWords = ChatFilter.parseNgWords(loaded.getString(KEY_NG_WORDS, null).orEmpty()),
                maxVisible = DisplaySettings.clampVisible(loaded.getInt(KEY_MAX_VISIBLE, defaults.maxVisible)),
                topChatOnly = loaded.getBoolean(KEY_TOP_CHAT_ONLY, defaults.topChatOnly),
                theme = ThemeMode.entries.find { it.name == loaded.getString(KEY_THEME, null) } ?: defaults.theme,
                largeHeaderButtons = loaded.getBoolean(KEY_LARGE_HEADER_BUTTONS, defaults.largeHeaderButtons),
            )
    }

    /** 設定を変えて保存する。 */
    fun update(transform: (DisplaySettings) -> DisplaySettings) {
        val next =
            transform(
                mutableState.value,
            ).let { it.copy(maxVisible = DisplaySettings.clampVisible(it.maxVisible)) }
        mutableState.value = next
        prefs?.edit {
            putBoolean(KEY_AUTHOR_NAME, next.showAuthorName)
            putBoolean(KEY_AUTHOR_ICON, next.showAuthorIcon)
            putBoolean(KEY_TIME, next.showTime)
            putBoolean(KEY_ONLY_PAID, next.onlyPaid)
            putBoolean(KEY_ONLY_MEMBERS, next.onlyMembers)
            putBoolean(KEY_ONLY_MODERATORS, next.onlyModerators)
            // NG ワードは 1 行 1 語で保存する（入力時に改行を含まない語へ分けている）
            putString(KEY_NG_WORDS, next.ngWords.joinToString("\n"))
            putInt(KEY_MAX_VISIBLE, next.maxVisible)
            putBoolean(KEY_TOP_CHAT_ONLY, next.topChatOnly)
            putString(KEY_THEME, next.theme.name)
            putBoolean(KEY_LARGE_HEADER_BUTTONS, next.largeHeaderButtons)
        }
    }
}
