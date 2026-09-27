package io.github.filderschoice.romcha.feature.overlay.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import coil.compose.SubcomposeAsyncImage
import io.github.filderschoice.romcha.core.chat.MessageRun
import io.github.filderschoice.romcha.feature.overlay.ImagePolicy

/**
 * メッセージ本文。カスタム絵文字・メンバースタンプは画像で文中に差し込む（F-CHAT-09）。
 *
 * 画像の URL が無い・許可していない配信元・読み込みに失敗した場合は代替テキスト（`:name:` など）を出す。
 * Unicode の絵文字は文字のまま表示する。
 */
@Composable
internal fun MessageText(
    runs: List<MessageRun>,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val images = runs.filterIsInstance<MessageRun.Emoji>().filter { imageOf(it) != null }.distinctBy { it.id }
    val text =
        buildAnnotatedString {
            runs.forEach { run ->
                when {
                    run is MessageRun.Text -> append(run.text)
                    run is MessageRun.Emoji && imageOf(run) != null -> appendInlineContent(run.id, run.alt)
                    run is MessageRun.Emoji -> append(run.alt)
                }
            }
        }
    val inline =
        images.associate { emoji ->
            emoji.id to
                InlineTextContent(Placeholder(EMOJI_SIZE, EMOJI_SIZE, PlaceholderVerticalAlign.TextCenter)) {
                    EmojiImage(emoji, color)
                }
        }
    Text(text = text, inlineContent = inline, color = color, fontSize = fontSize, modifier = modifier)
}

@Composable
private fun EmojiImage(
    emoji: MessageRun.Emoji,
    color: Color,
) {
    SubcomposeAsyncImage(
        model = imageOf(emoji),
        contentDescription = emoji.alt,
        modifier = Modifier.fillMaxSize(),
        error = { AltText(emoji.alt, color) },
    )
}

@Composable
private fun AltText(
    alt: String,
    color: Color,
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(alt, color = color, fontSize = ALT_SIZE, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

/** 画像で表示するのはカスタム絵文字・メンバースタンプのうち、許可した配信元の画像だけ */
private fun imageOf(emoji: MessageRun.Emoji): String? =
    emoji.imageUrl?.takeIf { emoji.isCustom && ImagePolicy.isAllowed(it) }

private val EMOJI_SIZE = 1.4.em
private val ALT_SIZE = 0.5.em
