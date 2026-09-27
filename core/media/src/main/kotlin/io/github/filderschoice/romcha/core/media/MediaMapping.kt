package io.github.filderschoice.romcha.core.media

import android.media.MediaMetadata
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

    /**
     * MediaMetadata のキーの値の型（`MediaMetadata` の型別の取得メソッドを選ぶために使う）。
     *
     * 型の違う取得メソッドで読むと、Android の Bundle が ClassCastException のスタックトレースを警告として
     * logcat へ出す（値は null / 0 が返るだけで落ちない）。再生状態の変化ごとに全キーを読むため大量に出る。
     */
    enum class MetadataValueType {
        TEXT,
        LONG,

        /** 画像（Bitmap）・評価（Rating）。文字列化しない */
        OTHER,

        /** 標準外のキー。型が分からないため文字列 → 数値の順に試す */
        UNKNOWN,
    }

    /**
     * MediaMetadata のキーの型。`MediaMetadata.Builder` が標準キーに型を強制するため、標準キーは型が確定する。
     * 公式アプリ独自のキーは実機で型を確かめたものだけを登録する。
     */
    fun metadataValueType(key: String): MetadataValueType =
        when (key) {
            in LONG_METADATA_KEYS -> MetadataValueType.LONG
            in OTHER_METADATA_KEYS -> MetadataValueType.OTHER
            in TEXT_METADATA_KEYS -> MetadataValueType.TEXT
            else -> MetadataValueType.UNKNOWN
        }

    private val LONG_METADATA_KEYS =
        setOf(
            MediaMetadata.METADATA_KEY_DURATION,
            MediaMetadata.METADATA_KEY_YEAR,
            MediaMetadata.METADATA_KEY_TRACK_NUMBER,
            MediaMetadata.METADATA_KEY_NUM_TRACKS,
            MediaMetadata.METADATA_KEY_DISC_NUMBER,
            MediaMetadata.METADATA_KEY_BT_FOLDER_TYPE,
            // 公式アプリ独自のキー（2026-09-27 実機確認。YouTube 21.38.130）
            "com.google.android.youtube.MEDIA_METADATA_VIDEO_HEIGHT_PX",
            "com.google.android.youtube.MEDIA_METADATA_VIDEO_WIDTH_PX",
        )

    private val OTHER_METADATA_KEYS =
        setOf(
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON,
            MediaMetadata.METADATA_KEY_RATING,
            MediaMetadata.METADATA_KEY_USER_RATING,
        )

    private val TEXT_METADATA_KEYS =
        setOf(
            MediaMetadata.METADATA_KEY_TITLE,
            MediaMetadata.METADATA_KEY_ARTIST,
            MediaMetadata.METADATA_KEY_AUTHOR,
            MediaMetadata.METADATA_KEY_WRITER,
            MediaMetadata.METADATA_KEY_COMPOSER,
            MediaMetadata.METADATA_KEY_COMPILATION,
            MediaMetadata.METADATA_KEY_DATE,
            MediaMetadata.METADATA_KEY_GENRE,
            MediaMetadata.METADATA_KEY_ALBUM,
            MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
            MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
            MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
            MediaMetadata.METADATA_KEY_MEDIA_ID,
            MediaMetadata.METADATA_KEY_MEDIA_URI,
        )

    /** デバッグ画面（M0 の Q-02 確認用）に出す「キー = 値」の一覧。キー順に並べる。 */
    fun debugLines(entries: Map<String, String>): List<String> =
        entries.toSortedMap().map { (key, value) -> "$key = $value" }
}
