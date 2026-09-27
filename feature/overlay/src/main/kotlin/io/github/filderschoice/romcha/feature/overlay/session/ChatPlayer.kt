package io.github.filderschoice.romcha.feature.overlay.session

import io.github.filderschoice.romcha.core.chat.FetchResult
import io.github.filderschoice.romcha.core.chat.VideoChatInfo
import io.github.filderschoice.romcha.core.sync.LiveChatSession
import io.github.filderschoice.romcha.core.sync.LiveState
import io.github.filderschoice.romcha.core.sync.LiveTimeline
import io.github.filderschoice.romcha.core.sync.PlaybackStatus
import io.github.filderschoice.romcha.core.sync.PositionEstimator
import io.github.filderschoice.romcha.core.sync.ReplaySession
import io.github.filderschoice.romcha.core.sync.ReplaySwitch
import io.github.filderschoice.romcha.core.sync.ReplaySwitcher
import io.github.filderschoice.romcha.core.sync.SessionTiming
import io.github.filderschoice.romcha.core.sync.VideoInfoSource
import io.github.filderschoice.romcha.feature.overlay.OverlayCandidate
import io.github.filderschoice.romcha.feature.overlay.OverlayUiState
import io.github.filderschoice.romcha.feature.overlay.SyncIndicator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/**
 * 1 本の動画のチャットを取得・同期して表示する（リプレイ F-CHAT-01〜03、ライブ・プレミア F-CHAT-04〜06 / F-SYNC-08）。
 *
 * 動画の情報から状態を判定し、アーカイブならリプレイを再生位置に同期させ、ライブ・プレミア中（待機中を含む）なら
 * 最新追従で表示する。ライブが終わったらリプレイの準備を待って切り替える。画面オフの間は取得を止める（N-03）。
 */
internal class ChatPlayer(
    private val backend: ChatBackend,
    private val env: SessionEnvironment,
    private val publisher: OverlayPublisher,
    private val timing: SessionTiming,
) {
    /** 動画を開く。キャンセルされるまで（またはチャットが使えないと分かるまで）戻らない。 */
    suspend fun open(
        videoId: String,
        alternatives: List<OverlayCandidate>,
    ) {
        publisher.show(
            OverlayUiState(
                title = publisher.base.title,
                notice = SessionMessages.LOADING_VIDEO,
                candidates = alternatives,
            ),
        )
        val info =
            when (val result = backend.videoInfo(videoId)) {
                is FetchResult.Failure -> {
                    publisher.show(publisher.base.copy(notice = SessionMessages.describe(result.failure)))
                    return
                }
                is FetchResult.Success -> result.value
            }
        val title = info.title.ifEmpty { null }
        when (info) {
            is VideoChatInfo.Unavailable ->
                publisher.show(
                    OverlayUiState(
                        title = title,
                        notice = info.message ?: SessionMessages.CHAT_UNAVAILABLE,
                        candidates = alternatives,
                    ),
                )
            is VideoChatInfo.Available -> {
                publisher.reset(OverlayUiState(title = title, candidates = alternatives))
                // 既定は「すべてのチャット」。閲覧専用ビューワーとして取りこぼしの無い表示を優先する（切り替え F-CHAT-07 は M4）
                val token = info.allChatToken ?: info.topChatToken
                if (info.isReplay) playReplay(token) else playLive(videoId, token)
            }
        }
    }

    private suspend fun playReplay(continuation: String) {
        whileScreenOn { replayLoop(continuation) }
    }

    /** ライブを最新追従で表示し、終わったらリプレイの準備を待って切り替える（F-CHAT-06）。 */
    private suspend fun playLive(
        videoId: String,
        continuation: String,
    ) {
        whileScreenOn { liveLoop(continuation) }
        val switcher = ReplaySwitcher(VideoInfoSource { backend.videoInfo(it) }, timing.replayWaitsMs)
        val switch =
            switcher.await(videoId) { attempt, _ ->
                publisher.show(publisher.base.copy(notice = SessionMessages.liveEnded(attempt)))
            }
        when (switch) {
            is ReplaySwitch.Ready -> playReplay(switch.continuation)
            is ReplaySwitch.StillLive -> playLive(videoId, switch.continuation)
            ReplaySwitch.Unavailable ->
                publisher.show(
                    publisher.base.copy(notice = SessionMessages.REPLAY_NOT_PROVIDED),
                )
        }
    }

    /** 画面が点いている間だけ [block] を動かし、消えたら止めて、点いたらやり直す（N-03）。[block] が戻ったら終わる。 */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun whileScreenOn(block: suspend () -> Unit) {
        env.screenOn
            .transformLatest { on ->
                if (on) {
                    block()
                    emit(Unit)
                } else {
                    publisher.show(publisher.base.copy(notice = SessionMessages.SCREEN_OFF))
                }
            }.first()
    }

    /** リプレイを再生位置に同期させて表示し続ける（戻らない）。 */
    private suspend fun replayLoop(continuation: String) {
        val session =
            ReplaySession(
                source = { token, offset, listener -> backend.replay(token, offset, listener) },
                initialContinuation = continuation,
                playback = { env.nowPlaying.value.snapshot },
                clock = env.clock,
                timing = timing,
            )
        coroutineScope {
            launch { session.run() }
            session.state.collect { replay ->
                val found = env.nowPlaying.value.sessionFound
                val notice =
                    SessionMessages.describe(replay.fetchStatus) ?: SessionMessages.NOT_DETECTED.takeIf { !found }
                publisher.frame(
                    publisher.base.copy(
                        messages = replay.messages,
                        positionMs = replay.positionMs,
                        indicator = SessionMessages.indicator(replay.status, found),
                        notice = notice,
                    ),
                )
            }
        }
    }

    /** ライブを最新追従で表示する。ライブが終わったら戻る。 */
    private suspend fun liveLoop(continuation: String) =
        coroutineScope {
            val session =
                LiveChatSession(
                    source = { token, listener -> backend.live(token, listener) },
                    initialContinuation = continuation,
                    playback = { env.nowPlaying.value.snapshot },
                    clock = env.clock,
                )
            val runner = launch { session.run() }
            val ticker =
                launch {
                    while (true) {
                        publishLive(session.state.value)
                        delay(timing.tickIntervalMs)
                    }
                }
            session.state.first { it.ended }
            publishLive(session.state.value)
            ticker.cancel()
            runner.cancel()
        }

    private fun publishLive(state: LiveState) {
        val now = env.clock()
        val snapshot = env.nowPlaying.value.snapshot
        val delayMs = LiveTimeline.delayMs(env.liveDelaySeconds.value)
        publisher.frame(
            publisher.base.copy(
                messages = LiveTimeline.visible(state.received, now, delayMs),
                positionMs = PositionEstimator.estimate(snapshot, now),
                indicator = if (snapshot.status == PlaybackStatus.PAUSED) SyncIndicator.PAUSED else SyncIndicator.LIVE,
                notice = SessionMessages.describe(state.fetchStatus),
            ),
        )
    }
}
