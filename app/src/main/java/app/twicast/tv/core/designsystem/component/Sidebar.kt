package app.twicast.tv.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiMotion
import app.twicast.tv.core.designsystem.theme.TwiTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Immutable
data class SidebarItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

/** Expansion progress of the sidebar. Values are read only in draw-phase lambdas. */
@Stable
class SidebarState internal constructor(private val scope: CoroutineScope) {
    private val expansion = Animatable(0f)
    private val labels = Animatable(0f)

    val expandProgress: Float get() = expansion.value
    val labelAlpha: Float get() = labels.value

    internal fun onFocusChanged(hasFocus: Boolean) {
        val target = if (hasFocus) 1f else 0f
        scope.launch { expansion.animateTo(target, TwiMotion.sidebar()) }
        scope.launch { labels.animateTo(target, if (hasFocus) TwiMotion.sidebarLabelIn() else TwiMotion.sidebarLabelOut()) }
    }
}

@Composable
fun rememberSidebarState(): SidebarState {
    val scope = rememberCoroutineScope()
    return remember(scope) { SidebarState(scope) }
}

/**
 * Screen shell: content offset by the collapsed rail (88), the sidebar drawn on top.
 * The sidebar expands to 264 over the content with a scrim — content width never changes, so no relayout.
 */
@Composable
fun SidebarLayout(
    items: ImmutableList<SidebarItem>,
    bottomItem: SidebarItem,
    selectedKey: String,
    onItemClick: (SidebarItem) -> Unit,
    modifier: Modifier = Modifier,
    showSidebar: Boolean = true,
    content: @Composable () -> Unit,
) {
    val state = rememberSidebarState()
    val scrimColor = TwiTheme.colors.scrim
    Box(modifier.fillMaxSize()) {
        // Full-screen screens (player) hide the sidebar; content then takes the whole width.
        val contentStart = if (showSidebar) TwiDimens.SidebarCollapsedWidth else 0.dp
        Box(Modifier.fillMaxSize().padding(start = contentStart)) { content() }
        if (!showSidebar) return@Box
        Box(
            Modifier.fillMaxSize().drawBehind {
                val fraction = state.expandProgress
                if (fraction > 0f) drawRect(scrimColor, alpha = fraction)
            },
        )
        Sidebar(items, bottomItem, selectedKey, onItemClick, state)
    }
}

@Composable
private fun Sidebar(
    items: ImmutableList<SidebarItem>,
    bottomItem: SidebarItem,
    selectedKey: String,
    onItemClick: (SidebarItem) -> Unit,
    state: SidebarState,
) {
    val selectedRequester = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .width(TwiDimens.SidebarExpandedWidth)
            .fillMaxHeight()
            .onFocusChanged { state.onFocusChanged(it.hasFocus) }
            // Entering from the content always lands on the current section, not the geometrically nearest item.
            .focusProperties { onEnter = { selectedRequester.requestFocus() } }
            .focusGroup(),
    ) {
        SidebarPanelBackground(state)
        Column(
            verticalArrangement = Arrangement.spacedBy(TwiDimens.SidebarItemSpacing),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = TwiDimens.SidebarHorizontalPadding,
                    end = TwiDimens.SidebarHorizontalPadding,
                    top = TwiDimens.SidebarTopPadding,
                    bottom = TwiDimens.SidebarBottomPadding,
                ),
        ) {
            SidebarLogo(state, Modifier.padding(bottom = TwiDimens.SidebarLogoBottomSpacing))
            items.forEach { item ->
                SidebarItemRow(item, item.key == selectedKey, state, selectedRequester, onItemClick)
            }
            Spacer(Modifier.weight(1f))
            SidebarItemRow(bottomItem, bottomItem.key == selectedKey, state, selectedRequester, onItemClick)
        }
    }
}

/** Panel surface: always 264 wide, slid left by translationX so only the 88 rail shows when collapsed. */
@Composable
private fun SidebarPanelBackground(state: SidebarState) {
    val colors = TwiTheme.colors
    val shadowColor = colors.onSurface
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val hiddenWidth = (TwiDimens.SidebarExpandedWidth - TwiDimens.SidebarCollapsedWidth).toPx()
                translationX = -hiddenWidth * (1f - state.expandProgress)
            }
            .drawBehind {
                drawRect(colors.surface)
                val lineWidth = TwiDimens.OutlineWidth.toPx()
                drawRect(colors.outlineVariant, Offset(size.width - lineWidth, 0f), Size(lineWidth, size.height))
                val fraction = state.expandProgress
                if (fraction > 0f) {
                    // Cheap edge shadow: a short gradient instead of a blurred shadow.
                    val shadowWidth = TwiDimens.SidebarEdgeShadowWidth.toPx()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(shadowColor.copy(alpha = EdgeShadowAlpha), Color.Transparent),
                            startX = size.width,
                            endX = size.width + shadowWidth,
                        ),
                        topLeft = Offset(size.width, 0f),
                        size = Size(shadowWidth, size.height),
                        alpha = fraction,
                    )
                }
            },
    )
}

