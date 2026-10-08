package io.github.filderschoice.romcha.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.filderschoice.romcha.R
import io.github.filderschoice.romcha.backup.BackupSettings

/** 設定のバックアップ（再インストール時の復元）のオン・オフ（BL-097）。 */
@Composable
internal fun BackupSection() {
    val settings = BackupSettings.shared(LocalContext.current)
    val enabled by settings.enabled.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
        SwitchRow(R.string.backup_switch, enabled, settings::setEnabled)
        Text(stringResource(R.string.backup_hint), style = MaterialTheme.typography.bodySmall)
    }
}
