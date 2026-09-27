package io.github.filderschoice.romcha.core.chat

import java.net.URI
import java.net.URISyntaxException

/**
 * YouTube の URL から動画IDを取り出す（F-VID-04 / F-VID-05）。
 *
 * 受理する形式: `youtu.be/ID`、`youtube.com/watch?v=ID`、`youtube.com/live/ID`、`youtube.com/shorts/ID`
 * （`www.` / `m.` 付き、`http` / `https`、スキーム省略を含む）。
 * 共有インテントのテキストは「タイトル + URL」のように URL 以外を含むため、文中の最初の該当 URL を使う。
 */
object VideoUrlParser {
    private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
    private val URL_CANDIDATE =
        Regex("""(?:https?://)?(?:www\.|m\.)?(?:youtube\.com|youtu\.be)/\S+""", RegexOption.IGNORE_CASE)
    private val HOSTS = setOf("youtube.com", "www.youtube.com", "m.youtube.com")
    private val PATH_PREFIXES = listOf("/live/", "/shorts/")

    /** 動画IDを返す。YouTube の動画 URL が含まれない場合は null。 */
    fun extractVideoId(text: String): String? =
        URL_CANDIDATE.findAll(text).firstNotNullOfOrNull { match -> parseUrl(match.value) }

    private fun parseUrl(candidate: String): String? {
        val withScheme = if (candidate.contains("://")) candidate else "https://$candidate"
        val uri =
            try {
                URI(withScheme)
            } catch (ignored: URISyntaxException) {
                null
            }
        val host = uri?.host?.lowercase()
        val path = uri?.rawPath.orEmpty()
        val id =
            when {
                host == "youtu.be" -> path.removePrefix("/").substringBefore('/')
                host !in HOSTS -> null
                path == "/watch" -> queryParameter(uri?.rawQuery, "v")
                else -> pathVideoId(path)
            }
        return id?.takeIf { VIDEO_ID.matches(it) }
    }

    /** `/live/ID` / `/shorts/ID` 形式のパスから ID 部分を取り出す。 */
    private fun pathVideoId(path: String): String? {
        val prefix = PATH_PREFIXES.firstOrNull { path.startsWith(it) } ?: return null
        return path.removePrefix(prefix).substringBefore('/')
    }

    private fun queryParameter(
        query: String?,
        name: String,
    ): String? =
        query
            ?.split('&')
            ?.map { it.split('=', limit = 2) }
            ?.firstOrNull { it.size == 2 && it[0] == name }
            ?.get(1)
}
