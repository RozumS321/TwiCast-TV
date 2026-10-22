package app.twicast.tv.feature.categories

import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.Page
import app.twicast.tv.testing.FakeCategoriesRepository
import app.twicast.tv.testing.FakeStreamsRepository
import app.twicast.tv.testing.MainDispatcherRule
import app.twicast.tv.testing.category
import app.twicast.tv.testing.stream
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private const val JustChatting = "509658"
private const val Music = "26936"

class CategoriesViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val repository = FakeCategoriesRepository()

    @Test
    fun `first page is ranked in order`() = runTest(mainRule.dispatcher) {
        repository.pages = mapOf(null to AppResult.Success(Page(listOf(category("a"), category("b")), nextCursor = null)))
        val viewModel = CategoriesViewModel(repository).also { runCurrent() }

        val content = viewModel.uiState.value as CategoriesUiState.Content
        assertEquals(listOf(1 to "a", 2 to "b"), content.categories.map { it.rank to it.category.id })
    }

    @Test
    fun `a filter keeps the overall rank`() = runTest(mainRule.dispatcher) {
        repository.pages = mapOf(
            null to AppResult.Success(Page(listOf(category(JustChatting), category("game"), category(Music)), null)),
        )
        val viewModel = CategoriesViewModel(repository).also { runCurrent() }

        viewModel.onFilterSelected(CategoryFilter.Music)
        assertEquals(listOf(3 to Music), (viewModel.uiState.value as CategoriesUiState.Content).categories.map { it.rank to it.category.id })

        viewModel.onFilterSelected(CategoryFilter.Games)
        assertEquals(listOf("game"), (viewModel.uiState.value as CategoriesUiState.Content).categories.map { it.category.id })
    }

    @Test
    fun `load more appends the next page without duplicates`() = runTest(mainRule.dispatcher) {
        repository.pages = mapOf(
            null to AppResult.Success(Page(listOf(category("a"), category("b")), nextCursor = "c1")),
            "c1" to AppResult.Success(Page(listOf(category("b"), category("c")), nextCursor = null)),
        )
        val viewModel = CategoriesViewModel(repository).also { runCurrent() }

        viewModel.onLoadMore()
        runCurrent()
        viewModel.onLoadMore() // no cursor left: ignored

        val content = viewModel.uiState.value as CategoriesUiState.Content
        assertEquals(listOf("a", "b", "c"), content.categories.map { it.category.id })
    }

    @Test
    fun `failed first page is an error`() = runTest(mainRule.dispatcher) {
        repository.pages = mapOf(null to AppResult.Failure(AppError.Network))
        val viewModel = CategoriesViewModel(repository).also { runCurrent() }
        assertEquals(CategoriesUiState.Error(AppError.Network), viewModel.uiState.value)
    }
}

class CategoryStreamsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val streams = FakeStreamsRepository()
    private val destination = CategoryStreamsDestination("32399", "Counter-Strike", "https://example.test/{width}x{height}.jpg")

    @Test
    fun `starts with all languages`() = runTest(mainRule.dispatcher) {
        val viewModel = CategoryStreamsViewModel(destination, streams).also { runCurrent() }
        assertEquals(null, viewModel.uiState.value.language)
        assertEquals(listOf<Pair<String?, String?>>(null to null), streams.categoryRequests)
    }

    @Test
    fun `changing the language reloads from the first page`() = runTest(mainRule.dispatcher) {
        streams.categoryPages = mapOf(null to AppResult.Success(Page(listOf(stream("1")), nextCursor = "next")))
        val viewModel = CategoryStreamsViewModel(destination, streams).also { runCurrent() }

        viewModel.onLanguageSelected("en")
        runCurrent()

        assertEquals(listOf<Pair<String?, String?>>(null to null, "en" to null), streams.categoryRequests)
        assertEquals(listOf("1"), (viewModel.uiState.value.grid as StreamsGridState.Content).streams.map { it.id })
    }

    @Test
    fun `no streams is empty`() = runTest(mainRule.dispatcher) {
        val viewModel = CategoryStreamsViewModel(destination, streams).also { runCurrent() }
        assertEquals(StreamsGridState.Empty, viewModel.uiState.value.grid)
    }
}
