package io.github.filderschoice.romcha.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.filderschoice.romcha.R
import io.github.filderschoice.romcha.backup.BackupSettings
import io.github.filderschoice.romcha.crash.CrashReporting

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
                onClick = onOpenDisplaySettings,
            )
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

/** グループの見出し。リスト項目の左端にそろえる。 */
@Composable
internal fun SettingsGroup(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** 別の画面や操作へ進む項目。行全体がタップ領域。 */
@Composable
internal fun SettingsLink(
    icon: ImageVector,
    title: Int,
    summary: Int,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(summary)) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
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
    SettingsLink(Icons.Filled.Build, R.string.cache_clear, R.string.cache_hint, actions::clearResolutionCache)
}
