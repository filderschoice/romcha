package io.github.filderschoice.romcha.core.chat

import io.github.filderschoice.romcha.core.chat.internal.arr
import io.github.filderschoice.romcha.core.chat.internal.bool
import io.github.filderschoice.romcha.core.chat.internal.obj
import io.github.filderschoice.romcha.core.chat.internal.str
import io.github.filderschoice.romcha.core.chat.internal.text
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** 動画のチャット提供状況（`next` 応答から得る）。 */
sealed interface VideoChatInfo {
    val videoId: String
    val title: String
    val channelName: String

    /**
     * チャットを取得できる。
     *
     * @property isReplay true ならアーカイブのリプレイ、false ならライブ・プレミア（公開中・待機中）
     * @property topChatToken 「上位チャット」の continuation（F-CHAT-07）
     * @property allChatToken 「すべてのチャット」の continuation。無い場合は [topChatToken] を使う
     */
    data class Available(
        override val videoId: String,
        override val title: String,
        override val channelName: String,
        val isReplay: Boolean,
        val topChatToken: String,
        val allChatToken: String?,
    ) : VideoChatInfo

    /**
     * チャットが無効・存在しない（F-VID-07）。
     *
     * @property message 応答に含まれる説明文（例「この動画ではチャットのリプレイを利用できません」）。無ければ null
     */
    data class Unavailable(
        override val videoId: String,
        override val title: String,
        override val channelName: String,
        val message: String?,
    ) : VideoChatInfo
}

/** `youtubei/v1/next` 応答から動画のタイトル・チャンネル名とチャットの continuation を読む（PLAN 4.4）。 */
object WatchInfoParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** 解析できた場合は [VideoChatInfo]、応答の構造が想定と異なる場合は null。 */
    fun parse(
        videoId: String,
        body: String,
    ): VideoChatInfo? {
        val root =
            try {
                json.parseToJsonElement(body)
            } catch (ignored: SerializationException) {
                null
            }
        return root.obj("contents").obj("twoColumnWatchNextResults")?.let { parseWatch(videoId, it) }
    }

    private fun parseWatch(
        videoId: String,
        watch: JsonObject,
    ): VideoChatInfo {
        val contents = watch.obj("results").obj("results").arr("contents").orEmpty()
        val primary = contents.firstNotNullOfOrNull { it.obj("videoPrimaryInfoRenderer") }
        val secondary = contents.firstNotNullOfOrNull { it.obj("videoSecondaryInfoRenderer") }
        val title = primary.text("title").orEmpty()
        val channel = secondary.obj("owner").obj("videoOwnerRenderer").text("title").orEmpty()
        val chat = watch.obj("conversationBar").obj("liveChatRenderer")
        val topToken = chat?.let(::reloadToken)
        if (chat == null || topToken == null) {
            val message = watch.obj("conversationBar").obj("conversationBarRenderer").let(::unavailableMessage)
            return VideoChatInfo.Unavailable(videoId, title, channel, message)
        }
        return VideoChatInfo.Available(
            videoId = videoId,
            title = title,
            channelName = channel,
            isReplay = chat.bool("isReplay") ?: false,
            topChatToken = subMenuToken(chat, TOP_CHAT_INDEX) ?: topToken,
            allChatToken = subMenuToken(chat, ALL_CHAT_INDEX),
        )
    }

    private fun reloadToken(node: JsonElement?): String? =
        node.arr("continuations")?.firstNotNullOfOrNull { it.obj("reloadContinuationData").str("continuation") }

    /** チャット欄の見出しにある表示切り替え（0 = 上位チャット、1 = すべてのチャット）の continuation。 */
    private fun subMenuToken(
        chat: JsonObject,
        index: Int,
    ): String? =
        chat
            .obj("header")
            .obj("liveChatHeaderRenderer")
            .obj("viewSelector")
            .obj("sortFilterSubMenuRenderer")
            .arr("subMenuItems")
            ?.getOrNull(index)
            .obj("continuation")
            .obj("reloadContinuationData")
            .str("continuation")

    private fun unavailableMessage(node: JsonObject?): String? =
        node.obj("availabilityMessage").obj("messageRenderer").text("text")

    private const val TOP_CHAT_INDEX = 0
    private const val ALL_CHAT_INDEX = 1
}
