package io.github.filderschoice.romcha.core.chat

import io.github.filderschoice.romcha.core.chat.resolve.SearchResultParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test

/**
 * YouTube の実応答から作った fixture（`fixtures/real/`）で各 Parser を検証する（BL-022）。
 *
 * fixture は `scripts/fetch-real-fixtures.py` で作る（投稿者の情報は置き換え済み）。内容は取得した動画で変わるため、
 * 値ではなく構造（解析できること・必要な値があること）だけを確かめる。fixture が無い場合はスキップする。
 */
class RealResponseFixtureTest {
    private fun realFixture(name: String): String {
        val text = javaClass.getResource("/fixtures/real/$name")?.readText()
        assumeNotNull(text)
        return requireNotNull(text)
    }

    private fun chat(name: String): ChatParseResult.Success {
        val result = ChatResponseParser.parse(realFixture(name))
        assertTrue("解析に失敗: $result", result is ChatParseResult.Success)
        return result as ChatParseResult.Success
    }

    private fun watch(name: String): VideoChatInfo.Available {
        val info = WatchInfoParser.parse("realvideo01", realFixture(name))
        assertTrue("チャットのある動画として読めない: $info", info is VideoChatInfo.Available)
        return info as VideoChatInfo.Available
    }

    @Test
    fun アーカイブのnext応答からタイトルとチャット欄のcontinuationを読む() {
        val info = watch("next_replay.json")

        assertTrue(info.isReplay)
        assertTrue(info.title.isNotEmpty())
        assertTrue(info.channelName.isNotEmpty())
        assertTrue(info.topChatToken.isNotEmpty())
    }

    @Test
    fun リプレイ応答からオフセット付きのメッセージと次の取得とすべてのチャットの継続トークンを読む() {
        val result = chat("replay_chunk.json")

        assertTrue(result.messages.isNotEmpty())
        assertTrue(result.messages.all { it.videoOffsetMs != null })
        assertEquals(ContinuationKind.REPLAY, result.continuation?.kind)
        assertNotNull(result.allChatToken)
    }

    @Test
    fun 配信中のnext応答はライブとして読む() {
        val info = watch("next_live.json")

        assertFalse(info.isReplay)
        assertTrue(info.topChatToken.isNotEmpty())
    }

    @Test
    fun ライブ応答から次の取得の継続トークンと推奨間隔とすべてのチャットの継続トークンを読む() {
        val result = chat("live_chunk.json")

        assertNotNull(result.continuation)
        assertTrue(result.messages.all { it.videoOffsetMs == null })
        assertNotNull(result.allChatToken)
    }

    @Test
    fun 検索応答から動画の候補を読む() {
        val candidates = requireNotNull(SearchResultParser.parse(realFixture("search_results.json")))

        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.all { it.videoId.length == VIDEO_ID_LENGTH && it.title.isNotEmpty() })
    }

    private companion object {
        const val VIDEO_ID_LENGTH = 11
    }
}
