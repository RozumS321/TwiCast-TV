package app.twicast.tv.feature.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppLanguage
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.Category
import app.twicast.tv.data.model.ChannelSearchResult
import app.twicast.tv.data.repository.LanguageSettingsRepository
import app.twicast.tv.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Wait for a pause in typing before calling Helix (rate limit, and the remote types slowly anyway). */
const val SearchDebounceMillis = 350L
private const val MinQueryLength = 2
private const val ChannelLimit = 8
private const val CategoryLimit = 6

enum class KeyboardLayout { Latin, Russian, Ukrainian }

sealed interface SearchResultsState {
    /** Query too short: show the hint. */
    data object Idle : SearchResultsState
    data object Loading : SearchResultsState
    data object Empty : SearchResultsState
    data class Error(val error: AppError) : SearchResultsState

    @Immutable
    data class Content(
        val channels: ImmutableList<ChannelSearchResult>,
        val categories: ImmutableList<Category>,
    ) : SearchResultsState
}

@Immutable
data class SearchUiState(
    val query: String = "",
    val layout: KeyboardLayout = KeyboardLayout.Latin,
    /** The layout key was pressed: the bottom row offers the layouts instead of Пробел / Стереть. */
    val layoutMenuOpen: Boolean = false,
    val results: SearchResultsState = SearchResultsState.Idle,
)

/** Mockup "Поиск": on-screen keyboard on the left, channels and categories on the right, live as you type. */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
    languages: LanguageSettingsRepository,
) : ViewModel() {

    // The keyboard starts in the interface language.
    private val _uiState = MutableStateFlow(
        SearchUiState(
            layout = when (languages.language()) {
                AppLanguage.Russian -> KeyboardLayout.Russian
                AppLanguage.Ukrainian -> KeyboardLayout.Ukrainian
                AppLanguage.English -> KeyboardLayout.Latin
            },
        ),
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.map { it.query.trim() }
                .distinctUntilChanged()
                .debounce(SearchDebounceMillis)
                // A newer query cancels the request still in flight.
                .collectLatest(::search)
        }
    }

    fun onKey(character: Char) = editQuery { it + character }

    fun onSpace() = editQuery { if (it.isEmpty() || it.endsWith(' ')) it else "$it " }

    fun onErase() = editQuery { it.dropLast(1) }

    fun onOpenLayoutMenu() = _uiState.update { it.copy(layoutMenuOpen = true) }

    fun onCloseLayoutMenu() = _uiState.update { it.copy(layoutMenuOpen = false) }

    fun onLayoutSelected(layout: KeyboardLayout) = _uiState.update { it.copy(layout = layout, layoutMenuOpen = false) }

    /** Text recognized by the system voice input replaces the query. */
    fun onVoiceResult(text: String) = editQuery { text.trim() }

    fun onRetry() {
        val query = _uiState.value.query.trim()
        viewModelScope.launch { search(query) }
    }

    private fun editQuery(edit: (String) -> String) = _uiState.update { it.copy(query = edit(it.query)) }

    private suspend fun search(query: String) {
        if (query.length < MinQueryLength) {
            setResults(SearchResultsState.Idle)
            return
        }
        setResults(SearchResultsState.Loading)
        val results = when (val result = repository.search(query, ChannelLimit, CategoryLimit)) {
            is AppResult.Failure -> SearchResultsState.Error(result.error)
            is AppResult.Success -> {
                val found = result.value
                if (found.channels.isEmpty() && found.categories.isEmpty()) {
                    SearchResultsState.Empty
                } else {
                    SearchResultsState.Content(found.channels.toImmutableList(), found.categories.toImmutableList())
                }
            }
        }
        setResults(results)
    }

    private fun setResults(results: SearchResultsState) = _uiState.update { it.copy(results = results) }
}
