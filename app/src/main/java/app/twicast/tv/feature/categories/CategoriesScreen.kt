package app.twicast.tv.feature.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.CategoryCard
import app.twicast.tv.core.designsystem.component.EmptyState
import app.twicast.tv.core.designsystem.component.ErrorState
import app.twicast.tv.core.designsystem.component.KeepFocusedVisibleScroll
import app.twicast.tv.core.designsystem.component.LoadMoreWhenNearEnd
import app.twicast.tv.core.designsystem.component.TwiChip
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.image.rememberTwitchImageUrl
import app.twicast.tv.core.designsystem.theme.TwiAspect
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.Category
import app.twicast.tv.feature.common.appErrorMessage
import app.twicast.tv.feature.common.GridSkeleton

private const val Columns = 6
/** Start loading the next page two rows before the end. */
private const val LoadMoreThreshold = Columns * 2

/** Mockup "Категории": title, type chips, 6-column grid of 3:4 covers with the overall rank. */
@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onFilterSelected: (CategoryFilter) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(top = TwiDimens.ScreenTopPadding)) {
        CategoriesHeader(
            selected = (state as? CategoriesUiState.Content)?.filter ?: CategoryFilter.All,
            onFilterSelected = onFilterSelected,
        )
        when (state) {
            CategoriesUiState.Loading -> GridSkeleton(columns = Columns, cover = true)
            is CategoriesUiState.Error -> ErrorState(appErrorMessage(state.error), onRetry)
            is CategoriesUiState.Content ->
                if (state.categories.isEmpty()) {
                    EmptyState(stringResource(R.string.categories_empty_filter))
                } else {
                    KeepFocusedVisibleScroll { CategoriesGrid(state, onCategoryClick, onLoadMore) }
                }
        }
    }
}

@Composable
private fun CategoriesHeader(selected: CategoryFilter, onFilterSelected: (CategoryFilter) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = TwiDimens.ContentStartPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.CardTextSpacing * 2)) {
            Text(stringResource(R.string.nav_categories), style = TwiTheme.typography.screenTitle, color = TwiTheme.colors.onSurface)
            Text(stringResource(R.string.categories_subtitle), style = TwiTheme.typography.bodySmall, color = TwiTheme.colors.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChipSpacing)) {
            CategoryFilter.entries.forEach { filter ->
                TwiChip(stringResource(filter.labelRes), selected = filter == selected, onClick = { onFilterSelected(filter) })
            }
        }
    }
}

@Composable
private fun CategoriesGrid(
    state: CategoriesUiState.Content,
    onCategoryClick: (Category) -> Unit,
    onLoadMore: () -> Unit,
) {
    // A new filter is a new list: start it from the top. With one state for all filters the grid kept its place by
    // item key, so going back to "Все" landed next to the category last seen in the filtered list.
    val gridState = rememberSaveable(state.filter, saver = LazyGridState.Saver) { LazyGridState() }
    gridState.LoadMoreWhenNearEnd(LoadMoreThreshold, onLoadMore)
    LazyVerticalGrid(
        columns = GridCells.Fixed(Columns),
        state = gridState,
        // The side inset is split between padding and content padding so a scaled card is not clipped.
        contentPadding = PaddingValues(
            start = TwiDimens.GridFocusInset,
            end = TwiDimens.GridFocusInset,
            top = TwiDimens.GridTopPadding,
            bottom = TwiDimens.FocusOverflowPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.CategoryGridHorizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(TwiDimens.GridVerticalSpacing),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = TwiDimens.ContentStartPadding - TwiDimens.GridFocusInset)
            .focusRestorer(),
    ) {
        itemsIndexed(state.categories, key = { _, item -> item.category.id }, contentType = { _, _ -> "category" }) { index, item ->
            CategoryCard(
                name = item.category.name,
                viewerCountText = null,
                coverUrl = rememberTwitchImageUrl(item.category.boxArtTemplate, TwiDimens.CategoryCardWidth, TwiAspect.Cover),
                onClick = { onCategoryClick(item.category) },
                rank = item.rank,
                modifier = if (index == 0) Modifier.requestInitialFocus() else Modifier,
            )
        }
    }
}
