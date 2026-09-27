package io.github.filderschoice.romcha.core.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoUrlParserTest {
    private val id = "dQw4w9WgXcQ"

    @Test
    fun 受理する各形式から動画IDを取り出す() {
        listOf(
            "https://youtu.be/$id",
            "https://youtu.be/$id?si=abcdef&t=30",
            "https://www.youtube.com/watch?v=$id",
            "https://m.youtube.com/watch?feature=share&v=$id&t=1m2s",
            "http://youtube.com/watch?v=$id",
            "https://www.youtube.com/live/$id?si=xyz",
            "https://youtube.com/shorts/$id",
            "youtu.be/$id",
            "www.youtube.com/watch?v=$id",
            "HTTPS://WWW.YOUTUBE.COM/watch?v=$id",
        ).forEach { assertEquals(it, id, VideoUrlParser.extractVideoId(it)) }
    }

    @Test
    fun 共有テキストに含まれるURLから取り出す() {
        val shared = "【アーカイブ】配信のタイトル\nhttps://youtube.com/live/$id?si=abc"
        assertEquals(id, VideoUrlParser.extractVideoId(shared))
    }

    @Test
    fun 動画以外のURLや不正な入力はnullを返す() {
        listOf(
            "",
            "こんにちは",
            id,
            "https://www.youtube.com/@channel",
            "https://www.youtube.com/playlist?list=PL0123456789",
            "https://www.youtube.com/watch?v=short",
            "https://www.youtube.com/watch?list=PL0&vv=$id",
            "https://example.com/watch?v=$id",
            "https://youtube.com.evil.example/watch?v=$id",
            "https://youtu.be/",
        ).forEach { assertNull(it, VideoUrlParser.extractVideoId(it)) }
    }
}
