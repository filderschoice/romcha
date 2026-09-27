package io.github.filderschoice.romcha.feature.overlay.session

import io.github.filderschoice.romcha.core.chat.ChatAuthor
import io.github.filderschoice.romcha.core.chat.ChatContinuation
import io.github.filderschoice.romcha.core.chat.ChatMessage
import io.github.filderschoice.romcha.core.chat.ChatMessageKind
import io.github.filderschoice.romcha.core.chat.ChatParseResult
import io.github.filderschoice.romcha.core.chat.ContinuationKind
import io.github.filderschoice.romcha.core.chat.FetchResult
import io.github.filderschoice.romcha.core.chat.MessageRun
import io.github.filderschoice.romcha.core.chat.RetryListener
import io.github.filderschoice.romcha.core.chat.VideoChatInfo
import io.github.filderschoice.romcha.core.chat.resolve.InMemoryResolutionCache
import io.github.filderschoice.romcha.core.chat.resolve.SearchCandidate
import io.github.filderschoice.romcha.core.chat.resolve.TrackMetadata
import io.github.filderschoice.romcha.core.chat.resolve.VideoResolver
import io.github.filderschoice.romcha.core.media.NowPlaying
import io.github.filderschoice.romcha.core.sync.PlaybackSnapshot
import io.github.filderschoice.romcha.core.sync.PlaybackStatus
import io.github.filderschoice.romcha.feature.overlay.OverlayEvent
import io.github.filderschoice.romcha.feature.overlay.OverlayUiState
import io.github.filderschoice.romcha.feature.overlay.SyncIndicator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchCoordinatorTest {
    /** 動画ごとの応答を返し、呼び出しを記録する通信の偽物。 */
    private class FakeBackend : ChatBackend {
        val infos = mutableMapOf<String, VideoChatInfo>()
        val searchResults = mutableMapOf<String, List<SearchCandidate>>()
        val replayCalls = mutableListOf<String>()
        val searchQueries = mutableListOf<String>()

        fun replayVideo(
            id: String,
            title: String,
        ) {
            infos[id] =
                VideoChatInfo.Available(
                    videoId = id,
                    title = title,
                    channelName = "チャンネル",
                    isReplay = true,
                    topChatToken = "TOP_$id",
                    allChatToken = "ALL_$id",
                )
        }

        override suspend fun videoInfo(videoId: String): FetchResult<VideoChatInfo> =
            FetchResult.Success(
                infos.getValue(videoId),
            )

        override suspend fun replay(
            continuation: String,
            playerOffsetMs: Long?,
            listener: RetryListener,
        ): FetchResult<ChatParseResult.Success> {
            replayCalls += continuation
            val id = continuation.substringAfter('_')
            val message =
                ChatMessage(
                    id = "m_$id",
                    kind = ChatMessageKind.TEXT,
                    author = ChatAuthor("視聴者", null, null, emptySet()),
                    runs = listOf(MessageRun.Text("$id のチャット")),
                    timestampUsec = 0,
                    videoOffsetMs = 1_000,
                )
            return FetchResult.Success(
                ChatParseResult.Success(
                    listOf(message),
                    ChatContinuation("NEXT_$id", ContinuationKind.REPLAY, null),
                    0,
                ),
            )
        }

        override suspend fun search(query: String): FetchResult<List<SearchCandidate>> {
            searchQueries += query
            return FetchResult.Success(searchResults[query].orEmpty())
        }
    }

    private class Harness(
        scope: TestScope,
    ) {
        val backend = FakeBackend()
        val cache = InMemoryResolutionCache()
        val nowPlaying = MutableStateFlow(NowPlaying())
        val screenOn = MutableStateFlow(true)
        val requested = MutableStateFlow<String?>(null)
        val events = MutableSharedFlow<OverlayEvent>(extraBufferCapacity = 8)
        var published = OverlayUiState()

        init {
            val coordinator =
                WatchCoordinator(
                    backend = backend,
                    resolver = VideoResolver(search = { backend.search(it) }, cache = cache),
                    device = DeviceState(nowPlaying, screenOn),
                    io =
                        SessionIo(
                            requestedVideo = requested,
                            takeRequestedVideo = { requested.getAndUpdate { null } },
                            events = events,
                            publish = { published = it },
                        ),
                    clock = { scope.testScheduler.currentTime },
                )
            scope.backgroundScope.launch { coordinator.run() }
        }

        fun play(
            title: String,
            durationMs: Long = 600_000,
        ) {
            nowPlaying.value =
                NowPlaying(
                    snapshot = PlaybackSnapshot(PlaybackStatus.PLAYING, 5_000, 0, 1f),
                    metadata = TrackMetadata(title = title, channelName = "チャンネル", durationMs = durationMs),
                    sessionFound = true,
                )
        }

        fun searchable(
            id: String,
            title: String,
        ) {
            backend.replayVideo(id, title)
            backend.searchResults["$title チャンネル"] = listOf(SearchCandidate(id, title, "チャンネル", 600_000, isLive = false))
        }
    }

    @Test
    fun 再生中の動画を特定してリプレイを同期表示する() =
        runTest {
            val h = Harness(this)
            h.searchable("video000001", "配信A")

            h.play("配信A")
            advanceTimeBy(1_000)

            assertEquals("配信A", h.published.title)
            assertEquals(SyncIndicator.SYNCING, h.published.indicator)
            assertEquals(listOf("m_video000001"), h.published.messages.map { it.id })
            // 既定は「すべてのチャット」
            assertEquals("ALL_video000001", h.backend.replayCalls.first())
        }

    @Test
    fun 動画が切り替わったら特定をやり直す() =
        runTest {
            val h = Harness(this)
            h.searchable("video000001", "配信A")
            h.searchable("video000002", "配信B")

            h.play("配信A")
            advanceTimeBy(1_000)
            h.play("配信B")
            advanceTimeBy(1_000)

            assertEquals(listOf("配信A チャンネル", "配信B チャンネル"), h.backend.searchQueries)
            assertEquals(listOf("m_video000002"), h.published.messages.map { it.id })
        }

    @Test
    fun 手動で指定した動画を優先し再生中の動画が変わるまで自動特定しない() =
        runTest {
            val h = Harness(this)
            h.backend.replayVideo("manual00001", "指定した動画")
            h.searchable("video000002", "配信B")

            h.requested.value = "manual00001"
            advanceTimeBy(500)
            h.play("別の表記のタイトル")
            advanceTimeBy(1_000)

            assertTrue(h.backend.searchQueries.isEmpty())
            assertEquals("指定した動画", h.published.title)

            h.play("配信B")
            advanceTimeBy(1_000)
            assertEquals(listOf("配信B チャンネル"), h.backend.searchQueries)
            assertEquals("配信B", h.published.title)
        }

    @Test
    fun 確度が低ければ候補を出し選ばれた動画を開いて覚える() =
        runTest {
            val h = Harness(this)
            h.backend.replayVideo("candidate01", "配信C（切り抜き）")
            h.backend.searchResults["配信C チャンネル"] =
                listOf(SearchCandidate("candidate01", "配信C（切り抜き）", "別チャンネル", null, isLive = false))

            h.play("配信C")
            advanceTimeBy(500)
            assertEquals(SessionMessages.AMBIGUOUS, h.published.notice)
            assertEquals(listOf("candidate01"), h.published.candidates.map { it.videoId })

            h.events.emit(OverlayEvent.CandidateSelected("candidate01"))
            advanceTimeBy(1_000)

            assertEquals(listOf("m_candidate01"), h.published.messages.map { it.id })
            assertEquals("candidate01", h.cache.get(h.nowPlaying.value.metadata!!.identity))
        }

    @Test
    fun チャットが無効な動画とライブ中の動画は説明を表示する() =
        runTest {
            val h = Harness(this)
            h.backend.infos["disabled001"] = VideoChatInfo.Unavailable("disabled001", "無効", "チャンネル", message = null)
            h.backend.infos["live0000001"] =
                VideoChatInfo.Available("live0000001", "ライブ", "チャンネル", false, topChatToken = "T", allChatToken = null)

            h.requested.value = "disabled001"
            advanceTimeBy(500)
            assertEquals(SessionMessages.CHAT_UNAVAILABLE, h.published.notice)

            h.requested.value = "live0000001"
            advanceTimeBy(500)
            assertEquals(SessionMessages.LIVE_NOT_SUPPORTED, h.published.notice)
        }

    @Test
    fun 画面オフの間はチャットを取得しない() =
        runTest {
            val h = Harness(this)
            h.searchable("video000001", "配信A")
            h.screenOn.value = false

            h.play("配信A")
            advanceTimeBy(5_000)

            assertEquals(SessionMessages.SCREEN_OFF, h.published.notice)
            assertTrue(h.backend.replayCalls.isEmpty())

            h.screenOn.value = true
            advanceTimeBy(500)
            assertEquals(listOf("ALL_video000001"), h.backend.replayCalls.take(1))
        }

    @Test
    fun 再生を検出していなければその旨を表示する() =
        runTest {
            val h = Harness(this)
            advanceTimeBy(500)
            assertEquals(SessionMessages.NOT_DETECTED, h.published.notice)
        }
}
