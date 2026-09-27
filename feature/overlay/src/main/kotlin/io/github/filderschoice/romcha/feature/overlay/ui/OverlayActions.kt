package io.github.filderschoice.romcha.feature.overlay.ui

import io.github.filderschoice.romcha.feature.overlay.OverlaySettings
import io.github.filderschoice.romcha.feature.overlay.WindowMode

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

    /** 表示状態を変える（バブルへの最小化 F-OVL-04、画面端への退避 BL-049、それらからの復帰） */
    fun onWindowModeChange(mode: WindowMode)

    /** ウィンドウからの単発の操作（タッチ透過・隠す・アプリを開く） */
    fun onCommand(command: OverlayCommand)

    fun onCandidateSelected(videoId: String)

    /** 手動タイマーモードの操作（F-SYNC-07） */
    fun onManual(command: ManualCommand)

    /** 文字入力の開始・終了。入力中だけウィンドウがフォーカスを取れるようにする */
    fun onInputFocus(focused: Boolean)
}

/** ウィンドウからの単発の操作。 */
enum class OverlayCommand {
    /** タッチ透過モードに入る（解除は常駐通知から。F-OVL-05） */
    TOUCH_THROUGH,

    /** ウィンドウを隠す（常駐通知から再表示） */
    HIDE,

    /** アプリ本体の画面を開く（BL-052。ウィンドウは表示したまま） */
    OPEN_APP,
}

/** 手動タイマーモードの操作（F-SYNC-07）。 */
sealed interface ManualCommand {
    data object Enable : ManualCommand

    data object Disable : ManualCommand

    data object Start : ManualCommand

    data object Stop : ManualCommand

    data class Seek(
        val positionMs: Long,
    ) : ManualCommand
}
