package io.github.filderschoice.romcha.core.media

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 公式アプリが一時停止したら、一定時間待って再開を依頼する（BL-118）。
 *
 * 再開できる状態（一時停止・PLAY が許可・画面オン・設定オン）になった時点から [waitMs] 待ち、待機中に状態が変わったら取り消す。
 * 再開しても止まったままなら同じ停止の間は再試行しない（一時停止のたびに 1 回だけ試み、回数は [limiter] も制限する）。
 * ログにはタイトルなどの個人情報・動画の情報を出さない。
 *
 * @param active 自動再開を働かせてよいか（設定がオンで、画面がオン）
 * @param resume 再開を依頼する（成功すれば true）
 * @param log Logcat への出力
 */
class AutoResumer(
    private val nowPlaying: Flow<NowPlaying>,
    private val active: Flow<Boolean>,
    private val resume: () -> Boolean,
    private val waitMs: Long = DEFAULT_WAIT_MS,
    private val limiter: ResumeLimiter = ResumeLimiter(SystemClock::elapsedRealtime),
    private val log: (String) -> Unit = {},
) {
    /** 戻らない。呼び出し側のコルーチンのキャンセルで止める。 */
    suspend fun run() {
        combine(nowPlaying, active) { playing, on ->
            on && playing.sessionFound && playing.canResume
        }.distinctUntilChanged().collectLatest { ready ->
            if (!ready) return@collectLatest
            log("paused detected, waiting ${waitMs}ms")
            delay(waitMs)
            attempt()
        }
    }

    private fun attempt() {
        when (limiter.tryAcquire()) {
            ResumeLimiter.Verdict.ALLOWED -> {
                val ok = resume()
                log("play() called, accepted=$ok")
                record(if (ok) ResumeEvent.RESUMED else ResumeEvent.FAILED)
            }
            ResumeLimiter.Verdict.LIMIT_REACHED -> {
                log("rate limit reached, auto resume paused")
                record(ResumeEvent.LIMIT_REACHED)
            }
            ResumeLimiter.Verdict.COOLING_DOWN -> log("cooling down, skipped")
        }
    }

    private fun record(event: ResumeEvent) = ResumeDiagnostics.record(event, System.currentTimeMillis())

    companion object {
        /** 一時停止を検知してから再開を依頼するまでの待ち時間 */
        const val DEFAULT_WAIT_MS = 2_000L
    }
}
