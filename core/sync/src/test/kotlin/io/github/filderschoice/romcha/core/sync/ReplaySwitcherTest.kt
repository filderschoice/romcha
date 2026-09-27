package io.github.filderschoice.romcha.core.sync

import io.github.filderschoice.romcha.core.chat.FetchFailure
import io.github.filderschoice.romcha.core.chat.FetchResult
import io.github.filderschoice.romcha.core.chat.VideoChatInfo
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReplaySwitcherTest {
    private fun available(
        isReplay: Boolean,
        token: String,
    ): FetchResult<VideoChatInfo> =
        FetchResult.Success(
            VideoChatInfo.Available("v", "t", "c", isReplay, topChatToken = "TOP_$token", allChatToken = token),
        )

    private val unavailable: FetchResult<VideoChatInfo> =
        FetchResult.Success(
            VideoChatInfo.Unavailable("v", "t", "c", null),
        )

    /** 呼び出しごとに順に応答を返す取得元。 */
    private fun source(vararg results: FetchResult<VideoChatInfo>): VideoInfoSource {
        val queue = ArrayDeque(results.toList())
        return VideoInfoSource { queue.removeFirst() }
    }

    @Test
    fun リプレイの準備ができるまで間隔を空けて確かめる() =
        runTest {
            val waits = mutableListOf<Pair<Int, Long>>()
            val switcher =
                ReplaySwitcher(
                    source(
                        unavailable,
                        FetchResult.Failure(FetchFailure.Http(503)),
                        available(isReplay = true, "REPLAY"),
                    ),
                    waitsMs = listOf(10, 20, 30, 40),
                )

            val result = switcher.await("v") { attempt, waitMs -> waits += attempt to waitMs }

            assertEquals(ReplaySwitch.Ready("REPLAY"), result)
            assertEquals(listOf(1 to 10L, 2 to 20L, 3 to 30L), waits)
            assertEquals(60L, currentTime)
        }

    @Test
    fun まだ配信中ならライブの取得に戻る() =
        runTest {
            val result = ReplaySwitcher(source(available(isReplay = false, "LIVE")), waitsMs = listOf(10)).await("v")
            assertEquals(ReplaySwitch.StillLive("LIVE"), result)
        }

    @Test
    fun 待っても準備されなければ諦める() =
        runTest {
            val result = ReplaySwitcher(source(unavailable, unavailable), waitsMs = listOf(10, 20)).await("v")
            assertEquals(ReplaySwitch.Unavailable, result)
        }

    @Test
    fun 既定では合計約18分待つ() {
        assertEquals(1_110_000L, ReplaySwitcher.DEFAULT_WAITS_MS.sum())
    }
}
