package app.twicast.tv.feature.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.Avatar
import app.twicast.tv.core.designsystem.component.TwiPrimaryButton
import app.twicast.tv.core.designsystem.component.TwiSecondaryButton
import app.twicast.tv.core.designsystem.format.formatPlaybackTime
import app.twicast.tv.core.designsystem.format.formatViewersLabel
import app.twicast.tv.core.designsystem.icon.TwiIcons
import app.twicast.tv.core.designsystem.image.rememberTwitchImageUrl
import app.twicast.tv.core.designsystem.theme.GeistMono
import app.twicast.tv.core.designsystem.theme.TwiAspect
import app.twicast.tv.core.designsystem.theme.TwiMotion
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.core.designsystem.theme.TwiThemeConfig
import app.twicast.tv.data.model.Stream
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

private val PreviewWidth = 560.dp
private val ContentGap = 40.dp
private val InfoSpacing = 16.dp
private val GlassPadding = 28.dp
private val LampSize = 8.dp
private val HeroAvatarSize = 40.dp
private const val UptimeTickMillis = 1_000L
/** "Эффекты интерфейса": the preview slowly drifts closer, like a stream that is playing. */
private const val PreviewZoom = 1.06f
private const val PreviewZoomMillis = 9_000
private const val PreviewShadeAlpha = 0.45f

/**
 * Mockup "Главная": the stream in focus, large — its preview, "в эфире" with the uptime ticking, the channel,
 * the title and «Смотреть» / «Канал». It follows the focus in the rows below.
 */
@Composable
fun HomeHero(stream: Stream, onWatch: () -> Unit, onChannel: () -> Unit, modifier: Modifier = Modifier) {
    val effects = TwiTheme.effects
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ContentGap),
        modifier = modifier.fillMaxWidth(),
    ) {
        HeroPreview(stream, effects)
        AnimatedContent(
            targetState = stream,
            contentKey = { it.id },
            transitionSpec = {
                val enter = if (effects) {
                    fadeIn(tween(TwiMotion.SidebarDurationMillis)) + slideInVertically(tween(TwiThemeConfig.motion.longMillis, easing = TwiMotion.Decelerate)) { it / 8 }
                } else {
                    fadeIn(tween(TwiMotion.FocusDurationMillis))
                }
                enter togetherWith fadeOut(tween(TwiMotion.FocusDurationMillis))
            },
            label = "hero",
            modifier = Modifier.weight(1f),
        ) { shown ->
            HeroInfo(shown, effects, onWatch, onChannel)
        }
    }
}

@Composable
private fun HeroPreview(stream: Stream, effects: Boolean) {
    val zoom = if (effects) {
        val transition = rememberInfiniteTransition(label = "hero-zoom")
        transition.animateFloat(1f, PreviewZoom, infiniteRepeatable(tween(PreviewZoomMillis, easing = TwiMotion.Decelerate), RepeatMode.Reverse), label = "zoom")
    } else {
        null
    }
    Box(
        Modifier
            .size(PreviewWidth, PreviewWidth / TwiAspect.Thumbnail)
            .clip(TwiShapes.Hero)
            .background(TwiTheme.colors.surfaceVariant),
    ) {
        AsyncImage(
            model = rememberTwitchImageUrl(stream.thumbnailTemplate, PreviewWidth, TwiAspect.Thumbnail),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                // Read in the draw phase only: the slow zoom never recomposes the hero.
                .graphicsLayer {
                    val scale = zoom?.value ?: 1f
                    scaleX = scale
                    scaleY = scale
                },
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = PreviewShadeAlpha))),
        )
        Text(
            text = formatViewersLabel(stream.viewerCount),
            style = TwiTheme.typography.countLarge,
            color = TwiTheme.colors.onOverlay,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        )
    }
}

@Composable
private fun HeroInfo(stream: Stream, effects: Boolean, onWatch: () -> Unit, onChannel: () -> Unit) {
    val colors = TwiTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(InfoSpacing),
        modifier = if (effects) {
            Modifier.fillMaxWidth().background(colors.surface, TwiShapes.Panel).padding(GlassPadding)
        } else {
            Modifier.fillMaxWidth()
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(LampSize).background(colors.live, CircleShape))
            Text(stringResource(R.string.home_hero_live), style = TwiTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            stream.startedAtMillis?.let { started -> Uptime(started) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(stream.avatarUrl, HeroAvatarSize)
            Column {
                Text(stream.channelName, style = TwiTheme.typography.listTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (stream.categoryName.isNotBlank()) {
                    Text(stream.categoryName, style = TwiTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Text(stream.title, style = TwiTheme.typography.heroTitle, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
            TwiPrimaryButton(text = stringResource(R.string.action_watch), onClick = onWatch, icon = TwiIcons.Play)
            TwiSecondaryButton(text = stringResource(R.string.action_channel), onClick = onChannel)
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
        style = TwiTheme.typography.bodySmall.copy(fontFamily = GeistMono),
        color = TwiTheme.colors.onSurface,
    )
}
