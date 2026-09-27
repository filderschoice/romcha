package io.github.filderschoice.romcha.feature.overlay

import io.github.filderschoice.romcha.core.chat.AuthorRole
import io.github.filderschoice.romcha.core.chat.ChatMessage
import io.github.filderschoice.romcha.core.chat.ChatMessageKind

/**
 * 表示するメッセージの絞り込み（F-VIEW-03。Android 非依存）。
 *
 * - 「スパチャのみ」「メンバーのみ」「モデレーター・配信者のみ」は、オンにしたもののいずれかに当てはまるメッセージだけを出す
 *   （どれもオフなら絞り込まない）。
 * - NG ワードを本文に含むメッセージは、上記によらず出さない（大文字・小文字を区別しない）。
 */
object ChatFilter {
    /** NG ワードの数と 1 語の長さの上限（入力の取り込み時に切り詰める） */
    const val MAX_NG_WORDS = 100
    const val MAX_NG_WORD_LENGTH = 50

    fun apply(
        messages: List<ChatMessage>,
        settings: DisplaySettings,
    ): List<ChatMessage> {
        val categories = settings.onlyPaid || settings.onlyMembers || settings.onlyModerators
        if (!categories && settings.ngWords.isEmpty()) return messages
        return messages.filter { message ->
            (!categories || matchesCategory(message, settings)) && !containsNgWord(message, settings.ngWords)
        }
    }

    /** 入力（1 行 1 語）を NG ワードの一覧にする。前後の空白を除き、空行・重複を捨て、数と長さを上限に収める。 */
    fun parseNgWords(text: String): List<String> =
        text
            .lines()
            .map { it.trim().take(MAX_NG_WORD_LENGTH) }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(MAX_NG_WORDS)

    private fun matchesCategory(
        message: ChatMessage,
        settings: DisplaySettings,
    ): Boolean {
        val roles = message.author.roles
        val paid = message.paid != null
        val member =
            AuthorRole.MEMBER in roles ||
                message.kind == ChatMessageKind.MEMBERSHIP ||
                message.kind == ChatMessageKind.GIFT_PURCHASE ||
                message.kind == ChatMessageKind.GIFT_REDEMPTION
        val moderator = AuthorRole.MODERATOR in roles || AuthorRole.OWNER in roles
        return (settings.onlyPaid && paid) || (settings.onlyMembers && member) || (settings.onlyModerators && moderator)
    }

    private fun containsNgWord(
        message: ChatMessage,
        ngWords: List<String>,
    ): Boolean {
        if (ngWords.isEmpty()) return false
        val text = message.plainText
        return ngWords.any { text.contains(it, ignoreCase = true) }
    }
}
