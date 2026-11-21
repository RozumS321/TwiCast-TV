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
    // Twitch's own tones (feedback: "цвета в оттенках твича"): near-black, Twitch purple, Twitch red.
    dark = TwiPalette(
        background = Color(0xFF0E0E10),
        backgroundDeep = Color(0xFF1F1F23),
        surface = Color(0xFF18181B),
        raised = Color(0xFF26262C),
        hairline = Color(0x1AEFEFF1),
        text = Color(0xFFEFEFF1),
        textSecondary = Color(0xFFADADB8),
        textMuted = Color(0xFF848494),
        accent = Color(0xFF9147FF),
        onAccent = Color(0xFFFFFFFF),
        accentText = Color(0xFFBF94FF),
        live = Color(0xFFEB0400),
        positive = Color(0xFF00C26F),
        info = Color(0xFF8FB0D8),
        scrim = Color(0x8C000000),
    ),
    light = TwiPalette(
        background = Color(0xFFF7F7F8),
        backgroundDeep = Color(0xFFEFEFF1),
        surface = Color(0xFFFFFFFF),
        raised = Color(0xFFEFEFF1),
        hairline = Color(0x1A0E0E10),
        text = Color(0xFF0E0E10),
        textSecondary = Color(0xFF53535F),
        textMuted = Color(0xFF6E6E7A),
        accent = Color(0xFF772CE8),
        onAccent = Color(0xFFFFFFFF),
        accentText = Color(0xFF772CE8),
        live = Color(0xFFEB0400),
        positive = Color(0xFF00A35C),
        info = Color(0xFF2F6DB5),
        scrim = Color(0x520E0E10),
    ),
    radius = TwiRadiusTokens(button = 10.dp, thumbnail = 10.dp, card = 14.dp, panel = 20.dp, badge = 6.dp),
    focus = TwiFocusTokens(ringWidth = 2.dp, ringGap = 2.dp, cardScale = 1.04f, buttonScale = 1.04f, shadows = false),
    motion = TwiMotionTokens(easing = CubicCurve(0.22f, 1f, 0.36f, 1f), shortMillis = 150, mediumMillis = 300, longMillis = 500),
    effects = TwiEffectsTokens(
        // The living background in Twitch purple instead of ember orange.
        emberCore = Color(0xFFA970FF),
        emberMid = Color(0xFF772CE8),
        emberEdge = Color(0xFF3A186E),
        emberBase = Color(0xFF0B0A10),
        bloomLoopMillis = 10_000,
        grainAlpha = 0.09f,
        glassAlpha = 0.10f,
        glassRimAlpha = 0.10f,
        // No glossy highlight: it made glass look like 3D buttons from the 2000s. Flat frost and a hairline.
        glassHighlightAlpha = 0f,
    ),
)
