package app.twicast.tv.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Raw palette of the light theme (docs/design/DESIGN.md). Screens never use it directly — only [TwiColorScheme]. */
object TwiColors {
    val Background = Color(0xFFF6F5FA)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFEFEDF5)
    val Outline = Color(0xFFE2DFEA)
    val OutlineVariant = Color(0xFFE8E5F0)
    val OnSurface = Color(0xFF17151F)
    val OnSurfaceVariant = Color(0xFF5F5A6E)
    val Primary = Color(0xFF9146FF)
    val PrimaryStrong = Color(0xFF5C16C5)
    val PrimaryContainer = Color(0xFFF1EAFF)
    val Live = Color(0xFFE91916)
    val Overlay = Color(0xB80F0E14)
    val Scrim = Color(0x5217151F)
    /** Light plate over images (rank badge): white @ 92%. */
    val SurfaceOverlay = Color(0xEBFFFFFF)
    /** Text on chips and tags (#3E3A4A in the mockups; missing from the DESIGN.md token table). */
    val OnSurfaceSecondary = Color(0xFF3E3A4A)
    /** Code separator on the sign-in screen (#B7B2C4 in the mockup). */
    val OnSurfaceFaint = Color(0xFFB7B2C4)
    /** Behind video in every theme. */
    val Black = Color(0xFF000000)
    /** Secondary text on the dark player panel (#CFCBD9). */
    val OnOverlayVariant = Color(0xFFCFCBD9)
    /** Release notes: "Исправлено" and "Улучшено" badges (mockup "Обновление"). */
    val Success = Color(0xFF1D9E75)
    val Info = Color(0xFF378ADD)
    val White = Color(0xFFFFFFFF)
}

/** Raw palette of the dark theme (canvas: "Палитра · тёмная тема"). Primary, live and video plates stay as in light. */
object TwiDarkColors {
    val Background = Color(0xFF0F0E14)
    val Surface = Color(0xFF1A1824)
    val SurfaceVariant = Color(0xFF262331)
    val Outline = Color(0xFF36323F)
    val OutlineVariant = Color(0xFF2A2733)
    val OnSurface = Color(0xFFF4F2F8)
    val OnSurfaceSecondary = Color(0xFFD6D2E0)
    val OnSurfaceVariant = Color(0xFFA49EB4)
    val OnSurfaceFaint = Color(0xFF6E6880)
    /** #5C16C5 is unreadable on near-black: accent text is lighter in the dark theme. */
    val PrimaryStrong = Color(0xFFC4A0FF)
    val PrimaryContainer = Color(0xFF2B1F47)
    /** Black @ 55%: the light scrim (#17151F @ 32%) barely darkens a dark screen. */
    val Scrim = Color(0x8C000000)
    /** Dark plate over images (rank badge): surface @ 92%. */
    val SurfaceOverlay = Color(0xEB1A1824)
}

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

val LightTwiColorScheme = TwiColorScheme(
    isDark = false,
    background = TwiColors.Background,
    surface = TwiColors.Surface,
    surfaceVariant = TwiColors.SurfaceVariant,
    outline = TwiColors.Outline,
    outlineVariant = TwiColors.OutlineVariant,
    onSurface = TwiColors.OnSurface,
    onSurfaceSecondary = TwiColors.OnSurfaceSecondary,
    onSurfaceFaint = TwiColors.OnSurfaceFaint,
    onSurfaceVariant = TwiColors.OnSurfaceVariant,
    primary = TwiColors.Primary,
    onPrimary = TwiColors.White,
    primaryStrong = TwiColors.PrimaryStrong,
    primaryContainer = TwiColors.PrimaryContainer,
    live = TwiColors.Live,
    onLive = TwiColors.White,
    overlay = TwiColors.Overlay,
    surfaceOverlay = TwiColors.SurfaceOverlay,
    onOverlay = TwiColors.White,
    onOverlayVariant = TwiColors.OnOverlayVariant,
    scrim = TwiColors.Scrim,
    playerBackground = TwiColors.Black,
    success = TwiColors.Success,
    info = TwiColors.Info,
    inverseSurface = TwiColors.OnSurface,
    inverseOnSurface = TwiColors.White,
    shadow = TwiColors.OnSurface,
)

val DarkTwiColorScheme = TwiColorScheme(
    isDark = true,
    background = TwiDarkColors.Background,
    surface = TwiDarkColors.Surface,
    surfaceVariant = TwiDarkColors.SurfaceVariant,
    outline = TwiDarkColors.Outline,
    outlineVariant = TwiDarkColors.OutlineVariant,
    onSurface = TwiDarkColors.OnSurface,
    onSurfaceSecondary = TwiDarkColors.OnSurfaceSecondary,
    onSurfaceFaint = TwiDarkColors.OnSurfaceFaint,
    onSurfaceVariant = TwiDarkColors.OnSurfaceVariant,
    primary = TwiColors.Primary,
    onPrimary = TwiColors.White,
    primaryStrong = TwiDarkColors.PrimaryStrong,
    primaryContainer = TwiDarkColors.PrimaryContainer,
    live = TwiColors.Live,
    onLive = TwiColors.White,
    overlay = TwiColors.Overlay,
    surfaceOverlay = TwiDarkColors.SurfaceOverlay,
    onOverlay = TwiColors.White,
    onOverlayVariant = TwiColors.OnOverlayVariant,
    scrim = TwiDarkColors.Scrim,
    playerBackground = TwiColors.Black,
    success = TwiColors.Success,
    info = TwiColors.Info,
    inverseSurface = TwiDarkColors.OnSurface,
    inverseOnSurface = TwiColors.OnSurface,
    shadow = TwiColors.Black,
)
