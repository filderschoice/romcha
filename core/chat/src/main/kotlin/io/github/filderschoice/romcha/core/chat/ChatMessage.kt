package io.github.filderschoice.romcha.core.chat

/** チャットメッセージの種別（F-CHAT-08）。 */
enum class ChatMessageKind {
    /** 通常のテキストメッセージ */
    TEXT,

    /** スーパーチャット */
    SUPER_CHAT,

    /** スーパーステッカー */
    SUPER_STICKER,

    /** メンバー加入・継続（マイルストーン） */
    MEMBERSHIP,

    /** メンバーシップギフトの購入 */
    GIFT_PURCHASE,

    /** メンバーシップギフトの受け取り */
    GIFT_REDEMPTION,
}

/** 投稿者の役割。フィルタ（F-VIEW-03）と表示の強調に使う。 */
enum class AuthorRole {
    OWNER,
    MODERATOR,
    MEMBER,
    VERIFIED,
}

data class ChatAuthor(
    val name: String,
    val channelId: String?,
    val photoUrl: String?,
    val roles: Set<AuthorRole>,
)

/** メッセージ本文の断片。テキストと絵文字（カスタム絵文字・メンバースタンプを含む）が混在する。 */
sealed interface MessageRun {
    data class Text(
        val text: String,
    ) : MessageRun

    /**
     * @property alt 画像が使えない時の代替テキスト（ショートカット `:name:` または絵文字そのもの。F-CHAT-09）
     */
    data class Emoji(
        val id: String,
        val alt: String,
        val imageUrl: String?,
        val isCustom: Boolean,
    ) : MessageRun
}

/**
 * スーパーチャット・スーパーステッカーの金額と色（F-VIEW-02）。
 *
 * 色は ARGB（0xAARRGGBB）。応答に無い場合は null。
 */
data class PaidInfo(
    val amountText: String,
    val bodyColorArgb: Int?,
    val headerColorArgb: Int?,
    val stickerImageUrl: String?,
)

/**
 * チャットメッセージ。
 *
 * @property timestampUsec 投稿時刻（UNIX 時刻・マイクロ秒）
 * @property videoOffsetMs 動画内オフセット（リプレイのみ。ライブ中は null）
 * @property headerText メンバー加入・ギフト等の見出し文（例「メンバー歴 3 か月」）
 */
data class ChatMessage(
    val id: String,
    val kind: ChatMessageKind,
    val author: ChatAuthor,
    val runs: List<MessageRun>,
    val timestampUsec: Long,
    val videoOffsetMs: Long?,
    val paid: PaidInfo? = null,
    val headerText: String? = null,
) {
    /** 本文をプレーンテキストにしたもの（絵文字は代替テキスト）。NG ワード判定・アクセシビリティ用。 */
    val plainText: String
        get() =
            runs.joinToString("") {
                when (it) {
                    is MessageRun.Text -> it.text
                    is MessageRun.Emoji -> it.alt
                }
            }
}
