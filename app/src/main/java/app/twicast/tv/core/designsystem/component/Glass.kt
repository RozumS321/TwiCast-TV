package app.twicast.tv.core.designsystem.component

import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Shader
import android.graphics.Matrix
import android.graphics.BitmapShader
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.draw.drawWithCache
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
fun Modifier.glassOverVideo(shape: Shape, alpha: Float? = null): Modifier {
    val tokens = TwiThemeConfig.effects
    val tint = TwiTheme.colors.overlay
    val base = background(if (alpha != null) tint.copy(alpha = alpha) else tint, shape)
    val lit = if (tokens.glassHighlightAlpha > 0f) {
        base.background(Brush.verticalGradient(0f to Color.White.copy(alpha = tokens.glassHighlightAlpha), HighlightEnd to Color.Transparent), shape)
    } else {
        base
    }
    return lit
        .frostGrain(shape)
        .border(TwiDimens.OutlineWidth, Color.White.copy(alpha = tokens.glassRimAlpha), shape)
}

/** The highlight of app glass is a bit stronger than over video: it has to read against a dark room. */
private const val SurfaceHighlightBoost = 1.6f
private const val SurfaceHighlightEnd = 0.45f

/**
 * A card or panel of the app (canvas: "Стекло"). Plain: the solid surface. With "Эффекты интерфейса": flat frosted
 * glass — a faint tint the background shows through and a hairline rim ([TwiThemeConfig] can add a highlight).
 */
@Composable
fun Modifier.twiSurface(shape: Shape): Modifier {
    val colors = TwiTheme.colors
    if (!TwiTheme.effects) return background(colors.surface, shape)
    val tokens = TwiThemeConfig.effects
    // Under the frost: the blurred background behind this very spot — frosted glass on Android 11.
    val base = backdropSlice(shape).background(colors.surface, shape)
    val lit = if (tokens.glassHighlightAlpha > 0f) {
        base.background(
            Brush.verticalGradient(0f to Color.White.copy(alpha = tokens.glassHighlightAlpha * SurfaceHighlightBoost), SurfaceHighlightEnd to Color.Transparent),
            shape,
        )
    } else {
        base
    }
    return lit
        .frostGrain(shape)
        // The theme's rim: a light hairline on the dark storm, a dark one on the light storm.
        .border(TwiDimens.OutlineWidth, colors.outline, shape)
}

/** Grain inside glass: frosted glass is matte, not clear. Drawn from one shared tiled noise bitmap. */
private const val FrostGrainAlpha = 0.03f
private val sharedGrain by lazy { grainBitmap() }

@Composable
private fun Modifier.frostGrain(shape: Shape): Modifier = drawWithCache {
    val brush = ShaderBrush(ImageShader(sharedGrain, TileMode.Repeated, TileMode.Repeated))
    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
    onDrawBehind { clipPath(path) { drawRect(brush, alpha = FrostGrainAlpha) } }
}

/**
 * Draws the piece of [EmberBackdrop.blurred] that lies behind this element, clipped to [shape]: the background
 * is known and blurred once per frame, so every glass surface just cuts out its own window.
 */
@Composable
fun Modifier.backdropSlice(shape: Shape): Modifier {
    val backdrop = LocalEmberBackdrop.current ?: return this
    val position = remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { position.value = it.positionInRoot() }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this@drawWithCache)
            val matrix = Matrix()
            onDrawBehind {
                val image = backdrop.blurred ?: return@onDrawBehind
                val screen = backdrop.size
                if (screen.width <= 0 || screen.height <= 0) return@onDrawBehind
                // The small blurred frame stretched back to screen size and shifted to this element's spot,
                // painted straight into its shape: one textured draw, no clipping layer (cheap on a TV GPU).
                matrix.reset()
                matrix.setScale(screen.width / image.width.toFloat(), screen.height / image.height.toFloat())
                matrix.postTranslate(-position.value.x, -position.value.y)
                val shader = BitmapShader(image.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                shader.setLocalMatrix(matrix)
                drawOutline(outline, ShaderBrush(shader))
            }
        }
}
