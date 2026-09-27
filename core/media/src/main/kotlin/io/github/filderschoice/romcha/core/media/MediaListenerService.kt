package io.github.filderschoice.romcha.core.media

import android.service.notification.NotificationListenerService

/**
 * 「通知へのアクセス」の許可を受けるためだけのサービス（PLAN 4.2 / 4.9）。
 *
 * `MediaSessionManager.getActiveSessions` を呼ぶには、許可済みの NotificationListenerService のコンポーネントが必要。
 * 本アプリは通知の内容を読まないため、通知の受信処理（onNotificationPosted 等）は実装しない。
 */
class MediaListenerService : NotificationListenerService()
