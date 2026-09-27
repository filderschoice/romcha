package io.github.filderschoice.romcha.core.sync

import io.github.filderschoice.romcha.core.chat.InnerTubeClient
import io.github.filderschoice.romcha.core.chat.RetryPolicy
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** ライブチャットのポーリング（F-CHAT-04/05/06）。InnerTube の応答は MockWebServer で模擬する（guardrails 12.5）。 */
class LiveChatSessionTest {
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

    /** ライブの応答。[next] が null なら継続トークン無し（終了）。 */
    private fun liveResponse(
        next: String?,
        timeoutMs: Long?,
        vararg ids: String,
    ): MockResponse {
        val actions =
            ids.joinToString(",") { id ->
                """{"addChatItemAction":{"item":{"liveChatTextMessageRenderer":{"id":"$id",
                   "message":{"runs":[{"text":"$id"}]},"authorName":{"simpleText":"視聴者"},"timestampUsec":"1"}}}}"""
            }
        val timeout = timeoutMs?.let { ""","timeoutMs":$it""" }.orEmpty()
        val continuations =
            next?.let { """"continuations":[{"timedContinuationData":{"continuation":"$it"$timeout}}],""" }.orEmpty()
        return MockResponse().setBody(
            """{"continuationContents":{"liveChatContinuation":{$continuations"actions":[$actions]}}}""",
        )
    }

    private fun requestedContinuation(): String =
        Json
            .parseToJsonElement(server.takeRequest(1, TimeUnit.SECONDS)!!.body.readUtf8())
            .jsonObject["continuation"]!!
            .jsonPrimitive.content

    private var status = PlaybackStatus.PLAYING

    private fun TestScope.session(): LiveChatSession {
        val client =
            InnerTubeClient(
                baseUrl = server.url("/"),
                retryPolicy = RetryPolicy(maxAttempts = 1),
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
        val session =
            LiveChatSession(
                source = { token, listener -> client.fetchLive(token, listener) },
                initialContinuation = "INITIAL",
                playback = { PlaybackSnapshot(status, 0, 0, 1f) },
                clock = { testScheduler.currentTime },
            )
        backgroundScope.launch { session.run() }
        return session
    }

    @Test
    fun 応答の継続トークンを更新しながら推奨間隔でポーリングする() =
        runTest {
            server.enqueue(liveResponse("NEXT1", 3_000, "a", "b"))
            server.enqueue(liveResponse("NEXT2", 7_000, "b", "c"))
            server.enqueue(liveResponse("NEXT3", 7_000))
            val session = session()

            runCurrent()
            assertEquals("INITIAL", requestedContinuation())
            assertEquals(listOf("a", "b"), session.state.value.received.map { it.message.id })

            advanceTimeBy(2_999)
            assertEquals(1, server.requestCount)
            advanceTimeBy(2)
            assertEquals("NEXT1", requestedContinuation())
            // 重複（b）は除き、受信時刻を記録する
            assertEquals(listOf("a", "b", "c"), session.state.value.received.map { it.message.id })
            assertEquals(3_000L, session.state.value.received.last().receivedAtMs)

            advanceTimeBy(7_000)
            assertEquals("NEXT2", requestedContinuation())
        }

    @Test
    fun 推奨間隔は上限と下限に収める() {
        val polling = LivePolling()
        assertEquals(1_000L, polling.intervalFor(10))
        assertEquals(10_000L, polling.intervalFor(60_000))
        assertEquals(5_000L, polling.intervalFor(null))
    }

    @Test
    fun 継続トークンが無くなったら終了として止まる() =
        runTest {
            server.enqueue(liveResponse(null, null, "last"))
            val session = session()

            advanceTimeBy(30_000)

            assertTrue(session.state.value.ended)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun 一時停止中は取得せず再開したら続ける() =
        runTest {
            status = PlaybackStatus.PAUSED
            server.enqueue(liveResponse("NEXT1", 1_000, "a"))
            session()

            advanceTimeBy(10_000)
            assertEquals(0, server.requestCount)

            // 再開は一時停止中の確認間隔（1 秒）ごとに確かめる
            status = PlaybackStatus.PLAYING
            advanceTimeBy(500)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun プレミアの待機中など再生中でなくても一時停止でなければ取得する() =
        runTest {
            status = PlaybackStatus.NONE
            server.enqueue(liveResponse("NEXT1", 1_000, "waiting"))
            val session = session()

            runCurrent()

            assertEquals(listOf("waiting"), session.state.value.received.map { it.message.id })
        }

    @Test
    fun 失敗したら状態を出し冷却期間の後に同じトークンで再取得する() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(503))
            server.enqueue(liveResponse("NEXT1", 1_000, "a"))
            val session = session()

            runCurrent()
            assertTrue(session.state.value.fetchStatus is FetchStatus.Failed)
            assertEquals("INITIAL", requestedContinuation())

            advanceTimeBy(LivePolling().failureCooldownMs + 1)
            assertEquals("INITIAL", requestedContinuation())
            assertEquals(FetchStatus.Idle, session.state.value.fetchStatus)
        }
}
