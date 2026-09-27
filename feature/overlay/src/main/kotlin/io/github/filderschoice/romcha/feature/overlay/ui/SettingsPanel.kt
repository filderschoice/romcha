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
import io.github.filderschoice.romcha.feature.overlay.OverlayFormat
import io.github.filderschoice.romcha.feature.overlay.R
import kotlin.math.roundToInt

/** ヘッダーの設定ボタンで開く設定パネル（不透明度・文字サイズ・表示遅延・タッチ透過）。 */
@Composable
internal fun SettingsPanel(
    live: Boolean,
    opacity: Float,
    fontScale: Float,
    liveDelaySeconds: Int,
    actions: OverlayActions,
    onTouchThrough: () -> Unit,
) {
    OpacitySlider(opacity, actions)
    FontScaleSlider(fontScale, actions)
    if (live) LiveDelaySlider(liveDelaySeconds, actions)
    TouchThroughButton(onTouchThrough)
}

@Composable
private fun OpacitySlider(
    opacity: Float,
    actions: OverlayActions,
) {
    Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.overlay_opacity, OverlayFormat.opacityPercent(opacity)),
            color = SubTextColor,
            fontSize = 11.sp,
        )
        Slider(
            value = OverlayFormat.clampOpacity(opacity),
            onValueChange = actions::onOpacityChange,
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
) {
    Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.overlay_font_scale, OverlayFormat.fontScalePercent(scale)),
            color = SubTextColor,
            fontSize = 11.sp,
        )
        Slider(
            value = OverlayFormat.clampFontScale(scale),
            onValueChange = actions::onFontScaleChange,
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
) {
    Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.overlay_live_delay, seconds), color = SubTextColor, fontSize = 11.sp)
        Slider(
            value = seconds.toFloat(),
            onValueChange = { actions.onLiveDelayChange(it.roundToInt()) },
            onValueChangeFinished = actions::onGestureEnd,
            valueRange = LiveTimeline.MIN_DELAY_SECONDS.toFloat()..LiveTimeline.MAX_DELAY_SECONDS.toFloat(),
            steps = LiveTimeline.MAX_DELAY_SECONDS - LiveTimeline.MIN_DELAY_SECONDS - 1,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
    }
}
