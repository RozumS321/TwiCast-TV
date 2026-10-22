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
    val White = Color(0xFFFFFFFF)
}

/**
 * Semantic color tokens. Every screen and component reads colors from here via [TwiTheme.colors],
 * so the dark theme becomes a second instance of this class.
 */
@Immutable
data class TwiColorScheme(
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
    /** Selected chip: inverted surface. */
    val inverseSurface: Color,
    val inverseOnSurface: Color,
)

val LightTwiColorScheme = TwiColorScheme(
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
    inverseSurface = TwiColors.OnSurface,
    inverseOnSurface = TwiColors.White,
)
