package io.github.filderschoice.romcha.core.chat

import io.github.filderschoice.romcha.core.chat.internal.arr
import io.github.filderschoice.romcha.core.chat.internal.bool
import io.github.filderschoice.romcha.core.chat.internal.long
import io.github.filderschoice.romcha.core.chat.internal.obj
import io.github.filderschoice.romcha.core.chat.internal.str
import io.github.filderschoice.romcha.core.chat.internal.text
import io.github.filderschoice.romcha.core.chat.internal.thumbnailUrl
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** 次の取得に使う継続トークンの種類。 */
enum class ContinuationKind {
    /** リプレイの続き（`liveChatReplayContinuationData`） */
    REPLAY,

    /** ライブの定期取得（`timedContinuationData`） */
    TIMED,

    /** ライブの更新通知型（`invalidationContinuationData`）。ポーリングで扱う */
    INVALIDATION,

    /** 取り直し（`reloadContinuationData`） */
    RELOAD,
}

/**
 * @property timeoutMs 次の取得までの推奨待ち時間（ライブのみ。F-CHAT-04）
 */
data class ChatContinuation(
    val token: String,
    val kind: ContinuationKind,
    val timeoutMs: Long?,
)

sealed interface ChatParseResult {
    /**
     * @property continuation 次の取得に使うトークン。null ならチャットの続きが無い（リプレイ終端・ライブ終了。F-CHAT-06）
     * @property skipped 未知・表示対象外の種別として読み飛ばした件数（N-08）
     * @property allChatToken チャット欄の見出しにある「すべてのチャット」の continuation。見出しが無い応答では null
     */
    data class Success(
        val messages: List<ChatMessage>,
        val continuation: ChatContinuation?,
        val skipped: Int,
        val allChatToken: String? = null,
    ) : ChatParseResult

    /** 応答の構造が想定と異なり解析できなかった（N-08。落とさずに画面へ表示する）。 */
    data class Failure(
        val reason: String,
    ) : ChatParseResult
}

/**
 * `live_chat/get_live_chat_replay` と `live_chat/get_live_chat` の応答を解析する（F-CHAT-01、F-CHAT-08）。
 *
 * 応答は必要な箇所だけを辿り、想定外の構造でも例外を投げずに [ChatParseResult.Failure] を返す。
 */
object ChatResponseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): ChatParseResult {
        val root =
            try {
                json.parseToJsonElement(body)
            } catch (e: SerializationException) {
                return ChatParseResult.Failure("JSON として読めない: ${e.message?.take(MAX_REASON_LENGTH)}")
            }
        val continuation =
            root.obj("continuationContents").obj("liveChatContinuation")
                ?: return ChatParseResult.Failure("liveChatContinuation が無い")
        return parseContinuation(continuation)
    }

    internal fun parseContinuation(node: JsonObject): ChatParseResult.Success {
        val messages = ArrayList<ChatMessage>()
        var skipped = 0
        val collect = { item: JsonObject?, offset: Long? ->
            if (item != null) {
                val message = parseItem(item, offset)
                if (message != null) messages += message else skipped++
            }
        }
        node.arr("actions")?.forEach { action ->
            val replay = action.obj("replayChatItemAction")
            if (replay != null) {
                val offset = replay.long("videoOffsetTimeMsec")
                replay.arr("actions")?.forEach { collect(chatItemOf(it), offset) }
            } else {
                collect(chatItemOf(action), null)
            }
        }
        return ChatParseResult.Success(messages, parseNextContinuation(node), skipped, parseAllChatToken(node))
    }

    /**
     * 見出しの表示切り替え（0 = 上位チャット、1 = すべてのチャット）から「すべてのチャット」の continuation を読む。
     *
     * `next` 応答の同じ位置にあるトークンは動画IDを含まない雛形で、送ると HTTP 400 になる（2026-09-27 実機検証）。
     * チャット取得の応答側のトークンは動画IDを含み、そのまま使える。
     */
    private fun parseAllChatToken(node: JsonObject): String? =
        node
            .obj("header")
            .obj("liveChatHeaderRenderer")
            .obj("viewSelector")
            .obj("sortFilterSubMenuRenderer")
            .arr("subMenuItems")
            ?.getOrNull(ALL_CHAT_INDEX)
            .obj("continuation")
            .obj("reloadContinuationData")
            .str("continuation")

    /** チャット項目の追加（`addChatItemAction`）の項目。ティッカー・バナー等の他のアクションは null。 */
    private fun chatItemOf(action: JsonElement): JsonObject? = action.obj("addChatItemAction").obj("item")

    /** 表示対象外・未知の種別、必須項目の欠けた項目は null（読み飛ばす。N-08）。 */
    private fun parseItem(
        item: JsonObject,
        videoOffsetMs: Long?,
    ): ChatMessage? {
        val (rendererName, renderer) = item.entries.firstOrNull() ?: return null
        if (renderer !is JsonObject) return null
        val kind = KIND_BY_RENDERER[rendererName] ?: return null
        val id = renderer.str("id") ?: return null
        return ChatMessage(
            id = id,
            kind = kind,
            author = parseAuthor(renderer),
            runs = parseRuns(renderer.obj("message")),
            timestampUsec = renderer.long("timestampUsec") ?: 0,
            videoOffsetMs = videoOffsetMs,
            paid = parsePaid(renderer, kind),
            headerText = parseHeader(renderer, kind),
        )
    }

    private fun parseAuthor(renderer: JsonObject): ChatAuthor {
        val header = renderer.obj("header").obj("liveChatSponsorshipsHeaderRenderer")
        val source = header ?: renderer
        val roles = mutableSetOf<AuthorRole>()
        source.arr("authorBadges")?.forEach { badge ->
            val badgeRenderer = badge.obj("liveChatAuthorBadgeRenderer")
            when (badgeRenderer.obj("icon").str("iconType")) {
                "OWNER" -> roles += AuthorRole.OWNER
                "MODERATOR" -> roles += AuthorRole.MODERATOR
                "VERIFIED", "CHECK_CIRCLE_THICK" -> roles += AuthorRole.VERIFIED
                else -> if (badgeRenderer.obj("customThumbnail") != null) roles += AuthorRole.MEMBER
            }
        }
        return ChatAuthor(
            name = source.text("authorName").orEmpty(),
            channelId = renderer.str("authorExternalChannelId"),
            photoUrl = source.thumbnailUrl("authorPhoto"),
            roles = roles,
        )
    }

    private fun parsePaid(
        renderer: JsonObject,
        kind: ChatMessageKind,
    ): PaidInfo? {
        if (kind != ChatMessageKind.SUPER_CHAT && kind != ChatMessageKind.SUPER_STICKER) return null
        return PaidInfo(
            amountText = renderer.text("purchaseAmountText").orEmpty(),
            bodyColorArgb = (renderer.long("bodyBackgroundColor") ?: renderer.long("backgroundColor"))?.toInt(),
            headerColorArgb = renderer.long("headerBackgroundColor")?.toInt(),
            stickerImageUrl = renderer.thumbnailUrl("sticker"),
        )
    }

    private fun parseHeader(
        renderer: JsonObject,
        kind: ChatMessageKind,
    ): String? =
        when (kind) {
            ChatMessageKind.MEMBERSHIP -> renderer.text("headerPrimaryText") ?: renderer.text("headerSubtext")
            ChatMessageKind.GIFT_PURCHASE ->
                renderer.obj("header").obj("liveChatSponsorshipsHeaderRenderer").text("primaryText")
            else -> null
        }

    private fun parseRuns(message: JsonObject?): List<MessageRun> {
        message ?: return emptyList()
        message.str("simpleText")?.let { return listOf(MessageRun.Text(it)) }
        return message.arr("runs").orEmpty().mapNotNull { run ->
            run.str("text")?.let { return@mapNotNull MessageRun.Text(it) }
            val emoji = run.obj("emoji") ?: return@mapNotNull null
            val isCustom = emoji.bool("isCustomEmoji") ?: false
            val shortcut = (emoji.arr("shortcuts")?.firstOrNull() as? JsonPrimitive)?.content
            val id = emoji.str("emojiId").orEmpty()
            MessageRun.Emoji(
                id = id,
                alt = if (isCustom) shortcut ?: id else id.ifEmpty { shortcut.orEmpty() },
                imageUrl = emoji.thumbnailUrl("image"),
                isCustom = isCustom,
            )
        }
    }

    private fun parseNextContinuation(node: JsonObject): ChatContinuation? {
        val entries = node.arr("continuations").orEmpty()
        return CONTINUATION_KEYS.firstNotNullOfOrNull { (key, kind) ->
            val data = entries.firstNotNullOfOrNull { it.obj(key) }
            data.str("continuation")?.let { ChatContinuation(it, kind, data.long("timeoutMs")) }
        }
    }

    private const val MAX_REASON_LENGTH = 120
    private const val ALL_CHAT_INDEX = 1

    private val KIND_BY_RENDERER =
        mapOf(
            "liveChatTextMessageRenderer" to ChatMessageKind.TEXT,
            "liveChatPaidMessageRenderer" to ChatMessageKind.SUPER_CHAT,
            "liveChatPaidStickerRenderer" to ChatMessageKind.SUPER_STICKER,
            "liveChatMembershipItemRenderer" to ChatMessageKind.MEMBERSHIP,
            "liveChatSponsorshipsGiftPurchaseAnnouncementRenderer" to ChatMessageKind.GIFT_PURCHASE,
            "liveChatSponsorshipsGiftRedemptionAnnouncementRenderer" to ChatMessageKind.GIFT_REDEMPTION,
        )

    private val CONTINUATION_KEYS =
        listOf(
            "liveChatReplayContinuationData" to ContinuationKind.REPLAY,
            "timedContinuationData" to ContinuationKind.TIMED,
            "invalidationContinuationData" to ContinuationKind.INVALIDATION,
            "reloadContinuationData" to ContinuationKind.RELOAD,
        )
}
