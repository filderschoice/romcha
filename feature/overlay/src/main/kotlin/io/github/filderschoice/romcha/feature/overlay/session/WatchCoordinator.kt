package io.github.filderschoice.romcha.feature.overlay.session

import io.github.filderschoice.romcha.core.chat.FetchResult
import io.github.filderschoice.romcha.core.chat.VideoChatInfo
import io.github.filderschoice.romcha.core.chat.resolve.Resolution
import io.github.filderschoice.romcha.core.chat.resolve.ScoredCandidate
import io.github.filderschoice.romcha.core.chat.resolve.VideoResolver
import io.github.filderschoice.romcha.core.media.NowPlaying
import io.github.filderschoice.romcha.core.sync.ReplaySession
import io.github.filderschoice.romcha.core.sync.SessionTiming
import io.github.filderschoice.romcha.feature.overlay.OverlayCandidate
import io.github.filderschoice.romcha.feature.overlay.OverlayEvent
import io.github.filderschoice.romcha.feature.overlay.OverlayUiState
import io.github.filderschoice.romcha.feature.overlay.SyncIndicator
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * セッションの入出力。オーバーレイとの受け渡し口（`OverlayChannel`）を差し替えてテストできるようにする。
 *
 * @property requestedVideo 共有・URL 入力で指定された動画ID（F-VID-04/05）
 * @property takeRequestedVideo 指定された動画IDを取り出して保持をやめる
 * @property events オーバーレイでの利用者の操作
 * @property publish オーバーレイの表示内容を更新する
 */
class SessionIo(
    val requestedVideo: StateFlow<String?>,
    val takeRequestedVideo: () -> String?,
    val events: Flow<OverlayEvent>,
    val publish: (OverlayUiState) -> Unit,
)

/**
 * 端末側の状態。
 *
 * @property nowPlaying 公式アプリの再生状態
 * @property screenOn 画面が点いているか（N-03）
 */
class DeviceState(
    val nowPlaying: StateFlow<NowPlaying>,
    val screenOn: StateFlow<Boolean>,
)

/**
 * 公式アプリの再生検出 → 動画の特定 → チャット取得 → 同期 → オーバーレイ表示をつなぐ（PLAN 4.1、BL-012）。
 *
 * - 再生中の動画が変わったら（タイトル・チャンネル名・長さの変化。F-VID-03）自動特定をやり直す。
 * - 共有・URL 入力の指定（F-VID-04/05）は自動特定より優先し、その動画を見ている間は自動特定しない。
 * - 候補の選択（F-VID-02）はキャッシュへ覚え、以後は同じ動画を通信なしで確定する。
 * - 画面オフの間はチャットの取得を止める（N-03）。
 *
 * [run] は単一スレッドのディスパッチャー（本番はメインスレッド）で実行する。
 */
