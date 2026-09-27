package io.github.filderschoice.romcha.feature.overlay.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.filderschoice.romcha.core.sync.LiveTimeline
import io.github.filderschoice.romcha.core.sync.SyncOffset
import io.github.filderschoice.romcha.feature.overlay.OverlayFormat
import io.github.filderschoice.romcha.feature.overlay.OverlaySettings
import io.github.filderschoice.romcha.feature.overlay.R
import io.github.filderschoice.romcha.feature.overlay.SyncIndicator
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * ヘッダーの設定ボタンで開く設定パネル（不透明度・文字サイズ・同期の補正または表示遅延・タッチ透過）。
 *
 * ライブ・プレミア中は表示遅延（F-SYNC-08）、それ以外（リプレイ）は同期オフセットの補正（F-SYNC-06）を出す。
 */
@Composable
internal fun SettingsPanel(
    indicator: SyncIndicator,
    settings: OverlaySettings,
    actions: OverlayActions,
    onTouchThrough: () -> Unit,
) {
    val change = { next: OverlaySettings -> actions.onSettingsChange(next) }
    OpacitySlider(settings.opacity, actions) { change(settings.copy(opacity = it)) }
    FontScaleSlider(settings.fontScale, actions) { change(settings.copy(fontScale = it)) }
    if (indicator == SyncIndicator.LIVE) {
        LiveDelaySlider(settings.liveDelaySeconds, actions) { change(settings.copy(liveDelaySeconds = it)) }
    } else {
        SyncOffsetSlider(settings.syncOffsetMs, actions) { change(settings.copy(syncOffsetMs = it)) }
    }
    TouchThroughButton(onTouchThrough)
}

/** リプレイの同期オフセットの補正（F-SYNC-06）。＋でチャットを早く、－で遅く表示する。 */
@Composable
private fun SyncOffsetSlider(
    offsetMs: Long,
    actions: OverlayActions,
    onChange: (Long) -> Unit,
) {
    Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.overlay_sync_offset, OverlayFormat.offsetSeconds(offsetMs)),
            color = SubTextColor,
            fontSize = 11.sp,
        )
        Slider(
            value = offsetMs.toFloat(),
            onValueChange = { onChange(it.roundToLong()) },
            onValueChangeFinished = actions::onGestureEnd,
            valueRange = SyncOffset.MIN_MS.toFloat()..SyncOffset.MAX_MS.toFloat(),
            steps = ((SyncOffset.MAX_MS - SyncOffset.MIN_MS) / SyncOffset.STEP_MS - 1).toInt(),
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
    }
}

@Composable
private fun OpacitySlider(
    opacity: Float,
    actions: OverlayActions,
    onChange: (Float) -> Unit,
) {
    Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.overlay_opacity, OverlayFormat.opacityPercent(opacity)),
            color = SubTextColor,
            fontSize = 11.sp,
        )
        Slider(
            value = OverlayFormat.clampOpacity(opacity),
            onValueChange = onChange,
            onValueChangeFinished = actions::onGestureEnd,
            valueRange = OverlayFormat.MIN_OPACITY..1f,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
    }
}

/** チャットの文字サイズ（F-VIEW-01）。100% が従来の大きさ。 */
@Composable
private fun FontScaleSlider(
    scale: Float,
    actions: OverlayActions,
    onChange: (Float) -> Unit,
) {
    Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.overlay_font_scale, OverlayFormat.fontScalePercent(scale)),
            color = SubTextColor,
            fontSize = 11.sp,
        )
        Slider(
            value = OverlayFormat.clampFontScale(scale),
            onValueChange = onChange,
            onValueChangeFinished = actions::onGestureEnd,
            valueRange = OverlayFormat.MIN_FONT_SCALE..OverlayFormat.MAX_FONT_SCALE,
            steps = OverlayFormat.FONT_SCALE_STEPS,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
    }
}

/** タッチ透過モードへ入るボタン（F-OVL-05）。透過中はウィンドウを触れないため、解除は常駐通知から行う。 */
@Composable
private fun TouchThroughButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(stringResource(R.string.overlay_touch_through), color = OverlayTextColor, fontSize = 11.sp)
    }
}

/** ライブ・プレミア中の表示遅延（F-SYNC-08）。映像より先にチャットが流れる場合に遅らせる。 */
@Composable
private fun LiveDelaySlider(
    seconds: Int,
    actions: OverlayActions,
    onChange: (Int) -> Unit,
) {
    Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.overlay_live_delay, seconds), color = SubTextColor, fontSize = 11.sp)
        Slider(
            value = seconds.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            onValueChangeFinished = actions::onGestureEnd,
            valueRange = LiveTimeline.MIN_DELAY_SECONDS.toFloat()..LiveTimeline.MAX_DELAY_SECONDS.toFloat(),
            steps = LiveTimeline.MAX_DELAY_SECONDS - LiveTimeline.MIN_DELAY_SECONDS - 1,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
    }
}
