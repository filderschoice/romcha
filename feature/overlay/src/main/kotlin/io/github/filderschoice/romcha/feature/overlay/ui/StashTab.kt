package io.github.filderschoice.romcha.feature.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.filderschoice.romcha.feature.overlay.OverlayFormat
import io.github.filderschoice.romcha.feature.overlay.R
import io.github.filderschoice.romcha.feature.overlay.StashRule
import io.github.filderschoice.romcha.feature.overlay.StashSide
import io.github.filderschoice.romcha.feature.overlay.WindowMode

/**
 * 画面端へ退避中のつまみ（BL-049）。画面の内側へスワイプするか、タップすると元の位置・大きさに戻る。
 *
 * 上下のドラッグでつまみ（とウィンドウの縦位置）を動かせる。矢印は戻す向き（画面の内側）を指す。
 */
@Composable
internal fun StashTab(
    side: StashSide,
    opacity: Float,
    actions: OverlayActions,
) {
    val restoreThreshold = with(LocalDensity.current) { RESTORE_THRESHOLD.toPx() }
    val restore = { actions.onWindowModeChange(WindowMode.Normal) }
    val shape =
        if (side == StashSide.LEFT) {
            RoundedCornerShape(topEnd = CORNER, bottomEnd = CORNER)
        } else {
            RoundedCornerShape(topStart = CORNER, bottomStart = CORNER)
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                // 画面端にあるため、OS の戻るジェスチャーに内側へのスワイプを奪われないようにする
                .systemGestureExclusion()
                .clip(shape)
                .background(HeaderColor.copy(alpha = OverlayFormat.clampOpacity(opacity).coerceAtLeast(MIN_ALPHA)))
                .pointerInput(side) {
                    var totalX = 0f
                    detectDragGestures(
                        onDragStart = { totalX = 0f },
                        onDragEnd = {
                            if (StashRule.shouldRestore(
                                    side,
                                    totalX,
                                    restoreThreshold,
                                )
                            ) {
                                restore()
                            } else {
                                actions.onGestureEnd()
                            }
                        },
                    ) { change, drag ->
                        change.consume()
                        totalX += drag.x
                        actions.onMove(0f, drag.y)
                    }
                }.pointerInput(Unit) { detectTapGestures { restore() } },
    ) {
        Icon(
            imageVector = if (side == StashSide.LEFT) Icons.AutoMirrored.Filled.KeyboardArrowRight else ArrowLeft,
            contentDescription = stringResource(R.string.overlay_unstash),
            tint = OverlayTextColor,
        )
    }
}

private val ArrowLeft = Icons.AutoMirrored.Filled.KeyboardArrowLeft
private val CORNER = 12.dp
private val RESTORE_THRESHOLD = 24.dp

/** 背景をほぼ透明にしていてもつまみは見失わないよう、下限を上げる */
private const val MIN_ALPHA = 0.7f
