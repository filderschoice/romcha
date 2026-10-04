package io.github.filderschoice.romcha.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.filderschoice.romcha.R
import io.github.filderschoice.romcha.crash.CrashReporting

/** クラッシュ情報の送信のオン・オフ（BL-089）。Firebase が無効なビルドでは出さない。 */
@Composable
internal fun CrashReportingSection() {
    val settings = CrashReporting.settings() ?: return
    val enabled by settings.enabled.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.crash_title), style = MaterialTheme.typography.titleMedium)
        SwitchRow(R.string.crash_switch, enabled, settings::setEnabled)
        Text(stringResource(R.string.crash_hint), style = MaterialTheme.typography.bodySmall)
    }
}
