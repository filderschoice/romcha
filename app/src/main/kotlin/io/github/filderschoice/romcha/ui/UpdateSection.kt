package io.github.filderschoice.romcha.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.filderschoice.romcha.R
import io.github.filderschoice.romcha.update.UpdateCheckResult

/** 更新確認の画面上の状態（F-APP-02）。 */
sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    data class Done(
        val result: UpdateCheckResult,
    ) : UpdateState
}

/** 更新確認の表示に使う値。 */
data class UpdateUiModel(
    val currentVersion: String,
    val state: UpdateState,
)

/**
 * 更新の確認（F-APP-02）。ボタンを押した時だけ GitHub へ問い合わせる（自動では通信しない）。
 * 新しい版があればダウンロードページを開くボタンを出す（自動インストールはしない）。
 */
@Composable
internal fun UpdateSection(
    currentVersion: String,
    state: UpdateState,
    actions: HomeActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.update_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.update_current, currentVersion), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = actions::checkForUpdate, enabled = state != UpdateState.Checking) {
                Text(stringResource(R.string.update_check))
            }
            val result = (state as? UpdateState.Done)?.result
            if (result is UpdateCheckResult.Available) {
                Button(onClick = actions::openReleasePage) { Text(stringResource(R.string.update_open_page)) }
            }
        }
        updateMessage(state)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text(stringResource(R.string.update_hint), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun updateMessage(state: UpdateState): String? =
    when (state) {
        UpdateState.Idle -> null
        UpdateState.Checking -> stringResource(R.string.update_checking)
        is UpdateState.Done ->
            when (val result = state.result) {
                is UpdateCheckResult.Available -> stringResource(R.string.update_available, result.latest.toString())
                is UpdateCheckResult.UpToDate -> stringResource(R.string.update_up_to_date)
                UpdateCheckResult.NoRelease -> stringResource(R.string.update_no_release)
                is UpdateCheckResult.NetworkError -> stringResource(R.string.update_network_error)
                is UpdateCheckResult.HttpError -> stringResource(R.string.update_http_error, result.code)
                is UpdateCheckResult.InvalidResponse -> stringResource(R.string.update_invalid)
            }
    }
