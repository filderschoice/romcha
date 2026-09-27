package io.github.filderschoice.romcha.feature.overlay.ui

import io.github.filderschoice.romcha.feature.overlay.OverlaySettings

/** ウィンドウの操作（ドラッグ量は px）。 */
interface OverlayActions {
    fun onMove(
        dx: Float,
        dy: Float,
    )

    fun onResize(
        dx: Float,
        dy: Float,
    )

    /** 移動・サイズ変更・スライダーの操作が終わった（位置・大きさ・設定を保存する契機） */
    fun onGestureEnd()

    /** 設定パネルの値を変えた（保存は [onGestureEnd] で行う） */
    fun onSettingsChange(settings: OverlaySettings)

    /** タッチ透過モードに入る（解除は常駐通知から。F-OVL-05） */
    fun onTouchThrough()

    /** バブルへ最小化する／バブルから元の大きさに戻す（F-OVL-04） */
    fun onMinimizeChange(minimized: Boolean)

    fun onHide()

    fun onCandidateSelected(videoId: String)
}
