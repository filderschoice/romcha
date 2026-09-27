package io.github.filderschoice.romcha.core.sync

import io.github.filderschoice.romcha.core.chat.ChatAuthor
import io.github.filderschoice.romcha.core.chat.ChatContinuation
import io.github.filderschoice.romcha.core.chat.ChatMessage
import io.github.filderschoice.romcha.core.chat.ChatMessageKind
import io.github.filderschoice.romcha.core.chat.ChatParseResult
import io.github.filderschoice.romcha.core.chat.ContinuationKind
import io.github.filderschoice.romcha.core.chat.FetchFailure
import io.github.filderschoice.romcha.core.chat.FetchResult
import io.github.filderschoice.romcha.core.chat.MessageRun
import io.github.filderschoice.romcha.core.chat.RetryListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplaySessionTest {
    private data class Call(
        val continuation: String,
        val offset: Long?,
    )

    /** 呼び出しを記録し、あらかじめ積んだ応答を順に返す取得元。 */
    private class FakeSource : ReplayChatSource {
        val calls = mutableListOf<Call>()
        val responses = ArrayDeque<suspend () -> FetchResult<ChatParseResult.Success>>()

        override suspend fun fetch(
            continuation: String,
            playerOffsetMs: Long?,
            listener: RetryListener,
        ): FetchResult<ChatParseResult.Success> {
            calls += Call(continuation, playerOffsetMs)
            return responses.removeFirstOrNull()?.invoke() ?: FetchResult.Failure(FetchFailure.Network("応答なし"))
        }
    }

    private fun message(
        id: String,
        offsetMs: Long,
    ) = ChatMessage(
        id = id,
        kind = ChatMessageKind.TEXT,
        author = ChatAuthor("視聴者", null, null, emptySet()),
        runs = listOf(MessageRun.Text(id)),
        timestampUsec = 0,
        videoOffsetMs = offsetMs,
    )

    private fun chunk(
        next: String?,
        vararg messages: ChatMessage,
    ): suspend () -> FetchResult<ChatParseResult.Success> =
        {
            val continuation = next?.let { ChatContinuation(it, ContinuationKind.REPLAY, null) }
            FetchResult.Success(ChatParseResult.Success(messages.toList(), continuation, skipped = 0))
        }

    /** 再生位置をテスト側から動かせる公式アプリの代わり。仮想時刻を elapsedRealtime とみなす。 */
    private class Player(
        private val scope: TestScope,
    ) {
        var snapshot = PlaybackSnapshot(PlaybackStatus.PLAYING, 0, 0, 1f)

        fun play(positionMs: Long) {
            snapshot = PlaybackSnapshot(PlaybackStatus.PLAYING, positionMs, scope.testScheduler.currentTime, 1f)
        }

        fun pause(positionMs: Long) {
            snapshot = PlaybackSnapshot(PlaybackStatus.PAUSED, positionMs, scope.testScheduler.currentTime, 0f)
        }
    }

    private fun TestScope.session(
        source: FakeSource,
        player: Player,
    ): ReplaySession {
        val session =
            ReplaySession(
                source = source,
                initialContinuation = "INITIAL",
                playback = { player.snapshot },
                clock = { testScheduler.currentTime },
            )
        backgroundScope.launch { session.run() }
        return session
    }

    @Test
    fun 初回は遡り位置から取得し再生位置に合わせて表示する() =
        runTest {
            val source = FakeSource()
            source.responses += chunk("NEXT1", message("a", 95_000), message("b", 101_000))
            val player = Player(this).apply { play(100_000) }
            val session = session(source, player)

            runCurrent()
            assertEquals(Call("INITIAL", 70_000), source.calls.first())
            advanceTimeBy(300)
            assertEquals(listOf("a"), session.state.value.messages.map { it.id })

            advanceTimeBy(1_000)
            assertEquals(listOf("a", "b"), session.state.value.messages.map { it.id })
        }

    @Test
    fun 続きは応答の継続トークンで位置を指定せずに取得する() =
        runTest {
            val source = FakeSource()
            source.responses += chunk("NEXT1", message("a", 75_000))
            source.responses += chunk("NEXT2", message("b", 200_000))
            val player = Player(this).apply { play(100_000) }
            session(source, player)

            advanceTimeBy(1_500)

            assertEquals(Call("NEXT1", null), source.calls[1])
        }

    @Test
    fun シークしたら初期トークンと新しい位置で取り直し古い応答は捨てる() =
        runTest {
            val source = FakeSource()
            val slow = CompletableDeferred<FetchResult<ChatParseResult.Success>>()
            source.responses += { slow.await() }
            source.responses += chunk("NEXT_AFTER_SEEK", message("new", 480_000))
            val player = Player(this).apply { play(10_000) }
            val session = session(source, player)
            runCurrent()

            player.play(500_000)
            advanceTimeBy(300)
            slow.complete(chunk("OLD", message("old", 9_000))())
            advanceTimeBy(300)

            // シークを検知した tick（250ms 時点）の推定位置 500,250ms から 30 秒遡る
            assertEquals(Call("INITIAL", 470_250), source.calls[1])
            assertEquals(listOf("new"), session.state.value.messages.map { it.id })
        }

    @Test
    fun 失敗したら状態を表示し冷却期間が過ぎるまで再取得しない() =
        runTest {
            val source = FakeSource()
            source.responses += { FetchResult.Failure(FetchFailure.Http(503)) }
            source.responses += chunk(null, message("a", 1_000))
            val player = Player(this).apply { play(0) }
            val session = session(source, player)

            advanceTimeBy(300)
            assertTrue(session.state.value.fetchStatus is FetchStatus.Failed)
            advanceTimeBy(5_000)
            assertEquals(1, source.calls.size)

            advanceTimeBy(SessionTiming().failureCooldownMs)
            assertEquals(2, source.calls.size)
            assertEquals(FetchStatus.Idle, session.state.value.fetchStatus)
        }

    @Test
    fun 継続トークンが無ければ終端として以後は取得しない() =
        runTest {
            val source = FakeSource()
            source.responses += chunk(null, message("a", 1_000))
            val player = Player(this).apply { play(0) }
            val session = session(source, player)

            advanceTimeBy(10_000)

            assertTrue(session.state.value.ended)
            assertEquals(1, source.calls.size)
        }

    @Test
    fun 一時停止中は先読みせず表示も進まない() =
        runTest {
            val source = FakeSource()
            source.responses += chunk("NEXT1", message("a", 50_500))
            val player = Player(this).apply { pause(50_000) }
            val session = session(source, player)

            advanceTimeBy(30_000)

            assertEquals(1, source.calls.size)
            assertTrue(session.state.value.messages.isEmpty())
            assertEquals(PlaybackStatus.PAUSED, session.state.value.status)
        }

    @Test
    fun 空の応答でも先へ進み取得を続ける() =
        runTest {
            val source = FakeSource()
            source.responses += chunk("NEXT1")
            source.responses += chunk("NEXT2")
            val player = Player(this).apply { play(0) }
            session(source, player)

            advanceTimeBy(1_500)

            assertEquals(2, source.calls.size)
            assertNull(source.calls[1].offset)
        }
}
