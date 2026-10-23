package app.twicast.tv

import android.app.UiModeManager
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.core.designsystem.theme.TwiCastTheme
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.ThemeMode
import app.twicast.tv.data.repository.AppearanceSettingsRepository
import app.twicast.tv.navigation.TwiCastApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appearanceSettings: AppearanceSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by appearanceSettings.themeMode.collectAsStateWithLifecycle(initialValue = null)
            // Until the choice is read the window background shows: drawing a guessed theme would flash.
            val mode = themeMode ?: return@setContent
            LaunchedEffect(mode) { syncSystemNightMode(mode) }
            val darkTheme = when (mode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            TwiCastTheme(darkTheme = darkTheme) {
                // uiMode changes don't recreate the activity, so the XML window background must follow by hand.
                val background = TwiTheme.colors.background
                SideEffect { window.setBackgroundDrawable(ColorDrawable(background.toArgb())) }
                TwiCastApp()
            }
        }
    }

    /**
     * Android 12+ remembers the app's night mode, so the next cold start already picks the values-night
     * window background for an explicit dark choice. Older TVs only get it right for [ThemeMode.System].
     */
    private fun syncSystemNightMode(mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val nightMode = when (mode) {
            ThemeMode.System -> UiModeManager.MODE_NIGHT_AUTO
            ThemeMode.Light -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.Dark -> UiModeManager.MODE_NIGHT_YES
        }
        getSystemService(UiModeManager::class.java).setApplicationNightMode(nightMode)
    }
}
