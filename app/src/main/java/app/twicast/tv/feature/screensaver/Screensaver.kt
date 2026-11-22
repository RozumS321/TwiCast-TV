package app.twicast.tv.feature.screensaver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.AuroraLogo
import app.twicast.tv.core.designsystem.component.EmberBackground
import app.twicast.tv.core.designsystem.component.auroraText
import app.twicast.tv.core.designsystem.component.rememberAuroraSeconds
import app.twicast.tv.core.designsystem.component.rememberEmberBackdrop
import app.twicast.tv.core.designsystem.theme.GeistMono
import app.twicast.tv.core.designsystem.theme.TwiTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** How long without a key press, outside the player, before the screensaver shows. */
const val ScreensaverIdleMillis = 5 * 60 * 1000L

private const val ClockTickMillis = 15_000L
private val MarkSize = 96.dp

/**
 * Canvas "Заставка при простое": the storm, the Aurora mark and wordmark, the time and who of the follows is live.
 * Any key closes it (the caller swallows that key, so it does not also act on the screen below).
 */
@Composable
fun Screensaver(liveFollowedCount: Int, modifier: Modifier = Modifier) {
    val backdrop = rememberEmberBackdrop()
    val seconds = rememberAuroraSeconds()
    Box(modifier.fillMaxSize().background(TwiTheme.colors.background)) {
        EmberBackground(backdrop, Modifier.fillMaxSize())
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                AuroraLogo(Modifier.size(MarkSize), seconds = seconds)
                Text(
                    text = stringResource(R.string.brand_word_start) + stringResource(R.string.brand_word_end),
                    style = TwiTheme.typography.logo.copy(fontSize = 64.sp, lineHeight = 72.sp),
                    color = TwiTheme.colors.onSurface,
                    modifier = Modifier.auroraText(seconds),
                )
            }
            Clock()
            if (liveFollowedCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(8.dp).background(TwiTheme.colors.live, CircleShape))
                    Text(
                        pluralStringResource(R.plurals.screensaver_live, liveFollowedCount, liveFollowedCount),
                        style = TwiTheme.typography.body,
                        color = TwiTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            stringResource(R.string.screensaver_hint),
            style = TwiTheme.typography.body,
            color = TwiTheme.colors.onSurfaceFaint,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
        )
    }
}

/** "21:47", refreshed every 15 seconds. */
@Composable
private fun Clock() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(ClockTickMillis)
            now = System.currentTimeMillis()
        }
    }
    val format = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Text(
        text = format.format(Date(now)),
        style = TwiTheme.typography.body.copy(fontFamily = GeistMono, fontSize = 40.sp, lineHeight = 48.sp, fontFeatureSettings = "tnum"),
        color = TwiTheme.colors.onSurface,
    )
}
