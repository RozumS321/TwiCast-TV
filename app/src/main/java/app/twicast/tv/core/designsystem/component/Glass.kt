package app.twicast.tv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.core.designsystem.theme.TwiThemeConfig

private const val HighlightEnd = 0.4f

/**
 * Dark glass over video (canvas: "Тёмное стекло"): the player's island, panels and notices. A translucent tint,
 * a light rim and a soft highlight at the top — no blur, so it looks the same on Android 11.
 */
@Composable
fun Modifier.glassOverVideo(shape: Shape): Modifier {
    val tokens = TwiThemeConfig.effects
    return this
        .background(TwiTheme.colors.overlay, shape)
        .background(
            Brush.verticalGradient(0f to Color.White.copy(alpha = tokens.glassHighlightAlpha), HighlightEnd to Color.Transparent),
            shape,
        )
        .border(TwiDimens.OutlineWidth, Color.White.copy(alpha = tokens.glassRimAlpha), shape)
}

/** The highlight of app glass is a bit stronger than over video: it has to read against a dark room. */
private const val SurfaceHighlightBoost = 1.6f
private const val SurfaceHighlightEnd = 0.45f

/**
 * A card or panel of the app (canvas: "Стекло"). Plain: the solid surface. With "Эффекты интерфейса": glass —
 * a translucent tint, a soft highlight at the top and a light rim, so it reads as glass over the living background.
 */
@Composable
fun Modifier.twiSurface(shape: Shape): Modifier {
    val colors = TwiTheme.colors
    if (!TwiTheme.effects) return background(colors.surface, shape)
    val tokens = TwiThemeConfig.effects
    return this
        .background(colors.surface, shape)
        .background(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = tokens.glassHighlightAlpha * SurfaceHighlightBoost),
                SurfaceHighlightEnd to Color.Transparent,
            ),
            shape,
        )
        .border(TwiDimens.OutlineWidth, Color.White.copy(alpha = tokens.glassRimAlpha), shape)
}
