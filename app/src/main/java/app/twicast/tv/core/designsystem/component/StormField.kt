package app.twicast.tv.core.designsystem.component

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** The storm frame: tiny (the clouds have no fine detail), scaled up on screen with smoothing. */
internal const val StormWidthPx = 160
internal const val StormHeightPx = 90

/** How fast the clouds billow and how fast the whole storm drifts left (canvas: "Эффекты · фон: тёмная гроза"). */
private const val StormTimeScale = 0.18f
private const val StormDrift = 0.4f
private const val StormSpanX = 3.2f
private const val StormSpanY = 1.8f
private const val WarpScale = 0.8f
private const val WarpStrength = 1.6f

/** Colours from the dark ground up to the lit edges of the clouds, and how the clouds fill the sky. */
internal data class StormLook(
    val ground: Int,
    val cloud: Int,
    val light: Int,
    val edge: Int,
    /** 0 = a few wisps, 1 = most of the sky covered. */
    val cover: Float,
    val brightness: Float,
)

/**
 * "Тёмная гроза": dim storm clouds billowing through the dark, lighter toward the bottom-right. Value-noise fbm
 * with a domain warp, so the clouds roll instead of sliding as a flat texture. Same formula as the canvas board.
 *
 * @return opaque ARGB pixels, [width] × [height].
 */
internal fun stormFrame(look: StormLook, seconds: Float, width: Int = StormWidthPx, height: Int = StormHeightPx): IntArray {
    val t = seconds * StormTimeScale
    val threshold = 0.62f - look.cover * 0.35f
    val range = 0.38f + look.cover * 0.35f
    val pixels = IntArray(width * height)
    for (j in 0 until height) {
        val y = j / height.toFloat() * StormSpanY
        for (i in 0 until width) {
            val x = i / width.toFloat() * StormSpanX + t * StormDrift
            val wx = fbm(x * WarpScale + t, y * WarpScale - t * 0.6f)
            val wy = fbm(x * WarpScale - t * 0.7f + 5.2f, y * WarpScale + t * 0.4f + 1.3f)
            var v = fbm(x + wx * WarpStrength + t * 0.5f, y + wy * WarpStrength)
            // Dark gaps between the clouds: the screen stays mostly night.
            v = max(0f, (v - threshold) / range)
            // The storm's light comes from the bottom-right.
            v *= look.brightness * (0.65f + 0.45f * (i / width.toFloat()) * (0.5f + j / height.toFloat()))
            pixels[j * width + i] = ramp(look, min(1f, v))
        }
    }
    return pixels
}

private fun ramp(look: StormLook, k: Float): Int = when {
    k <= 0.45f -> mix(look.ground, look.cloud, k / 0.45f)
    k <= 0.78f -> mix(look.cloud, look.light, (k - 0.45f) / 0.33f)
    else -> mix(look.light, look.edge, (k - 0.78f) / 0.22f)
}

private fun mix(a: Int, b: Int, f: Float): Int {
    fun ch(shift: Int) = (((a shr shift) and 0xFF) + (((b shr shift) and 0xFF) - ((a shr shift) and 0xFF)) * f).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

private fun hash(x: Int, y: Int): Float {
    var h = x * 374761393 + y * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)).toLong() and 0xFFFFFFFFL) / 4294967295f
}

private fun noise(x: Float, y: Float): Float {
    val xi = floor(x).toInt()
    val yi = floor(y).toInt()
    val xf = x - xi
    val yf = y - yi
    val u = xf * xf * (3 - 2 * xf)
    val v = yf * yf * (3 - 2 * yf)
    val a = hash(xi, yi)
    val b = hash(xi + 1, yi)
    val c = hash(xi, yi + 1)
    val d = hash(xi + 1, yi + 1)
    return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v
}

private fun fbm(x0: Float, y0: Float): Float {
    var x = x0
    var y = y0
    var sum = 0f
    var amp = 0.5f
    repeat(4) {
        sum += amp * noise(x, y)
        val nx = x * 1.6f + y * 1.2f
        val ny = -x * 1.2f + y * 1.6f
        x = nx + 3.1f
        y = ny + 1.7f
        amp *= 0.5f
    }
    return sum
}
