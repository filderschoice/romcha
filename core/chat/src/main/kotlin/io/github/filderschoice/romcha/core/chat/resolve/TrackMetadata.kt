package io.github.filderschoice.romcha.core.chat.resolve

/**
 * 公式アプリが MediaSession で公開している再生中の動画の情報（PLAN 4.2 / 4.3）。Android 非依存。
 *
 * @property channelName `METADATA_KEY_ARTIST`（公式アプリではチャンネル名が入る想定。M0 で確認）
 * @property durationMs 動画の長さ。0 以下はライブ・プレミア中などで不明（4.3 手順3）
 * @property videoIdHints メタデータの全キーから見つかった動画IDの候補（4.3 手順1。取れるかは M0 で確認）
 */
data class TrackMetadata(
    val title: String,
    val channelName: String,
    val durationMs: Long,
    val videoIdHints: List<String> = emptyList(),
) {
    /** 動画の切り替え検知（F-VID-03）に使う識別キー。タイトル・チャンネル名・長さのいずれかが変われば別の動画とみなす。 */
    val identity: String get() = "$title\u0000$channelName\u0000$durationMs"
}
