package io.github.filderschoice.romcha.feature.overlay.session

import io.github.filderschoice.romcha.core.chat.FetchFailure
import io.github.filderschoice.romcha.core.sync.FetchStatus
import io.github.filderschoice.romcha.feature.overlay.NoticeLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionMessagesTest {
    @Test
    fun 待ち状態と操作で進められる状態は案内にする() {
        listOf(
            SessionMessages.NOT_DETECTED,
            SessionMessages.RESOLVING,
            SessionMessages.AMBIGUOUS,
            SessionMessages.LOADING_VIDEO,
            SessionMessages.SCREEN_OFF,
            SessionMessages.liveEnded(1),
            SessionMessages.describe(FetchStatus.Retrying(2, FetchFailure.Network(null))),
        ).forEach { assertEquals(it?.text, NoticeLevel.INFO, it?.level) }
    }

    @Test
    fun 失敗とチャットを出せない状態はエラーにする() {
        listOf(
            SessionMessages.NOT_FOUND,
            SessionMessages.CHAT_UNAVAILABLE,
            SessionMessages.REPLAY_NOT_PROVIDED,
            SessionMessages.chatUnavailable("チャットはオフになっています"),
            SessionMessages.describe(FetchFailure.Network(null)),
            SessionMessages.describe(FetchStatus.Failed(FetchFailure.Http(500))),
        ).forEach { assertEquals(it?.text, NoticeLevel.ERROR, it?.level) }
    }

    @Test
    fun チャット無効の説明は応答の文言を優先し無ければ既定文にする() {
        assertEquals("チャットはオフになっています", SessionMessages.chatUnavailable("チャットはオフになっています").text)
        assertEquals(SessionMessages.CHAT_UNAVAILABLE, SessionMessages.chatUnavailable(null))
    }

    @Test
    fun 取得中と待機中はお知らせを出さない() {
        assertNull(SessionMessages.describe(FetchStatus.Idle))
        assertNull(SessionMessages.describe(FetchStatus.Loading))
    }
}
