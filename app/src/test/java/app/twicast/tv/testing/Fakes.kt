package app.twicast.tv.testing

import app.twicast.tv.data.repository.WatchProgress
import app.twicast.tv.data.repository.WatchProgressRepository
import app.twicast.tv.data.repository.WatchProgressRules
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.model.Category
import app.twicast.tv.data.model.Page
import app.twicast.tv.data.model.Stream
import app.twicast.tv.data.model.User
import app.twicast.tv.data.repository.AuthRepository
import app.twicast.tv.data.repository.CategoriesRepository
import app.twicast.tv.data.repository.DeviceAuthorization
import app.twicast.tv.data.repository.DevicePollResult
import app.twicast.tv.data.repository.StreamsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Replaces Dispatchers.Main (viewModelScope) with a test dispatcher. */
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

fun stream(id: String, language: String = "ru", categoryId: String = "game", viewers: Int = 1000) = Stream(
    id = id,
    userId = "user-$id",
    userLogin = "login_$id",
    channelName = "Channel $id",
    title = "Title $id",
    categoryId = categoryId,
    categoryName = "Game $categoryId",
    viewerCount = viewers,
    startedAtMillis = 0L,
    language = language,
    thumbnailTemplate = "https://example.test/{width}x{height}.jpg",
    avatarUrl = null,
    tags = emptyList(),
)

val testUser = User(id = "42", login = "nightowl", displayName = "nightowl", avatarUrl = null)

fun category(id: String) = Category(id, "Category $id", "https://example.test/{width}x{height}.jpg")

/** Top streams answer by language (null = worldwide). */
class FakeStreamsRepository(
    var results: Map<String?, AppResult<List<Stream>>> = emptyMap(),
    var followed: AppResult<List<Stream>> = AppResult.Success(emptyList()),
    var inCategories: AppResult<List<Stream>> = AppResult.Success(emptyList()),
) : StreamsRepository {
    var requestedCategoryIds: List<String> = emptyList()
        private set
    var live: AppResult<Boolean> = AppResult.Success(true)

    override suspend fun isLive(login: String): AppResult<Boolean> = live
    var requestedLanguages: List<String?> = emptyList()
        private set

    override suspend fun getTopStreams(language: String?, limit: Int): AppResult<List<Stream>> {
        requestedLanguages = requestedLanguages + language
        return results[language] ?: AppResult.Success(emptyList())
    }

    override suspend fun getFollowedStreams(userId: String, limit: Int): AppResult<List<Stream>> = followed

    /** Category pages by cursor (null = first page). */
    var categoryPages: Map<String?, AppResult<Page<Stream>>> = emptyMap()
    var categoryRequests: List<Pair<String?, String?>> = emptyList()
        private set

    override suspend fun getCategoryStreams(categoryId: String, language: String?, limit: Int, cursor: String?): AppResult<Page<Stream>> {
        categoryRequests = categoryRequests + (language to cursor)
        return categoryPages[cursor] ?: AppResult.Success(Page(emptyList(), null))
    }

    override suspend fun getTopStreamsInCategories(categoryIds: List<String>, language: String?, limit: Int): AppResult<List<Stream>> {
        requestedCategoryIds = categoryIds
        requestedLanguages = requestedLanguages + language
        return inCategories
    }
}

/** Pages by cursor (null = first page). */
class FakeCategoriesRepository(
    var pages: Map<String?, AppResult<Page<Category>>> = emptyMap(),
) : CategoriesRepository {
    override suspend fun getTopCategories(limit: Int, cursor: String?): AppResult<Page<Category>> =
        pages[cursor] ?: AppResult.Success(Page(emptyList(), null))
}

class FakeAuthRepository(
    initialState: AuthState = AuthState.SignedOut,
    var startResult: AppResult<DeviceAuthorization> = AppResult.Failure(AppError.Unknown),
    /** Poll answers in order; the last one repeats. */
    var pollResults: List<DevicePollResult> = listOf(DevicePollResult.Pending),
) : AuthRepository {
    private val state = MutableStateFlow(initialState)
    override val authState: StateFlow<AuthState> = state
    var pollCount = 0
        private set

    override suspend fun validateSession() = Unit

    override suspend fun startDeviceAuthorization(): AppResult<DeviceAuthorization> = startResult

    override suspend fun pollDeviceAuthorization(deviceCode: String): DevicePollResult =
        pollResults[minOf(pollCount++, pollResults.lastIndex)]

    override suspend fun signOut() {
        state.value = AuthState.SignedOut
    }
}

/** Positions in recordings, in memory; [saved] records every write. */
class FakeWatchProgressRepository(initial: Map<String, WatchProgress> = emptyMap()) : WatchProgressRepository {
    override val progress = MutableStateFlow(initial)
    val saved = mutableListOf<Triple<String, Long, Long>>()

    override suspend fun get(videoId: String): WatchProgress? = progress.value[videoId]

    override suspend fun save(videoId: String, positionMs: Long, durationMs: Long) {
        saved += Triple(videoId, positionMs, durationMs)
        progress.value = WatchProgressRules.updated(progress.value, videoId, WatchProgress(positionMs, durationMs, saved.size.toLong()))
    }
}
