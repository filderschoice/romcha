package io.github.filderschoice.romcha.core.media

/**
 * 自動再開の暴走を防ぐ回数制限（BL-116）。[windowMs] の間に [maxCount] 回まで許し、超えたら [cooldownMs] の間は止める。
 *
 * 時刻は [clock] で読む（単調な経過時間。端末の時刻変更に影響されない値を使う）。メインスレッドからだけ使う。
 */
class ResumeLimiter(
    private val clock: () -> Long,
    private val maxCount: Int = DEFAULT_MAX_COUNT,
    private val windowMs: Long = DEFAULT_WINDOW_MS,
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
) {
    /** 再開してよいかの判定結果。 */
    enum class Verdict {
        ALLOWED,

        /** この呼び出しで回数の上限に達し、停止期間に入った（診断に 1 回だけ出す契機） */
        LIMIT_REACHED,

        /** 停止期間中 */
        COOLING_DOWN,
    }

    private val history = ArrayDeque<Long>()
    private var blockedUntilMs = Long.MIN_VALUE

    fun tryAcquire(): Verdict {
        val nowMs = clock()
        if (nowMs < blockedUntilMs) return Verdict.COOLING_DOWN
        while (history.isNotEmpty() && nowMs - history.first() >= windowMs) history.removeFirst()
        if (history.size >= maxCount) {
            history.clear()
            blockedUntilMs = nowMs + cooldownMs
            return Verdict.LIMIT_REACHED
        }
        history.addLast(nowMs)
        return Verdict.ALLOWED
    }

    companion object {
        const val DEFAULT_MAX_COUNT = 3
        const val DEFAULT_WINDOW_MS = 30_000L
        const val DEFAULT_COOLDOWN_MS = 60_000L
    }
}