class WatchCoordinator(
    private val backend: ChatBackend,
    private val resolver: VideoResolver,
    private val device: DeviceState,
    private val io: SessionIo,
    private val clock: () -> Long,
    private val timing: SessionTiming = SessionTiming(),
) {
    private sealed interface Command {
        data object PlaybackChanged : Command

        data class Open(
            val videoId: String,
            val manual: Boolean,
        ) : Command

        data class Choose(
            val videoId: String,
        ) : Command
    }

    private val nowPlaying = device.nowPlaying
    private val screenOn = device.screenOn
    private var base = OverlayUiState()
    private var job: Job? = null

    /** 手動で指定した動画を見ている間の再生中の動画（null は未確定。最初に検出した動画をそれとみなす） */
    private var pinned: Pinned? = null

    private data class Pinned(
        val identity: String?,
    )

    suspend fun run() =
        coroutineScope {
            val commands = Channel<Command>(Channel.UNLIMITED)
            launch {
                nowPlaying.map { it.metadata?.identity }.distinctUntilChanged().collect {
                    commands.send(
                        Command.PlaybackChanged,
                    )
                }
            }
            launch {
                io.requestedVideo.filterNotNull().collect {
                    io.takeRequestedVideo()?.let { id -> commands.send(Command.Open(id, manual = true)) }
                }
            }
            launch {
                io.events.collect {
                    if (it is OverlayEvent.CandidateSelected) {
                        commands.send(
                            Command.Choose(it.videoId),
                        )
                    }
                }
            }
            for (command in commands) {
                val task = plan(command) ?: continue
                job?.cancel()
                job = launch { task() }
            }
        }

    /** 命令に対する処理を決める。何もしない場合は null（実行中の処理を止めない）。 */
    private fun plan(command: Command): (suspend () -> Unit)? =
        when (command) {
            is Command.Open -> {
                pinned = if (command.manual) Pinned(nowPlaying.value.metadata?.identity) else null
                suspend { open(command.videoId, alternatives = emptyList()) }
            }
            is Command.Choose -> {
                nowPlaying.value.metadata?.let { resolver.remember(it, command.videoId) }
                suspend {
                    open(
                        command.videoId,
                        alternatives = base.candidates.filter { it.videoId != command.videoId },
                    )
                }
            }
            Command.PlaybackChanged -> planForPlayback()
        }

    private fun planForPlayback(): (suspend () -> Unit)? {
        val metadata = nowPlaying.value.metadata
        if (keepPinned(metadata?.identity)) return null
        if (metadata == null) return suspend { show(OverlayUiState(notice = SessionMessages.NOT_DETECTED)) }
        return suspend { resolveAndOpen() }
    }

    /**
     * 手動で指定した動画を見続けているか。見続けていれば true（自動特定しない）。
     *
     * 指定時に再生を検出していなかった場合は、最初に検出した動画を指定した動画とみなす。
     * 別の動画に変わったら指定を解除して false を返す。
     */
    private fun keepPinned(identity: String?): Boolean {
        val pin = pinned ?: return false
        val keep =
            when {
                identity == null -> true
                pin.identity == null -> {
                    pinned = Pinned(identity)
                    true
                }
                else -> pin.identity == identity
            }
        if (!keep) pinned = null
        return keep
    }

    private suspend fun resolveAndOpen() {
        val metadata = nowPlaying.value.metadata ?: return
        show(OverlayUiState(title = metadata.title, notice = SessionMessages.RESOLVING))
        when (val resolution = resolver.resolve(metadata)) {
            is Resolution.Confirmed -> open(resolution.videoId, resolution.alternatives.map(::toCandidate))
            is Resolution.Ambiguous ->
                show(
                    OverlayUiState(
                        title = metadata.title,
                        notice = SessionMessages.AMBIGUOUS,
                        candidates = resolution.candidates.map(::toCandidate),
                    ),
                )
            Resolution.NotFound -> show(OverlayUiState(title = metadata.title, notice = SessionMessages.NOT_FOUND))
            is Resolution.Failed ->
                show(
                    OverlayUiState(title = metadata.title, notice = SessionMessages.describe(resolution.failure)),
                )
        }
    }

    private suspend fun open(
        videoId: String,
        alternatives: List<OverlayCandidate>,
    ) {
        show(OverlayUiState(title = base.title, notice = SessionMessages.LOADING_VIDEO, candidates = alternatives))
        val info =
            when (val result = backend.videoInfo(videoId)) {
                is FetchResult.Failure -> {
                    show(base.copy(notice = SessionMessages.describe(result.failure)))
                    return
                }
                is FetchResult.Success -> result.value
            }
        val title = info.title.ifEmpty { null }
        when {
            info is VideoChatInfo.Unavailable ->
                show(
                    OverlayUiState(
                        title = title,
                        notice = info.message ?: SessionMessages.CHAT_UNAVAILABLE,
                        candidates = alternatives,
                    ),
                )
            info is VideoChatInfo.Available && !info.isReplay ->
                show(
                    OverlayUiState(
                        title = title,
                        notice = SessionMessages.LIVE_NOT_SUPPORTED,
                        candidates = alternatives,
                    ),
                )
            info is VideoChatInfo.Available -> {
                base = OverlayUiState(title = title, candidates = alternatives)
                runReplay(info.allChatToken ?: info.topChatToken)
            }
        }
    }

    /** 画面が点いている間だけリプレイを同期させる（N-03）。 */
    private suspend fun runReplay(continuation: String) {
        screenOn.collectLatest { on ->
            if (!on) {
                show(base.copy(notice = SessionMessages.SCREEN_OFF))
                return@collectLatest
            }
            val session =
                ReplaySession(
                    source = { token, offset, listener -> backend.replay(token, offset, listener) },
                    initialContinuation = continuation,
                    playback = { nowPlaying.value.snapshot },
                    clock = clock,
                    timing = timing,
                )
            coroutineScope {
                launch { session.run() }
                session.state.collect { replay ->
                    val found = nowPlaying.value.sessionFound
                    val notice =
                        SessionMessages.describe(replay.fetchStatus) ?: SessionMessages.NOT_DETECTED.takeIf { !found }
                    io.publish(
                        base.copy(
                            messages = replay.messages,
                            positionMs = replay.positionMs,
                            indicator = SessionMessages.indicator(replay.status, found),
                            notice = notice,
                        ),
                    )
                }
            }
        }
    }

    private fun show(state: OverlayUiState) {
        base = state.copy(messages = emptyList(), indicator = SyncIndicator.NOT_DETECTED)
        io.publish(state)
    }

    private fun toCandidate(scored: ScoredCandidate) =
        OverlayCandidate(scored.candidate.videoId, scored.candidate.title, scored.candidate.channelName)
}
