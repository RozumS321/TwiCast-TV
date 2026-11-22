package app.twicast.tv.core.designsystem.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween

/** Durations and the easing from [TwiThemeConfig]. All animated values are applied in the draw phase (graphicsLayer / drawBehind). */
object TwiMotion {
    private val tokens = TwiThemeConfig.motion
    val FocusDurationMillis = tokens.shortMillis
    val SidebarDurationMillis = tokens.mediumMillis
    const val SidebarLabelFadeInMillis = 150
    const val SidebarLabelFadeInDelayMillis = 80
    const val SidebarLabelFadeOutMillis = 100
    const val ScreenFadeMillis = 180

    /** A card growing into the player. */
    const val LaunchMillis = 450
    const val SkeletonPulseMillis = 900

    /** The one curve of [TwiThemeConfig] (fast start, long soft landing). */
    val Decelerate: Easing = tokens.easing.let { CubicBezierEasing(it.a, it.b, it.c, it.d) }
    val Standard: Easing = Decelerate

    fun <T> focus(): AnimationSpec<T> = tween(FocusDurationMillis, easing = Decelerate)

    fun <T> sidebar(): AnimationSpec<T> = tween(SidebarDurationMillis, easing = Standard)

    fun <T> sidebarLabelIn(): AnimationSpec<T> =
        tween(SidebarLabelFadeInMillis, delayMillis = SidebarLabelFadeInDelayMillis, easing = Standard)

    fun <T> sidebarLabelOut(): AnimationSpec<T> = tween(SidebarLabelFadeOutMillis, easing = Standard)

    fun <T> skeletonPulse(): InfiniteRepeatableSpec<T> =
        infiniteRepeatable(tween(SkeletonPulseMillis, easing = Standard), RepeatMode.Reverse)
}
