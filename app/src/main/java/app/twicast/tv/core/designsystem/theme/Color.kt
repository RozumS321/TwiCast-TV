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
    /** Dialogs and panels with text to read (update, sign-in, QR): always solid, even with glass effects. */
    val dialog: Color = surface,
    /** A block inside a dialog (release notes): one step off [dialog], solid too. */
    val dialogInset: Color = background,
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

/** Glass over the Ember Bloom background ("Эффекты интерфейса"): surfaces turn translucent with a light rim. */
fun TwiColorScheme.withGlass(tokens: TwiEffectsTokens) = copy(
    background = tokens.emberBase,
    // Frost, not a dark film: a faint light tint the background's light passes through. Over our soft, blurry
    // background this reads as frosted glass even without real blur (Android 11 has none).
    surface = White.copy(alpha = tokens.glassAlpha),
    surfaceVariant = White.copy(alpha = GlassRaisedAlpha),
    outline = White.copy(alpha = tokens.glassRimAlpha),
    outlineVariant = White.copy(alpha = tokens.glassRimAlpha),
    surfaceOverlay = GlassTint.copy(alpha = DenseGlassAlpha),
    // Glass behind a dialog would let the screen show through its text: dialogs stay solid.
    dialog = GlassDialog,
    dialogInset = GlassDialogInset,
)

/**
 * Light effects (canvas "Эффекты · светлая тема"): frosted white glass over the pale storm; dialogs stay solid white.
 */
fun TwiColorScheme.withLightGlass(tokens: TwiEffectsTokens) = copy(
    background = tokens.stormLight.ground,
    surface = White.copy(alpha = LightGlassAlpha),
    surfaceVariant = White.copy(alpha = LightGlassRaisedAlpha),
    // A dark hairline: white glass on a light room needs an edge to read as a pane at all.
    outline = Black.copy(alpha = LightGlassRimAlpha),
    outlineVariant = Black.copy(alpha = LightGlassRimAlpha),
    surfaceOverlay = White.copy(alpha = DenseGlassAlpha),
    dialog = White,
    dialogInset = LightDialogInset,
)

private val LightDialogInset = Color(0xFFEFEFF1)
private const val LightGlassAlpha = 0.5f
private const val LightGlassRaisedAlpha = 0.7f
private const val LightGlassRimAlpha = 0.14f

/** Lighter than the dark room behind it, so glass reads as a frosted pane, not a hole. */
private val GlassTint = Color(0xFF2C2B30)
private val GlassDialog = Color(0xFF1A1A1D)
private val GlassDialogInset = Color(0xFF232326)
private const val GlassRaisedAlpha = 0.14f
/** Sidebar and panels over the effects background: glass, but dense enough to read as a surface. */
private const val DenseGlassAlpha = 0.94f
