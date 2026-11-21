package app.twicast.tv.feature.home.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.Avatar
import app.twicast.tv.core.designsystem.format.formatPlaybackTime
import app.twicast.tv.core.designsystem.format.formatViewersLabel
import app.twicast.tv.core.designsystem.image.rememberTwitchImageUrl
import app.twicast.tv.core.designsystem.theme.GeistMono
import app.twicast.tv.core.designsystem.theme.TwiAspect
import app.twicast.tv.core.designsystem.theme.TwiMotion
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.Stream
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private val PreviewWidth = 213.dp
private val ContentGap = 24.dp
private val LineGap = 8.dp
private val LampSize = 8.dp
private val AvatarSize = 24.dp
private const val UptimeTickMillis = 1_000L

/**
 * Hero follows the card in focus only once focus settles there: walking along a row costs nothing
 * (no new preview to load per step), and the banner never flickers while the viewer is still moving.
 */
internal const val HeroSettleMillis = 300L

/**
 * Mockup "Главная" (compact): a small preview of the stream in focus and three lines — "● В эфире 3:12:40 · 15,8 тыс
 * зрителей", the title, the channel with its category. No buttons: the card below already opens the stream.
 */
@Composable
fun HomeHero(focused: State<Stream?>, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(focused.value) }
    LaunchedEffect(focused) {
        snapshotFlow { focused.value }.collectLatest { next ->
            if (shown == null) shown = next else { delay(HeroSettleMillis); shown = next }
        }
    }
    val stream = shown ?: return
    Crossfade(targetState = stream, animationSpec = tween(TwiMotion.FocusDurationMillis), label = "hero", modifier = modifier.fillMaxWidth()) { item ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ContentGap)) {
            Box(
                Modifier
                    .size(PreviewWidth, PreviewWidth / TwiAspect.Thumbnail)
                    .clip(TwiShapes.Thumbnail)
                    .background(TwiTheme.colors.surfaceVariant),
            ) {
                AsyncImage(
                    model = rememberTwitchImageUrl(item.thumbnailTemplate, PreviewWidth, TwiAspect.Thumbnail),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            HeroInfo(item)
        }
    }
}

@Composable
private fun HeroInfo(stream: Stream) {
    val colors = TwiTheme.colors
    val small = TwiTheme.typography.bodySmall
    Column(verticalArrangement = Arrangement.spacedBy(LineGap)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(LampSize).background(colors.live, CircleShape))
            Text(stringResource(R.string.home_hero_live), style = small, color = colors.onSurfaceVariant)
            stream.startedAtMillis?.let { started -> Uptime(started) }
            Text("·", style = small, color = colors.onSurfaceVariant)
            Text(formatViewersLabel(stream.viewerCount), style = small.copy(fontFeatureSettings = "tnum"), color = colors.onSurfaceVariant)
        }
        Text(
            stream.title,
            style = TwiTheme.typography.heroTitle.copy(fontSize = 20.sp, lineHeight = 26.sp),
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(stream.avatarUrl, AvatarSize)
            val line = if (stream.categoryName.isBlank()) stream.channelName else "${stream.channelName} · ${stream.categoryName}"
            Text(line, style = small, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** "3:12:40", ticking every second; only this text recomposes. */
@Composable
private fun Uptime(startedAtMillis: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(UptimeTickMillis)
            now = System.currentTimeMillis()
        }
    }
    Text(
        text = formatPlaybackTime(now - startedAtMillis),
        style = TwiTheme.typography.bodySmall.copy(fontFamily = GeistMono, fontFeatureSettings = "tnum"),
        color = TwiTheme.colors.onSurface,
    )
}
