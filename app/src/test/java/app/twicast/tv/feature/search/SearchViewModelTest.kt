package app.twicast.tv.feature.search

import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppLanguage
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.ChannelSearchResult
import app.twicast.tv.data.repository.LanguageSettingsRepository
import app.twicast.tv.data.repository.SearchRepository
import app.twicast.tv.data.repository.SearchResults
import app.twicast.tv.testing.MainDispatcherRule
import app.twicast.tv.testing.FakeStreamsRepository
import app.twicast.tv.testing.stream
import app.twicast.tv.data.repository.SearchHistoryEntry
import app.twicast.tv.data.repository.SearchHistoryRepository
import app.twicast.tv.data.repository.SearchHistoryRules
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

private class FakeSearchHistoryRepository : SearchHistoryRepository {
    override val entries = MutableStateFlow(emptyList<SearchHistoryEntry>())
    override suspend fun record(entry: SearchHistoryEntry) {
        entries.value = SearchHistoryRules.updated(entries.value, entry)
    }
    override suspend fun clear() {
        entries.value = emptyList()
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
    private val history = FakeSearchHistoryRepository()
    private val streams = FakeStreamsRepository()

    @Test
    fun `typing searches once after a pause`() = runTest(mainRule.dispatcher) {
        repository.result = AppResult.Success(SearchResults(listOf(channel("kotofey")), emptyList()))
        val viewModel = SearchViewModel(repository, FixedLanguage(AppLanguage.English), history, streams).also { runCurrent() }

        "kot".forEach { viewModel.onKey(it); advanceTimeBy(SearchDebounceMillis / 2) }
        assertEquals("nothing sent while typing", emptyList<String>(), repository.queries)

        advanceTimeBy(SearchDebounceMillis + 1)
        assertEquals(listOf("kot"), repository.queries)
        assertEquals(listOf("kotofey"), (viewModel.uiState.value.results as SearchResultsState.Content).channels.map { it.login })
    }

    @Test
    fun `one character is not searched`() = runTest(mainRule.dispatcher) {
        val viewModel = SearchViewModel(repository, FixedLanguage(AppLanguage.English), history, streams).also { runCurrent() }
        viewModel.onKey('k')
        advanceTimeBy(SearchDebounceMillis + 1)
        assertEquals(emptyList<String>(), repository.queries)
        assertEquals(SearchResultsState.Idle, viewModel.uiState.value.results)
    }

    @Test
    fun `nothing found and errors are reported`() = runTest(mainRule.dispatcher) {
        val viewModel = SearchViewModel(repository, FixedLanguage(AppLanguage.English), history, streams).also { runCurrent() }
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
        val viewModel = SearchViewModel(repository, FixedLanguage(AppLanguage.Russian), history, streams)

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
        val layouts = AppLanguage.entries.associateWith { SearchViewModel(repository, FixedLanguage(it), history, streams).uiState.value.layout }

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
        val viewModel = SearchViewModel(repository, FixedLanguage(AppLanguage.Russian), history, streams)

        viewModel.onOpenLayoutMenu()
        assertEquals(true, viewModel.uiState.value.layoutMenuOpen)
        viewModel.onLayoutSelected(KeyboardLayout.Ukrainian)

        assertEquals(KeyboardLayout.Ukrainian, viewModel.uiState.value.layout)
        assertEquals(false, viewModel.uiState.value.layoutMenuOpen)
    }

    private fun TestScope.newViewModel() =
        SearchViewModel(repository, FixedLanguage(AppLanguage.English), history, streams).also { runCurrent() }

    @Test
    fun `an opened channel and a query that led to a category are remembered, newest first`() = runTest(mainRule.dispatcher) {
        val viewModel = newViewModel()
        "cs2".forEach(viewModel::onKey)
        viewModel.onCategoryOpened()
        viewModel.onChannelOpened(channel("kotofey"))
        runCurrent()
        assertEquals(listOf("channel:kotofey", "query:cs2"), viewModel.uiState.value.history.map { it.entry.key })
    }

    @Test
    fun `remembered channels show LIVE when they broadcast now`() = runTest(mainRule.dispatcher) {
        streams.liveStreams = AppResult.Success(listOf(stream("a")))
        history.entries.value = listOf(SearchHistoryEntry("Channel a", "login_a"), SearchHistoryEntry("Off", "off"))
        val history = newViewModel().uiState.value.history
        assertEquals(listOf(true, false), history.map { it.live })
    }

    @Test
    fun `a remembered query is typed again, and clearing empties the list`() = runTest(mainRule.dispatcher) {
        history.entries.value = listOf(SearchHistoryEntry("dota 2"))
        val viewModel = newViewModel()
        viewModel.onHistoryQuery(viewModel.uiState.value.history.first().entry)
        assertEquals("dota 2", viewModel.uiState.value.query)

        viewModel.onClearHistory()
        runCurrent()
        assertTrue(viewModel.uiState.value.history.isEmpty())
    }

    @Test
    fun `only five are shown and a short query is not remembered`() = runTest(mainRule.dispatcher) {
        history.entries.value = (1..8).map { SearchHistoryEntry("q$it") }
        val viewModel = newViewModel()
        assertEquals(SearchHistoryRules.Shown, viewModel.uiState.value.history.size)

        viewModel.onKey('a')
        viewModel.onCategoryOpened()
        runCurrent()
        assertEquals("q1", history.entries.value.first().text)
    }
}
