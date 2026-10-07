package io.github.filderschoice.romcha.core.chat.resolve

import io.github.filderschoice.romcha.core.chat.internal.arr
import io.github.filderschoice.romcha.core.chat.internal.obj
import io.github.filderschoice.romcha.core.chat.internal.str
import io.github.filderschoice.romcha.core.chat.internal.text
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.logging.Logger

/**
 * 検索結果の動画1件（PLAN 4.3 手順4）。
 *
 * @property durationMs 動画の長さ。ライブ中など表示が無い場合は null
 * @property isLive 配信中・プレミア公開中の表示がある
 */
data class SearchCandidate(
    val videoId: String,
    val title: String,
    val channelName: String,
    val durationMs: Long?,
    val isLive: Boolean,
)

/** `youtubei/v1/search` 応答から動画の検索結果を読む。 */
object SearchResultParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val LIVE_BADGE_STYLES = setOf("BADGE_STYLE_TYPE_LIVE_NOW")
    private const val SECONDS_PER_UNIT = 60L
    private const val MILLIS_PER_SECOND = 1_000L

    /** 時:分:秒 */
    private const val MAX_DURATION_PARTS = 3

    /** 解析できた場合は動画の一覧（0件を含む）、構造が想定と異なる場合は null。 */
    fun parse(body: String): List<SearchCandidate>? {
        val root =
            try {
                json.parseToJsonElement(body)
            } catch (ignored: SerializationException) {
                null
            }
        val sections =
            root
                .obj("contents")
                .obj("twoColumnSearchResultsRenderer")
                .obj("primaryContents")
                .obj("sectionListRenderer")
                .arr("contents") ?: return null
        val items = sections.flatMap { it.obj("itemSectionRenderer").arr("contents").orEmpty() }
        val videos = items.mapNotNull { it.obj("videoRenderer")?.let(::parseVideo) }
        if (videos.isEmpty()) {
            // 想定の構造で動画が 1 件も読めない時は、仕様変更の調査のため項目の種別（キー名）だけを記録する（内容は記録しない）
            val kinds = items.map { (it as? JsonObject)?.keys?.joinToString("+").orEmpty() }
            log.warning("検索結果に videoRenderer が無い。sections=${sections.size} items=${items.size} kinds=$kinds")
        }
        return videos
    }

    private val log = Logger.getLogger("romcha.search")

    private fun parseVideo(video: JsonElement): SearchCandidate? {
        val videoId = video.str("videoId") ?: return null
        val channel = video.text("ownerText") ?: video.text("longBylineText") ?: video.text("shortBylineText")
        val isLive =
            video.arr("badges").orEmpty().any {
                it.obj("metadataBadgeRenderer").str("style") in LIVE_BADGE_STYLES
            }
        return SearchCandidate(
            videoId = videoId,
            title = video.text("title").orEmpty(),
            channelName = channel.orEmpty(),
            durationMs = video.text("lengthText")?.let(::parseDuration),
            isLive = isLive,
        )
    }

    /** `1:23:45` / `12:34` 形式の長さをミリ秒にする。解釈できない場合は null。 */
    internal fun parseDuration(text: String): Long? {
        val parts = text.trim().split(':').map { it.toLongOrNull() ?: return null }
        if (parts.isEmpty() || parts.size > MAX_DURATION_PARTS) return null
        return parts.fold(0L) { acc, part -> acc * SECONDS_PER_UNIT + part } * MILLIS_PER_SECOND
    }
}