private const val EdgeShadowAlpha = 0.12f

private val NavigationItemShape = RoundedCornerShape(TwiDimens.NavigationItemRadius)

/** Brand mark "Две сферы" in the icon column; the wordmark Twi·Cast fades in with the item labels. Not focusable. */
@Composable
private fun SidebarLogo(state: SidebarState, modifier: Modifier = Modifier) {
    val colors = TwiTheme.colors
    val wordStart = stringResource(R.string.brand_word_start)
    val wordEnd = stringResource(R.string.brand_word_end)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.NavigationLabelGap),
        modifier = modifier.height(TwiDimens.SidebarItemHeight),
    ) {
        Box(Modifier.size(TwiDimens.SidebarItemHeight), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.ic_logo_mark),
                contentDescription = null,
                modifier = Modifier.size(TwiDimens.LogoMarkSize),
            )
        }
        Text(
            text = buildAnnotatedString {
                append(wordStart)
                withStyle(SpanStyle(color = colors.primary)) { append(wordEnd) }
            },
            style = TwiTheme.typography.logo,
            color = colors.onSurface,
            maxLines = 1,
            modifier = Modifier.graphicsLayer { alpha = state.labelAlpha },
        )
    }
}

@Composable
private fun SidebarItemRow(
    item: SidebarItem,
    isSelected: Boolean,
    state: SidebarState,
    selectedRequester: FocusRequester,
    onItemClick: (SidebarItem) -> Unit,
) {
    val colors = TwiTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val pillColor = when {
        isFocused -> colors.primary
        isSelected -> colors.primaryContainer
        else -> Color.Transparent
    }
    val iconColor = when {
        isFocused -> colors.onPrimary
        isSelected -> colors.primary
        else -> colors.onSurfaceVariant
    }
    val labelColor = when {
        isFocused -> colors.onPrimary
        isSelected -> colors.primaryStrong
        else -> colors.onSurface
    }
    val showIndicator = isSelected && !isFocused

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.NavigationLabelGap),
        modifier = Modifier
            .fillMaxWidth()
            .height(TwiDimens.SidebarItemHeight)
            .graphicsLayer {
                // Soft primary shadow under the focused pill only; it grows in with the expansion.
                shape = NavigationItemShape
                shadowElevation = if (isFocused) TwiDimens.ButtonShadowElevation.toPx() * state.expandProgress else 0f
                ambientShadowColor = colors.primary
                spotShadowColor = colors.primary
            }
            .drawBehind {
                // Pill grows from the icon square (52) to the full row width in the draw phase only.
                val collapsedWidth = TwiDimens.SidebarItemHeight.toPx()
                val pillWidth = collapsedWidth + (size.width - collapsedWidth) * state.expandProgress
                val radius = CornerRadius(TwiDimens.NavigationItemRadius.toPx())
                drawRoundRect(pillColor, size = Size(pillWidth, size.height), cornerRadius = radius)
                if (showIndicator) {
                    val inset = TwiDimens.SidebarIndicatorVerticalInset.toPx()
                    val barWidth = TwiDimens.SidebarIndicatorWidth.toPx()
                    drawRoundRect(
                        color = colors.primary,
                        topLeft = Offset(-TwiDimens.SidebarHorizontalPadding.toPx() + barWidth, inset),
                        size = Size(barWidth, size.height - inset * 2),
                        cornerRadius = CornerRadius(barWidth),
                    )
                }
            }
            .then(if (isSelected) Modifier.focusRequester(selectedRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null) { onItemClick(item) },
    ) {
        Box(Modifier.size(TwiDimens.SidebarItemHeight), contentAlignment = Alignment.Center) {
            Icon(item.icon, contentDescription = item.label, tint = iconColor, modifier = Modifier.size(TwiDimens.NavigationIconSize))
        }
        Text(
            text = item.label,
            style = if (isFocused || isSelected) TwiTheme.typography.navigationLabelActive else TwiTheme.typography.navigationLabel,
            color = labelColor,
            maxLines = 1,
            modifier = Modifier.graphicsLayer { alpha = state.labelAlpha },
        )
    }
}

