package io.github.filderschoice.romcha.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.filderschoice.romcha.R
import io.github.filderschoice.romcha.core.media.NowPlaying
import kotlinx.coroutines.flow.StateFlow

/** 公式アプリの MediaSession の診断表示（M0 の Q-01 / Q-02 を実機で確認するため）。端末の画面に表示するだけで送信しない。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiagnosticsScreen(
    nowPlaying: StateFlow<NowPlaying>,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val state by nowPlaying.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnostics_title)) },
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
        ) {
            SelectionContainer {
                Text(
                    text = diagnosticsText(state),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun diagnosticsText(state: NowPlaying): String {
    if (!state.sessionFound) return stringResource(R.string.diagnostics_no_session)
    val snapshot = state.snapshot
    val metadata = state.metadata
    return buildString {
        appendLine("status = ${snapshot.status} / position = ${snapshot.positionMs} ms / speed = ${snapshot.speed}")
        appendLine(
            "title = ${metadata?.title} / channel = ${metadata?.channelName} / duration = ${metadata?.durationMs} ms",
        )
        appendLine("videoIdHints = ${metadata?.videoIdHints}")
        appendLine("---")
        state.debugLines.forEach(::appendLine)
    }
}
