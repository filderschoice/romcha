package io.github.filderschoice.romcha.core.chat.resolve

import io.github.filderschoice.romcha.core.chat.FetchFailure
import io.github.filderschoice.romcha.core.chat.FetchResult
import java.text.Normalizer
import kotlin.math.abs

/** 検索の実行元。本番は `InnerTubeClient.search`、テストは偽物を渡す。 */
fun interface VideoSearchSource {
    suspend fun search(query: String): FetchResult<List<SearchCandidate>>
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
 * 手順1（MediaSession の ID）→ 手順2（端末内キャッシュ）→ 手順4（検索照合）の順に試し、確定した時点で打ち切る。
 * 手順3（ライブ・プレミアのチャンネル照合）は M2 で追加する。
 */
class VideoResolver(
    private val search: VideoSearchSource,
    private val cache: ResolutionCache,
    private val rule: ScoringRule = ScoringRule(),
) {
    suspend fun resolve(metadata: TrackMetadata): Resolution {
        metadata.videoIdHints.firstOrNull()?.let {
            cache.put(metadata.identity, it)
            return Resolution.Confirmed(it, ResolutionSource.METADATA)
        }
        cache.get(metadata.identity)?.let { return Resolution.Confirmed(it, ResolutionSource.CACHE) }
        return when (val result = search.search(buildQuery(metadata))) {
            is FetchResult.Failure -> Resolution.Failed(result.failure)
            is FetchResult.Success -> judge(metadata, rank(metadata, result.value))
        }
    }

    /** ユーザーが候補を選んだ・切り替えた時に呼び、以後はキャッシュで確定させる。 */
    fun remember(
        metadata: TrackMetadata,
        videoId: String,
    ) {
        cache.put(metadata.identity, videoId)
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
    ): Resolution {
        val relevant = ranked.filter { it.score > 0 }.take(rule.maxCandidates)
        val top = relevant.firstOrNull() ?: return Resolution.NotFound
        val margin = top.score - (relevant.getOrNull(1)?.score ?: 0)
        if (top.score < rule.autoConfirmThreshold || margin < rule.minMargin) return Resolution.Ambiguous(relevant)
        cache.put(metadata.identity, top.candidate.videoId)
        return Resolution.Confirmed(top.candidate.videoId, ResolutionSource.SEARCH, relevant.drop(1))
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
