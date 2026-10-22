package app.twicast.tv.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import kotlin.math.abs

/**
 * Scrolls only as much as needed to show the focused element fully. Compose's TV default "pivots" the focused
 * element to ~30% of the list, which scrolled the hero off the top of home although it was fully visible.
 */
@OptIn(ExperimentalFoundationApi::class)
private object KeepVisibleBringIntoViewSpec : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val trailingEdge = offset + size
        return when {
            offset >= 0f && trailingEdge <= containerSize -> 0f
            // Taller than the viewport, or cut at the top: align the leading edge.
            size > containerSize || offset < 0f -> offset
            else -> trailingEdge - containerSize
        }.takeUnless { abs(it) < 1f } ?: 0f
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeepFocusedVisibleScroll(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBringIntoViewSpec provides KeepVisibleBringIntoViewSpec, content = content)
}
