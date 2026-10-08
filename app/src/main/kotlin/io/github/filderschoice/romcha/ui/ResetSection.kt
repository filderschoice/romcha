package io.github.filderschoice.romcha.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.filderschoice.romcha.R

/** 設定の初期化（BL-097）。誤操作を防ぐため、確認ダイアログを挟む。 */
@Composable
internal fun ResetSection(actions: HomeActions) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.reset_title), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { confirming = true }) { Text(stringResource(R.string.reset_button)) }
        Text(stringResource(R.string.reset_hint), style = MaterialTheme.typography.bodySmall)
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            text = { Text(stringResource(R.string.reset_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        actions.resetSettings()
                    },
                ) { Text(stringResource(R.string.reset_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.reset_cancel)) }
            },
        )
    }
}
