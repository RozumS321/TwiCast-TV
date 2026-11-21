package app.twicast.tv.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Behind video and in hand-drawn shadows, in every theme. */
private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)

/** Dark glass over video: the player's island, panels and notices. */
private val VideoOverlay = Color(0xC70C0C0E)
private val OnVideo = Color(0xFFEDEDED)
private val OnVideoSecondary = Color(0xFFA1A1A6)

/** How strongly the accent tints a selected surface (selected chip, "у вас" badge background). */
private const val AccentContainerAmount = 0.16f
private const val SurfaceOverlayAlpha = 0.92f

/**
 * Semantic color tokens. Every screen and component reads colors from here via [TwiTheme.colors],
 * so the dark theme becomes a second instance of this class.
 */
@Immutable
data class TwiColorScheme(
    /** For the few colors computed against the background (chat names), not for picking tokens. */
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val onSurface: Color,
    val onSurfaceSecondary: Color,
    val onSurfaceFaint: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryStrong: Color,
    val primaryContainer: Color,
    val live: Color,
    val onLive: Color,
    val overlay: Color,
    val surfaceOverlay: Color,
    val onOverlay: Color,
    val onOverlayVariant: Color,
    val scrim: Color,
    val playerBackground: Color,
    val success: Color,
    val info: Color,
    /** Selected chip: inverted surface. */
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    /** Soft shadows drawn by hand (sidebar edge): a light-text color would glow instead of shade. */
    val shadow: Color,
)

/** Semantic tokens from one palette of [TwiThemeConfig]: screens never see the palette itself. */
fun twiColorScheme(palette: TwiPalette, isDark: Boolean) = TwiColorScheme(
    isDark = isDark,
    background = palette.background,
    surface = palette.surface,
    surfaceVariant = palette.raised,
    outline = palette.hairline,
    outlineVariant = palette.hairline,
    onSurface = palette.text,
    onSurfaceSecondary = palette.textSecondary,
    onSurfaceFaint = palette.textMuted,
    onSurfaceVariant = palette.textSecondary,
    primary = palette.accent,
    onPrimary = palette.onAccent,
    primaryStrong = palette.accentText,
    primaryContainer = lerp(palette.surface, palette.accent, AccentContainerAmount),
    live = palette.live,
    onLive = White,
    overlay = VideoOverlay,
    surfaceOverlay = palette.surface.copy(alpha = SurfaceOverlayAlpha),
    onOverlay = OnVideo,
    onOverlayVariant = OnVideoSecondary,
    scrim = palette.scrim,
    playerBackground = Black,
    success = palette.positive,
    info = palette.info,
    inverseSurface = palette.text,
    inverseOnSurface = palette.background,
    shadow = if (isDark) Black else palette.text,
)

val LightTwiColorScheme = twiColorScheme(TwiThemeConfig.light, isDark = false)

val DarkTwiColorScheme = twiColorScheme(TwiThemeConfig.dark, isDark = true)
