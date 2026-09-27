package io.github.filderschoice.romcha.core.sync

import io.github.filderschoice.romcha.core.chat.FetchResult
import io.github.filderschoice.romcha.core.chat.VideoChatInfo
import kotlinx.coroutines.delay

/** 動画のチャット提供状況の取得元。本番は `InnerTubeClient.fetchVideoChatInfo`。 */
fun interface VideoInfoSource {
    suspend fun fetch(videoId: String): FetchResult<VideoChatInfo>
}

/** リプレイへの切り替えの結果。 */
sealed interface ReplaySwitch {
    /** リプレイが使えるようになった。[continuation] で `ReplaySession` を始める */
    data class Ready(
        val continuation: String,
    ) : ReplaySwitch

    /** 配信がまだ続いている（終了の判定が早すぎた）。ライブの取得をやり直す */
    data class StillLive(
        val continuation: String,
    ) : ReplaySwitch

    /** チャットのリプレイが提供されない（無効化・一定時間待っても準備されない） */
    data object Unavailable : ReplaySwitch
}

/**
 * ライブ・プレミアの終了後、チャットのリプレイが使えるようになるのを待つ（F-CHAT-06）。
 *
 * 終了直後はリプレイの準備ができていないことがあるため、[waitsMs] の間隔で動画の情報を取り直す。
 * 既定では 30 秒・1 分・2 分・5 分・10 分後（合計約 18 分）に確かめ、それでも準備されなければ諦める。
 */
class ReplaySwitcher(
    private val source: VideoInfoSource,
    private val waitsMs: List<Long> = DEFAULT_WAITS_MS,
) {
    /**
     * @param onWaiting 次の確認まで待つ前に呼ぶ（UI に「リプレイの準備を待っています（n 回目）」を出す）
     */
    suspend fun await(
        videoId: String,
        onWaiting: (attempt: Int, waitMs: Long) -> Unit = { _, _ -> },
    ): ReplaySwitch {
        waitsMs.forEachIndexed { index, waitMs ->
            onWaiting(index + 1, waitMs)
            delay(waitMs)
            // Unavailable は準備中の可能性もあるため、通信失敗と同様に次の確認まで待つ
            val result = source.fetch(videoId) as? FetchResult.Success
            val info = result?.value as? VideoChatInfo.Available ?: return@forEachIndexed
            val token = info.topChatToken
            return if (info.isReplay) ReplaySwitch.Ready(token) else ReplaySwitch.StillLive(token)
        }
        return ReplaySwitch.Unavailable
    }

    companion object {
        val DEFAULT_WAITS_MS = listOf(30_000L, 60_000L, 120_000L, 300_000L, 600_000L)
    }
}
