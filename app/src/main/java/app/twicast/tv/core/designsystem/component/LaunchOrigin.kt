package app.twicast.tv.core.designsystem.component

import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates

/**
 * Where on screen the card that opened the player sat (canvas: "Плеер · открытие эфира"): the player grows out of
 * it and shrinks back into it. Set by a card right before its click is handled; read by the navigation transition.
 */
object LaunchOrigin {
    /** Centre of the last opened card as a fraction of the screen; the screen centre until a card is opened. */
    var origin: TransformOrigin = TransformOrigin.Center
        private set

    /** Remembers [coordinates] (a card's, still attached) as the place the next screen grows from. */
    fun from(coordinates: LayoutCoordinates?) {
        val card = coordinates?.takeIf { it.isAttached } ?: return
        val root = card.findRootCoordinates().size
        if (root.width == 0 || root.height == 0) return
        val center = card.boundsInRoot().center
        origin = TransformOrigin(center.x / root.width, center.y / root.height)
    }
}
