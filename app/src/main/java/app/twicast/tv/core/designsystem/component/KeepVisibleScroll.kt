package app.twicast.tv.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import app.twicast.tv.core.designsystem.theme.TwiDimens
import kotlin.math.abs

/**
 * Scrolls only when the focused element is not fully visible. Compose's TV default "pivots" the focused
 * element to ~30% of the list, which scrolled the hero off the top of home although it was fully visible.
 * When it does scroll, it leaves [edgeMarginPx] beyond the element: only cards take focus, so without it the row
 * title above a card (and the screen header above the first row) or the list's end padding stayed cut off.
 */
@OptIn(ExperimentalFoundationApi::class)
private class KeepVisibleBringIntoViewSpec(private val edgeMarginPx: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val trailingEdge = offset + size
        // Never so much margin that the element itself would not fit.
        val margin = edgeMarginPx.coerceAtMost(((containerSize - size) / 2).coerceAtLeast(0f))
        return when {
            offset >= 0f && trailingEdge <= containerSize -> 0f
            // Taller than the viewport, or cut at the top: align the leading edge.
            size > containerSize || offset < 0f -> offset - margin
            else -> trailingEdge - containerSize + margin
        }.takeUnless { abs(it) < 1f } ?: 0f
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeepFocusedVisibleScroll(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val spec = remember(density) { KeepVisibleBringIntoViewSpec(with(density) { TwiDimens.ScrollEdgeMargin.toPx() }) }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}
