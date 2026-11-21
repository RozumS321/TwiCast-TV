package app.twicast.tv.core.designsystem

import app.twicast.tv.core.designsystem.component.frostBlur
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrostBlurTest {

    private fun gray(v: Int) = (0xFF shl 24) or (v shl 16) or (v shl 8) or v

    @Test
    fun `a flat color stays the same`() {
        val pixels = IntArray(16 * 9) { gray(120) }

        frostBlur(pixels, 16, 9, radius = 3)

        assertTrue(pixels.all { it == gray(120) })
    }

    @Test
    fun `a hard edge becomes a soft one`() {
        val width = 20
        val pixels = IntArray(width * 4) { i -> if (i % width < width / 2) gray(0) else gray(200) }

        frostBlur(pixels, width, 4, radius = 2)

        val row = (0 until width).map { pixels[it] and 0xFF }
        assertEquals(0, row.first())
        assertEquals(200, row.last())
        // Around the old edge the values now step through the middle instead of jumping.
        assertTrue(row.any { it in 40..160 })
    }

    @Test
    fun `brighten lifts colors without overflowing`() {
        val pixels = IntArray(4) { gray(240) }

        frostBlur(pixels, 2, 2, radius = 1, brighten = 1.5f)

        assertTrue(pixels.all { it and 0xFF == 255 })
    }
}
