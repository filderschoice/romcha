package io.github.filderschoice.romcha.feature.overlay.session

import io.github.filderschoice.romcha.core.chat.ChatParseResult
import io.github.filderschoice.romcha.core.chat.FetchResult
import io.github.filderschoice.romcha.core.chat.RetryListener
import io.github.filderschoice.romcha.core.chat.VideoChatInfo
import io.github.filderschoice.romcha.core.sync.LiveChatSession
import io.github.filderschoice.romcha.core.sync.LiveState
import io.github.filderschoice.romcha.core.sync.LiveTimeline
import io.github.filderschoice.romcha.core.sync.PlaybackSnapshot
import io.github.filderschoice.romcha.core.sync.PlaybackStatus
import io.github.filderschoice.romcha.core.sync.PositionEstimator
import io.github.filderschoice.romcha.core.sync.ReplaySession
import io.github.filderschoice.romcha.core.sync.ReplaySwitch
import io.github.filderschoice.romcha.core.sync.ReplaySwitcher
import io.github.filderschoice.romcha.core.sync.SessionTiming
import io.github.filderschoice.romcha.core.sync.SyncOffset
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
                if (info.isReplay) playReplay(info.topChatToken) else playLive(videoId, info.topChatToken)
            }
        }
    }

    private suspend fun playReplay(topChatToken: String) {
        whileScreenOn {
            replayLoop(allChatToken(topChatToken) { token, listener -> backend.replay(token, 0L, listener) })
        }
    }

    /** ライブを最新追従で表示し、終わったらリプレイの準備を待って切り替える（F-CHAT-06）。 */
    private suspend fun playLive(
        videoId: String,
        topChatToken: String,
    ) {
        whileScreenOn { liveLoop(allChatToken(topChatToken) { token, listener -> backend.live(token, listener) }) }
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

    /**
     * 「上位チャット」の continuation で 1 回取得し、応答の見出しから「すべてのチャット」の continuation を得る。
     *
     * 既定は「すべてのチャット」。閲覧専用ビューワーとして取りこぼしの無い表示を優先する（切り替え F-CHAT-07 は M4）。
     * 取得に失敗した・見出しが無い場合は「上位チャット」のまま続ける（失敗の表示と再試行は各セッションに任せる）。
     * 画面オンのたびに取り直す（前回の失敗で「上位チャット」になっていても、次の画面オンで切り替えられる）。
     */
    private suspend fun allChatToken(
        topChatToken: String,
        fetch: suspend (String, RetryListener) -> FetchResult<ChatParseResult.Success>,
    ): String {
        val result = fetch(topChatToken) { _, _, _ -> }
        return (result as? FetchResult.Success)?.value?.allChatToken ?: topChatToken
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
                playback = { SyncOffset.apply(replayPlayback(), env.syncOffsetMs.value) },
                clock = env.clock,
                timing = timing,
            )
        coroutineScope {
            launch { session.run() }
            session.state.collect { replay ->
                val manual = env.manualTimer.value != null
                val found = env.nowPlaying.value.sessionFound
                val notDetected = SessionMessages.NOT_DETECTED.takeIf { !found && !manual }
                val notice = SessionMessages.describe(replay.fetchStatus) ?: notDetected
                publisher.frame(
                    publisher.base.copy(
                        messages = replay.messages,
                        positionMs = replay.positionMs,
                        indicator =
                            if (manual) {
                                SyncIndicator.MANUAL
                            } else {
                                SessionMessages.indicator(
                                    replay.status,
                                    found,
                                )
                            },
                        notice = notice,
                    ),
                )
            }
        }
    }

    /** リプレイの同期に使う再生状態。手動タイマーモード中はその状態を使う（F-SYNC-07） */
    private fun replayPlayback(): PlaybackSnapshot = env.manualTimer.value ?: env.nowPlaying.value.snapshot

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
