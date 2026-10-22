package app.twicast.tv.core.designsystem.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween

/** Durations and easings from DESIGN.md. All animated values are applied in the draw phase (graphicsLayer / drawBehind). */
object TwiMotion {
    const val FocusDurationMillis = 200
    const val SidebarDurationMillis = 250
    const val SidebarLabelFadeInMillis = 150
    const val SidebarLabelFadeInDelayMillis = 80
    const val SidebarLabelFadeOutMillis = 100
    const val ScreenFadeMillis = 180
    const val SkeletonPulseMillis = 900

    /** Android's DecelerateInterpolator equivalent. */
    val Decelerate: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)
    val Standard: Easing = FastOutSlowInEasing

    fun <T> focus(): AnimationSpec<T> = tween(FocusDurationMillis, easing = Decelerate)

    fun <T> sidebar(): AnimationSpec<T> = tween(SidebarDurationMillis, easing = Standard)

    fun <T> sidebarLabelIn(): AnimationSpec<T> =
        tween(SidebarLabelFadeInMillis, delayMillis = SidebarLabelFadeInDelayMillis, easing = Standard)

    fun <T> sidebarLabelOut(): AnimationSpec<T> = tween(SidebarLabelFadeOutMillis, easing = Standard)

    fun <T> skeletonPulse(): InfiniteRepeatableSpec<T> =
        infiniteRepeatable(tween(SkeletonPulseMillis, easing = Standard), RepeatMode.Reverse)
}
