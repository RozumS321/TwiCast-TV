package app.twicast.tv.feature.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Home on the remote (or another app on top) stops the screen: [onBackground] runs then, [onForeground] when the
 * viewer comes back. Unlike a plain ON_START observer, [onForeground] never runs for the first start.
 */
@Composable
fun BackgroundEffect(key: Any, onBackground: () -> Unit, onForeground: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBackground = rememberUpdatedState(onBackground)
    val currentOnForeground = rememberUpdatedState(onForeground)
    DisposableEffect(key, lifecycleOwner) {
        var inBackground = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    inBackground = true
                    currentOnBackground.value()
                }
                Lifecycle.Event.ON_START -> if (inBackground) {
                    inBackground = false
                    currentOnForeground.value()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
