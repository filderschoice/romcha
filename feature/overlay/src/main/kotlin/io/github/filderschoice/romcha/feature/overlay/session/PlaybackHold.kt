package io.github.filderschoice.romcha.feature.overlay.session

import io.github.filderschoice.romcha.core.sync.PositionEstimator
import io.github.filderschoice.romcha.core.sync.SessionTiming
import io.github.filderschoice.romcha.feature.overlay.OverlayCandidate
import io.github.filderschoice.romcha.feature.overlay.OverlayNotice
import io.github.filderschoice.romcha.feature.overlay.OverlayUiState
import io.github.filderschoice.romcha.feature.overlay.SyncIndicator
import kotlinx.coroutines.delay

/**
 * チャットを出せない間も、同期状態と再生位置を更新し続ける（BL-108）。
 *
 * 動画は特定できているため、公式アプリが再生中なら「同期中」と再生位置を出す。出さないと「未検出 0:00」のまま止まって見える。
 */
internal class PlaybackHold(
    private val env: SessionEnvironment,
    private val publisher: OverlayPublisher,
    private val timing: SessionTiming,
) {
    /** チャットを出せない旨を表示し、そのまま同期状態と再生位置を更新し続ける（戻らない）。 */
    suspend fun showUnavailable(
        title: String?,
        notice: OverlayNotice,
        alternatives: List<OverlayCandidate>,
    ) {
        val state = OverlayUiState(title = title, notice = notice, candidates = alternatives)
        publisher.show(state)
        hold(state)
    }

    /** [state] を表示し続ける（戻らない。呼び出し側がキャンセルする）。 */
    suspend fun hold(state: OverlayUiState) {
        while (true) {
            val manual = env.manualTimer.value
            val snapshot = manual ?: env.nowPlaying.value.snapshot
            publisher.frame(
                state.copy(
                    positionMs = PositionEstimator.estimate(snapshot, env.clock()),
                    indicator =
                        if (manual != null) {
                            SyncIndicator.MANUAL
                        } else {
                            SessionMessages.indicator(snapshot.status, env.nowPlaying.value.sessionFound)
                        },
                ),
            )
            delay(timing.tickIntervalMs)
        }
    }
}
