package io.github.filderschoice.romcha.feature.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.filderschoice.romcha.core.chat.ChatMessage
import io.github.filderschoice.romcha.core.sync.LiveTimeline
import io.github.filderschoice.romcha.feature.overlay.AutoScrollPolicy
import io.github.filderschoice.romcha.feature.overlay.OverlayCandidate
import io.github.filderschoice.romcha.feature.overlay.OverlayFormat
import io.github.filderschoice.romcha.feature.overlay.OverlayUiState
import io.github.filderschoice.romcha.feature.overlay.R
import io.github.filderschoice.romcha.feature.overlay.SyncIndicator
import kotlin.math.roundToInt

internal val OverlayTextColor = Color(0xFFF5F5F5)
internal val SubTextColor = Color(0xFFB0BEC5)

/** フローティングウィンドウの中身（F-OVL-01〜03/07、F-VIEW-02）。 */
@Composable
fun ChatOverlay(
    state: OverlayUiState,
    opacity: Float,
    fontScale: Float,
    liveDelaySeconds: Int,
    actions: OverlayActions,
) {
    var showSettings by remember { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = OverlayFormat.clampOpacity(opacity))),
    ) {
        Header(state, actions, onToggleSettings = { showSettings = !showSettings })
        if (showSettings) {
            OpacitySlider(opacity, actions)
            FontScaleSlider(fontScale, actions)
            if (state.indicator == SyncIndicator.LIVE) LiveDelaySlider(liveDelaySeconds, actions)
        }
        state.notice?.let { Notice(it) }
        if (state.candidates.isNotEmpty()) Candidates(state.candidates, actions)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            MessageList(state.messages, fontScale)
            ResizeHandle(actions, Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun Header(
    state: OverlayUiState,
    actions: OverlayActions,
    onToggleSettings: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.08f))
                .pointerInput(Unit) {
                    detectDragGestures(onDragEnd = actions::onGestureEnd) { change, drag ->
                        change.consume()
                        actions.onMove(drag.x, drag.y)
                    }
                }.padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title ?: stringResource(R.string.overlay_title_placeholder),
                color = OverlayTextColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${OverlayFormat.indicatorLabel(state.indicator)}  ${OverlayFormat.position(state.positionMs)}",
                color = SubTextColor,
                fontSize = 10.sp,
            )
        }
        IconButton(onClick = onToggleSettings, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Settings, stringResource(R.string.overlay_settings), tint = SubTextColor)
        }
        IconButton(onClick = actions::onHide, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, stringResource(R.string.overlay_hide), tint = SubTextColor)
        }
    }
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

@Composable
private fun Notice(text: String) {
    Text(
        text = text,
        color = OverlayTextColor,
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().background(Color(0x66B71C1C)).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun Candidates(
    candidates: List<OverlayCandidate>,
    actions: OverlayActions,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        items(candidates, key = { it.videoId }) { candidate ->
            AssistChip(
                onClick = { actions.onCandidateSelected(candidate.videoId) },
                label = {
                    Text(
                        candidate.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                        color = OverlayTextColor,
                    )
                },
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    fontScale: Float,
) {
    val listState = rememberLazyListState()
    val policy = remember { AutoScrollPolicy() }
    var showJump by remember { mutableStateOf(false) }
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            last == null || last.index >= info.totalItemsCount - 1
        }
    }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect {
            if (it is DragInteraction.Stop || it is DragInteraction.Cancel) {
                policy.onUserScrolled(atBottom)
                showJump = policy.showJumpToLatest
            }
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { atBottom }.collect { bottom ->
            if (bottom) {
                policy.onReachedBottom()
                showJump = false
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        // チャット欄の sp だけを倍率で拡大・縮小する（端末の文字サイズ設定には掛け合わせる）
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * fontScale)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
                items(messages, key = { it.id }) { ChatItem(it) }
            }
        }
        if (showJump) {
            JumpToLatestButton(Modifier.align(Alignment.BottomCenter)) {
                policy.onReachedBottom()
                showJump = false
            }
        }
    }
    // 新着時と「最新へ」ボタンの押下時に、追従中なら最下部へ移動する
    LaunchedEffect(showJump, messages) {
        if (!showJump && messages.isNotEmpty() && policy.following) listState.scrollToItem(messages.lastIndex)
    }
}

@Composable
private fun JumpToLatestButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    SmallFloatingActionButton(onClick = onClick, modifier = modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            Text(stringResource(R.string.overlay_jump_to_latest), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ResizeHandle(
    actions: OverlayActions,
    modifier: Modifier,
) {
    Box(
        modifier =
            modifier
                .size(24.dp)
                .pointerInput(Unit) {
                    detectDragGestures(onDragEnd = actions::onGestureEnd) { change, drag ->
                        change.consume()
                        actions.onResize(drag.x, drag.y)
                    }
                }.padding(6.dp)
                .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp)),
    )
}
