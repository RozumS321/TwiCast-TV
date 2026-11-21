package app.twicast.tv.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The whole look of TwiCast in one place (canvas: "UI-библиотека"). Colors, corner radii, focus and motion
 * are read from here by [TwiColorScheme], [TwiShapes], [TwiScale] and [TwiMotion]: change a value here and it
 * changes on every screen. "Эффекты интерфейса" only switches [TwiEffectsTokens] on; the tokens stay the same.
 */
@Immutable
data class TwiThemeTokens(
    val dark: TwiPalette,
    val light: TwiPalette,
    val radius: TwiRadiusTokens,
    val focus: TwiFocusTokens,
    val motion: TwiMotionTokens,
    val effects: TwiEffectsTokens,
)

/** One neutral ramp, three text roles and one accent (unslop: quiet chrome, the color lives in the content). */
@Immutable
data class TwiPalette(
    val background: Color,
    /** Sidebar and other deepest surfaces. */
    val backgroundDeep: Color,
    val surface: Color,
    /** Active row, input, secondary button. */
    val raised: Color,
    val hairline: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    /** Focus ring, selection, the one thing that matters on a screen. */
    val accent: Color,
    /** Text and icons on [accent]. */
    val onAccent: Color,
    /** The accent as text on [surface]: dark enough to read on a light theme. */
    val accentText: Color,
    /** The "в эфире" lamp. */
    val live: Color,
    val positive: Color,
    val info: Color,
    val scrim: Color,
)

@Immutable
data class TwiRadiusTokens(
    val button: Dp,
    val thumbnail: Dp,
    val card: Dp,
    val panel: Dp,
    val badge: Dp,
)

@Immutable
data class TwiFocusTokens(
    val ringWidth: Dp,
    /** Gap between a card and its ring, filled with the background. */
    val ringGap: Dp,
    val cardScale: Float,
    val buttonScale: Float,
    /**
     * Shadows under focused cards and buttons. Off: on a dark screen depth comes from surfaces and the ring,
     * and a colored glow under a card reads as AI decoration (and gets cut off by the row it sits in).
     */
    val shadows: Boolean,
)

/** One easing curve and three durations for everything. */
@Immutable
data class TwiMotionTokens(
    val easing: CubicCurve,
    /** Focus, press, toggles. */
    val shortMillis: Int,
    /** Cards and panels moving, colors crossfading. */
    val mediumMillis: Int,
    /** Entrances, the morphing island, the background following the focus. */
    val longMillis: Int,
)

@Immutable
data class CubicCurve(val a: Float, val b: Float, val c: Float, val d: Float)

/** "Эффекты интерфейса": the Ember Bloom background and glass surfaces. No blur: Android 11 TVs have none. */
@Immutable
data class TwiEffectsTokens(
    /** Ember Bloom: warm light rising from the dark, from the core outwards. */
    val emberCore: Color,
    val emberMid: Color,
    val emberEdge: Color,
    val emberBase: Color,
    val bloomLoopMillis: Int,
    /** Film grain over the background. */
    val grainAlpha: Float,
    /** Glass: a translucent surface with a light rim and a soft highlight at the top. */
    val glassAlpha: Float,
    val glassRimAlpha: Float,
    val glassHighlightAlpha: Float,
)

val TwiThemeConfig = TwiThemeTokens(
    dark = TwiPalette(
        background = Color(0xFF141415),
        backgroundDeep = Color(0xFF0F0F10),
        surface = Color(0xFF19191B),
        raised = Color(0xFF232325),
        hairline = Color(0x14EDEDED),
        text = Color(0xFFEDEDED),
        textSecondary = Color(0xFFA1A1A6),
        textMuted = Color(0xFF85858B),
        // Coral: warm like the Ember Bloom light, used rarely (focus ring, main action, the current section).
        accent = Color(0xFFFF7A59),
        onAccent = Color(0xFF141415),
        accentText = Color(0xFFFF8E72),
        live = Color(0xFFE5484D),
        positive = Color(0xFF4CC38A),
        info = Color(0xFF8FB0D8),
        scrim = Color(0x8C000000),
    ),
    light = TwiPalette(
        background = Color(0xFFFAFAFA),
        backgroundDeep = Color(0xFFF4F4F5),
        surface = Color(0xFFFFFFFF),
        raised = Color(0xFFF4F4F5),
        hairline = Color(0x14171717),
        text = Color(0xFF171717),
        textSecondary = Color(0xFF525252),
        textMuted = Color(0xFF737373),
        accent = Color(0xFFE5603F),
        onAccent = Color(0xFF171717),
        accentText = Color(0xFFB0412A),
        live = Color(0xFFDC2626),
        positive = Color(0xFF16A34A),
        info = Color(0xFF2F6DB5),
        scrim = Color(0x52171717),
    ),
    radius = TwiRadiusTokens(button = 10.dp, thumbnail = 10.dp, card = 14.dp, panel = 20.dp, badge = 6.dp),
    focus = TwiFocusTokens(ringWidth = 2.dp, ringGap = 2.dp, cardScale = 1.04f, buttonScale = 1.04f, shadows = false),
    motion = TwiMotionTokens(easing = CubicCurve(0.22f, 1f, 0.36f, 1f), shortMillis = 150, mediumMillis = 300, longMillis = 500),
    effects = TwiEffectsTokens(
        emberCore = Color(0xFFFF8A28),
        emberMid = Color(0xFFD65414),
        emberEdge = Color(0xFF78240A),
        emberBase = Color(0xFF070605),
        bloomLoopMillis = 10_000,
        grainAlpha = 0.24f,
        glassAlpha = 0.42f,
        glassRimAlpha = 0.12f,
        glassHighlightAlpha = 0.07f,
    ),
)
