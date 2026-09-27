package io.github.filderschoice.romcha.core.sync

import kotlin.math.abs

/**
 * シーク判定（F-SYNC-04、PLAN 4.5）。
 *
 * 前回の推定位置から「経過時間×速度」だけ進んだ位置を期待値とし、そこから [toleranceMs] を超えてずれたらシークとみなす。
 */
class SeekDetector(
    private val toleranceMs: Long = DEFAULT_TOLERANCE_MS,
) {
    private var lastPositionMs: Long? = null
    private var lastElapsedMs: Long = 0
    private var lastPlaying: Boolean = false
    private var lastSpeed: Float = 1f

    /** 新しい推定位置を与え、前回からシークが起きたかを返す。初回は false。 */
    fun update(
        positionMs: Long,
        nowElapsedMs: Long,
        playing: Boolean,
        speed: Float,
    ): Boolean {
        val previous = lastPositionMs
        val seeked =
            if (previous == null) {
                false
            } else {
                val advance = if (lastPlaying) ((nowElapsedMs - lastElapsedMs) * lastSpeed).toLong() else 0L
                abs(positionMs - (previous + advance)) > toleranceMs
            }
        lastPositionMs = positionMs
        lastElapsedMs = nowElapsedMs
        lastPlaying = playing
        lastSpeed = speed
        return seeked
    }

    fun reset() {
        lastPositionMs = null
    }

    companion object {
        const val DEFAULT_TOLERANCE_MS = 2_000L
    }
}
