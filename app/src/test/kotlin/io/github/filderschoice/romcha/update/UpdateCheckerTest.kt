package io.github.filderschoice.romcha.update

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 実通信は行わず MockWebServer で GitHub Releases API の応答を模擬する（guardrails 12.5）。 */
class UpdateCheckerTest {
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

    private fun TestScope.checker() =
        UpdateChecker(
            userAgent = "Romcha/test",
            baseUrl = server.url("/"),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

    private fun release(tag: String) =
        MockResponse().setBody(
            """{"tag_name":"$tag","html_url":"https://example.com/other","draft":false,"prerelease":false}""",
        )

    @Test
    fun 新しい版があればAvailable() =
        runTest {
            server.enqueue(release("v1.1.0"))
            assertEquals(UpdateCheckResult.Available(AppVersion(1, 1, 0)), checker().check("1.0.0"))
        }

    @Test
    fun 同じ版や古い版ならUpToDate() =
        runTest {
            server.enqueue(release("v1.0.0"))
            server.enqueue(release("v0.9.0"))
            assertEquals(UpdateCheckResult.UpToDate(AppVersion(1, 0, 0)), checker().check("1.0.0"))
            assertEquals(UpdateCheckResult.UpToDate(AppVersion(0, 9, 0)), checker().check("1.0.0"))
        }

    @Test
    fun 最新リリースのAPIへ認証なしで問い合わせる() =
        runTest {
            server.enqueue(release("v1.0.0"))
            checker().check("1.0.0")
            val request = server.takeRequest()
            assertEquals("/repos/filderschoice/romcha/releases/latest", request.path)
            assertEquals("GET", request.method)
            assertEquals("application/vnd.github+json", request.getHeader("Accept"))
            assertEquals("Romcha/test", request.getHeader("User-Agent"))
            assertNull(request.getHeader("Authorization"))
        }

    @Test
    fun リリースが無ければNoRelease() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(404))
            assertEquals(UpdateCheckResult.NoRelease, checker().check("1.0.0"))
        }

    @Test
    fun 回数制限などのHTTPエラー() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(403))
            assertEquals(UpdateCheckResult.HttpError(403), checker().check("1.0.0"))
        }

    @Test
    fun 通信できなければNetworkError() =
        runTest {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
            assertTrue(checker().check("1.0.0") is UpdateCheckResult.NetworkError)
        }

    @Test
    fun 応答や版の形式が違えばInvalidResponse() =
        runTest {
            server.enqueue(MockResponse().setBody("not json"))
            server.enqueue(MockResponse().setBody("""{"name":"x"}"""))
            server.enqueue(MockResponse().setBody("""{"tag_name":1}"""))
            server.enqueue(release("nightly"))
            repeat(4) { assertTrue(checker().check("1.0.0") is UpdateCheckResult.InvalidResponse) }
        }

    @Test
    fun 現在の版を読めなければ通信しない() =
        runTest {
            assertTrue(checker().check("dev") is UpdateCheckResult.InvalidResponse)
            assertEquals(0, server.requestCount)
        }
}
