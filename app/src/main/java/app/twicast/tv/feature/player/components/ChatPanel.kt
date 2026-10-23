package app.twicast.tv.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.repository.ChatMessage
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.absoluteValue

/** Names without a Twitch color get one of the mockup's, stable per name. */
private val FallbackNameColors = listOf(Color(0xFF9146FF), Color(0xFF1D9E75), Color(0xFFD85A30), Color(0xFF378ADD))

/** Light Twitch colors (yellow, cyan) are unreadable on the white panel: darken them. */
private const val MaxNameLuminance = 0.5f

/** Dark ones (navy, deep red) are unreadable on the dark panel: lighten them. */
private const val MinNameLuminanceOnDark = 0.15f
private const val AdjustFraction = 0.45f

/**
 * Read-only chat next to the video (mockup "Плеер · чат"): colored name, then the text; newest at the bottom.
 * [messages] come newest first.
 */
@Composable
fun ChatPanel(messages: ImmutableList<ChatMessage>, modifier: Modifier = Modifier) {
    val colors = TwiTheme.colors
    Column(
        modifier = modifier
            .background(colors.surface)
            .padding(horizontal = TwiDimens.PlayerChatHorizontalPadding, vertical = TwiDimens.PlayerChatVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(TwiDimens.PlayerChatSpacing),
    ) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.player_chat_title), style = TwiTheme.typography.listTitle, color = colors.onSurface, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.player_chat_read_only), style = TwiTheme.typography.caption, color = colors.onSurfaceVariant)
        }
        if (messages.isEmpty()) {
            Text(stringResource(R.string.player_chat_connecting), style = TwiTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        // Bottom-anchored like any chat: the newest line sits at the bottom and pushes the rest up.
        LazyColumn(
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(TwiDimens.PlayerChatSpacing),
            contentPadding = PaddingValues(bottom = TwiDimens.PlayerChatSpacing),
            modifier = Modifier.weight(1f),
        ) {
            items(messages, key = { it.id }) { message -> ChatLine(message) }
        }
    }
}

@Composable
private fun ChatLine(message: ChatMessage) {
    val colors = TwiTheme.colors
    val nameColor = nameColor(message, onDarkPanel = colors.isDark)
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.ExtraBold)) { append(message.author) }
            append(' ')
            append(message.text)
        },
        style = TwiTheme.typography.bodySmall,
        color = colors.onSurfaceSecondary,
    )
}

private fun nameColor(message: ChatMessage, onDarkPanel: Boolean): Color {
    val color = message.color?.let { Color(it) }
        ?: FallbackNameColors[message.author.hashCode().absoluteValue % FallbackNameColors.size]
    return readableNameColor(color, onDarkPanel)
}

/** Keeps the author's color recognizable but readable on the panel. */
internal fun readableNameColor(color: Color, onDarkPanel: Boolean): Color = when {
    onDarkPanel && color.luminance() < MinNameLuminanceOnDark -> lerp(color, Color.White, AdjustFraction)
    !onDarkPanel && color.luminance() > MaxNameLuminance -> lerp(color, Color.Black, AdjustFraction)
    else -> color
}
