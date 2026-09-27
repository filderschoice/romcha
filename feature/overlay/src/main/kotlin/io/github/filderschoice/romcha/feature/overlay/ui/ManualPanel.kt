package io.github.filderschoice.romcha.feature.overlay.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.filderschoice.romcha.core.sync.PlaybackSnapshot
import io.github.filderschoice.romcha.core.sync.PlaybackStatus
import io.github.filderschoice.romcha.feature.overlay.OverlayFormat
import io.github.filderschoice.romcha.feature.overlay.R

/**
 * 手動タイマーモードの操作（F-SYNC-07）。オン・オフ、開始・停止、位置の入力。
 *
 * ウィンドウは通常フォーカスを取らない（公式アプリの操作を妨げない）ため、位置の入力中だけフォーカスを取れるようにする。
 *
 * @param timer 手動タイマーの状態。null ならオフ
 */
@Composable
internal fun ManualPanel(
    timer: PlaybackSnapshot?,
    actions: OverlayActions,
) {
    var editing by remember { mutableStateOf(false) }
    Row(modifier = Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        PanelButton(if (timer == null) R.string.overlay_manual_on else R.string.overlay_manual_off) {
            editing = false
            actions.onManual(if (timer == null) ManualCommand.Enable else ManualCommand.Disable)
        }
        if (timer != null && !editing) {
            val running = timer.status == PlaybackStatus.PLAYING
            PanelButton(if (running) R.string.overlay_manual_stop else R.string.overlay_manual_start) {
                actions.onManual(if (running) ManualCommand.Stop else ManualCommand.Start)
            }
            PanelButton(R.string.overlay_manual_input) {
                editing = true
                actions.onInputFocus(true)
            }
        }
    }
    if (timer != null && editing) {
        PositionInput(
            onSubmit = { positionMs ->
                editing = false
                actions.onInputFocus(false)
                positionMs?.let { actions.onManual(ManualCommand.Seek(it)) }
            },
        )
    }
}

/** 位置の入力欄（`1:23:45`・`23:45`・秒数）。確定で [onSubmit] に位置を渡す（取り消し・解釈できない時は null）。 */
@Composable
private fun PositionInput(onSubmit: (Long?) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(color = OverlayTextColor, fontSize = 14.sp),
            cursorBrush = SolidColor(OverlayTextColor),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit(OverlayFormat.parsePosition(text)) }),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text(
                        stringResource(R.string.overlay_manual_hint),
                        color = SubTextColor,
                        fontSize = 14.sp,
                    )
                }
                inner()
            },
            modifier = Modifier.width(96.dp).focusRequester(focus),
        )
        PanelButton(R.string.overlay_manual_seek) { onSubmit(OverlayFormat.parsePosition(text)) }
        PanelButton(R.string.overlay_manual_cancel) { onSubmit(null) }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun PanelButton(
    label: Int,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(stringResource(label), color = OverlayTextColor, fontSize = 11.sp)
    }
}
