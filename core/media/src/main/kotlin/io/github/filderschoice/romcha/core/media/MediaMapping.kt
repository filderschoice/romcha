package io.github.filderschoice.romcha.core.media

import android.media.session.PlaybackState
import io.github.filderschoice.romcha.core.chat.VideoUrlParser
import io.github.filderschoice.romcha.core.chat.resolve.TrackMetadata
import io.github.filderschoice.romcha.core.sync.PlaybackSnapshot
import io.github.filderschoice.romcha.core.sync.PlaybackStatus

/**
 * MediaSession の値を Android 非依存のモデルへ変換する純粋関数群。
 *
 * Android の型を受け取らず、取り出した値（数値・文字列）だけを扱うため JVM の単体テストで検証できる。
 */
object MediaMapping {
    private val BARE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

    /** `PlaybackState.getState()` の値を [PlaybackStatus] へ写す。 */
    fun toStatus(state: Int): PlaybackStatus =
        when (state) {
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            -> PlaybackStatus.PLAYING
            PlaybackState.STATE_PAUSED -> PlaybackStatus.PAUSED
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_CONNECTING,
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM,
            -> PlaybackStatus.BUFFERING
            PlaybackState.STATE_STOPPED, PlaybackState.STATE_ERROR -> PlaybackStatus.STOPPED
            else -> PlaybackStatus.NONE
        }

    /**
     * PlaybackState の値から [PlaybackSnapshot] を作る（F-SYNC-01）。
     *
     * 一時停止中は速度 0 が報告されることがあるため、速度が正でない場合は 1.0 とみなす（再生中以外は位置推定に使わない）。
     */
    fun toSnapshot(
        state: Int,
        positionMs: Long,
        lastPositionUpdateTimeMs: Long,
        playbackSpeed: Float,
    ): PlaybackSnapshot =
        PlaybackSnapshot(
            status = toStatus(state),
            positionMs = positionMs.coerceAtLeast(0),
            updatedAtElapsedMs = lastPositionUpdateTimeMs,
            speed = if (playbackSpeed > 0f) playbackSpeed else 1f,
        )

    /**
     * メタデータの値から [TrackMetadata] を作る。
     *
     * @param entries MediaMetadata・MediaDescription・extras・キューの全キーと文字列値（[videoIdHints] の探索対象）
     */
    fun toTrackMetadata(
        title: String?,
        artist: String?,
        durationMs: Long,
        entries: Map<String, String>,
    ): TrackMetadata? {
        if (title.isNullOrBlank()) return null
        return TrackMetadata(
            title = title,
            channelName = artist.orEmpty(),
            durationMs = durationMs,
            videoIdHints = videoIdHints(entries),
        )
    }

    /**
     * 全キーの値から動画IDの候補を探す（PLAN 4.3 手順1）。
     *
     * 値に YouTube の URL が含まれればその ID、キー名の末尾要素（最後の `.` 以降）に `id` を含み
     * 値が 11 桁の ID 形式ならその値を候補にする（`android.media...` の `android` に一致させないため末尾要素だけを見る）。
     */
    fun videoIdHints(entries: Map<String, String>): List<String> =
        entries
            .mapNotNull { (key, value) -> VideoUrlParser.extractVideoId(value) ?: bareIdHint(key, value) }
            .distinct()

    private fun bareIdHint(
        key: String,
        value: String,
    ): String? {
        val idLikeKey = key.substringAfterLast('.').contains("id", ignoreCase = true)
        return value.takeIf { idLikeKey && BARE_VIDEO_ID.matches(it) }
    }

    /** デバッグ画面（M0 の Q-02 確認用）に出す「キー = 値」の一覧。キー順に並べる。 */
    fun debugLines(entries: Map<String, String>): List<String> =
        entries.toSortedMap().map { (key, value) -> "$key = $value" }
}
