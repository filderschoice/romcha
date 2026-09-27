package io.github.filderschoice.romcha.feature.overlay

import java.net.URI

/**
 * 表示する画像の配信元の制限（N-05「通信先は YouTube と画像 CDN に限定する」）。
 *
 * 応答に含まれる URL は外部入力のため、HTTPS かつ YouTube の画像配信元のものだけを読み込む。
 */
object ImagePolicy {
    private val allowedSuffixes = listOf(".ggpht.com", ".ytimg.com", ".googleusercontent.com")

    fun isAllowed(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        return uri.scheme == "https" && allowedSuffixes.any { host.endsWith(it) }
    }
}
