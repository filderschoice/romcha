package io.github.filderschoice.romcha.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SyncOffsetTest {
    private val snapshot =
        PlaybackSnapshot(PlaybackStatus.PLAYING, positionMs = 5_000, updatedAtElapsedMs = 100, speed = 1f)

    @Test
    fun 補正値は範囲内に収めて0点5秒刻みに丸める() {
        assertEquals(SyncOffset.MAX_MS, SyncOffset.clamp(60_000))
        assertEquals(SyncOffset.MIN_MS, SyncOffset.clamp(-60_000))
        assertEquals(1_500L, SyncOffset.clamp(1_400))
        assertEquals(-1_000L, SyncOffset.clamp(-1_100))
    }

    @Test
    fun 補正値を再生位置に足す() {
        assertEquals(6_500L, SyncOffset.apply(snapshot, 1_500).positionMs)
        assertEquals(3_000L, SyncOffset.apply(snapshot, -2_000).positionMs)
        assertEquals(100L, SyncOffset.apply(snapshot, 1_500).updatedAtElapsedMs)
    }

    @Test
    fun 補正後の位置は0未満にしない() {
        assertEquals(0L, SyncOffset.apply(snapshot, -10_000).positionMs)
    }

    @Test
    fun 補正が0なら同じ状態を返す() {
        assertSame(snapshot, SyncOffset.apply(snapshot, 0))
    }
}
