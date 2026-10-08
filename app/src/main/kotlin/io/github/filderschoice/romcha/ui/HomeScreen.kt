package io.github.filderschoice.romcha.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.filderschoice.romcha.PermissionStatus
import io.github.filderschoice.romcha.PermissionStep
import io.github.filderschoice.romcha.R
import io.github.filderschoice.romcha.core.chat.VideoUrlParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    status: PermissionStatus,
    actions: HomeActions,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, stringResource(R.string.settings_title))
                    }
                    MoreMenu(onOpenDiagnostics = onOpenDiagnostics, onOpenLicenses = onOpenLicenses)
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.app_subtitle), style = MaterialTheme.typography.bodyMedium)
            Permissions(status, actions)
            OverlayControls(status, actions)
            UrlInput(enabled = status.canStartOverlay, onOpen = actions::openVideo)
            Disclaimer()
        }
    }
}

/** 三点メニュー。普段は使わない情報系の画面（診断情報・ライセンス）への入口。 */
@Composable
private fun MoreMenu(
    onOpenDiagnostics: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Filled.MoreVert, stringResource(R.string.more_menu))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.diagnostics_title)) },
            onClick = {
                open = false
                onOpenDiagnostics()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.licenses_title)) },
            onClick = {
                open = false
                onOpenLicenses()
            },
        )
    }
}

/** 非公式な取得方式であり、YouTube / Google とは無関係である旨の免責表示（F-APP-04、PLAN 5.5）。 */
@Composable
private fun Disclaimer() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Text(
            stringResource(R.string.disclaimer),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** 権限案内（F-APP-01）。すべて許可済みなら 1 行にたたみ、開くと各権限を確かめられる（BL-080）。 */
@Composable
private fun Permissions(
    status: PermissionStatus,
    actions: HomeActions,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val allGranted = status.nextStep == null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.permissions_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (allGranted) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(stringResource(if (expanded) R.string.permissions_collapse else R.string.permissions_expand))
                }
            }
        }
        if (allGranted && !expanded) {
            Text(stringResource(R.string.permissions_all_granted), style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        PermissionStep.entries.forEach { step ->
            PermissionRow(
                step = step,
                granted = status.isGranted(step),
                highlighted = status.nextStep == step,
                onRequest =
                    when (step) {
                        PermissionStep.OVERLAY -> actions::requestOverlay
                        PermissionStep.NOTIFICATION_ACCESS -> actions::requestNotificationAccess
                        PermissionStep.POST_NOTIFICATIONS -> actions::requestPostNotifications
                    },
            )
        }
    }
}

@Composable
private fun PermissionRow(
    step: PermissionStep,
    granted: Boolean,
    highlighted: Boolean,
    onRequest: () -> Unit,
) {
    val (title, description) = permissionTexts(step)
    val colorScheme = MaterialTheme.colorScheme
    val container = if (highlighted) colorScheme.primaryContainer else colorScheme.surfaceVariant
    Card(colors = CardDefaults.cardColors(containerColor = container), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(title), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(stringResource(if (granted) R.string.permission_granted else R.string.permission_not_granted))
            }
            Text(stringResource(description), style = MaterialTheme.typography.bodySmall)
            if (!granted) {
                Button(onClick = onRequest) { Text(stringResource(R.string.permission_open_settings)) }
            }
        }
    }
}

/** 権限ごとの見出しと説明（文字列リソースID）。 */
private fun permissionTexts(step: PermissionStep): Pair<Int, Int> =
    when (step) {
        PermissionStep.OVERLAY ->
            R.string.permission_overlay to R.string.permission_overlay_description
        PermissionStep.NOTIFICATION_ACCESS ->
            R.string.permission_notification_access to R.string.permission_notification_access_description
        PermissionStep.POST_NOTIFICATIONS ->
            R.string.permission_post_notifications to R.string.permission_post_notifications_description
    }

@Composable
private fun OverlayControls(
    status: PermissionStatus,
    actions: HomeActions,
) {
    // ボタン名を省略せずに書くと横に並ばないため縦に並べる（BL-080。「終了」だけでは何を終えるのか分からなかった）
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = actions::startOverlay, enabled = status.canStartOverlay, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.overlay_start))
        }
        OutlinedButton(onClick = actions::stopOverlay, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.overlay_stop))
        }
    }
}

/**
 * URL 手入力（F-VID-05）。自動特定できない場合の代替。
 *
 * 「クリップボードから貼り付け」（F-VID-06）は押した時だけクリップボードを読む（開いただけでは読まない。プライバシーのため）。
 */
@Composable
private fun UrlInput(
    enabled: Boolean,
    onOpen: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    val submit = {
        val videoId = VideoUrlParser.extractVideoId(text)
        error = videoId == null
        if (videoId != null) onOpen(videoId)
    }
    val clipboard = LocalClipboardManager.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.url_title), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                error = false
            },
            label = { Text(stringResource(R.string.url_label)) },
            isError = error,
            supportingText = { if (error) Text(stringResource(R.string.url_invalid)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = submit, enabled = enabled && text.isNotBlank()) { Text(stringResource(R.string.url_open)) }
            OutlinedButton(onClick = {
                clipboard.getText()?.text?.let {
                    text = it.trim()
                    error = VideoUrlParser.extractVideoId(text) == null
                }
            }) {
                Text(stringResource(R.string.url_paste))
            }
        }
    }
}
