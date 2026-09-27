package io.github.filderschoice.romcha.feature.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import io.github.filderschoice.romcha.core.chat.AuthorRole
import io.github.filderschoice.romcha.core.chat.ChatMessage
import io.github.filderschoice.romcha.core.chat.ChatMessageKind
import io.github.filderschoice.romcha.feature.overlay.DisplaySettings
import io.github.filderschoice.romcha.feature.overlay.ImagePolicy
import io.github.filderschoice.romcha.feature.overlay.OverlayFormat
import io.github.filderschoice.romcha.feature.overlay.R

private val OwnerColor = Color(0xFFFFD600)
private val ModeratorColor = Color(0xFF5E84F1)
private val MemberColor = Color(0xFF2BA640)
private val MembershipColor = Color(0xFF0F9D58)

/** チャット1件の表示。種別ごとに強調を変える（F-CHAT-08、F-VIEW-02）。 */
@Composable
internal fun ChatItem(
    message: ChatMessage,
    display: DisplaySettings,
) {
    val paid = message.paid
    when {
        paid != null -> PaidItem(message, display)
        message.kind == ChatMessageKind.MEMBERSHIP ||
            message.kind == ChatMessageKind.GIFT_PURCHASE ||
            message.kind == ChatMessageKind.GIFT_REDEMPTION -> BandItem(message, MembershipColor)
        else -> TextItem(message, display)
    }
}

@Composable
private fun TextItem(
    message: ChatMessage,
    display: DisplaySettings,
) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        if (display.showTime) TimeText(message, SubTextColor)
        if (display.showAuthorIcon) AuthorIcon(message.author.photoUrl)
        if (display.showAuthorName) {
            Text(
                text = message.author.name,
                color = authorColor(message.author.roles),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(AUTHOR_WEIGHT, fill = false).padding(end = 6.dp),
            )
        }
        MessageText(message.runs, color = OverlayTextColor, fontSize = 13.sp)
    }
}

/** 時刻（F-VIEW-01）。リプレイは動画内の位置、ライブ・プレミアは投稿時刻 */
@Composable
private fun TimeText(
    message: ChatMessage,
    color: Color,
) {
    Text(
        text = OverlayFormat.messageTime(message),
        color = color,
        fontSize = 11.sp,
        maxLines = 1,
        modifier = Modifier.padding(end = 4.dp),
    )
}

/** 投稿者のアイコン（F-VIEW-01）。許可した配信元の画像だけを読み込み、無い・失敗した時は何も出さない */
@Composable
private fun AuthorIcon(url: String?) {
    if (url == null || !ImagePolicy.isAllowed(url)) return
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier.padding(end = 4.dp).size(ICON_SIZE).clip(CircleShape),
    )
}

/** スーパーチャット・スーパーステッカーは金額と色帯で強調する（F-VIEW-02）。 */
@Composable
private fun PaidItem(
    message: ChatMessage,
    display: DisplaySettings,
) {
    val paid = message.paid ?: return
    val body = Color(paid.bodyColorArgb ?: DEFAULT_PAID_ARGB)
    val header = Color(paid.headerColorArgb ?: paid.bodyColorArgb ?: DEFAULT_PAID_ARGB)
    val textColor =
        if (OverlayFormat.prefersDarkText(
                paid.bodyColorArgb ?: DEFAULT_PAID_ARGB,
            )
        ) {
            Color.Black
        } else {
            Color.White
        }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(4.dp))) {
        Row(modifier = Modifier.fillMaxWidth().background(header).padding(horizontal = 6.dp, vertical = 2.dp)) {
            if (display.showTime) TimeText(message, textColor)
            // 金額の帯は誰の支援かが要点のため、投稿者名は表示設定によらず出す
            Text(message.author.name, color = textColor, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
            Text(paid.amountText, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        val bodyModifier = Modifier.fillMaxWidth().background(body).padding(horizontal = 6.dp, vertical = 3.dp)
        when {
            message.kind == ChatMessageKind.SUPER_STICKER -> Sticker(paid.stickerImageUrl, textColor, bodyModifier)
            message.runs.isNotEmpty() -> MessageText(message.runs, textColor, 13.sp, bodyModifier)
        }
    }
}

@Composable
private fun BandItem(
    message: ChatMessage,
    color: Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(4.dp)).background(color),
    ) {
        Text(
            text = listOfNotNull(message.author.name.takeIf { it.isNotEmpty() }, message.headerText).joinToString("  "),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
        if (message.runs.isNotEmpty()) {
            MessageText(
                message.runs,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** スーパーステッカーの画像（F-CHAT-09）。表示できない時は「（スーパーステッカー）」と出す。 */
@Composable
private fun Sticker(
    url: String?,
    textColor: Color,
    modifier: Modifier,
) {
    val fallback = @Composable { Text(stringResource(R.string.overlay_sticker), color = textColor, fontSize = 13.sp) }
    Box(modifier = modifier) {
        if (url != null && ImagePolicy.isAllowed(url)) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = stringResource(R.string.overlay_sticker),
                modifier = Modifier.size(STICKER_SIZE),
                error = { fallback() },
            )
        } else {
            fallback()
        }
    }
}

private fun authorColor(roles: Set<AuthorRole>): Color =
    when {
        AuthorRole.OWNER in roles -> OwnerColor
        AuthorRole.MODERATOR in roles -> ModeratorColor
        AuthorRole.MEMBER in roles -> MemberColor
        else -> SubTextColor
    }

private const val AUTHOR_WEIGHT = 0.4f
private val STICKER_SIZE = 56.dp
private val ICON_SIZE = 18.dp
private const val DEFAULT_PAID_ARGB = 0xFF1E88E5.toInt()
