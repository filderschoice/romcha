package io.github.filderschoice.romcha.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import io.github.filderschoice.romcha.PermissionStatus
import io.github.filderschoice.romcha.R
import io.github.filderschoice.romcha.core.media.NowPlaying
import io.github.filderschoice.romcha.feature.overlay.DisplaySettingsStore
import io.github.filderschoice.romcha.feature.overlay.ThemeMode
import kotlinx.coroutines.flow.StateFlow

private enum class Screen { HOME, DISPLAY, LICENSES }

@Composable
fun RomchaApp(
    status: PermissionStatus,
    nowPlaying: StateFlow<NowPlaying>,
    update: UpdateUiModel,
    actions: HomeActions,
) {
    val context = LocalContext.current
    val theme by DisplaySettingsStore.state.collectAsState()
    val dark =
        when (theme.theme) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    val colors = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    MaterialTheme(colorScheme = colors) {
        when (screen) {
            Screen.HOME ->
                HomeScreen(
                    status = status,
                    nowPlaying = nowPlaying,
                    update = update,
                    actions = actions,
                    onOpenDisplaySettings = { screen = Screen.DISPLAY },
                    onOpenLicenses = { screen = Screen.LICENSES },
                )
            Screen.DISPLAY -> DisplaySettingsScreen(onBack = { screen = Screen.HOME })
            Screen.LICENSES -> LicensesScreen(onBack = { screen = Screen.HOME })
        }
    }
}

/** OSS ライセンス一覧（F-APP-03）。依存ライブラリの情報は AboutLibraries がビルド時に生成する。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicensesScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.licenses_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LibrariesContainer(modifier = Modifier.fillMaxSize().padding(padding))
    }
}
