package io.github.filderschoice.romcha.feature.overlay.session

import io.github.filderschoice.romcha.core.media.NowPlaying
import io.github.filderschoice.romcha.core.sync.LiveTimeline
import io.github.filderschoice.romcha.core.sync.PlaybackSnapshot
import io.github.filderschoice.romcha.core.sync.SyncConfig
import io.github.filderschoice.romcha.core.sync.SyncOffset
import io.github.filderschoice.romcha.feature.overlay.OverlayEvent
import io.github.filderschoice.romcha.feature.overlay.OverlayUiState
import io.github.filderschoice.romcha.feature.overlay.SyncIndicator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
 * 端末側の状態と設定。
 *
 * @property nowPlaying 公式アプリの再生状態
 * @property screenOn 画面が点いているか（N-03）
 * @property settings 利用者が変える設定
 * @property clock 経過時間（本番は `SystemClock.elapsedRealtime`。PlaybackState の位置の報告時刻と同じ時計）
 * @property manualTimer 手動タイマーモードの状態（F-SYNC-07）。null ならオフで、公式アプリの再生状態に同期する
 */
class SessionEnvironment(
    val nowPlaying: StateFlow<NowPlaying>,
    val screenOn: StateFlow<Boolean>,
    val settings: SessionSettings,
    val clock: () -> Long,
    val manualTimer: StateFlow<PlaybackSnapshot?> = MutableStateFlow(null),
)

/**
 * セッションが使う利用者の設定。
 *
 * @property liveDelaySeconds ライブ・プレミア中の表示遅延（秒。F-SYNC-08）
 * @property syncOffsetMs リプレイの同期オフセットの手動補正（ミリ秒。F-SYNC-06）
 * @property maxVisible 表示保持件数の上限（F-VIEW-04）。取得・同期の開始時の値を使う
 */
class SessionSettings(
    val liveDelaySeconds: StateFlow<Int> = MutableStateFlow(LiveTimeline.DEFAULT_DELAY_SECONDS),
    val syncOffsetMs: StateFlow<Long> = MutableStateFlow(SyncOffset.DEFAULT_MS),
    val maxVisible: StateFlow<Int> = MutableStateFlow(SyncConfig().maxVisible),
)

/** オーバーレイへ出す表示内容の土台（タイトル・候補）を保持し、表示を更新する。 */
internal class OverlayPublisher(
    private val publish: (OverlayUiState) -> Unit,
) {
    /** 現在の動画のタイトル・候補・お知らせ（メッセージを除く） */
    var base = OverlayUiState()
        private set

    /** メッセージ一覧を伴わない表示（読み込み中・エラー・候補の提示など）に切り替える。 */
    fun show(state: OverlayUiState) {
        base = state.copy(messages = emptyList(), indicator = SyncIndicator.NOT_DETECTED)
        publish(state)
    }

    /** 以後の表示の土台だけを差し替える（表示はまだ更新しない）。 */
    fun reset(state: OverlayUiState) {
        base = state
    }

    fun frame(state: OverlayUiState) = publish(state)
}
