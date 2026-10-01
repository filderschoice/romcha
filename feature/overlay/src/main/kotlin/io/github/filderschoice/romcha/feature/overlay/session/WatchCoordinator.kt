package io.github.filderschoice.romcha.feature.overlay.session

import io.github.filderschoice.romcha.core.chat.resolve.Resolution
import io.github.filderschoice.romcha.core.chat.resolve.ScoredCandidate
import io.github.filderschoice.romcha.core.chat.resolve.VideoResolver
import io.github.filderschoice.romcha.core.sync.SessionTiming
import io.github.filderschoice.romcha.feature.overlay.OverlayCandidate
import io.github.filderschoice.romcha.feature.overlay.OverlayEvent
import io.github.filderschoice.romcha.feature.overlay.OverlayUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 公式アプリの再生検出 → 動画の特定 → チャット取得 → 同期 → オーバーレイ表示をつなぐ（PLAN 4.1、BL-012 / BL-018）。
 *
 * - 再生中の動画が変わったら（タイトル・チャンネル名・長さの変化。F-VID-03）自動特定をやり直す。
 * - 共有・URL 入力の指定（F-VID-04/05）は自動特定より優先し、その動画を見ている間は自動特定しない。
 * - 候補の選択（F-VID-02）はキャッシュへ覚え、以後は同じ動画を通信なしで確定する。
 * - キャッシュが消されたら（アプリ画面の操作）、手動指定でなければ見ている動画の特定をやり直す。
 * - 動画の状態（アーカイブ／ライブ・プレミア）に応じた取得と表示は [ChatPlayer] が行う。
 *
 * [run] は単一スレッドのディスパッチャー（本番はメインスレッド）で実行する。
 */
class WatchCoordinator(
    backend: ChatBackend,
    private val resolver: VideoResolver,
    private val env: SessionEnvironment,
    private val io: SessionIo,
    timing: SessionTiming = SessionTiming(),
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

        data object ResolutionCacheCleared : Command
    }

    private val nowPlaying = env.nowPlaying
    private val publisher = OverlayPublisher(io.publish)
    private val player = ChatPlayer(backend, env, publisher, timing)
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
                nowPlaying
                    .map { it.metadata?.identity }
                    .distinctUntilChanged()
                    .collect { commands.send(Command.PlaybackChanged) }
            }
            launch {
                io.requestedVideo.filterNotNull().collect {
                    io.takeRequestedVideo()?.let { id -> commands.send(Command.Open(id, manual = true)) }
                }
            }
            launch {
                io.events.collect { event ->
                    when (event) {
                        is OverlayEvent.CandidateSelected -> commands.send(Command.Choose(event.videoId))
                        OverlayEvent.ResolutionCacheCleared -> commands.send(Command.ResolutionCacheCleared)
                        OverlayEvent.StopRequested -> Unit
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
                suspend { player.open(command.videoId, alternatives = emptyList()) }
            }
            is Command.Choose -> {
                nowPlaying.value.metadata?.let { resolver.remember(it, command.videoId) }
                val alternatives = publisher.base.candidates.filter { it.videoId != command.videoId }
                suspend { player.open(command.videoId, alternatives) }
            }
            Command.PlaybackChanged -> planForPlayback()
            Command.ResolutionCacheCleared -> planForClearedCache()
        }

    /**
     * キャッシュが消された時、見ている動画の特定をやり直す。手動で指定した動画（キャッシュを使わない）と、
     * 再生を検出していない間は何もしない。
     */
    private fun planForClearedCache(): (suspend () -> Unit)? {
        if (pinned != null || nowPlaying.value.metadata == null) return null
        return suspend { resolveAndOpen() }
    }

    private fun planForPlayback(): (suspend () -> Unit)? {
        val metadata = nowPlaying.value.metadata
        if (keepPinned(metadata?.identity)) return null
        if (metadata == null) return suspend { publisher.show(OverlayUiState(notice = SessionMessages.NOT_DETECTED)) }
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
        publisher.show(OverlayUiState(title = metadata.title, notice = SessionMessages.RESOLVING))
        val state =
            when (val resolution = resolver.resolve(metadata)) {
                is Resolution.Confirmed -> {
                    player.open(resolution.videoId, resolution.alternatives.map(::toCandidate))
                    return
                }
                is Resolution.Ambiguous ->
                    OverlayUiState(
                        notice = SessionMessages.AMBIGUOUS,
                        candidates = resolution.candidates.map(::toCandidate),
                    )
                Resolution.NotFound -> OverlayUiState(notice = SessionMessages.NOT_FOUND)
                is Resolution.Failed -> OverlayUiState(notice = SessionMessages.describe(resolution.failure))
            }
        publisher.show(state.copy(title = metadata.title))
    }

    private fun toCandidate(scored: ScoredCandidate) =
        OverlayCandidate(scored.candidate.videoId, scored.candidate.title, scored.candidate.channelName)
}
