package io.github.filderschoice.romcha.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.filderschoice.romcha.R
import io.github.filderschoice.romcha.backup.BackupSettings
import io.github.filderschoice.romcha.crash.CrashReporting
import io.github.filderschoice.romcha.language.AppLanguage
import io.github.filderschoice.romcha.language.AppLanguageSettings

/**
 * 設定の入口（BL-101）。ホームには操作だけを置き、設定はここへグループ化して集める。
 * 各項目は「アイコン・タイトル・1〜2 行の説明」のリスト形式にそろえる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    update: UpdateUiModel,
    actions: HomeActions,
    onBack: () -> Unit,
    onOpenDisplaySettings: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingsLink(
                icon = Icons.Filled.Settings,
                title = R.string.display_title,
                summary = R.string.display_summary,
                navigates = true,
                onClick = onOpenDisplaySettings,
            )
            LanguageAction()
            HorizontalDivider()
            SettingsGroup(R.string.settings_group_privacy)
            BackupSwitch()
            CrashReportingSwitch()
            HorizontalDivider()
            SettingsGroup(R.string.settings_group_data)
            CacheAction(actions)
            ResetAction(actions)
            HorizontalDivider()
            SettingsGroup(R.string.update_title)
            UpdateSection(currentVersion = update.currentVersion, state = update.state, actions = actions)
        }
    }
}

/** グループの見出し。リスト項目の左端にそろえる（親が余白を持つ画面では [horizontalPadding] を 0 にする）。 */
@Composable
internal fun SettingsGroup(
    title: Int,
    horizontalPadding: Dp = 16.dp,
) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = horizontalPadding, end = horizontalPadding, top = 16.dp, bottom = 4.dp),
    )
}

/**
 * 別の画面や操作へ進む項目。行全体がタップ領域。
 *
 * [navigates] が true の項目は別の画面へ進むため、行末に矢印を出して、その場で動作する項目と見分けられるようにする。
 */
@Composable
internal fun SettingsLink(
    icon: ImageVector,
    title: Int,
    summary: Int,
    navigates: Boolean = false,
    onClick: () -> Unit,
) {
    // ListItem は説明が複数行になると行末の要素が上に寄るため、矢印を縦方向の中央に置ける Row で組む
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (navigates) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

/** スイッチの項目。行全体のタップで切り替え、スクリーンリーダーには 1 つのスイッチとして読ませる。 */
@Composable
internal fun SettingsSwitch(
    icon: ImageVector,
    title: Int,
    summary: Int,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(summary)) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    )
}

/** 設定のバックアップ（再インストール時の復元）のオン・オフ（BL-097）。 */
@Composable
private fun BackupSwitch() {
    val settings = BackupSettings.shared(LocalContext.current)
    val enabled by settings.enabled.collectAsState()
    SettingsSwitch(Icons.Filled.Done, R.string.backup_switch, R.string.backup_hint, enabled, settings::setEnabled)
}

/** クラッシュ情報の送信のオン・オフ（BL-089）。Firebase が無効なビルドでは出さない。 */
@Composable
private fun CrashReportingSwitch() {
    val settings = CrashReporting.settings() ?: return
    val enabled by settings.enabled.collectAsState()
    SettingsSwitch(Icons.Filled.Warning, R.string.crash_switch, R.string.crash_hint, enabled, settings::setEnabled)
}

/** 動画特定のキャッシュの消去。誤った動画のチャットが出続ける時に、ユーザーが自分で実行する。 */
@Composable
private fun CacheAction(actions: HomeActions) {
    SettingsLink(Icons.Filled.Build, R.string.cache_clear, R.string.cache_hint, onClick = actions::clearResolutionCache)
}

/** 表示言語の選択（BL-104）。選ぶと OS が画面を作り直して、アプリとフローティングウィンドウの言語が切り替わる。 */
@Composable
private fun LanguageAction() {
    val context = LocalContext.current
    var choosing by rememberSaveable { mutableStateOf(false) }
    SettingsLink(Icons.Filled.Place, R.string.language_title, R.string.language_summary) { choosing = true }
    if (choosing) {
        val current = AppLanguageSettings.current(context)
        AlertDialog(
            onDismissRequest = { choosing = false },
            title = { Text(stringResource(R.string.language_title)) },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    AppLanguage.entries.forEach { language ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .selectable(
                                        selected = language == current,
                                        role = Role.RadioButton,
                                        onClick = {
                                            choosing = false
                                            AppLanguageSettings.set(context, language)
                                        },
                                    ),
                        ) {
                            RadioButton(selected = language == current, onClick = null)
                            Text(stringResource(languageLabel(language)), modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { choosing = false },
                ) { Text(stringResource(R.string.language_cancel)) }
            },
        )
    }
}

private fun languageLabel(language: AppLanguage): Int =
    when (language) {
        AppLanguage.JAPANESE -> R.string.language_ja
        AppLanguage.ENGLISH -> R.string.language_en
    }
