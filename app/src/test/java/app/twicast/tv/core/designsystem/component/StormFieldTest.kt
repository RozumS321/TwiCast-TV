package app.twicast.tv.core.designsystem.component

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StormFieldTest {
    private val look = StormLook(ground = 0x07060C, cloud = 0x241A52, light = 0x5B3FB8, edge = 0x8C6CFF, cover = 0.55f, brightness = 0.7f)

    private fun luma(argb: Int) = ((argb shr 16) and 0xFF) + ((argb shr 8) and 0xFF) + (argb and 0xFF)

    private fun meanLuma(pixels: IntArray, x0: Int, y0: Int, x1: Int, y1: Int): Double {
        var sum = 0L
        for (y in y0 until y1) for (x in x0 until x1) sum += luma(pixels[y * StormWidthPx + x])
        return sum.toDouble() / ((x1 - x0) * (y1 - y0))
    }

    @Test
    fun `every pixel is opaque and no brighter than the cloud edge colour`() {
        val edge = luma(look.edge)
        stormFrame(look, 7f).forEach { px ->
            assertTrue(px ushr 24 == 0xFF)
            assertTrue(luma(px) <= edge + 3)
        }
    }

    @Test
    fun `the storm is lighter toward the bottom-right, averaged over time`() {
        var topLeft = 0.0
        var bottomRight = 0.0
        for (s in 0 until 40) {
            val frame = stormFrame(look, s * 3f)
            topLeft += meanLuma(frame, 0, 0, 40, 22)
            bottomRight += meanLuma(frame, 120, 68, 160, 90)
        }
        assertTrue("top-left $topLeft, bottom-right $bottomRight", bottomRight > topLeft)
    }

    @Test
    fun `the clouds move, and the same moment always looks the same`() {
        assertArrayEquals(stormFrame(look, 5f), stormFrame(look, 5f))
        assertFalse(stormFrame(look, 5f).contentEquals(stormFrame(look, 6f)))
    }
}
