package app.twicast.tv.core.designsystem.component

import androidx.compose.ui.geometry.Rect
import app.twicast.tv.core.designsystem.theme.TwiThemeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuroraLogoTest {
    private val tokens = TwiThemeConfig.logo
    private val box = Rect(10f, 10f, 58f, 66f)

    @Test
    fun `three patches in the logo colours, scaled by intensity`() {
        val patches = auroraPatches(tokens, AuroraRestSeconds, box)
        assertEquals(listOf(tokens.glowA, tokens.glowB, tokens.glowC), patches.map { it.color })
        val dimmer = auroraPatches(tokens.copy(intensity = tokens.intensity / 2), AuroraRestSeconds, box)
        patches.zip(dimmer).forEach { (full, half) -> assertEquals(full.alpha / 2, half.alpha, 1e-5f) }
    }

    @Test
    fun `the colour flows over time and every patch keeps near the play`() {
        val rest = auroraPatches(tokens, AuroraRestSeconds, box)
        val later = auroraPatches(tokens, AuroraRestSeconds + 2f, box)
        rest.zip(later).forEach { (a, b) -> assertNotEquals(a.center, b.center) }
        // A centre may drift a little past the play's edge (as on the canvas); its light still falls inside.
        val near = box.inflate(box.height * 0.1f)
        for (s in 0 until 600) {
            auroraPatches(tokens, s / 10f, box).forEach { p -> assertTrue("${p.center} at ${s / 10f}s", near.contains(p.center)) }
        }
    }

    @Test
    fun `the still frame is always the same`() {
        assertEquals(auroraPatches(tokens, AuroraRestSeconds, box), auroraPatches(tokens, AuroraRestSeconds, box))
    }
}
