package io.github.filderschoice.romcha.feature.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.filderschoice.romcha.feature.overlay.OverlayFormat
import io.github.filderschoice.romcha.feature.overlay.R
import io.github.filderschoice.romcha.feature.overlay.SyncIndicator

/**
 * 最小化中のバブル（F-OVL-04）。タップで元の大きさに戻し、ドラッグで移動する。
 *
 * 縁の色で同期状態を示す（同期中・ライブ＝緑、それ以外＝灰）。
 */
@Composable
internal fun Bubble(
    indicator: SyncIndicator,
    opacity: Float,
    actions: OverlayActions,
) {
    val ring = if (indicator == SyncIndicator.SYNCING || indicator == SyncIndicator.LIVE) ActiveRing else SubTextColor
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(ring)
                .pointerInput(Unit) {
                    detectDragGestures(onDragEnd = actions::onGestureEnd) { change, drag ->
                        change.consume()
                        actions.onMove(drag.x, drag.y)
                    }
                }.pointerInput(Unit) { detectTapGestures { actions.onMinimizeChange(false) } },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(BubbleInner)
                    .clip(CircleShape)
                    .background(HeaderColor.copy(alpha = OverlayFormat.clampOpacity(opacity).coerceAtLeast(MIN_ALPHA))),
        ) {
            Icon(Icons.Default.List, stringResource(R.string.overlay_restore), tint = OverlayTextColor)
        }
    }
}

private val ActiveRing = Color(0xFF2BA640)
private val BubbleInner = 42.dp

/** 背景をほぼ透明にしていてもバブルは見失わないよう、下限を上げる */
private const val MIN_ALPHA = 0.7f
