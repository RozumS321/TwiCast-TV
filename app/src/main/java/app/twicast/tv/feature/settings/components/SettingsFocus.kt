package app.twicast.tv.feature.settings.components

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager

/** The open settings tab: Left out of a section lands here, never in the main menu. */
val LocalSettingsTab = staticCompositionLocalOf<FocusRequester?> { null }

/**
 * A row of side-by-side choices (chips, player cards): Left moves to the neighbour in the row; past its first item
 * it goes to the open tab. Geometric search was not trusted elsewhere in settings: a focused card grows 4%, so
 * its edge pokes left of the cards stacked under it, and Left jumped to them, then on to the main menu.
 */
@Composable
fun Modifier.settingsRow(): Modifier {
    val focusManager = LocalFocusManager.current
    val tab = LocalSettingsTab.current
    val inRow = remember { booleanArrayOf(false) }
    return onFocusChanged { inRow[0] = it.hasFocus }
        .onKeyEvent { event ->
            if (event.key != Key.DirectionLeft || event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val moved = focusManager.moveFocus(FocusDirection.Left)
            if (!moved || !inRow[0]) tab?.requestFocus()
            true
        }
        .focusGroup()
}
