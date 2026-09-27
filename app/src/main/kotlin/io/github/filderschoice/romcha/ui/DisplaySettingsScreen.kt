package io.github.filderschoice.romcha.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.filderschoice.romcha.R
import io.github.filderschoice.romcha.feature.overlay.DisplaySettingsStore

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
            Text(stringResource(R.string.display_font_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun SwitchRow(
    label: Int,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
