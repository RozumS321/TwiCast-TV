package app.twicast.tv.core.designsystem.component

/**
 * Box blur run three times (≈ a Gaussian), in place, on ARGB pixels: the "Stack Blur" idea without RenderScript
 * (deprecated) or a library. Used on a small 320×180 frame of the background, so it takes about a millisecond.
 * [brighten] lifts the colors a little: frosted glass gathers light.
 */
internal fun frostBlur(pixels: IntArray, width: Int, height: Int, radius: Int, brighten: Float = 1f) {
    if (radius < 1 || width < 1 || height < 1) return
    val buffer = IntArray(pixels.size)
    repeat(Passes) {
        boxBlurHorizontal(pixels, buffer, width, height, radius)
        boxBlurVertical(buffer, pixels, width, height, radius)
    }
    if (brighten != 1f) {
        for (i in pixels.indices) {
            val c = pixels[i]
            val a = c ushr 24
            val r = ((c shr 16 and 0xFF) * brighten).toInt().coerceAtMost(255)
            val g = ((c shr 8 and 0xFF) * brighten).toInt().coerceAtMost(255)
            val b = ((c and 0xFF) * brighten).toInt().coerceAtMost(255)
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
}

private const val Passes = 3

private fun boxBlurHorizontal(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int) {
    val window = radius * 2 + 1
    for (y in 0 until height) {
        val row = y * width
        var a = 0; var r = 0; var g = 0; var b = 0
        for (k in -radius..radius) {
            val c = src[row + k.coerceIn(0, width - 1)]
            a += c ushr 24; r += c shr 16 and 0xFF; g += c shr 8 and 0xFF; b += c and 0xFF
        }
        for (x in 0 until width) {
            dst[row + x] = ((a / window) shl 24) or ((r / window) shl 16) or ((g / window) shl 8) or (b / window)
            val out = src[row + (x - radius).coerceIn(0, width - 1)]
            val inn = src[row + (x + radius + 1).coerceIn(0, width - 1)]
            a += (inn ushr 24) - (out ushr 24)
            r += (inn shr 16 and 0xFF) - (out shr 16 and 0xFF)
            g += (inn shr 8 and 0xFF) - (out shr 8 and 0xFF)
            b += (inn and 0xFF) - (out and 0xFF)
        }
    }
}

private fun boxBlurVertical(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int) {
    val window = radius * 2 + 1
    for (x in 0 until width) {
        var a = 0; var r = 0; var g = 0; var b = 0
        for (k in -radius..radius) {
            val c = src[k.coerceIn(0, height - 1) * width + x]
            a += c ushr 24; r += c shr 16 and 0xFF; g += c shr 8 and 0xFF; b += c and 0xFF
        }
        for (y in 0 until height) {
            dst[y * width + x] = ((a / window) shl 24) or ((r / window) shl 16) or ((g / window) shl 8) or (b / window)
            val out = src[(y - radius).coerceIn(0, height - 1) * width + x]
            val inn = src[(y + radius + 1).coerceIn(0, height - 1) * width + x]
            a += (inn ushr 24) - (out ushr 24)
            r += (inn shr 16 and 0xFF) - (out shr 16 and 0xFF)
            g += (inn shr 8 and 0xFF) - (out shr 8 and 0xFF)
            b += (inn and 0xFF) - (out and 0xFF)
        }
    }
}
