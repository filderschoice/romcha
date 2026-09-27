package io.github.filderschoice.romcha.core.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 応答解析のテスト（N-10）。
 *
 * fixture は既知の応答構造に基づいて作成した合成データで、実応答との一致確認は人手検証（BACKLOG BL-022）で行う。
 */
class ChatResponseParserTest {
    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/$name")) { "fixture が無い: $name" }.readText()

    private fun parseSuccess(name: String): ChatParseResult.Success {
        val result = ChatResponseParser.parse(fixture(name))
        assertTrue("解析に失敗: $result", result is ChatParseResult.Success)
        return result as ChatParseResult.Success
    }

    @Test
    fun リプレイ応答から動画内オフセット付きのメッセージを読む() {
        val result = parseSuccess("replay_chunk.json")

        assertEquals(listOf("msg1", "msg2", "sc1", "st1", "mb1", "gf1"), result.messages.map { it.id })
        assertEquals(listOf(1000L, 2000L, 3000L, 4000L, 5000L, 6000L), result.messages.map { it.videoOffsetMs })
        assertEquals(ChatContinuation("REPLAY_NEXT_TOKEN", ContinuationKind.REPLAY, null), result.continuation)
    }

    @Test
    fun 未知の種別と必須項目の欠けた項目は読み飛ばして件数を数える() {
        val result = parseSuccess("replay_chunk.json")
        // システムメッセージ（表示対象外）と id の無い項目の2件。ティッカーはチャット項目ではないので数えない
        assertEquals(2, result.skipped)
    }

    @Test
    fun 通常メッセージの本文と絵文字と投稿者を読む() {
        val message = parseSuccess("replay_chunk.json").messages.first { it.id == "msg1" }

        assertEquals(ChatMessageKind.TEXT, message.kind)
        assertEquals("視聴者A", message.author.name)
        assertEquals("https://yt4.ggpht.com/a/large=s64", message.author.photoUrl)
        assertEquals(setOf(AuthorRole.MODERATOR), message.author.roles)
        assertEquals("こんにちは 😀", message.plainText)
        assertEquals(1_700_000_000_000_000L, message.timestampUsec)
    }

    @Test
    fun カスタム絵文字はショートカットを代替テキストにしメンバーと所有者を判定する() {
        val message = parseSuccess("replay_chunk.json").messages.first { it.id == "msg2" }
        val emoji = message.runs.single() as MessageRun.Emoji

        assertTrue(emoji.isCustom)
        assertEquals(":_stamp:", emoji.alt)
        assertEquals("https://yt3.ggpht.com/stamp", emoji.imageUrl)
        assertEquals(setOf(AuthorRole.MEMBER, AuthorRole.OWNER), message.author.roles)
    }

    @Test
    fun スーパーチャットの金額と色を読む() {
        val message = parseSuccess("replay_chunk.json").messages.first { it.id == "sc1" }

        assertEquals(ChatMessageKind.SUPER_CHAT, message.kind)
        assertEquals("¥1,000", message.paid?.amountText)
        assertEquals(0xFF1DE9B6.toInt(), message.paid?.bodyColorArgb)
        assertEquals(0xFF00BFA5.toInt(), message.paid?.headerColorArgb)
        assertEquals("応援しています", message.plainText)
    }

    @Test
    fun スーパーステッカーの金額と色と画像を読む() {
        val message = parseSuccess("replay_chunk.json").messages.first { it.id == "st1" }

        assertEquals(ChatMessageKind.SUPER_STICKER, message.kind)
        assertEquals("¥200", message.paid?.amountText)
        assertEquals(0xFF1E88E5.toInt(), message.paid?.bodyColorArgb)
        assertEquals("https://lh3.googleusercontent.com/sticker", message.paid?.stickerImageUrl)
    }

    @Test
    fun メンバー加入とギフトの見出しを読む() {
        val messages = parseSuccess("replay_chunk.json").messages

        val membership = messages.first { it.id == "mb1" }
        assertEquals(ChatMessageKind.MEMBERSHIP, membership.kind)
        assertEquals("メンバー歴 3 か月", membership.headerText)
        assertEquals("いつもありがとう", membership.plainText)

        val gift = messages.first { it.id == "gf1" }
        assertEquals(ChatMessageKind.GIFT_PURCHASE, gift.kind)
        assertEquals("視聴者F", gift.author.name)
        assertEquals("5 件のメンバーシップ ギフトを贈りました", gift.headerText)
    }

    @Test
    fun ライブ応答は推奨間隔付きの継続トークンを返しオフセットを持たない() {
        val result = parseSuccess("live_chunk.json")

        assertEquals(ChatContinuation("LIVE_NEXT_TOKEN", ContinuationKind.TIMED, 5000), result.continuation)
        assertNull(result.messages.single().videoOffsetMs)
    }

    @Test
    fun 継続トークンが無ければ終了とみなせる() {
        assertNull(parseSuccess("live_ended.json").continuation)
    }

    @Test
    fun 想定外の構造や壊れたJSONでは例外を投げず失敗を返す() {
        assertTrue(ChatResponseParser.parse("""{"responseContext":{}}""") is ChatParseResult.Failure)
        assertTrue(ChatResponseParser.parse("<html>") is ChatParseResult.Failure)
        assertTrue(ChatResponseParser.parse("""{"continuationContents":[]}""") is ChatParseResult.Failure)
    }
}
