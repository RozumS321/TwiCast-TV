package app.twicast.tv.feature.categories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twicast.tv.data.model.Category

@Composable
fun CategoriesRoute(onCategoryClick: (Category) -> Unit, viewModel: CategoriesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoriesScreen(
        state = state,
        onFilterSelected = viewModel::onFilterSelected,
        onCategoryClick = onCategoryClick,
        onLoadMore = viewModel::onLoadMore,
        onRetry = viewModel::onRetry,
    )
}
