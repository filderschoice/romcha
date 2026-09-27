package io.github.filderschoice.romcha.feature.overlay

import io.github.filderschoice.romcha.core.sync.ManualTimer
import io.github.filderschoice.romcha.core.sync.PlaybackSnapshot
import io.github.filderschoice.romcha.feature.overlay.ui.ManualCommand

/** 手動タイマーモードの操作を状態へ反映する（F-SYNC-07。Android 非依存）。 */
internal object ManualControl {
    /**
     * @param timer 現在の状態（null はオフ）
     * @param displayedPositionMs オンにした時の開始位置（表示中の位置から同期の補正を除いたもの）
     * @return 操作後の状態（null はオフ）
     */
    fun apply(
        timer: PlaybackSnapshot?,
        command: ManualCommand,
        displayedPositionMs: Long,
        nowElapsedMs: Long,
    ): PlaybackSnapshot? =
        when (command) {
            ManualCommand.Enable -> timer ?: ManualTimer.paused(displayedPositionMs, nowElapsedMs)
            ManualCommand.Disable -> null
            ManualCommand.Start -> timer?.let { ManualTimer.start(it, nowElapsedMs) }
            ManualCommand.Stop -> timer?.let { ManualTimer.stop(it, nowElapsedMs) }
            is ManualCommand.Seek -> timer?.let { ManualTimer.seek(it, command.positionMs, nowElapsedMs) }
        }
}
