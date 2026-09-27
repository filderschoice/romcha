package io.github.filderschoice.romcha.core.chat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** 取得の失敗理由。画面の状態表示に使う（F-CHAT-10、N-08）。 */
sealed interface FetchFailure {
    /** 通信できない（オフライン・タイムアウト等） */
    data class Network(
        val message: String?,
    ) : FetchFailure

    /** HTTP エラー（429・5xx は再試行後、それ以外は即時） */
    data class Http(
        val code: Int,
    ) : FetchFailure

    /** 応答の構造が想定と異なる（非公式 API の仕様変更。K-03） */
    data class Parse(
        val reason: String,
    ) : FetchFailure
}

sealed interface FetchResult<out T> {
    data class Success<T>(
        val value: T,
    ) : FetchResult<T>

    data class Failure(
        val failure: FetchFailure,
    ) : FetchResult<Nothing>
}

/**
 * 指数バックオフの再試行方針（F-CHAT-10）。
 *
 * 待ち時間は `initialDelayMs × 2^(試行回数-1)` を [maxDelayMs] で頭打ちにする。
 */
data class RetryPolicy(
    val maxAttempts: Int = 5,
    val initialDelayMs: Long = 1_000,
    val maxDelayMs: Long = 30_000,
) {
    fun delayBeforeRetry(attempt: Int): Long =
        (initialDelayMs shl (attempt - 1).coerceIn(0, MAX_SHIFT)).coerceAtMost(maxDelayMs)

    private companion object {
        const val MAX_SHIFT = 20
    }
}

/** 再試行の通知。UI に「再接続中（n 回目）」を表示するために使う。 */
fun interface RetryListener {
    fun onRetry(
        attempt: Int,
        delayMs: Long,
        failure: FetchFailure,
    )
}

/**
 * YouTube Web クライアントの内部 API（InnerTube）でチャットを取得する（PLAN 4.4、N-09）。
 *
 * 非公式 API のため、仕様変更時の修正範囲をこのクラスと各 Parser に閉じ込める。
 * 通信先は [baseUrl]（既定 `https://www.youtube.com/`）に限る（N-05）。Cookie は保持しない（N-06）。
 */
class InnerTubeClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
    private val clientVersion: String = DEFAULT_CLIENT_VERSION,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /** 動画のタイトル・チャンネル名とチャットの continuation を取得する。 */
    suspend fun fetchVideoChatInfo(
        videoId: String,
        listener: RetryListener? = null,
    ): FetchResult<VideoChatInfo> {
        val body = buildJsonObject { put("videoId", videoId) }
        return post("youtubei/v1/next", body, listener) { response ->
            WatchInfoParser.parse(videoId, response)?.let { FetchResult.Success(it) }
                ?: FetchResult.Failure(FetchFailure.Parse("next 応答にチャット情報の構造が無い"))
        }
    }

    /**
     * リプレイチャットを取得する（F-CHAT-01〜03）。
     *
     * @param playerOffsetMs 取得開始位置。初回・シーク後に指定し、続きの取得では null
     */
    suspend fun fetchReplay(
        continuation: String,
        playerOffsetMs: Long?,
        listener: RetryListener? = null,
    ): FetchResult<ChatParseResult.Success> {
        val body =
            buildJsonObject {
                put("continuation", continuation)
                if (playerOffsetMs != null) {
                    putJsonObject("currentPlayerState") { put("playerOffsetMs", playerOffsetMs.toString()) }
                }
            }
        return post("youtubei/v1/live_chat/get_live_chat_replay", body, listener, ::toChatResult)
    }

    /** ライブチャットを取得する（F-CHAT-04）。次の取得間隔は応答の `timeoutMs` に従う。 */
    suspend fun fetchLive(
        continuation: String,
        listener: RetryListener? = null,
    ): FetchResult<ChatParseResult.Success> {
        val body = buildJsonObject { put("continuation", continuation) }
        return post("youtubei/v1/live_chat/get_live_chat", body, listener, ::toChatResult)
    }

    private fun toChatResult(response: String): FetchResult<ChatParseResult.Success> =
        when (val parsed = ChatResponseParser.parse(response)) {
            is ChatParseResult.Success -> FetchResult.Success(parsed)
            is ChatParseResult.Failure -> FetchResult.Failure(FetchFailure.Parse(parsed.reason))
        }

    private suspend fun <T> post(
        path: String,
        payload: JsonObject,
        listener: RetryListener?,
        parse: (String) -> FetchResult<T>,
    ): FetchResult<T> {
        val request = buildRequest(path, payload)
        var attempt = 1
        while (true) {
            val outcome = execute(request)
            val failure =
                when (outcome) {
                    is HttpOutcome.Body -> return parse(outcome.body)
                    is HttpOutcome.Failed -> outcome.failure
                }
            if (!isRetryable(failure) || attempt >= retryPolicy.maxAttempts) return FetchResult.Failure(failure)
            val wait = retryPolicy.delayBeforeRetry(attempt)
            listener?.onRetry(attempt, wait, failure)
            delay(wait)
            attempt++
        }
    }

    private fun buildRequest(
        path: String,
        payload: JsonObject,
    ): Request {
        val body =
            buildJsonObject {
                putJsonObject("context") {
                    putJsonObject("client") {
                        put("clientName", "WEB")
                        put("clientVersion", clientVersion)
                        put("hl", "ja")
                        put("gl", "JP")
                    }
                }
                payload.forEach { (key, value) -> put(key, value) }
            }
        val url =
            baseUrl
                .newBuilder()
                .addPathSegments(path)
                .addQueryParameter("prettyPrint", "false")
                .build()
        return Request
            .Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "ja")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    private suspend fun execute(request: Request): HttpOutcome =
        withContext(ioDispatcher) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        HttpOutcome.Body(response.body?.string().orEmpty())
                    } else {
                        HttpOutcome.Failed(FetchFailure.Http(response.code))
                    }
                }
            } catch (e: IOException) {
                HttpOutcome.Failed(FetchFailure.Network(e.message))
            }
        }

    private fun isRetryable(failure: FetchFailure): Boolean =
        when (failure) {
            is FetchFailure.Network -> true
            is FetchFailure.Http -> failure.code == HTTP_TOO_MANY_REQUESTS || failure.code >= HTTP_SERVER_ERROR
            is FetchFailure.Parse -> false
        }

    private sealed interface HttpOutcome {
        data class Body(
            val body: String,
        ) : HttpOutcome

        data class Failed(
            val failure: FetchFailure,
        ) : HttpOutcome
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://www.youtube.com/"

        /** InnerTube の WEB クライアント版数。実応答での有効性は人手検証（BL-022）で確認する。 */
        const val DEFAULT_CLIENT_VERSION = "2.20250925.01.00"

        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/140.0.0.0 Safari/537.36"
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_SERVER_ERROR = 500
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
