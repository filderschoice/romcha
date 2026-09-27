package io.github.filderschoice.romcha.core.chat

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 実通信は行わず MockWebServer で InnerTube の応答を模擬する（guardrails 12.5）。 */
class InnerTubeClientTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun fixture(name: String): String = requireNotNull(javaClass.getResource("/fixtures/$name")).readText()

    private fun TestScope.client(retryPolicy: RetryPolicy = RetryPolicy()) =
        InnerTubeClient(
            baseUrl = server.url("/"),
            retryPolicy = retryPolicy,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

    private fun requestJson() = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject

    @Test
    fun next応答からリプレイのcontinuationとタイトルを取得する() =
        runTest {
            server.enqueue(MockResponse().setBody(fixture("next_replay.json")))

            val result = client().fetchVideoChatInfo("abcdefghijk")

            val info = (result as FetchResult.Success).value as VideoChatInfo.Available
            assertTrue(info.isReplay)
            assertEquals("アーカイブ配信のタイトル", info.title)
            assertEquals("テストチャンネル", info.channelName)
            // 見出しの表示切り替え（TOP_TOKEN / ALL_TOKEN）は動画IDを含まない雛形で使えないため、チャット欄本体の continuation を使う
            assertEquals("RELOAD_TOKEN", info.topChatToken)

            val recorded = server.takeRequest()
            assertEquals("/youtubei/v1/next?prettyPrint=false", recorded.path)
            val body = Json.parseToJsonElement(recorded.body.readUtf8()).jsonObject
            assertEquals("abcdefghijk", body["videoId"]!!.jsonPrimitive.content)
            assertEquals(
                "WEB",
                body["context"]!!.jsonObject["client"]!!.jsonObject["clientName"]!!.jsonPrimitive.content,
            )
        }

    @Test
    fun ライブ中の動画はリプレイではないと判定する() =
        runTest {
            server.enqueue(MockResponse().setBody(fixture("next_live.json")))
            val info = (client().fetchVideoChatInfo("live0000000") as FetchResult.Success).value
            assertEquals(false, (info as VideoChatInfo.Available).isReplay)
        }

    @Test
    fun チャットが無効な動画は説明文付きで利用不可を返す() =
        runTest {
            server.enqueue(MockResponse().setBody(fixture("next_chat_disabled.json")))
            server.enqueue(MockResponse().setBody(fixture("next_no_chat.json")))

            val disabled = (client().fetchVideoChatInfo("disabled000") as FetchResult.Success).value
            assertEquals("この動画ではチャットのリプレイを利用できません。", (disabled as VideoChatInfo.Unavailable).message)

            val none = (client().fetchVideoChatInfo("nochat00000") as FetchResult.Success).value
            assertNull((none as VideoChatInfo.Unavailable).message)
        }

    @Test
    fun リプレイ取得ではplayerOffsetMsを指定して呼び出す() =
        runTest {
            server.enqueue(MockResponse().setBody(fixture("replay_chunk.json")))

            val result = client().fetchReplay("TOP_TOKEN", playerOffsetMs = 83_000)

            assertEquals(6, (result as FetchResult.Success).value.messages.size)
            val recorded = server.takeRequest()
            assertEquals("/youtubei/v1/live_chat/get_live_chat_replay?prettyPrint=false", recorded.path)
            val body = Json.parseToJsonElement(recorded.body.readUtf8()).jsonObject
            assertEquals("TOP_TOKEN", body["continuation"]!!.jsonPrimitive.content)
            assertEquals("83000", body["currentPlayerState"]!!.jsonObject["playerOffsetMs"]!!.jsonPrimitive.content)
        }

    @Test
    fun 続きの取得ではplayerOffsetMsを送らない() =
        runTest {
            server.enqueue(MockResponse().setBody(fixture("replay_chunk.json")))
            client().fetchReplay("REPLAY_NEXT_TOKEN", playerOffsetMs = null)
            assertNull(requestJson()["currentPlayerState"])
        }

    @Test
    fun サーバーエラーは指数バックオフで再試行し成功すれば結果を返す() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(503))
            server.enqueue(MockResponse().setResponseCode(429))
            server.enqueue(MockResponse().setBody(fixture("live_chunk.json")))
            val retries = mutableListOf<Pair<Int, Long>>()

            val result = client().fetchLive("LIVE_TOKEN") { attempt, delayMs, _ -> retries += attempt to delayMs }

            assertTrue(result is FetchResult.Success)
            assertEquals(listOf(1 to 1_000L, 2 to 2_000L), retries)
            assertEquals(3_000L, currentTime)
            assertEquals(3, server.requestCount)
        }

    @Test
    fun 通信断も再試行し上限回数で失敗を返す() =
        runTest {
            repeat(3) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) }

            val result = client(RetryPolicy(maxAttempts = 3)).fetchLive("LIVE_TOKEN")

            assertTrue((result as FetchResult.Failure).failure is FetchFailure.Network)
            assertEquals(3, server.requestCount)
        }

    @Test
    fun クライアントエラーは再試行しない() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(403))
            val result = client().fetchLive("LIVE_TOKEN")
            assertEquals(FetchFailure.Http(403), (result as FetchResult.Failure).failure)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun 構造が想定と異なる応答は解析失敗として再試行しない() =
        runTest {
            server.enqueue(MockResponse().setBody("""{"responseContext":{}}"""))
            val result = client().fetchReplay("TOKEN", null)
            assertTrue((result as FetchResult.Failure).failure is FetchFailure.Parse)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun ライブに絞った検索では絞り込みの指定を送る() =
        runTest {
            server.enqueue(MockResponse().setBody(fixture("search_results.json")))
            server.enqueue(MockResponse().setBody(fixture("search_results.json")))

            val result = client().search("テストチャンネル", liveOnly = true)
            client().search("タイトル")

            assertEquals(3, (result as FetchResult.Success).value.size)
            val live = server.takeRequest()
            assertEquals("/youtubei/v1/search?prettyPrint=false", live.path)
            val liveBody = Json.parseToJsonElement(live.body.readUtf8()).jsonObject
            assertEquals(InnerTubeClient.SEARCH_PARAMS_LIVE, liveBody["params"]!!.jsonPrimitive.content)
            assertNull(requestJson()["params"])
        }

    @Test
    fun 待ち時間は倍々で増え上限で頭打ちになる() {
        val policy = RetryPolicy(initialDelayMs = 1_000, maxDelayMs = 30_000)
        assertEquals(
            listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L),
            (1..7).map(policy::delayBeforeRetry),
        )
    }
}
