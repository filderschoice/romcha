package io.github.filderschoice.romcha.core.sync

import io.github.filderschoice.romcha.core.chat.ChatAuthor
import io.github.filderschoice.romcha.core.chat.ChatMessage
import io.github.filderschoice.romcha.core.chat.ChatMessageKind
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveTimelineTest {
    private fun received(
        id: String,
        atMs: Long,
    ) = ReceivedMessage(
        ChatMessage(id, ChatMessageKind.TEXT, ChatAuthor("視聴者", null, null, emptySet()), emptyList(), 0, null),
        receivedAtMs = atMs,
    )

    private val messages = listOf(received("a", 1_000), received("b", 2_000), received("c", 3_000))

    @Test
    fun 遅延0なら受信したものをすべて表示する() {
        assertEquals(listOf("a", "b", "c"), LiveTimeline.visible(messages, nowMs = 3_000, delayMs = 0).map { it.id })
    }

    @Test
    fun 受信から表示遅延だけ経ったものを表示する() {
        assertEquals(listOf("a", "b"), LiveTimeline.visible(messages, nowMs = 7_000, delayMs = 5_000).map { it.id })
        assertEquals(emptyList<String>(), LiveTimeline.visible(messages, nowMs = 5_999, delayMs = 5_000).map { it.id })
    }

    @Test
    fun 表示保持件数の上限を超えたら新しいものを残す() {
        assertEquals(
            listOf("b", "c"),
            LiveTimeline.visible(messages, nowMs = 3_000, delayMs = 0, maxVisible = 2).map { it.id },
        )
    }

    @Test
    fun 表示遅延は0秒から30秒に収める() {
        assertEquals(0L, LiveTimeline.delayMs(-5))
        assertEquals(12_000L, LiveTimeline.delayMs(12))
        assertEquals(30_000L, LiveTimeline.delayMs(99))
    }
}
