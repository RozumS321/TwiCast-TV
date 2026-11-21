package app.twicast.tv.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.twicast.tv.R

/** Geist: one family for the whole interface, with Cyrillic and Ukrainian letters. A variable font: 100–900. */
@OptIn(ExperimentalTextApi::class)
val Geist = FontFamily(
    Font(R.font.geist, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.geist, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.geist, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

/** Figures that tick: viewers, uptime, time codes, versions, the sign-in code. */
@OptIn(ExperimentalTextApi::class)
val GeistMono = FontFamily(
    Font(R.font.geist_mono, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.geist_mono, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
)

/** Text styles from DESIGN.md. Sizes are in design units (see [TwiCastTheme]). Colors are applied by components. */
@Immutable
data class TwiTypography(
    val screenTitle: TextStyle,
    val heroTitle: TextStyle,
    val rowTitle: TextStyle,
    val rowTitleDense: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val caption: TextStyle,
    val captionDense: TextStyle,
    val overline: TextStyle,
    val button: TextStyle,
    val buttonSecondary: TextStyle,
    val chip: TextStyle,
    val tag: TextStyle,
    val tagDense: TextStyle,
    val navigationLabel: TextStyle,
    val navigationLabelActive: TextStyle,
    val logo: TextStyle,
    val channelName: TextStyle,
    val greeting: TextStyle,
    val query: TextStyle,
    val key: TextStyle,
    val actionKey: TextStyle,
    val listTitle: TextStyle,
    val tab: TextStyle,
    val channelCardName: TextStyle,
    val channelCardNameCompact: TextStyle,
    val liveCount: TextStyle,
    val statValue: TextStyle,
    val avatarInitials: TextStyle,
    val loginTitle: TextStyle,
    val stepNumber: TextStyle,
    val step: TextStyle,
    val label: TextStyle,
    val status: TextStyle,
    val codeCharacter: TextStyle,
    val codeSeparator: TextStyle,
    val bodySmall: TextStyle,
    val badge: TextStyle,
    val badgeLarge: TextStyle,
    val count: TextStyle,
    val countLarge: TextStyle,
    val rank: TextStyle,
)

/**
 * Hierarchy from size and color, not weight: two weights only (unslop). The weights the mockups once asked for
 * (bold, extra bold) all become medium; negative tracking stays only on large display text.
 */
private fun style(size: Int, weight: FontWeight, letterSpacing: Double = 0.0, mono: Boolean = false, figures: Boolean = false) = TextStyle(
    fontFamily = if (mono) GeistMono else Geist,
    // Counts and durations: digits of one width, so "18 214" does not jitter as it changes.
    fontFeatureSettings = if (figures) "tnum" else null,
    fontSize = size.sp,
    fontWeight = if (weight >= FontWeight.Medium) FontWeight.Medium else FontWeight.Normal,
    letterSpacing = if (size >= LargeTextSize) letterSpacing.coerceAtMost(0.0).em else 0.em,
)

private const val LargeTextSize = 24

val DefaultTwiTypography = TwiTypography(
    screenTitle = style(32, FontWeight.ExtraBold, letterSpacing = -0.02),
    heroTitle = style(26, FontWeight.ExtraBold, letterSpacing = -0.01).copy(lineHeight = 1.2.em),
    rowTitle = style(22, FontWeight.ExtraBold, letterSpacing = -0.01),
    rowTitleDense = style(20, FontWeight.ExtraBold, letterSpacing = -0.01),
    cardTitle = style(14, FontWeight.Bold),
    body = style(15, FontWeight.Medium),
    bodyStrong = style(16, FontWeight.SemiBold),
    caption = style(13, FontWeight.Normal),
    captionDense = style(12, FontWeight.Normal),
    overline = style(13, FontWeight.ExtraBold, letterSpacing = 0.08),
    button = style(16, FontWeight.ExtraBold),
    buttonSecondary = style(16, FontWeight.Bold),
    chip = style(14, FontWeight.Bold),
    tag = style(13, FontWeight.SemiBold),
    tagDense = style(12, FontWeight.SemiBold),
    navigationLabel = style(16, FontWeight.SemiBold),
    navigationLabelActive = style(16, FontWeight.Bold),
    logo = style(22, FontWeight.ExtraBold, letterSpacing = -0.02),
    channelName = style(18, FontWeight.ExtraBold),
    greeting = style(30, FontWeight.ExtraBold, letterSpacing = -0.02),
    query = style(22, FontWeight.Bold),
    key = style(17, FontWeight.Bold),
    actionKey = style(14, FontWeight.Bold),
    listTitle = style(16, FontWeight.ExtraBold),
    tab = style(14, FontWeight.ExtraBold),
    channelCardName = style(15, FontWeight.ExtraBold),
    channelCardNameCompact = style(13, FontWeight.ExtraBold),
    liveCount = style(12, FontWeight.Bold, figures = true),
    statValue = style(22, FontWeight.ExtraBold, figures = true),
    avatarInitials = style(44, FontWeight.ExtraBold),
    loginTitle = style(32, FontWeight.ExtraBold, letterSpacing = -0.02).copy(lineHeight = 1.15.em),
    stepNumber = style(16, FontWeight.ExtraBold),
    step = style(16, FontWeight.Normal),
    label = style(13, FontWeight.Bold),
    status = style(15, FontWeight.Bold),
    codeCharacter = style(30, FontWeight.ExtraBold, letterSpacing = 0.02, mono = true),
    codeSeparator = style(28, FontWeight.ExtraBold),
    bodySmall = style(14, FontWeight.Normal),
    badge = style(10, FontWeight.ExtraBold, letterSpacing = 0.04),
    badgeLarge = style(12, FontWeight.ExtraBold, letterSpacing = 0.04),
    count = style(11, FontWeight.Bold, figures = true),
    countLarge = style(13, FontWeight.Bold, figures = true),
    rank = style(11, FontWeight.ExtraBold, figures = true),
)
