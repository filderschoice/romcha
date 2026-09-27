package io.github.filderschoice.romcha.core.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class ManualTimerTest {
    @Test
    fun 停止中は位置が進まない() {
        val timer = ManualTimer.paused(10_000, nowElapsedMs = 0)
        assertEquals(10_000L, PositionEstimator.estimate(timer, 5_000))
    }

    @Test
    fun 開始すると経過時間だけ進み停止するとその位置で止まる() {
        val started = ManualTimer.start(ManualTimer.paused(10_000, 0), nowElapsedMs = 1_000)
        assertEquals(13_000L, PositionEstimator.estimate(started, 4_000))

        val stopped = ManualTimer.stop(started, nowElapsedMs = 4_000)
        assertEquals(PlaybackStatus.PAUSED, stopped.status)
        assertEquals(13_000L, PositionEstimator.estimate(stopped, 9_000))
    }

    @Test
    fun 位置を入力しても再生中なら進み続ける() {
        val started = ManualTimer.start(ManualTimer.paused(0, 0), nowElapsedMs = 0)
        val seeked = ManualTimer.seek(started, 60_000, nowElapsedMs = 2_000)
        assertEquals(PlaybackStatus.PLAYING, seeked.status)
        assertEquals(61_000L, PositionEstimator.estimate(seeked, 3_000))
    }

    @Test
    fun 負の位置は0にする() {
        assertEquals(0L, ManualTimer.seek(ManualTimer.paused(0, 0), -5_000, 0).positionMs)
    }
}
