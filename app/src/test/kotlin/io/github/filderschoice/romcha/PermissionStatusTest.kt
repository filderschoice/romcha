package io.github.filderschoice.romcha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStatusTest {
    @Test
    fun 未許可の権限をオーバーレイ通知アクセス通知の順に案内する() {
        assertEquals(PermissionStep.OVERLAY, PermissionStatus(false, false, false).nextStep)
        assertEquals(PermissionStep.NOTIFICATION_ACCESS, PermissionStatus(true, false, false).nextStep)
        assertEquals(PermissionStep.POST_NOTIFICATIONS, PermissionStatus(true, true, false).nextStep)
        assertNull(PermissionStatus(true, true, true).nextStep)
    }

    @Test
    fun オーバーレイ権限があればフローティング表示を始められる() {
        assertTrue(
            PermissionStatus(overlay = true, notificationAccess = false, postNotifications = false).canStartOverlay,
        )
        assertFalse(
            PermissionStatus(overlay = false, notificationAccess = true, postNotifications = true).canStartOverlay,
        )
    }

    @Test
    fun 共有テキストか件名から動画IDを取り出す() {
        assertEquals("dQw4w9WgXcQ", SharedTextHandler.videoIdFrom("タイトル https://youtu.be/dQw4w9WgXcQ?si=x", null))
        assertEquals("dQw4w9WgXcQ", SharedTextHandler.videoIdFrom(null, "https://youtube.com/live/dQw4w9WgXcQ"))
        assertNull(SharedTextHandler.videoIdFrom("URL の無いテキスト", null))
    }
}
