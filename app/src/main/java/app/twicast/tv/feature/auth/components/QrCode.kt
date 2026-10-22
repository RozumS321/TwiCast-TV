package app.twicast.tv.feature.auth.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

/** QR for [content], drawn as one path (a single draw call, cached by the RenderNode). */
@Composable
fun QrCode(content: String, color: Color, modifier: Modifier = Modifier) {
    val matrix = remember(content) { encode(content) }
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.minDimension / matrix.width
            val path = Path()
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    if (matrix[x, y]) path.addRect(Rect(x * cell, y * cell, (x + 1) * cell, (y + 1) * cell))
                }
            }
            drawPath(path, color)
        }
    }
}

private fun encode(content: String): BitMatrix =
    // Size 0: ZXing returns the bare module grid; the canvas scales it. The quiet zone is the panel padding.
    QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0))
