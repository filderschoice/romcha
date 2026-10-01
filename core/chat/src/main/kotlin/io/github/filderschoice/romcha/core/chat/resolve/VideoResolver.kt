package io.github.filderschoice.romcha.core.chat.resolve

import io.github.filderschoice.romcha.core.chat.FetchFailure
import io.github.filderschoice.romcha.core.chat.FetchResult
import java.text.Normalizer
import kotlin.math.abs

/** 検索の実行元。本番は `InnerTubeClient.search`、テストは偽物を渡す。 */
fun interface VideoSearchSource {
    /** @param liveOnly true なら配信中・プレミア公開中の動画に絞る（PLAN 4.3 手順3） */
    suspend fun search(
        query: String,
        liveOnly: Boolean,
    ): FetchResult<List<SearchCandidate>>
}

/**
 * 「タイトル＋チャンネル名＋長さ → 動画ID」の端末内キャッシュ（PLAN 4.3 手順2）。
 *
 * 実装は端末内にのみ保存し、外部へ送らない（N-06）。
 */
interface ResolutionCache {
    fun get(identity: String): String?

    fun put(
        identity: String,
        videoId: String,
    )
}

/** 最大件数を超えたら古いものから捨てるメモリ上のキャッシュ。 */
class InMemoryResolutionCache(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) : ResolutionCache {
    private val entries = LinkedHashMap<String, String>(maxEntries, LOAD_FACTOR, true)

    @Synchronized
    override fun get(identity: String): String? = entries[identity]

    @Synchronized
    override fun put(
        identity: String,
        videoId: String,
    ) {
        entries[identity] = videoId
        while (entries.size > maxEntries) entries.remove(entries.keys.first())
    }

    /** 永続化用に中身を取り出す（古い順）。 */
    @Synchronized
    fun snapshot(): Map<String, String> = LinkedHashMap(entries)

    private companion object {
        const val DEFAULT_MAX_ENTRIES = 200
        const val LOAD_FACTOR = 0.75f
    }
}

/** 特定の方法。確度の表示と、誤特定時の切り替え候補（F-VID-02）に使う。 */
enum class ResolutionSource {
    METADATA,
    CACHE,

    /** 配信中・プレミア公開中の動画との照合（手順3） */
    LIVE,
    SEARCH,
    USER,
}

data class ScoredCandidate(
    val candidate: SearchCandidate,
    val score: Int,
)

sealed interface Resolution {
    /**
     * 動画を確定した。
     *
     * @property alternatives 検索で確定した場合の他の候補（誤特定時にワンタップで切り替える。F-VID-02）
     */
    data class Confirmed(
        val videoId: String,
        val source: ResolutionSource,
        val alternatives: List<ScoredCandidate> = emptyList(),
    ) : Resolution

    /** 確度が閾値未満。候補をユーザーに選んでもらう（F-VID-02）。 */
    data class Ambiguous(
        val candidates: List<ScoredCandidate>,
    ) : Resolution

    /** 候補が見つからない。共有・URL 入力を案内する（PLAN 4.3 手順5） */
    data object NotFound : Resolution

    /** 検索の通信に失敗した。 */
    data class Failed(
        val failure: FetchFailure,
    ) : Resolution
}

/**
 * 採点の配点と閾値（PLAN 4.3 手順4）。
 *
 * 既定では「タイトル完全一致＋チャンネル名一致」で自動確定の閾値に達し、長さの一致で加点する。
 */
data class ScoringRule(
    val titleExact: Int = 50,
    val titlePartial: Int = 25,
    val channelMatch: Int = 30,
    val durationMatch: Int = 20,
    val durationToleranceMs: Long = 2_000,
    val autoConfirmThreshold: Int = 80,
    val minMargin: Int = 10,
    val maxCandidates: Int = 5,
)

/**
 * 再生中の動画を自動特定するパイプライン（F-VID-01/02、PLAN 4.3）。
 *
 * 手順1（MediaSession の ID）→ 手順2（端末内キャッシュ。長さが不明な動画は対象外）→ 手順3（長さが不明なら配信中・プレミア公開中の動画と照合）
 * → 手順4（検索照合）の順に試し、確定した時点で打ち切る。
 */
