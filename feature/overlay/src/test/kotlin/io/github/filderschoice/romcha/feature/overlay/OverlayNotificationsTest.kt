package io.github.filderschoice.romcha.feature.overlay

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayNotificationsTest {
    @Test
    fun 通知の本文はタイトルが変わったときだけ更新する() =
        runTest {
            val states =
                flowOf(
                    OverlayUiState(title = "配信A"),
                    OverlayUiState(title = "配信A", positionMs = 1_000),
                    OverlayUiState(title = null),
                    OverlayUiState(title = "配信B"),
                    OverlayUiState(title = "配信B", positionMs = 2_000),
                )

            assertEquals(listOf("配信A", null, "配信B"), OverlayNotifications.titleChanges(states).toList())
        }
}
