package io.github.filderschoice.romcha.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.filderschoice.romcha.R

/** 設定の初期化（BL-097）。誤操作を防ぐため、確認ダイアログを挟む。 */
@Composable
internal fun ResetAction(actions: HomeActions) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    SettingsLink(Icons.Filled.Delete, R.string.reset_button, R.string.reset_hint) { confirming = true }
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
