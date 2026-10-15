package io.github.filderschoice.romcha.core.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 自動再開で最後に起きたこと。 */
enum class ResumeEvent {
    /** 再開を依頼した（play() を呼んだ） */
    RESUMED,

    /** 回数制限に達して止めた */
    LIMIT_REACHED,

    /** 再開できなかった（セッションが無い・PLAY が許可されていない） */
    FAILED,
}

/**
 * 自動再開の実績（診断情報の画面に出す。BL-116）。
 *
 * @property resumedCount 再開を依頼した回数（プロセスが生きている間の累計）
 * @property limitReachedCount 回数制限で止めた回数
 * @property lastEvent 最後に起きたこと。まだ何も無ければ null
 * @property lastEventAtMs 最後に起きた時刻（端末の時刻。画面表示用）
 */
data class ResumeStats(
    val resumedCount: Int = 0,
    val limitReachedCount: Int = 0,
    val lastEvent: ResumeEvent? = null,
    val lastEventAtMs: Long = 0,
)

/** 自動再開の実績の共有場所。単一プロセスのため、サービスが書き、診断画面が読む。 */
object ResumeDiagnostics {
    private val mutableStats = MutableStateFlow(ResumeStats())
    val stats: StateFlow<ResumeStats> = mutableStats.asStateFlow()

    fun record(
        event: ResumeEvent,
        atMs: Long,
    ) = mutableStats.update {
        it.copy(
            resumedCount = it.resumedCount + if (event == ResumeEvent.RESUMED) 1 else 0,
            limitReachedCount = it.limitReachedCount + if (event == ResumeEvent.LIMIT_REACHED) 1 else 0,
            lastEvent = event,
            lastEventAtMs = atMs,
        )
    }
}
