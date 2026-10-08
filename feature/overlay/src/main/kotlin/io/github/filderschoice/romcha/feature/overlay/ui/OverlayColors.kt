package io.github.filderschoice.romcha.feature.overlay.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import io.github.filderschoice.romcha.feature.overlay.ThemeMode

/**
 * フローティングウィンドウの配色（F-VIEW-05）。
 *
 * @property header ヘッダー（ドラッグで移動する領域）の背景。チャット欄と見分けられる色にする
 * @property infoBand 案内のお知らせ帯（読み込み中など）の背景。失敗の赤帯 [ErrorBand] と見分けられる中立色にする
 * @property textShadow 文字の縁取り（影）。半透明の背景が暗い動画の上に重なっても文字が読めるようにする（BL-115）。不要なら null
 */
internal data class OverlayColors(
    val text: Color,
    val subText: Color,
    val header: Color,
    val background: Color,
    val handle: Color,
    val owner: Color,
    val moderator: Color,
    val member: Color,
    val infoBand: Color,
    val textShadow: Shadow? = null,
) {
    companion object {
        val Dark =
            OverlayColors(
                text = Color(0xFFF5F5F5),
                subText = Color(0xFFB0BEC5),
                header = Color(0xFF37474F),
                background = Color.Black,
                handle = Color.White.copy(alpha = 0.4f),
                owner = Color(0xFFFFD600),
                moderator = Color(0xFF5E84F1),
                member = Color(0xFF2BA640),
                infoBand = Color(0x6678909C),
            )

        val Light =
            OverlayColors(
                text = Color(0xFF212121),
                // 半透明の背景が暗い動画に重なっても読めるよう、補助の文字・アイコンも濃くする（BL-115）
                subText = Color(0xFF37474F),
                header = Color(0xFFB0BEC5),
                background = Color.White,
                handle = Color.Black.copy(alpha = 0.3f),
                owner = Color(0xFF9A6B00),
                moderator = Color(0xFF2448C8),
                member = Color(0xFF1B7F33),
                infoBand = Color(0x6690A4AE),
                // 濃い文字は暗い背景の上で沈むため、白の縁取りで対比を確保する。背景が白なら縁取りは見えない
                textShadow = Shadow(color = Color.White, offset = Offset.Zero, blurRadius = TEXT_HALO_RADIUS),
            )

        /** テーマ設定に対する配色。システム追従は従来どおり暗色にする（フローティングの既定の見た目を変えないため） */
        fun of(theme: ThemeMode): OverlayColors = if (theme == ThemeMode.LIGHT) Light else Dark
    }
}

private const val TEXT_HALO_RADIUS = 6f

internal val LocalOverlayColors = staticCompositionLocalOf { OverlayColors.Dark }

/** 失敗のお知らせ帯の背景（配色によらず同じ赤） */
internal val ErrorBand = Color(0x66B71C1C)

internal val OverlayTextColor: Color
    @Composable @ReadOnlyComposable
    get() = LocalOverlayColors.current.text

internal val SubTextColor: Color
    @Composable @ReadOnlyComposable
    get() = LocalOverlayColors.current.subText

internal val HeaderColor: Color
    @Composable @ReadOnlyComposable
    get() = LocalOverlayColors.current.header
