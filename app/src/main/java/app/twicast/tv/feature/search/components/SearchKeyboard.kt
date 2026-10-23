package app.twicast.tv.feature.search.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.icon.TwiIcons
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.feature.search.KeyboardLayout

private const val KeyboardColumns = 7
private const val LatinKeys = "abcdefghijklmnopqrstuvwxyz0123456789_"
private const val RussianKeys = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя0123456789"
private const val UkrainianKeys = "абвгґдеєжзиіїйклмнопрстуфхцчшщьюя0123456789"

/** Order of the layout menu; each layout is named the same in every interface language. */
private val Layouts = listOf(KeyboardLayout.Latin, KeyboardLayout.Russian, KeyboardLayout.Ukrainian)

private fun KeyboardLayout.keys(): String = when (this) {
    KeyboardLayout.Latin -> LatinKeys
    KeyboardLayout.Russian -> RussianKeys
    KeyboardLayout.Ukrainian -> UkrainianKeys
}

private fun KeyboardLayout.label(): Int = when (this) {
    KeyboardLayout.Latin -> R.string.search_layout_latin
    KeyboardLayout.Russian -> R.string.search_layout_russian
    KeyboardLayout.Ukrainian -> R.string.search_layout_ukrainian
}

/** Query field (always shows the caret: typing happens on the keyboard below, not in a text field). */
@Composable
fun QueryField(query: String, modifier: Modifier = Modifier) {
    val colors = TwiTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.KeySpacing),
        modifier = modifier
            .fillMaxWidth()
            .height(TwiDimens.QueryFieldHeight)
            .background(colors.surface, TwiShapes.QueryField)
            .border(TwiDimens.QueryFieldRing, colors.primary, TwiShapes.QueryField)
            .padding(horizontal = TwiDimens.QueryFieldPadding),
    ) {
        Icon(TwiIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(TwiDimens.QueryFieldIconSize))
        Text(
            text = query,
            style = TwiTheme.typography.query,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(TwiDimens.QueryCaretWidth).height(TwiDimens.QueryCaretHeight).background(colors.primary))
    }
}

/**
 * 7-column letter keys plus Пробел / Стереть / layout / voice. The first key takes focus when search opens.
 * The layout key names the current layout; OK on it swaps the bottom row for the layouts (LAT / РУС / УКР).
 */
@Composable
fun SearchKeyboard(
    layout: KeyboardLayout,
    layoutMenuOpen: Boolean,
    voiceAvailable: Boolean,
    onKey: (Char) -> Unit,
    onSpace: () -> Unit,
    onErase: () -> Unit,
    onOpenLayoutMenu: () -> Unit,
    onCloseLayoutMenu: () -> Unit,
    onLayoutSelected: (KeyboardLayout) -> Unit,
    onVoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keys = layout.keys()
    val layoutKeyRequester = remember { FocusRequester() }
    // The menu row leaves composition with the focus in it: hand the focus back to the layout key
    // (only after the menu was open, not when search first opens).
    val menuWasOpen = remember { mutableStateOf(false) }
    LaunchedEffect(layoutMenuOpen) {
        if (!layoutMenuOpen && menuWasOpen.value) layoutKeyRequester.requestFocus()
        menuWasOpen.value = layoutMenuOpen
    }
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.KeySpacing), modifier = modifier) {
        keys.chunked(KeyboardColumns).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TwiDimens.KeySpacing)) {
                row.forEachIndexed { index, character ->
                    LetterKey(
                        character = character,
                        onClick = { onKey(character) },
                        modifier = if (rowIndex == 0 && index == 0) Modifier.requestInitialFocus() else Modifier,
                    )
                }
                // Keep the last, shorter row aligned to the grid.
                repeat(KeyboardColumns - row.length) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (layoutMenuOpen) {
            LayoutMenu(layout, onLayoutSelected, onCloseLayoutMenu)
            return@Column
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.KeySpacing),
            modifier = Modifier.padding(top = TwiDimens.KeySpacing),
        ) {
            ActionKey(onClick = onSpace, width = null) { color ->
                Text(stringResource(R.string.search_space), style = TwiTheme.typography.actionKey, color = color)
            }
            ActionKey(onClick = onErase, width = TwiDimens.EraseKeyWidth) { color ->
                Icon(TwiIcons.Erase, stringResource(R.string.search_erase), tint = color, modifier = Modifier.size(TwiDimens.ActionKeyIconSize))
            }
            ActionKey(
                onClick = onOpenLayoutMenu,
                width = TwiDimens.LayoutKeyWidth,
                modifier = Modifier.focusRequester(layoutKeyRequester),
            ) { color ->
                Text(stringResource(layout.label()), style = TwiTheme.typography.actionKey, color = color)
            }
            if (voiceAvailable) {
                ActionKey(onClick = onVoice, width = TwiDimens.EraseKeyWidth, accent = true) { color ->
                    Icon(TwiIcons.Microphone, stringResource(R.string.search_voice), tint = color, modifier = Modifier.size(TwiDimens.ActionKeyIconSize))
                }
            }
        }
    }
}

/** The layouts in place of the bottom row; the current one is filled and takes the focus. Back closes it. */
@Composable
private fun LayoutMenu(current: KeyboardLayout, onLayoutSelected: (KeyboardLayout) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Row(
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.KeySpacing),
        modifier = Modifier.padding(top = TwiDimens.KeySpacing),
    ) {
        Layouts.forEach { layout ->
            val isCurrent = layout == current
            ActionKey(
                onClick = { onLayoutSelected(layout) },
                width = null,
                accent = isCurrent,
                modifier = if (isCurrent) Modifier.requestInitialFocus() else Modifier,
            ) { color ->
                Text(stringResource(layout.label()), style = TwiTheme.typography.actionKey, color = color)
            }
        }
    }
}

@Composable
private fun RowScope.LetterKey(character: Char, onClick: () -> Unit, modifier: Modifier) {
    KeySurface(onClick = onClick, accent = false, modifier = modifier.weight(1f).height(TwiDimens.KeyHeight)) { color ->
        Text(character.toString(), style = TwiTheme.typography.key, color = color)
    }
}

/** [width] null stretches the key over the free space (Пробел). */
@Composable
private fun RowScope.ActionKey(
    onClick: () -> Unit,
    width: Dp?,
    accent: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable (Color) -> Unit,
) {
    val size = if (width == null) Modifier.weight(1f) else Modifier.width(width)
    KeySurface(onClick = onClick, accent = accent, modifier = modifier.then(size).height(TwiDimens.ActionKeyHeight), content = content)
}

/** Key look: outlined surface at rest, primary when focused (accent keys are always primary). */
@Composable
private fun KeySurface(onClick: () -> Unit, accent: Boolean, modifier: Modifier, content: @Composable (Color) -> Unit) {
    val colors = TwiTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val filled = focused || accent
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(if (filled) colors.primary else colors.surface, TwiShapes.Key)
            .border(TwiDimens.OutlineWidth, if (filled) Color.Transparent else colors.outline, TwiShapes.Key)
            .then(if (focused && accent) Modifier.border(TwiDimens.FocusBorderWidth, colors.primaryStrong, TwiShapes.Key) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        content(if (filled) colors.onPrimary else colors.onSurface)
    }
}
