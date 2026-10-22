package app.twicast.tv.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.lightColorScheme

/** Width of the design canvas (the 1280×720 mockups). */
private const val DesignCanvasWidth = 1280f

private val LocalTwiColors = staticCompositionLocalOf { LightTwiColorScheme }
private val LocalTwiTypography = staticCompositionLocalOf { DefaultTwiTypography }

object TwiTheme {
    val colors: TwiColorScheme
        @Composable @ReadOnlyComposable
        get() = LocalTwiColors.current

    val typography: TwiTypography
        @Composable @ReadOnlyComposable
        get() = LocalTwiTypography.current
}

/**
 * Root theme. Besides colors and type it maps the 1280-wide design canvas onto the TV screen:
 * 1dp == 1px of the mockup on any TV resolution, so [TwiDimens] are the numbers from DESIGN.md as is.
 * At 1:1 dp (960dp wide on a 1080p TV) the mockup layout does not fit — DESIGN.md's 0.75 fallback.
 */
@Composable
fun TwiCastTheme(content: @Composable () -> Unit) {
    val colors = LightTwiColorScheme
    val windowWidthPx = LocalWindowInfo.current.containerSize.width
    val systemDensity = LocalDensity.current
    val canvasDensity = remember(windowWidthPx, systemDensity) {
        if (windowWidthPx > 0) {
            Density(density = windowWidthPx / DesignCanvasWidth, fontScale = systemDensity.fontScale)
        } else {
            systemDensity
        }
    }
    val materialColors = remember(colors) {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.primaryStrong,
            background = colors.background,
            onBackground = colors.onSurface,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.onSurfaceVariant,
            border = colors.outline,
            borderVariant = colors.outlineVariant,
            scrim = colors.scrim,
            error = colors.live,
        )
    }

    CompositionLocalProvider(
        LocalDensity provides canvasDensity,
        LocalTwiColors provides colors,
        LocalTwiTypography provides DefaultTwiTypography,
    ) {
        MaterialTheme(colorScheme = materialColors, content = content)
    }
}
