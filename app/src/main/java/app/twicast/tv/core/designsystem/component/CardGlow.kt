package app.twicast.tv.core.designsystem.component

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * The glow is drawn at this fraction of its size and scaled up smoothly: it has no detail to lose, and the blur
 * is four times cheaper to make. Made once per card size and colour, then shared by every card of that size.
 */
private const val GlowScale = 4

/** How strong the glow is at full focus. */
private const val GlowAlpha = 0.6f

private val glowCache = HashMap<Long, ImageBitmap>()

/**
 * A soft coloured glow around a focused card, like the category covers' shadow, but pre-rendered: the frames of
 * the focus animation only fade one bitmap in, instead of the GPU recomputing a real shadow each frame (which
 * cost weak 2 GB TVs about a tenth of their scrolling frames).
 *
 * @param spread how far the glow reaches past the card; [drop] moves it down a little, as light from above.
 */
fun Modifier.focusGlow(progress: FocusProgress, color: Color, radius: Dp, spread: Dp, drop: Dp): Modifier = drawWithCache {
    val spreadPx = spread.toPx().roundToInt()
    val dropPx = drop.toPx().roundToInt()
    val glow = glowBitmap(size.width.roundToInt(), size.height.roundToInt(), radius.toPx(), spreadPx, color.toArgb())
    val offset = IntOffset(-spreadPx, -spreadPx + dropPx)
    val dst = IntSize(size.width.roundToInt() + spreadPx * 2, size.height.roundToInt() + spreadPx * 2)
    onDrawBehind {
        val p = progress.value
        if (p > 0f) drawImage(glow, dstOffset = offset, dstSize = dst, alpha = GlowAlpha * p * p, filterQuality = FilterQuality.Low)
    }
}

private fun glowBitmap(width: Int, height: Int, radiusPx: Float, spreadPx: Int, color: Int): ImageBitmap {
    val key = (width.toLong() shl 40) or (height.toLong() shl 20) or (spreadPx.toLong() shl 8) or (color.toLong() and 0xFF)
    return glowCache.getOrPut(key xor color.toLong()) {
        val w = ((width + spreadPx * 2) / GlowScale).coerceAtLeast(1)
        val h = ((height + spreadPx * 2) / GlowScale).coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val inset = spreadPx / GlowScale.toFloat()
        Canvas(bitmap).drawRoundRect(
            RectF(inset, inset, w - inset, h - inset),
            radiusPx / GlowScale,
            radiusPx / GlowScale,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color },
        )
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        frostBlur(pixels, w, h, (spreadPx / GlowScale / 2).coerceAtLeast(1))
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.asImageBitmap()
    }
}
