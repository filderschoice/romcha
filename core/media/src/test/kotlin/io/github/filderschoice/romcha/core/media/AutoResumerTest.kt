package io.github.filderschoice.romcha.core.media

import io.github.filderschoice.romcha.core.sync.PlaybackSnapshot
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoResumerTest {
    private val paused = NowPlaying(PlaybackSnapshot.NONE, sessionFound = true, canResume = true)
    private val playing = NowPlaying(PlaybackSnapshot.NONE, sessionFound = true, canResume = false)

    private class Rig(
        initial: NowPlaying,
        screen: Boolean = true,
        enabled: Boolean = true,
    ) {
        val nowPlaying = MutableStateFlow(initial)
        val screenOn = MutableStateFlow(screen)
        val enabled = MutableStateFlow(enabled)
        var resumed = 0
        val limiter = ResumeLimiter(clock = { 0L }, maxCount = 1, windowMs = 30_000, cooldownMs = 60_000)
        val resumer =
            AutoResumer(
                nowPlaying = nowPlaying,
                active = combine(this.screenOn, this.enabled) { screen, on -> screen && on },
                resume = { resumed++ >= 0 },
                waitMs = WAIT,
                limiter = limiter,
            )
    }

    private fun check(
        rig: Rig,
        body: suspend () -> Unit,
    ) = runBlocking {
        val job = launch(start = CoroutineStart.UNDISPATCHED) { rig.resumer.run() }
        body()
        job.cancel()
    }

    @Test
    fun 一時停止を待ち時間の後に再開する() {
        val rig = Rig(paused)
        check(rig) {
            delay(WAIT * 5)
            assertEquals(1, rig.resumed)
        }
    }

    @Test
    fun 待機中に再生へ戻ったら取り消す() {
        val rig = Rig(paused)
        check(rig) {
            delay(WAIT / 3)
            rig.nowPlaying.value = playing
            delay(WAIT * 5)
            assertEquals(0, rig.resumed)
        }
    }

    @Test
    fun 設定オフと画面オフでは何もしない() {
        val off = Rig(paused, enabled = false)
        check(off) {
            delay(WAIT * 5)
            assertEquals(0, off.resumed)
        }
        val dark = Rig(paused, screen = false)
        check(dark) {
            delay(WAIT * 5)
            assertEquals(0, dark.resumed)
        }
    }

    @Test
    fun セッションが無ければ何もしない() {
        val rig = Rig(NowPlaying())
        check(rig) {
            delay(WAIT * 5)
            assertEquals(0, rig.resumed)
        }
    }

    @Test
    fun 回数制限に達したら再開せず診断へ記録する() {
        val rig = Rig(paused)
        val before = ResumeDiagnostics.stats.value.limitReachedCount
        check(rig) {
            delay(WAIT * 5)
            rig.nowPlaying.value = playing
            delay(WAIT)
            rig.nowPlaying.value = paused
            delay(WAIT * 5)
            assertEquals(1, rig.resumed)
            assertEquals(before + 1, ResumeDiagnostics.stats.value.limitReachedCount)
        }
    }

    private companion object {
        const val WAIT = 60L
    }
}
