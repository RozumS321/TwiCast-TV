package app.twicast.tv.feature.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.Text
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme

private const val Separator = '-'

/** User code as cells, "WDJB-MJHT". [code] null draws empty cells (loading). */
@Composable
fun CodeCells(code: String?, cellCount: Int, modifier: Modifier = Modifier) {
    val characters = code?.toList() ?: List(cellCount) { ' ' }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.CodeCellSpacing),
        modifier = modifier,
    ) {
        characters.forEach { character ->
            if (character == Separator) {
                Text(
                    text = Separator.toString(),
                    style = TwiTheme.typography.codeSeparator,
                    color = TwiTheme.colors.onSurfaceFaint,
                    modifier = Modifier.padding(horizontal = TwiDimens.CodeSeparatorPadding),
                )
            } else {
                CodeCell(character)
            }
        }
    }
}

@Composable
private fun CodeCell(character: Char) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(TwiDimens.CodeCellWidth, TwiDimens.CodeCellHeight)
            .background(TwiTheme.colors.background, TwiShapes.CodeCell)
            .border(TwiDimens.OutlineWidth, TwiTheme.colors.outline, TwiShapes.CodeCell),
    ) {
        Text(character.toString(), style = TwiTheme.typography.codeCharacter, color = TwiTheme.colors.onSurface)
    }
}
