package io.github.filderschoice.romcha.feature.overlay.session

import io.github.filderschoice.romcha.core.chat.ChatParseResult
import io.github.filderschoice.romcha.core.chat.FetchResult
import io.github.filderschoice.romcha.core.chat.InnerTubeClient
import io.github.filderschoice.romcha.core.chat.RetryListener
import io.github.filderschoice.romcha.core.chat.VideoChatInfo
import io.github.filderschoice.romcha.core.chat.resolve.SearchCandidate

/** セッションが使う通信の入口。本番は [InnerTubeBackend]、テストは偽物を渡す。 */
interface ChatBackend {
    suspend fun videoInfo(videoId: String): FetchResult<VideoChatInfo>

    suspend fun replay(
        continuation: String,
        playerOffsetMs: Long?,
        listener: RetryListener,
    ): FetchResult<ChatParseResult.Success>

    suspend fun search(
        query: String,
        liveOnly: Boolean,
    ): FetchResult<List<SearchCandidate>>
}

class InnerTubeBackend(
    private val client: InnerTubeClient = InnerTubeClient(),
) : ChatBackend {
    override suspend fun videoInfo(videoId: String) = client.fetchVideoChatInfo(videoId)

    override suspend fun replay(
        continuation: String,
        playerOffsetMs: Long?,
        listener: RetryListener,
    ) = client.fetchReplay(continuation, playerOffsetMs, listener)

    override suspend fun search(
        query: String,
        liveOnly: Boolean,
    ) = client.search(query, liveOnly)
}
