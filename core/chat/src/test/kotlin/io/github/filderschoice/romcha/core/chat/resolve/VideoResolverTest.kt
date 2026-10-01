package io.github.filderschoice.romcha.core.chat.resolve

import io.github.filderschoice.romcha.core.chat.FetchFailure
import io.github.filderschoice.romcha.core.chat.FetchResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoResolverTest {
    private val metadata = TrackMetadata(title = "【歌枠】夜の配信", channelName = "テストチャンネル", durationMs = 3_723_000)

    private class FakeSearch(
        private val result: FetchResult<List<SearchCandidate>>,
        private val liveResult: FetchResult<List<SearchCandidate>> = FetchResult.Success(emptyList()),
    ) : VideoSearchSource {
        val queries = mutableListOf<String>()
        val liveQueries = mutableListOf<String>()

        override suspend fun search(
            query: String,
            liveOnly: Boolean,
        ): FetchResult<List<SearchCandidate>> {
            if (liveOnly) {
                liveQueries += query
                return liveResult
            }
            queries += query
            return result
        }
    }

    private fun candidate(
        id: String,
        title: String,
        channel: String = "テストチャンネル",
        durationMs: Long? = 3_723_000,
    ) = SearchCandidate(id, title, channel, durationMs, isLive = false)

    private fun liveCandidate(live: TrackMetadata) =
        SearchCandidate("live0000000", live.title, live.channelName, durationMs = null, isLive = true)

    private fun fixture(name: String): String = requireNotNull(javaClass.getResource("/fixtures/$name")).readText()

    @Test
    fun MediaSessionに動画IDがあれば通信せずに確定しキャッシュする() =
        runTest {
            val search = FakeSearch(FetchResult.Success(emptyList()))
            val cache = InMemoryResolutionCache()

            val result = VideoResolver(search, cache).resolve(metadata.copy(videoIdHints = listOf("hint0000000")))

            assertEquals(Resolution.Confirmed("hint0000000", ResolutionSource.METADATA), result)
            assertTrue(search.queries.isEmpty())
            assertEquals("hint0000000", cache.get(metadata.identity))
        }

    @Test
    fun キャッシュにあれば通信せずに確定する() =
        runTest {
            val search = FakeSearch(FetchResult.Success(emptyList()))
            val cache = InMemoryResolutionCache().apply { put(metadata.identity, "cached00000") }

            val result = VideoResolver(search, cache).resolve(metadata)

            assertEquals(Resolution.Confirmed("cached00000", ResolutionSource.CACHE), result)
            assertTrue(search.queries.isEmpty())
        }

    @Test
    fun キャッシュを消すと以後は検索して特定し直す() =
        runTest {
            val candidates = listOf(candidate("fresh000000", metadata.title))
            val search = FakeSearch(FetchResult.Success(candidates))
            val cache = InMemoryResolutionCache().apply { put(metadata.identity, "stale000000") }
            val resolver = VideoResolver(search, cache)

            cache.clear()
            val result = resolver.resolve(metadata)

            assertEquals(Resolution.Confirmed("fresh000000", ResolutionSource.SEARCH), result)
        }

    @Test
    fun 長さが不明なライブ中はキャッシュを使わず同名の過去配信へ誤ヒットしない() =
        runTest {
            val live = metadata.copy(durationMs = 0)
            val search =
                FakeSearch(
                    result = FetchResult.Success(emptyList()),
                    liveResult = FetchResult.Success(listOf(liveCandidate(live))),
                )
            val cache = InMemoryResolutionCache().apply { put(live.identity, "oldstream00") }

            val result = VideoResolver(search, cache).resolve(live)

            assertEquals(Resolution.Confirmed("live0000000", ResolutionSource.LIVE), result)
            assertEquals("oldstream00", cache.get(live.identity))
        }

    @Test
    fun 長さが不明な動画は確定してもユーザー選択でもキャッシュへ保存しない() =
        runTest {
            val live = metadata.copy(durationMs = 0)
            val search =
                FakeSearch(
                    result = FetchResult.Success(emptyList()),
                    liveResult = FetchResult.Success(listOf(liveCandidate(live))),
                )
            val cache = InMemoryResolutionCache()
            val resolver = VideoResolver(search, cache)

            resolver.resolve(live)
            resolver.remember(live, "chosen00000")

            assertNull(cache.get(live.identity))
        }

    @Test
    fun 検索結果のタイトルとチャンネル名と長さが一致すれば自動確定し他の候補を残す() =
        runTest {
            val candidates = SearchResultParser.parse(fixture("search_results.json"))!!
            val search = FakeSearch(FetchResult.Success(candidates))
            val cache = InMemoryResolutionCache()

            val result = VideoResolver(search, cache).resolve(metadata) as Resolution.Confirmed

            assertEquals("aaaaaaaaaaa", result.videoId)
            assertEquals(ResolutionSource.SEARCH, result.source)
            // 採点順（チャンネル一致30点 > タイトル部分一致25点）
            assertEquals(listOf("ccccccccccc", "bbbbbbbbbbb"), result.alternatives.map { it.candidate.videoId })
            assertEquals(listOf("【歌枠】夜の配信 テストチャンネル"), search.queries)
            assertEquals("aaaaaaaaaaa", cache.get(metadata.identity))
        }

    @Test
    fun 確度が閾値未満なら候補を提示する() =
        runTest {
            val search =
                FakeSearch(FetchResult.Success(listOf(candidate("x0000000000", "【歌枠】夜の配信", channel = "別チャンネル"))))
            val cache = InMemoryResolutionCache()

            val result = VideoResolver(search, cache).resolve(metadata)

            assertTrue(result is Resolution.Ambiguous)
            assertNull(cache.get(metadata.identity))
        }

    @Test
    fun 上位2件の差が小さければ候補を提示する() =
        runTest {
            val search =
                FakeSearch(
                    FetchResult.Success(
                        listOf(candidate("first000000", "【歌枠】夜の配信"), candidate("second00000", "【歌枠】夜の配信")),
                    ),
                )

            val result = VideoResolver(search, InMemoryResolutionCache()).resolve(metadata) as Resolution.Ambiguous

            assertEquals(listOf("first000000", "second00000"), result.candidates.map { it.candidate.videoId })
        }

    @Test
    fun 一致する候補が無ければ見つからないを返す() =
        runTest {
            val search =
                FakeSearch(FetchResult.Success(listOf(candidate("z0000000000", "無関係", channel = "他", durationMs = 1))))
            assertEquals(Resolution.NotFound, VideoResolver(search, InMemoryResolutionCache()).resolve(metadata))
        }

    @Test
    fun 検索の通信失敗を返す() =
        runTest {
            val search = FakeSearch(FetchResult.Failure(FetchFailure.Http(503)))
            assertEquals(
                Resolution.Failed(FetchFailure.Http(503)),
                VideoResolver(search, InMemoryResolutionCache()).resolve(metadata),
            )
        }

    @Test
    fun 全角半角や大文字小文字の違いと長さの誤差2秒を吸収して採点する() {
        val resolver = VideoResolver(FakeSearch(FetchResult.Success(emptyList())), InMemoryResolutionCache())
        val score = resolver.score(metadata, candidate("a0000000000", "【歌枠】 夜の配信 ", durationMs = 3_724_900))
        val rule = ScoringRule()
        assertEquals(rule.titleExact + rule.channelMatch + rule.durationMatch, score)

        val metadataAscii = metadata.copy(title = "ＡＢＣ Live")
        assertEquals(
            rule.titleExact + rule.channelMatch,
            resolver.score(metadataAscii, candidate("b0000000000", "abc live", durationMs = null)),
        )
    }

    @Test
    fun ユーザーが選んだ動画を以後はキャッシュで確定する() =
        runTest {
            val cache = InMemoryResolutionCache()
            val resolver = VideoResolver(FakeSearch(FetchResult.Success(emptyList())), cache)
            resolver.remember(metadata, "chosen00000")
            assertEquals(Resolution.Confirmed("chosen00000", ResolutionSource.CACHE), resolver.resolve(metadata))
        }

    @Test
    fun キャッシュは上限を超えたら最も古く使われたものから捨てる() {
        val cache = InMemoryResolutionCache(maxEntries = 2)
        cache.put("a", "1")
        cache.put("b", "2")
        cache.get("a")
        cache.put("c", "3")
        assertNull(cache.get("b"))
        assertEquals(listOf("a", "c"), cache.snapshot().keys.toList())
    }

    @Test
    fun 長さが不明ならチャンネルの配信中の動画とタイトルを照合して確定する() =
        runTest {
            val live = metadata.copy(title = "【雑談】ライブ中", durationMs = 0)
            val liveCandidates = SearchResultParser.parse(fixture("search_results.json"))!!
            val search = FakeSearch(FetchResult.Success(emptyList()), liveResult = FetchResult.Success(liveCandidates))

            val result = VideoResolver(search, InMemoryResolutionCache()).resolve(live) as Resolution.Confirmed

            assertEquals("ccccccccccc", result.videoId)
            assertEquals(ResolutionSource.LIVE, result.source)
            assertEquals(listOf("テストチャンネル"), search.liveQueries)
            assertTrue(search.queries.isEmpty())
        }

    @Test
    fun 配信中の動画と照合できなければ通常の検索へ進む() =
        runTest {
            val live = metadata.copy(durationMs = 0)
            val normal = listOf(candidate("normal00000", "【歌枠】夜の配信", durationMs = null))
            val search =
                FakeSearch(FetchResult.Success(normal), liveResult = FetchResult.Failure(FetchFailure.Http(500)))

            val result = VideoResolver(search, InMemoryResolutionCache()).resolve(live) as Resolution.Confirmed

            assertEquals("normal00000", result.videoId)
            assertEquals(ResolutionSource.SEARCH, result.source)
        }

    @Test
    fun 長さが分かっている動画では配信中の照合をしない() =
        runTest {
            val search = FakeSearch(FetchResult.Success(emptyList()))
            VideoResolver(search, InMemoryResolutionCache()).resolve(metadata)
            assertTrue(search.liveQueries.isEmpty())
        }

    @Test
    fun 検索結果の長さ表記を読む() {
        assertEquals(3_723_000L, SearchResultParser.parseDuration("1:02:03"))
        assertEquals(754_000L, SearchResultParser.parseDuration("12:34"))
        assertNull(SearchResultParser.parseDuration("ライブ"))
        val parsed = SearchResultParser.parse(fixture("search_results.json"))!!
        assertEquals(listOf(false, false, true), parsed.map { it.isLive })
        assertNull(parsed[2].durationMs)
        assertNull(SearchResultParser.parse("""{"contents":{}}"""))
    }
}
