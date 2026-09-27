package io.github.filderschoice.romcha.core.media

import android.media.session.PlaybackState
import io.github.filderschoice.romcha.core.sync.PlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaMappingTest {
    @Test
    fun 再生状態を写す() {
        assertEquals(PlaybackStatus.PLAYING, MediaMapping.toStatus(PlaybackState.STATE_PLAYING))
        assertEquals(PlaybackStatus.PAUSED, MediaMapping.toStatus(PlaybackState.STATE_PAUSED))
        assertEquals(PlaybackStatus.BUFFERING, MediaMapping.toStatus(PlaybackState.STATE_BUFFERING))
        assertEquals(PlaybackStatus.STOPPED, MediaMapping.toStatus(PlaybackState.STATE_ERROR))
        assertEquals(PlaybackStatus.NONE, MediaMapping.toStatus(PlaybackState.STATE_NONE))
    }

    @Test
    fun スナップショットは負の位置を0にし速度0を等速とみなす() {
        val snapshot =
            MediaMapping.toSnapshot(
                state = PlaybackState.STATE_PAUSED,
                positionMs = -5,
                lastPositionUpdateTimeMs = 1_000,
                playbackSpeed = 0f,
            )
        assertEquals(0L, snapshot.positionMs)
        assertEquals(1f, snapshot.speed)
        assertEquals(1_000L, snapshot.updatedAtElapsedMs)
    }

    @Test
    fun 再生速度をそのまま保持する() {
        val snapshot = MediaMapping.toSnapshot(PlaybackState.STATE_PLAYING, 10_000, 0, 1.5f)
        assertEquals(1.5f, snapshot.speed)
    }

    @Test
    fun メタデータの全キーから動画IDの候補を探す() {
        val entries =
            mapOf(
                "metadata.android.media.metadata.TITLE" to "配信のタイトル",
                "description.mediaUri" to "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "metadata.android.media.metadata.MEDIA_ID" to "abcdefghijk",
                "metadata.android.media.metadata.ARTIST" to "abcdefghijk",
                "queue[0].mediaId" to "dQw4w9WgXcQ",
            )
        assertEquals(listOf("dQw4w9WgXcQ", "abcdefghijk"), MediaMapping.videoIdHints(entries))
    }

    @Test
    fun タイトルが無ければメタデータ無しとみなす() {
        assertNull(MediaMapping.toTrackMetadata(title = " ", artist = "ch", durationMs = 0, entries = emptyMap()))
    }

    @Test
    fun タイトルとチャンネル名と長さを保持する() {
        val metadata = MediaMapping.toTrackMetadata("タイトル", null, 3_600_000, emptyMap())!!
        assertEquals("タイトル", metadata.title)
        assertEquals("", metadata.channelName)
        assertEquals(3_600_000L, metadata.durationMs)
    }

    @Test
    fun デバッグ表示はキー順に並べる() {
        assertEquals(listOf("a = 1", "b = 2"), MediaMapping.debugLines(mapOf("b" to "2", "a" to "1")))
    }
}
