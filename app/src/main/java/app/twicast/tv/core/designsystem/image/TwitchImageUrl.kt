package app.twicast.tv.core.designsystem.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

/**
 * Twitch image templates contain `{width}x{height}`. Requests exactly the slot size in pixels
 * (e.g. 360×203 for a 240 card on a 1080p TV) instead of a full-size preview.
 */
@Composable
fun rememberTwitchImageUrl(template: String, width: Dp, aspectRatio: Float): String {
    val density = LocalDensity.current
    return remember(template, width, aspectRatio, density) {
        val widthPx = with(density) { width.toPx() }.roundToInt()
        val heightPx = (widthPx / aspectRatio).roundToInt()
        template
            // Some endpoints use the "%{width}" form; replace it first so "{width}" does not leave a stray "%".
            .replace("%{width}", widthPx.toString())
            .replace("%{height}", heightPx.toString())
            .replace("{width}", widthPx.toString())
            .replace("{height}", heightPx.toString())
    }
}
