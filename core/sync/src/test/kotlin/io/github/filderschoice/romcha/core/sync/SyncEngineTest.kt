package io.github.filderschoice.romcha.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncEngineTest {
    private data class Item(
        val id: String,
        val offsetMs: Long,
    )

    private fun engine(config: SyncConfig = SyncConfig()) = SyncEngine<Item>({ it.offsetMs }, { it.id }, config)

    private fun playing(
        positionMs: Long,
        at: Long,
        speed: Float = 1f,
    ) = PlaybackSnapshot(PlaybackStatus.PLAYING, positionMs, at, speed)

    private fun paused(positionMs: Long) = PlaybackSnapshot(PlaybackStatus.PAUSED, positionMs, 0, 0f)

    @Test
    fun 初回は遡り範囲からの取得を要求する() {
        val engine = engine()
        val frame = engine.tick(playing(positionMs = 100_000, at = 0), nowElapsedMs = 0)

        assertTrue(frame.rebuilt)
        val request = frame.fetchRequest!!
        assertEquals(70_000, request.fromMs)
        assertTrue(request.restart)
    }

    @Test
    fun 推定位置以下のメッセージだけを時刻順に表示する() {
        val engine = engine()
        val request = engine.tick(playing(10_000, at = 0), 0).fetchRequest!!
        engine.onFetched(
            request.generation,
            listOf(Item("b", 11_000), Item("a", 9_000), Item("c", 12_500)),
            coveredUntilMs = 80_000,
            hasMore = true,
        )

        val first = engine.tick(playing(10_000, at = 0), nowElapsedMs = 1_000)
        assertEquals(listOf("a", "b"), first.visible.map { it.id })

        // 変化が無ければ changed=false で、表示は前回のものを維持する
        val second = engine.tick(playing(10_000, at = 0), nowElapsedMs = 2_000)
        assertFalse(second.changed)

        val third = engine.tick(playing(10_000, at = 0), nowElapsedMs = 2_600)
        assertEquals(listOf("a", "b", "c"), third.visible.map { it.id })
    }

    @Test
    fun 一時停止中は表示が進まず先読みもしない() {
        val engine = engine()
        val request = engine.tick(paused(10_000), 0).fetchRequest!!
        engine.onFetched(request.generation, listOf(Item("a", 10_500)), coveredUntilMs = 20_000, hasMore = true)

        val frame = engine.tick(paused(10_000), nowElapsedMs = 60_000)

        assertTrue(frame.visible.isEmpty())
        assertNull(frame.fetchRequest)
    }

    @Test
    fun 再生速度に比例して位置が進む() {
        assertEquals(12_000, PositionEstimator.estimate(playing(10_000, at = 0, speed = 2f), nowElapsedMs = 1_000))
        assertEquals(10_250, PositionEstimator.estimate(playing(10_000, at = 0, speed = 0.25f), nowElapsedMs = 1_000))
    }

    @Test
    fun シークすると表示を作り直し古い世代の応答を捨てる() {
        val engine = engine()
        val first = engine.tick(playing(10_000, at = 0), 0).fetchRequest!!
        engine.onFetched(first.generation, listOf(Item("a", 9_000)), coveredUntilMs = 80_000, hasMore = true)
        engine.tick(playing(10_000, at = 0), 100)

        val seek = engine.tick(playing(500_000, at = 200), 200)
        assertTrue(seek.rebuilt)
        assertTrue(seek.visible.isEmpty())
        val request = seek.fetchRequest!!
        assertEquals(470_000, request.fromMs)
        assertTrue(request.restart)

        // シーク前の要求に対する遅れた応答は破棄される
        engine.onFetched(first.generation, listOf(Item("old", 480_000)), coveredUntilMs = 90_000, hasMore = true)
        engine.onFetched(request.generation, listOf(Item("new", 480_000)), coveredUntilMs = 560_000, hasMore = true)

        val frame = engine.tick(playing(500_000, at = 200), 300)
        assertEquals(listOf("new"), frame.visible.map { it.id })
    }

    @Test
    fun 小さな揺らぎはシークとみなさない() {
        val engine = engine()
        engine.tick(playing(10_000, at = 0), 0)
        val frame = engine.tick(playing(11_500, at = 250), 250)
        assertFalse(frame.rebuilt)
    }

    @Test
    fun 先読み範囲が不足したら続きを要求し応答待ちの間は重ねて要求しない() {
        val engine = engine()
        val first = engine.tick(playing(0, at = 0), 0).fetchRequest!!
        engine.onFetched(first.generation, emptyList(), coveredUntilMs = 30_000, hasMore = true)

        val next = engine.tick(playing(0, at = 0), 250).fetchRequest
        assertEquals(FetchRequest(30_000, restart = false, generation = first.generation), next)
        assertNull(engine.tick(playing(0, at = 0), 500).fetchRequest)

        engine.onFetchFailed(first.generation)
        assertNotNull(engine.tick(playing(0, at = 0), 750).fetchRequest)
    }

    @Test
    fun 終端に達したら要求しない() {
        val engine = engine()
        val first = engine.tick(playing(0, at = 0), 0).fetchRequest!!
        engine.onFetched(first.generation, emptyList(), coveredUntilMs = 5_000, hasMore = false)
        assertNull(engine.tick(playing(0, at = 0), 250).fetchRequest)
    }

    @Test
    fun 遅れて届いた過去分は時刻順に差し込む() {
        val engine = engine()
        val first = engine.tick(playing(40_000, at = 0), 0).fetchRequest!!
        engine.onFetched(first.generation, listOf(Item("b", 35_000)), coveredUntilMs = 100_000, hasMore = true)
        engine.tick(playing(40_000, at = 0), 100)
        val late = listOf(Item("a", 20_000), Item("b", 35_000))
        engine.onFetched(first.generation, late, coveredUntilMs = 100_000, hasMore = true)

        val frame = engine.tick(playing(40_000, at = 0), 200)
        assertTrue(frame.changed)
        assertEquals(listOf("a", "b"), frame.visible.map { it.id })
    }

    @Test
    fun 表示保持件数の上限を超えたら古いものから捨てる() {
        val engine = engine(SyncConfig(maxVisible = 3))
        val first = engine.tick(playing(0, at = 0), 0).fetchRequest!!
        val items = (1..5).map { Item("m$it", it * 100L) }
        engine.onFetched(first.generation, items, coveredUntilMs = 100_000, hasMore = true)

        val frame = engine.tick(playing(0, at = 0), 1_000)
        assertEquals(listOf("m3", "m4", "m5"), frame.visible.map { it.id })
    }

    @Test
    fun リセット後は初回と同じく作り直す() {
        val engine = engine()
        engine.tick(playing(10_000, at = 0), 0)
        engine.reset()
        val frame = engine.tick(playing(10_100, at = 100), 100)
        assertTrue(frame.rebuilt)
        assertTrue(frame.fetchRequest!!.restart)
    }
}
