package io.github.filderschoice.romcha.core.sync

/**
 * 手動タイマーモード（F-SYNC-07）。MediaSession が取れない時に、利用者の開始・停止・位置入力で再生位置を作る。
 *
 * 状態は [PlaybackSnapshot] で表し（等速）、再生中の位置は [PositionEstimator] で経過時間から求める。
 */
object ManualTimer {
    /** 停止した状態で始める。 */
    fun paused(
        positionMs: Long,
        nowElapsedMs: Long,
    ): PlaybackSnapshot = PlaybackSnapshot(PlaybackStatus.PAUSED, positionMs.coerceAtLeast(0), nowElapsedMs, speed = 1f)

    /** 現在位置から進め始める。 */
    fun start(
        timer: PlaybackSnapshot,
        nowElapsedMs: Long,
    ): PlaybackSnapshot =
        at(
            timer,
            PlaybackStatus.PLAYING,
            PositionEstimator.estimate(timer, nowElapsedMs),
            nowElapsedMs,
        )

    /** 現在位置で止める。 */
    fun stop(
        timer: PlaybackSnapshot,
        nowElapsedMs: Long,
    ): PlaybackSnapshot =
        at(
            timer,
            PlaybackStatus.PAUSED,
            PositionEstimator.estimate(timer, nowElapsedMs),
            nowElapsedMs,
        )

    /** 位置を入力する（再生中・停止中の状態は保つ）。 */
    fun seek(
        timer: PlaybackSnapshot,
        positionMs: Long,
        nowElapsedMs: Long,
    ): PlaybackSnapshot = at(timer, timer.status, positionMs, nowElapsedMs)

    private fun at(
        timer: PlaybackSnapshot,
        status: PlaybackStatus,
        positionMs: Long,
        nowElapsedMs: Long,
    ) = timer.copy(
        status = status,
        positionMs = positionMs.coerceAtLeast(0),
        updatedAtElapsedMs = nowElapsedMs,
        speed = 1f,
    )
}
