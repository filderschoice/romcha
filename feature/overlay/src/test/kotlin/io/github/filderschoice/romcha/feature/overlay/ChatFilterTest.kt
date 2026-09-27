package io.github.filderschoice.romcha.feature.overlay

import io.github.filderschoice.romcha.core.chat.AuthorRole
import io.github.filderschoice.romcha.core.chat.ChatAuthor
import io.github.filderschoice.romcha.core.chat.ChatMessage
import io.github.filderschoice.romcha.core.chat.ChatMessageKind
import io.github.filderschoice.romcha.core.chat.MessageRun
import io.github.filderschoice.romcha.core.chat.PaidInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ChatFilterTest {
    private val normal = message("normal", "こんにちは")
    private val paid =
        message("paid", "応援しています", paid = PaidInfo("¥500", null, null, null), kind = ChatMessageKind.SUPER_CHAT)
    private val member = message("member", "メンバーです", roles = setOf(AuthorRole.MEMBER))
    private val joined = message("joined", "", kind = ChatMessageKind.MEMBERSHIP)
    private val moderator = message("moderator", "ルールを守ってください", roles = setOf(AuthorRole.MODERATOR))
    private val owner = message("owner", "ありがとう", roles = setOf(AuthorRole.OWNER))
    private val all = listOf(normal, paid, member, joined, moderator, owner)

    @Test
    fun 何も設定しなければそのまま返す() {
        assertSame(all, ChatFilter.apply(all, DisplaySettings()))
    }

    @Test
    fun スパチャのみ() {
        assertEquals(listOf("paid"), ids(DisplaySettings(onlyPaid = true)))
    }

    @Test
    fun メンバーのみはメンバーの投稿と加入の通知を出す() {
        assertEquals(listOf("member", "joined"), ids(DisplaySettings(onlyMembers = true)))
    }

    @Test
    fun モデレーター配信者のみ() {
        assertEquals(listOf("moderator", "owner"), ids(DisplaySettings(onlyModerators = true)))
    }

    @Test
    fun 複数オンにしたらいずれかに当てはまるものを出す() {
        assertEquals(listOf("paid", "moderator", "owner"), ids(DisplaySettings(onlyPaid = true, onlyModerators = true)))
    }

    @Test
    fun NGワードを含むものは大文字小文字を区別せず除く() {
        val spam = message("spam", "Buy NOW")
        val result = ChatFilter.apply(all + spam, DisplaySettings(ngWords = listOf("now", "ルール")))
        assertEquals(listOf("normal", "paid", "member", "joined", "owner"), result.map { it.id })
    }

    @Test
    fun NGワードの入力は空行と重複を捨てて上限に収める() {
        assertEquals(listOf("abc", "あいう"), ChatFilter.parseNgWords(" abc \n\nABC\nあいう\n"))
        assertEquals(ChatFilter.MAX_NG_WORDS, ChatFilter.parseNgWords((1..200).joinToString("\n")).size)
        assertEquals(ChatFilter.MAX_NG_WORD_LENGTH, ChatFilter.parseNgWords("x".repeat(80)).single().length)
    }

    @Test
    fun 表示保持件数は範囲内に収めて100件刻みに丸める() {
        assertEquals(DisplaySettings.MIN_VISIBLE, DisplaySettings.clampVisible(0))
        assertEquals(DisplaySettings.MAX_VISIBLE, DisplaySettings.clampVisible(5_000))
        assertEquals(300, DisplaySettings.clampVisible(349))
        assertEquals(400, DisplaySettings.clampVisible(350))
        assertEquals(500, DisplaySettings().maxVisible)
    }

    @Test
    fun テーマの既定はシステム追従() {
        assertEquals(ThemeMode.SYSTEM, DisplaySettings().theme)
    }

    private fun ids(settings: DisplaySettings) = ChatFilter.apply(all, settings).map { it.id }

    private fun message(
        id: String,
        text: String,
        roles: Set<AuthorRole> = emptySet(),
        paid: PaidInfo? = null,
        kind: ChatMessageKind = ChatMessageKind.TEXT,
    ) = ChatMessage(
        id = id,
        kind = kind,
        author = ChatAuthor(id, null, null, roles),
        runs = listOf(MessageRun.Text(text)),
        timestampUsec = 0,
        videoOffsetMs = 0,
        paid = paid,
    )
}
