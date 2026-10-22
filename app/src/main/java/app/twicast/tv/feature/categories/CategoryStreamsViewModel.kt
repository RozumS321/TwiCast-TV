package app.twicast.tv.feature.categories

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.Stream
import app.twicast.tv.data.repository.StreamsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

/** The card's data travels with the route, so the header shows at once. */
@Serializable
data class CategoryStreamsDestination(
    val categoryId: String,
    val categoryName: String,
    val boxArtTemplate: String,
)

/** Language chips of the mockup; null = all languages. */
val CategoryLanguages: ImmutableList<String?> = persistentListOf(null, "ru", "uk", "en")

private const val PageSize = 40

sealed interface StreamsGridState {
    data object Loading : StreamsGridState
    data object Empty : StreamsGridState
    data class Error(val error: AppError) : StreamsGridState

    @Immutable
    data class Content(val streams: ImmutableList<Stream>, val isLoadingMore: Boolean) : StreamsGridState
}

@Immutable
data class CategoryStreamsUiState(
    val category: CategoryStreamsDestination,
    val language: String?,
    val grid: StreamsGridState = StreamsGridState.Loading,
)

@HiltViewModel
class CategoryStreamsViewModel internal constructor(
    destination: CategoryStreamsDestination,
    private val repository: StreamsRepository,
) : ViewModel() {

    // Hilt entry: the route comes from navigation. Tests use the primary constructor (no Android in JVM tests).
    @Inject
    constructor(savedStateHandle: SavedStateHandle, repository: StreamsRepository) :
        this(savedStateHandle.toRoute<CategoryStreamsDestination>(), repository)

    // Starts with all languages; the viewer narrows it with the chips.
    private val _uiState = MutableStateFlow(CategoryStreamsUiState(destination, language = null))
    val uiState: StateFlow<CategoryStreamsUiState> = _uiState.asStateFlow()

    private var loaded: List<Stream> = emptyList()
    private var nextCursor: String? = null
    private var loadJob: Job? = null

    init {
        loadFirstPage()
    }

    fun onLanguageSelected(language: String?) {
        if (language == _uiState.value.language) return
        _uiState.update { it.copy(language = language) }
        loadFirstPage()
    }

    fun onRetry() = loadFirstPage()

    fun onLoadMore() {
        val cursor = nextCursor ?: return
        val grid = _uiState.value.grid as? StreamsGridState.Content ?: return
        if (grid.isLoadingMore) return
        setGrid(grid.copy(isLoadingMore = true))
        loadJob = viewModelScope.launch {
            val state = _uiState.value
            val result = repository.getCategoryStreams(state.category.categoryId, state.language, PageSize, cursor)
            if (result is AppResult.Success) {
                // Viewer counts move between requests, so pages can overlap.
                loaded = (loaded + result.value.items).distinctBy { it.id }
                nextCursor = result.value.nextCursor
            }
            setGrid(StreamsGridState.Content(loaded.toImmutableList(), isLoadingMore = false))
        }
    }

    private fun loadFirstPage() {
        loadJob?.cancel()
        setGrid(StreamsGridState.Loading)
        loadJob = viewModelScope.launch {
            val state = _uiState.value
            when (val result = repository.getCategoryStreams(state.category.categoryId, state.language, PageSize)) {
                is AppResult.Failure -> setGrid(StreamsGridState.Error(result.error))
                is AppResult.Success -> {
                    loaded = result.value.items
                    nextCursor = result.value.nextCursor
                    setGrid(if (loaded.isEmpty()) StreamsGridState.Empty else StreamsGridState.Content(loaded.toImmutableList(), false))
                }
            }
        }
    }

    private fun setGrid(grid: StreamsGridState) = _uiState.update { it.copy(grid = grid) }
}
