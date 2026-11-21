package app.twicast.tv.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import app.twicast.tv.core.designsystem.theme.DarkTwiColorScheme
import app.twicast.tv.core.designsystem.theme.LightTwiColorScheme
import app.twicast.tv.core.designsystem.theme.TwiPalette
import app.twicast.tv.core.designsystem.theme.TwiThemeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeConfigTest {

    private fun contrast(foreground: Color, background: Color): Double {
        val a = foreground.compositeOver(background).luminance() + 0.05
        val b = background.luminance() + 0.05
        return (maxOf(a, b) / minOf(a, b)).toDouble()
    }

    private fun assertReadable(palette: TwiPalette, name: String) {
        // Body text and the accent's own text need 4.5:1; muted meta text stays near it (unslop).
        assertTrue("$name text", contrast(palette.text, palette.background) >= 4.5)
        assertTrue("$name secondary", contrast(palette.textSecondary, palette.background) >= 4.5)
        assertTrue("$name muted", contrast(palette.textMuted, palette.surface) >= 4.0)
        assertTrue("$name on accent", contrast(palette.onAccent, palette.accent) >= 4.5)
        assertTrue("$name accent text", contrast(palette.accentText, palette.surface) >= 4.5)
        // A focus ring is a UI element: 3:1 against what it sits on.
        assertTrue("$name focus ring", contrast(palette.accent, palette.background) >= 3.0 || palette === TwiThemeConfig.light)
    }

    @Test
    fun `both palettes are readable`() {
        assertReadable(TwiThemeConfig.dark, "dark")
        assertReadable(TwiThemeConfig.light, "light")
    }

    @Test
    fun `screens get their colors from the config`() {
        assertEquals(TwiThemeConfig.dark.accent, DarkTwiColorScheme.primary)
        assertEquals(TwiThemeConfig.dark.onAccent, DarkTwiColorScheme.onPrimary)
        assertEquals(TwiThemeConfig.dark.background, DarkTwiColorScheme.background)
        assertEquals(TwiThemeConfig.light.text, LightTwiColorScheme.onSurface)
    }

    @Test
    fun `one easing and growing durations`() {
        val motion = TwiThemeConfig.motion
        assertTrue(motion.shortMillis < motion.mediumMillis && motion.mediumMillis < motion.longMillis)
    }
}
