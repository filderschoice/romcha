package io.github.filderschoice.romcha.feature.overlay

import android.content.Context
import androidx.core.content.edit

/**
 * 設定の初期化（BL-097）。ウィンドウの位置・大きさ・不透明度などの保存値（`overlay`）を消し、
 * 表示設定（NG ワードを含む）を初期値へ戻す。オーバーレイの動作中に呼ぶと表示と保存値がずれるため、呼び出し側で先に止める。
 */
object OverlaySettingsReset {
    fun resetAll(context: Context) {
        context.applicationContext.getSharedPreferences("overlay", Context.MODE_PRIVATE).edit { clear() }
        DisplaySettingsStore.init(context)
        DisplaySettingsStore.update { DisplaySettings() }
    }
}