class VideoResolver(
    private val search: VideoSearchSource,
    private val cache: ResolutionCache,
    private val rule: ScoringRule = ScoringRule(),
) {
    suspend fun resolve(metadata: TrackMetadata): Resolution {
        resolveLocally(metadata)?.let { return it }
        resolveLive(metadata)?.let { return it }
        return when (val result = search.search(buildQuery(metadata), liveOnly = false)) {
            is FetchResult.Failure -> Resolution.Failed(result.failure)
            is FetchResult.Success -> judge(metadata, rank(metadata, result.value), ResolutionSource.SEARCH)
        }
    }

    /** 手順1・2: 通信せずに確定できるか（MediaSession の動画ID、端末内キャッシュ）。 */
    private fun resolveLocally(metadata: TrackMetadata): Resolution.Confirmed? {
        metadata.videoIdHints.firstOrNull()?.let {
            store(metadata, it)
            return Resolution.Confirmed(it, ResolutionSource.METADATA)
        }
        if (!cacheable(metadata)) return null
        return cache.get(metadata.identity)?.let { Resolution.Confirmed(it, ResolutionSource.CACHE) }
    }

    /**
     * キャッシュのキー（[TrackMetadata.identity]）が動画を一意に表すか。
     *
     * 長さが 0 以下（ライブ・プレミア中）はキーが「タイトル＋チャンネル名」だけになり、同名の過去・次回の配信と衝突して
     * 別の動画のチャットを取得してしまうため、キャッシュの読み書きをしない。
     */
    private fun cacheable(metadata: TrackMetadata): Boolean = metadata.durationMs > 0

    private fun store(
        metadata: TrackMetadata,
        videoId: String,
    ) {
        if (cacheable(metadata)) cache.put(metadata.identity, videoId)
    }

    /**
     * 手順3: 長さが 0／不明なら配信中とみなし、チャンネル名で配信中・プレミア公開中の動画を探してタイトル照合する。
     *
     * 確定しなかった場合（長さが分かっている・候補の確度不足・見つからない・通信失敗）は null を返し、手順4へ進む。
     */
    private suspend fun resolveLive(metadata: TrackMetadata): Resolution.Confirmed? {
        if (metadata.durationMs > 0 || metadata.channelName.isBlank()) return null
        val result = search.search(metadata.channelName, liveOnly = true) as? FetchResult.Success ?: return null
        val live = result.value.filter { it.isLive }
        return judge(metadata, rank(metadata, live), ResolutionSource.LIVE) as? Resolution.Confirmed
    }

    /** ユーザーが候補を選んだ・切り替えた時に呼び、以後はキャッシュで確定させる。 */
    fun remember(
        metadata: TrackMetadata,
        videoId: String,
    ) {
        store(metadata, videoId)
    }

    internal fun rank(
        metadata: TrackMetadata,
        candidates: List<SearchCandidate>,
    ): List<ScoredCandidate> =
        candidates
            .distinctBy { it.videoId }
            .map { ScoredCandidate(it, score(metadata, it)) }
            .sortedByDescending { it.score }

    internal fun score(
        metadata: TrackMetadata,
        candidate: SearchCandidate,
    ): Int {
        val title = normalize(metadata.title)
        val candidateTitle = normalize(candidate.title)
        val partial = candidateTitle.contains(title) || title.contains(candidateTitle)
        var score =
            when {
                title.isEmpty() -> 0
                title == candidateTitle -> rule.titleExact
                partial -> rule.titlePartial
                else -> 0
            }
        val channel = normalize(metadata.channelName)
        if (channel.isNotEmpty() && channel == normalize(candidate.channelName)) score += rule.channelMatch
        val duration = candidate.durationMs
        if (metadata.durationMs > 0 && duration != null) {
            if (abs(metadata.durationMs - duration) <= rule.durationToleranceMs) score += rule.durationMatch
        }
        return score
    }

    private fun judge(
        metadata: TrackMetadata,
        ranked: List<ScoredCandidate>,
        source: ResolutionSource,
    ): Resolution {
        val relevant = ranked.filter { it.score > 0 }.take(rule.maxCandidates)
        val top = relevant.firstOrNull() ?: return Resolution.NotFound
        val margin = top.score - (relevant.getOrNull(1)?.score ?: 0)
        if (top.score < rule.autoConfirmThreshold || margin < rule.minMargin) return Resolution.Ambiguous(relevant)
        store(metadata, top.candidate.videoId)
        return Resolution.Confirmed(top.candidate.videoId, source, relevant.drop(1))
    }

    private fun buildQuery(metadata: TrackMetadata): String =
        listOf(metadata.title, metadata.channelName)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    private companion object {
        val WHITESPACE = Regex("\\s+")

        /** 全角・半角、大文字・小文字、空白の有無の違いを吸収する（空白はすべて除く）。 */
        fun normalize(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase().replace(WHITESPACE, "")
    }
}
