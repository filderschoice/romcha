package io.github.filderschoice.romcha.update

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** 更新確認の結果（F-APP-02）。 */
sealed interface UpdateCheckResult {
    /** 現在の版が最新（または公開版より新しい開発版） */
    data class UpToDate(
        val latest: AppVersion,
    ) : UpdateCheckResult

    /** 新しい版が公開されている */
    data class Available(
        val latest: AppVersion,
    ) : UpdateCheckResult

    /** リリースがまだ 1 件も公開されていない（API が 404 を返す） */
    data object NoRelease : UpdateCheckResult

    /** 通信できない */
    data class NetworkError(
        val message: String?,
    ) : UpdateCheckResult

    /** HTTP エラー（未認証の API の回数制限 403・429 を含む） */
    data class HttpError(
        val code: Int,
    ) : UpdateCheckResult

    /** 応答または版の形式が想定と異なる */
    data class InvalidResponse(
        val reason: String,
    ) : UpdateCheckResult
}

/**
 * GitHub Releases API で最新リリースの版を確かめる（F-APP-02、PLAN 6章）。
 *
 * 利用者が「更新を確認」を押した時だけ呼ぶ（自動では通信しない。2026-09-27 ユーザー判断）。
 * 通信先は [baseUrl]（既定 `https://api.github.com/`）の `repos/<REPOSITORY>/releases/latest` のみで、認証情報は送らない。
 * 自動インストールはせず、新しい版があれば呼び出し側が [RELEASES_PAGE] を開く（応答に含まれる URL は開かない）。
 */
class UpdateChecker(
    private val userAgent: String,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun check(currentVersion: String): UpdateCheckResult {
        val current =
            AppVersion.parse(currentVersion)
                ?: return UpdateCheckResult.InvalidResponse("現在の版を読めません: $currentVersion")
        val request =
            Request
                .Builder()
                .url(baseUrl.newBuilder().addPathSegments("repos/$REPOSITORY/releases/latest").build())
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", API_VERSION)
                .header("User-Agent", userAgent)
                .build()
        return withContext(ioDispatcher) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    when {
                        response.code == HTTP_NOT_FOUND -> UpdateCheckResult.NoRelease
                        !response.isSuccessful -> UpdateCheckResult.HttpError(response.code)
                        else -> compare(current, response.body?.string().orEmpty())
                    }
                }
            } catch (e: IOException) {
                UpdateCheckResult.NetworkError(e.message)
            }
        }
    }

    private fun compare(
        current: AppVersion,
        body: String,
    ): UpdateCheckResult {
        val tag = tagName(body) ?: return UpdateCheckResult.InvalidResponse("tag_name がありません")
        val latest = AppVersion.parse(tag) ?: return UpdateCheckResult.InvalidResponse("版の形式ではありません: $tag")
        return if (latest > current) UpdateCheckResult.Available(latest) else UpdateCheckResult.UpToDate(latest)
    }

    private fun tagName(body: String): String? {
        val root =
            try {
                Json.parseToJsonElement(body)
            } catch (ignored: SerializationException) {
                return null
            }
        val tag = (root as? JsonObject)?.get("tag_name") as? JsonPrimitive
        return tag?.takeIf { it.isString }?.content
    }

    companion object {
        const val REPOSITORY = "filderschoice/romcha"

        /** 新しい版がある時に開くページ（最新リリース）。 */
        const val RELEASES_PAGE = "https://github.com/$REPOSITORY/releases/latest"

        private const val DEFAULT_BASE_URL = "https://api.github.com/"
        private const val API_VERSION = "2022-11-28"
        private const val HTTP_NOT_FOUND = 404
    }
}
