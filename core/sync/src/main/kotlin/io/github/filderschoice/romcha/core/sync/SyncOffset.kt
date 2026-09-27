package io.github.filderschoice.romcha.core.sync

/**
 * 同期オフセットの手動補正（F-SYNC-06、PLAN 4.5 の「推定位置 + 手動補正」）。
 *
 * 補正値を公式アプリの再生位置に足してからリプレイの同期に使う。正の値でチャットを早く、負の値で遅く表示する。
 */
object SyncOffset {
    /** 補正の範囲と刻み（ミリ秒）。±10 秒、0.5 秒刻み */
    const val MIN_MS = -10_000L
    const val MAX_MS = 10_000L
    const val STEP_MS = 500L
    const val DEFAULT_MS = 0L

    /** 補正値を範囲に収め、刻みに丸める。 */
    fun clamp(offsetMs: Long): Long {
        val clamped = offsetMs.coerceIn(MIN_MS, MAX_MS)
        return Math.floorDiv(clamped + STEP_MS / 2, STEP_MS) * STEP_MS
    }

    /** 再生状態の位置に補正値を足す（0 未満にはしない）。 */
    fun apply(
        snapshot: PlaybackSnapshot,
        offsetMs: Long,
    ): PlaybackSnapshot {
        if (offsetMs == 0L) return snapshot
        return snapshot.copy(positionMs = (snapshot.positionMs + clamp(offsetMs)).coerceAtLeast(0))
    }
}
