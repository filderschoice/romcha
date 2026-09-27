package io.github.filderschoice.romcha.feature.overlay

import io.github.filderschoice.romcha.core.sync.LiveTimeline
import io.github.filderschoice.romcha.core.sync.SyncOffset

/**
 * フローティングウィンドウの設定パネルで変える値。
 *
 * @property opacity 背景の不透明度（F-OVL-03）
 * @property fontScale チャットの文字サイズの倍率（1.0 が中。F-VIEW-01）
 * @property liveDelaySeconds ライブ・プレミア中の表示遅延（秒。F-SYNC-08）
 * @property syncOffsetMs リプレイの同期オフセットの手動補正（ミリ秒。F-SYNC-06）
 */
data class OverlaySettings(
    val opacity: Float = DEFAULT_OPACITY,
    val fontScale: Float = OverlayFormat.DEFAULT_FONT_SCALE,
    val liveDelaySeconds: Int = LiveTimeline.DEFAULT_DELAY_SECONDS,
    val syncOffsetMs: Long = SyncOffset.DEFAULT_MS,
) {
    /** 各値を設定範囲・刻みに収める。 */
    fun normalized(): OverlaySettings =
        OverlaySettings(
            opacity = OverlayFormat.clampOpacity(opacity),
            fontScale = OverlayFormat.clampFontScale(fontScale),
            liveDelaySeconds =
                liveDelaySeconds.coerceIn(
                    LiveTimeline.MIN_DELAY_SECONDS,
                    LiveTimeline.MAX_DELAY_SECONDS,
                ),
            syncOffsetMs = SyncOffset.clamp(syncOffsetMs),
        )

    companion object {
        const val DEFAULT_OPACITY = 0.6f
    }
}
