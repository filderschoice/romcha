package io.github.filderschoice.romcha.feature.overlay.ui

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

    /** 移動・サイズ変更の操作が終わった（位置と大きさを保存する契機） */
    fun onGestureEnd()

    fun onOpacityChange(opacity: Float)

    /** チャットの文字サイズの倍率を変えた（F-VIEW-01） */
    fun onFontScaleChange(scale: Float)

    /** ライブ・プレミア中の表示遅延（秒）を変えた（F-SYNC-08） */
    fun onLiveDelayChange(seconds: Int)

    /** タッチ透過モードに入る（解除は常駐通知から。F-OVL-05） */
    fun onTouchThrough()

    /** バブルへ最小化する／バブルから元の大きさに戻す（F-OVL-04） */
    fun onMinimizeChange(minimized: Boolean)

    fun onHide()

    fun onCandidateSelected(videoId: String)
}
