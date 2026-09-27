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

    fun onHide()

    fun onCandidateSelected(videoId: String)
}
