package io.github.filderschoice.romcha.core.media

import io.github.filderschoice.romcha.core.media.ResumeLimiter.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class ResumeLimiterTest {
    private var now = 0L

    private fun limiter() = ResumeLimiter(clock = { now }, maxCount = 3, windowMs = 30_000, cooldownMs = 60_000)

    private fun ResumeLimiter.at(ms: Long): Verdict {
        now = ms
        return tryAcquire()
    }

    @Test
    fun 上限までは許す() {
        val limiter = limiter()
        repeat(3) { assertEquals(Verdict.ALLOWED, limiter.at(it * 1_000L)) }
    }

    @Test
    fun 上限を超えたら停止期間に入り_その間は止める() {
        val limiter = limiter()
        repeat(3) { limiter.at(it * 1_000L) }
        assertEquals(Verdict.LIMIT_REACHED, limiter.at(3_000))
        assertEquals(Verdict.COOLING_DOWN, limiter.at(10_000))
        assertEquals(Verdict.COOLING_DOWN, limiter.at(62_999))
    }

    @Test
    fun 停止期間が明けたら再び許す() {
        val limiter = limiter()
        repeat(3) { limiter.at(it * 1_000L) }
        limiter.at(3_000)
        assertEquals(Verdict.ALLOWED, limiter.at(63_000))
    }

    @Test
    fun 窓を過ぎた履歴は数えない() {
        val limiter = limiter()
        repeat(3) { limiter.at(it * 1_000L) }
        assertEquals(Verdict.ALLOWED, limiter.at(32_000))
    }
}
