package app.twicast.tv.feature.search

import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppLanguage
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.ChannelSearchResult
import app.twicast.tv.data.repository.LanguageSettingsRepository
import app.twicast.tv.data.repository.SearchRepository
import app.twicast.tv.data.repository.SearchResults
import app.twicast.tv.testing.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private class FakeSearchRepository : SearchRepository {
    val queries = mutableListOf<String>()
    var result: AppResult<SearchResults> = AppResult.Success(SearchResults(emptyList(), emptyList()))

    override suspend fun search(query: String, channelLimit: Int, categoryLimit: Int): AppResult<SearchResults> {
        queries += query
        return result
    }
}

private class FixedLanguage(private val language: AppLanguage) : LanguageSettingsRepository {
    override fun language() = language
    override fun setLanguage(language: AppLanguage) = Unit
}

private fun channel(login: String) = ChannelSearchResult(login, login, login, null, isLive = true, categoryName = "Game", title = "")

class SearchViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val repository = FakeSearchRepository()

    @Test
    fun `typing searches once after a pause`() = runTest(mainRule.dispatcher) {
        repository.result = AppResult.Success(SearchResults(listOf(channel("kotofey")), emptyList()))
        val viewModel = SearchViewModel(repository, FixedLanguage(AppLanguage.English)).also { runCurrent() }

        "kot".forEach { viewModel.onKey(it); advanceTimeBy(SearchDebounceMillis / 2) }
        assertEquals("nothing sent while typing", emptyList<String>(), repository.queries)

        advanceTimeBy(SearchDebounceMillis + 1)
        assertEquals(listOf("kot"), repository.queries)
        assertEquals(listOf("kotofey"), (viewModel.uiState.value.results as SearchResultsState.Content).channels.map { it.login })
    }

    @Test
    fun `one character is not searched`() = runTest(mainRule.dispatcher) {
        val viewModel = SearchViewModel(repository, FixedLanguage(AppLanguage.English)).also { runCurrent() }
        viewModel.onKey('k')
        advanceTimeBy(SearchDebounceMillis + 1)
        assertEquals(emptyList<String>(), repository.queries)
        assertEquals(SearchResultsState.Idle, viewModel.uiState.value.results)
    }

    @Test
    fun `nothing found and errors are reported`() = runTest(mainRule.dispatcher) {
        val viewModel = SearchViewModel(repository, FixedLanguage(AppLanguage.English)).also { runCurrent() }
        viewModel.onVoiceResult("  zzz ")
        advanceTimeBy(SearchDebounceMillis + 1)
        assertEquals(listOf("zzz"), repository.queries)
        assertEquals(SearchResultsState.Empty, viewModel.uiState.value.results)

        repository.result = AppResult.Failure(AppError.Network)
        viewModel.onRetry()
        runCurrent()
        assertEquals(SearchResultsState.Error(AppError.Network), viewModel.uiState.value.results)
    }

    @Test
    fun `keyboard edits the query`() = runTest(mainRule.dispatcher) {
        val viewModel = SearchViewModel(repository, FixedLanguage(AppLanguage.Russian))

        viewModel.onSpace()
        viewModel.onKey('a')
        viewModel.onSpace()
        viewModel.onSpace()
        viewModel.onKey('b')
        viewModel.onErase()

        assertEquals("a ", viewModel.uiState.value.query)
    }

    @Test
    fun `keyboard starts in the interface language`() {
        val layouts = AppLanguage.entries.associateWith { SearchViewModel(repository, FixedLanguage(it)).uiState.value.layout }

        assertEquals(
            mapOf(
                AppLanguage.Russian to KeyboardLayout.Russian,
                AppLanguage.Ukrainian to KeyboardLayout.Ukrainian,
                AppLanguage.English to KeyboardLayout.Latin,
            ),
            layouts,
        )
    }

    @Test
    fun `layout menu picks a layout and closes`() = runTest(mainRule.dispatcher) {
        val viewModel = SearchViewModel(repository, FixedLanguage(AppLanguage.Russian))

        viewModel.onOpenLayoutMenu()
        assertEquals(true, viewModel.uiState.value.layoutMenuOpen)
        viewModel.onLayoutSelected(KeyboardLayout.Ukrainian)

        assertEquals(KeyboardLayout.Ukrainian, viewModel.uiState.value.layout)
        assertEquals(false, viewModel.uiState.value.layoutMenuOpen)
    }
}
