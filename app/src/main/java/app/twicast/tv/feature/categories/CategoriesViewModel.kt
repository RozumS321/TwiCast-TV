package app.twicast.tv.feature.categories

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.Category
import app.twicast.tv.data.repository.CategoriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Helix allows up to 100 per page; one page covers almost everything people browse. */
private const val PageSize = 100

/** [rank] is the place in the overall top, kept when a filter hides other categories. */
@Immutable
data class RankedCategory(val rank: Int, val category: Category)

sealed interface CategoriesUiState {
    data object Loading : CategoriesUiState

    data class Error(val error: AppError) : CategoriesUiState

    @Immutable
    data class Content(
        val filter: CategoryFilter,
        val categories: ImmutableList<RankedCategory>,
        val isLoadingMore: Boolean,
    ) : CategoriesUiState
}

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: CategoriesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    private var loaded: List<Category> = emptyList()
    private var nextCursor: String? = null
    private var filter = CategoryFilter.All
    private var isLoadingMore = false

    init {
        loadFirstPage()
    }

    fun onRetry() = loadFirstPage()

    fun onFilterSelected(selected: CategoryFilter) {
        filter = selected
        publish()
    }

    /** Called when the focused card nears the end of the grid. */
    fun onLoadMore() {
        val cursor = nextCursor ?: return
        if (isLoadingMore) return
        isLoadingMore = true
        publish()
        viewModelScope.launch {
            when (val result = repository.getTopCategories(PageSize, cursor)) {
                is AppResult.Success -> {
                    // Pages can overlap when the ranking shifts between requests.
                    loaded = (loaded + result.value.items).distinctBy { it.id }
                    nextCursor = result.value.nextCursor
                }
                // A failed extra page keeps what is shown; the next scroll retries.
                is AppResult.Failure -> Unit
            }
            isLoadingMore = false
            publish()
        }
    }

    private fun loadFirstPage() {
        _uiState.value = CategoriesUiState.Loading
        viewModelScope.launch {
            when (val result = repository.getTopCategories(PageSize)) {
                is AppResult.Failure -> _uiState.value = CategoriesUiState.Error(result.error)
                is AppResult.Success -> {
                    loaded = result.value.items
                    nextCursor = result.value.nextCursor
                    publish()
                }
            }
        }
    }

    private fun publish() {
        if (_uiState.value is CategoriesUiState.Error) return
        val ranked = loaded.mapIndexed { index, category -> RankedCategory(index + 1, category) }
        _uiState.value = CategoriesUiState.Content(
            filter = filter,
            categories = ranked.filter { filter.matches(it.category.id) }.toImmutableList(),
            isLoadingMore = isLoadingMore,
        )
    }
}
