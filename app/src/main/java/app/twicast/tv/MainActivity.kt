package app.twicast.tv

import android.app.UiModeManager
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.webkit.WebSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.core.designsystem.theme.TwiCastTheme
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.core.device.DeviceProfile
import app.twicast.tv.core.locale.withAppLanguage
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.data.model.ThemeMode
import app.twicast.tv.data.repository.AppearanceSettingsRepository
import app.twicast.tv.data.repository.OptimizationSettingsRepository
import app.twicast.tv.data.repository.PlayerSettingsRepository
import app.twicast.tv.data.repository.SharedPreferencesLanguageSettingsRepository
import app.twicast.tv.feature.player.components.LocalWarmTwitchPlayer
import app.twicast.tv.feature.player.components.WarmTwitchPlayer
import app.twicast.tv.navigation.TwiCastApp
import coil3.SingletonImageLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appearanceSettings: AppearanceSettingsRepository

    @Inject lateinit var playerSettings: PlayerSettingsRepository

    @Inject lateinit var deviceProfile: DeviceProfile

    @Inject lateinit var optimizationSettings: OptimizationSettingsRepository

    /** "Освобождать память при сворачивании", kept current for onStop. */
    private var freeMemoryInBackground = true
    private var freedInBackground = false

    /** Kept by the activity so it lives between streams; not on low-memory TVs (see [WarmTwitchPlayer]). */
    private var warmPlayer: WarmTwitchPlayer? = null

    // Runs before Hilt injects anything, so the language store is created by hand here.
    override fun attachBaseContext(newBase: Context) {
        val language = SharedPreferencesLanguageSettingsRepository(newBase.applicationContext).language()
        super.attachBaseContext(newBase.withAppLanguage(language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmUpWebViewForTwitchPlayer()
        if (!deviceProfile.isLowMemory) warmPlayer = WarmTwitchPlayer(this, playerSettings, optimizationSettings)
        // Turning the option off, or switching to the TwiCast player, frees the warm page at once.
        lifecycleScope.launch {
            combine(playerSettings.playerEngine, optimizationSettings.warmTwitchPlayer) { engine, warm -> engine == PlayerEngine.Twitch && warm }
                .collect { wanted -> if (!wanted) warmPlayer?.destroy() }
        }
        lifecycleScope.launch { optimizationSettings.freeMemoryInBackground.collect { freeMemoryInBackground = it } }
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
                CompositionLocalProvider(LocalWarmTwitchPlayer provides warmPlayer) { TwiCastApp() }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!freedInBackground) return
        freedInBackground = false
        lifecycleScope.launch { warmPlayer?.restore() }
    }

    // Sent to the background (Home, another app): give back what is quick to rebuild. Images come back from the
    // disk cache, the warm player from Home.
    override fun onStop() {
        super.onStop()
        if (!freeMemoryInBackground || isChangingConfigurations) return
        warmPlayer?.freeMemory()
        SingletonImageLoader.get(this).memoryCache?.clear()
        freedInBackground = true
    }

    override fun onDestroy() {
        warmPlayer?.destroy()
        super.onDestroy()
    }

    /**
     * With the Twitch player chosen, loads the WebView engine (~0.5 s on a TV box) once the home screen is idle,
     * instead of when the first stream opens. Not done for the TwiCast player: the engine costs memory.
     */
    private fun warmUpWebViewForTwitchPlayer() {
        lifecycleScope.launch {
            if (playerSettings.playerEngine.first() != PlayerEngine.Twitch || !optimizationSettings.warmTwitchPlayer.first()) return@launch
            Looper.myQueue().addIdleHandler {
                WebSettings.getDefaultUserAgent(applicationContext)
                false
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
