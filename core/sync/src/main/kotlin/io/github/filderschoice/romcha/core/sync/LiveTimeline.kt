package io.github.filderschoice.romcha.core.sync

import io.github.filderschoice.romcha.core.chat.ChatMessage

/**
 * ライブ・プレミア中の「最新追従」表示（F-SYNC-08）。
 *
 * 受信したメッセージを、受信時刻から表示遅延だけ経ったものから順に表示する。公式アプリの映像は配信より遅れて届くため、
 * チャットが映像より先に流れないよう、利用者が遅延（秒）を設定できるようにする。
 */
object LiveTimeline {
    /** 表示遅延の設定範囲（秒）。 */
    const val MIN_DELAY_SECONDS = 0
    const val MAX_DELAY_SECONDS = 30
    const val DEFAULT_DELAY_SECONDS = 0

    private const val MILLIS_PER_SECOND = 1_000L

    /** 表示遅延を設定範囲に収めてミリ秒にする。 */
    fun delayMs(seconds: Int): Long = seconds.coerceIn(MIN_DELAY_SECONDS, MAX_DELAY_SECONDS) * MILLIS_PER_SECOND

    /**
     * 現在表示すべきメッセージ（受信順）。
     *
     * @param received 受信順のメッセージ（`LiveState.received`）
     * @param nowMs 現在時刻（受信時刻と同じ時計）
     * @param maxVisible 表示保持件数の上限（N-04）
     */
    fun visible(
        received: List<ReceivedMessage>,
        nowMs: Long,
        delayMs: Long,
        maxVisible: Int = SyncConfig().maxVisible,
    ): List<ChatMessage> {
        val cutoff = nowMs - delayMs
        // 受信順＝受信時刻の昇順のため、遅延を満たさない最初の要素より前だけを表示する
        val shown = received.indexOfFirst { it.receivedAtMs > cutoff }.let { if (it < 0) received.size else it }
        return received.subList((shown - maxVisible).coerceAtLeast(0), shown).map { it.message }
    }
}
