package io.github.filderschoice.romcha.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.filderschoice.romcha.R
import io.github.filderschoice.romcha.feature.overlay.ChatFilter
import io.github.filderschoice.romcha.feature.overlay.DisplaySettings
import io.github.filderschoice.romcha.feature.overlay.DisplaySettingsStore
import io.github.filderschoice.romcha.feature.overlay.ThemeMode
import kotlin.math.roundToInt

/** チャットの表示設定（F-VIEW-01）。変更は表示中のフローティングウィンドウへすぐ反映する。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DisplaySettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val settings by DisplaySettingsStore.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.display_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.display_items), style = MaterialTheme.typography.titleMedium)
            SwitchRow(R.string.display_author_name, settings.showAuthorName) { on ->
                DisplaySettingsStore.update { it.copy(showAuthorName = on) }
            }
            SwitchRow(R.string.display_author_icon, settings.showAuthorIcon) { on ->
                DisplaySettingsStore.update { it.copy(showAuthorIcon = on) }
            }
            SwitchRow(R.string.display_time, settings.showTime) { on ->
                DisplaySettingsStore.update { it.copy(showTime = on) }
            }
            ThemeSelector(settings.theme)
            Text(stringResource(R.string.display_chat_kind), style = MaterialTheme.typography.titleMedium)
            SwitchRow(R.string.display_top_chat_only, settings.topChatOnly) { on ->
                DisplaySettingsStore.update { it.copy(topChatOnly = on) }
            }
            Text(stringResource(R.string.display_top_chat_hint), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.display_filter), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.display_filter_hint), style = MaterialTheme.typography.bodySmall)
            SwitchRow(R.string.display_only_paid, settings.onlyPaid) { on ->
                DisplaySettingsStore.update { it.copy(onlyPaid = on) }
            }
            SwitchRow(R.string.display_only_members, settings.onlyMembers) { on ->
                DisplaySettingsStore.update { it.copy(onlyMembers = on) }
            }
            SwitchRow(R.string.display_only_moderators, settings.onlyModerators) { on ->
                DisplaySettingsStore.update { it.copy(onlyModerators = on) }
            }
            NgWordsInput(settings.ngWords)
            MaxVisibleSlider(settings.maxVisible)
            Text(stringResource(R.string.display_font_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** NG ワードの入力（1 行 1 語）。「保存」で取り込み、空行・重複を除いて上限に収める（F-VIEW-03）。 */
@Composable
private fun NgWordsInput(saved: List<String>) {
    var text by rememberSaveable(saved) { mutableStateOf(saved.joinToString("\n")) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.display_ng_words)) },
            supportingText = { Text(stringResource(R.string.display_ng_words_hint, ChatFilter.MAX_NG_WORDS)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { DisplaySettingsStore.update { it.copy(ngWords = ChatFilter.parseNgWords(text)) } },
            enabled = ChatFilter.parseNgWords(text) != saved,
        ) {
            Text(stringResource(R.string.display_ng_words_save))
        }
    }
}

/** テーマ（F-VIEW-05）。システム追従ではフローティングウィンドウは従来どおり暗色。行全体のタップで選ぶ（BL-079）。 */
@Composable
private fun ThemeSelector(theme: ThemeMode) {
    Column {
        Text(stringResource(R.string.display_theme), style = MaterialTheme.typography.titleMedium)
        Column(modifier = Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = MIN_ROW_HEIGHT)
                            .selectable(
                                selected = theme == mode,
                                role = Role.RadioButton,
                                onClick = { DisplaySettingsStore.update { it.copy(theme = mode) } },
                            ),
                ) {
                    RadioButton(selected = theme == mode, onClick = null)
                    Text(stringResource(themeLabel(mode)), modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

private fun themeLabel(mode: ThemeMode): Int =
    when (mode) {
        ThemeMode.SYSTEM -> R.string.display_theme_system
        ThemeMode.LIGHT -> R.string.display_theme_light
        ThemeMode.DARK -> R.string.display_theme_dark
    }

/** 表示保持件数の上限（F-VIEW-04）。多くすると遡れる量が増える代わりにメモリを使う（N-04）。 */
@Composable
private fun MaxVisibleSlider(count: Int) {
    Column {
        Text(stringResource(R.string.display_max_visible, count), style = MaterialTheme.typography.titleMedium)
        Slider(
            value = count.toFloat(),
            onValueChange = { value -> DisplaySettingsStore.update { it.copy(maxVisible = value.roundToInt()) } },
            valueRange = DisplaySettings.MIN_VISIBLE.toFloat()..DisplaySettings.MAX_VISIBLE.toFloat(),
            steps = (DisplaySettings.MAX_VISIBLE - DisplaySettings.MIN_VISIBLE) / DisplaySettings.VISIBLE_STEP - 1,
        )
        Text(stringResource(R.string.display_max_visible_hint), style = MaterialTheme.typography.bodySmall)
    }
}

/** スイッチの行。行全体のタップで切り替え、スクリーンリーダーには 1 つのスイッチとして読ませる（BL-079）。 */
@Composable
internal fun SwitchRow(
    label: Int,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MIN_ROW_HEIGHT)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    ) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** 行のタップ領域の最小の高さ（Material の推奨タッチターゲット） */
private val MIN_ROW_HEIGHT = 48.dp
