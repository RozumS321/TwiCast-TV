package app.twicast.tv.feature.player.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadableNameColorTest {

    private val yellow = Color(0xFFFFFF00)
    private val navy = Color(0xFF000080)
    private val purple = Color(0xFF9146FF)

    @Test
    fun `light panel darkens light names and keeps the rest`() {
        assertTrue(readableNameColor(yellow, onDarkPanel = false).luminance() < yellow.luminance())
        assertEquals(navy, readableNameColor(navy, onDarkPanel = false))
    }

    @Test
    fun `dark panel lightens dark names and keeps the rest`() {
        assertTrue(readableNameColor(navy, onDarkPanel = true).luminance() > navy.luminance())
        assertEquals(yellow, readableNameColor(yellow, onDarkPanel = true))
        assertEquals(purple, readableNameColor(purple, onDarkPanel = true))
    }
}
