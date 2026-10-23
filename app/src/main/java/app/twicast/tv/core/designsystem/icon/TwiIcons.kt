package app.twicast.tv.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Icons drawn on the design canvas (24×24 viewport). Tint them via Icon's `tint`. */
object TwiIcons {
    val Search = strokeIcon("Search", "M11 4a7 7 0 1 1 0 14a7 7 0 0 1 0-14zM20 20l-3.6-3.6")
    val Home = strokeIcon("Home", "M3.5 11L12 4l8.5 7M5.5 9.5V20h4.5v-6h4v6h4.5V9.5")
    val Categories = strokeIcon(
        "Categories",
        "M4.5 3h4A1.5 1.5 0 0 1 10 4.5v4A1.5 1.5 0 0 1 8.5 10h-4A1.5 1.5 0 0 1 3 8.5v-4A1.5 1.5 0 0 1 4.5 3z" +
            "M15.5 3h4A1.5 1.5 0 0 1 21 4.5v4a1.5 1.5 0 0 1-1.5 1.5h-4A1.5 1.5 0 0 1 14 8.5v-4A1.5 1.5 0 0 1 15.5 3z" +
            "M4.5 14h4a1.5 1.5 0 0 1 1.5 1.5v4A1.5 1.5 0 0 1 8.5 21h-4A1.5 1.5 0 0 1 3 19.5v-4A1.5 1.5 0 0 1 4.5 14z" +
            "M15.5 14h4a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-4a1.5 1.5 0 0 1-1.5-1.5v-4a1.5 1.5 0 0 1 1.5-1.5z",
    )
    val Following = strokeIcon(
        "Following",
        "M12 20s-7-4.3-9.2-8.4C1.2 8.4 3.1 4.8 6.6 4.8c2 0 3.6 1.1 5.4 3c1.8-1.9 3.4-3 5.4-3" +
            "c3.5 0 5.4 3.6 3.8 6.8C19 15.7 12 20 12 20z",
    )
    val Profile = strokeIcon("Profile", "M12 12a4 4 0 1 0 0-8a4 4 0 0 0 0 8zM4 21c1.5-4 4.4-6 8-6s6.5 2 8 6")
    val Settings = strokeIcon(
        "Settings",
        "M12 15a3 3 0 1 0 0-6a3 3 0 0 0 0 6zM19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3" +
            "a1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5a1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1" +
            "a1.7 1.7 0 0 0 .3-1.8a1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1a1.7 1.7 0 0 0-.3-1.8l-.1-.1" +
            "a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5" +
            "a1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1" +
            "a1.7 1.7 0 0 0-1.5 1z",
    )
    val Back = strokeIcon("Back", "M15 18l-6-6l6-6", strokeWidth = 2f)
    val Play = fillIcon("Play", "M7 4v16l13-8z")
    val Video = strokeIcon("Video", "M4 5h16v14H4zM10 9v6l5-3z")
    val Gauge = strokeIcon("Gauge", "M4 18a8 8 0 1 1 16 0M12 18l4-6")
    val Pause = strokeIcon("Pause", "M8 5v14M16 5v14", strokeWidth = 2.4f)
    val Quality = strokeIcon("Quality", "M4 7h9M17 7h3M15 5v4M4 17h3M11 17h9M9 15v4", strokeWidth = 2f)
    val Chat = strokeIcon("Chat", "M4 5h16v11H9l-5 4z", strokeWidth = 2f)
    val Erase = strokeIcon("Erase", "M9 5h11v14H9l-6-7zM13 10l4 4M17 10l-4 4", strokeWidth = 2f)
    val SignOut = strokeIcon("SignOut", "M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3M10 17l-5-5l5-5M5 12h11", strokeWidth = 2f)
    val Plus = strokeIcon("Plus", "M12 5v14M5 12h14", strokeWidth = 2.6f)
    val Check = strokeIcon("Check", "M5 12l5 5L19 7", strokeWidth = 2.6f)
    val ArrowUp = strokeIcon("ArrowUp", "M12 19V5M5 12l7-7l7 7", strokeWidth = 2.6f)
    val Microphone = strokeIcon("Microphone", "M12 3a3 3 0 0 1 3 3v6a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3zM5 11a7 7 0 0 0 14 0M12 18v3", strokeWidth = 2f)
}

private const val ViewportSize = 24f
private const val StrokeWidth = 1.9f

private fun strokeIcon(name: String, pathData: String, strokeWidth: Float = StrokeWidth): ImageVector =
    iconBuilder(name).addPath(
        pathData = addPathNodes(pathData),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()

private fun fillIcon(name: String, pathData: String): ImageVector = iconBuilder(name).addPath(
    pathData = addPathNodes(pathData),
    fill = SolidColor(Color.Black),
).build()

private fun iconBuilder(name: String) = ImageVector.Builder(
    name = name,
    defaultWidth = ViewportSize.dp,
    defaultHeight = ViewportSize.dp,
    viewportWidth = ViewportSize,
    viewportHeight = ViewportSize,
)
