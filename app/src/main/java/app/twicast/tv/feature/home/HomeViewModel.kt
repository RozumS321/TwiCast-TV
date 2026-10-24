package app.twicast.tv.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.core.network.Clock
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.model.Stream
import app.twicast.tv.data.repository.AuthRepository
import app.twicast.tv.data.repository.HistoryEntry
import app.twicast.tv.data.repository.StreamsRepository
import app.twicast.tv.data.repository.WatchHistoryRepository
import app.twicast.tv.data.repository.WatchHistoryRules
import app.twicast.tv.data.repository.WatchProgress
import app.twicast.tv.data.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val FollowedLimit = 100
private const val RowSize = 20
/** Recommendations come from the categories of the most-watched followed streams. */
private const val RecommendationCategories = 5
/** Twitch's value for streams without a declared language. */
private const val UnknownLanguage = "other"

/** Coming back to home after this long reloads it (the streams cache keeps answers for a minute anyway). */
const val HomeStaleMillis = 60_000L

/** While home stays on screen it refreshes at this pace: viewer counts and live channels change. */
const val HomeAutoRefreshMillis = 3 * 60_000L

/**
 * Signed-in home: live followed channels, recommendations, popular in the viewer's language.
 * Twitch exposes neither the viewer's region nor recommendations, so (MVP):
 *  - the viewer's language is the most common one among live followed channels, else the device language;
 *  - recommendations are the top streams in that language in the followed streams' categories (ТЗ 2.5).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val streamsRepository: StreamsRepository,
    private val history: WatchHistoryRepository,
    private val watchProgress: WatchProgressRepository,
    locale: Locale,
    private val clock: Clock,
) : ViewModel() {

    private val deviceLanguage = locale.language
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var autoRefreshJob: Job? = null
    private var loadedAtMillis: Long? = null

    /** "Недавно смотрели" as last built; a reload of the other rows keeps it. */
    private var recent: List<RecentItem> = emptyList()
    private var historyEntries: List<HistoryEntry> = emptyList()
    private var positions: Map<String, WatchProgress> = emptyMap()
    private var recentJob: Job? = null

    init {
        load(quietly = false)
        // Back from the player: the row changes at once, without waiting for the next refresh.
        viewModelScope.launch {
            combine(history.entries, watchProgress.progress, ::Pair).collect { (entries, progress) ->
                historyEntries = entries
                positions = progress
                refreshRecent()
            }
        }
    }

    /** Rebuilds "Недавно смотрели": which watched channels broadcast now comes from one request. */
    private fun refreshRecent() {
        recentJob?.cancel()
        recentJob = viewModelScope.launch {
            val entries = historyEntries.take(WatchHistoryRules.RowSize)
            val logins = entries.filterNot { it.isRecording }.map { it.channelLogin }
            // Unknown (no network): channels show as off air rather than hiding the row.
            val live = (streamsRepository.getLiveStreams(logins) as? AppResult.Success)?.value.orEmpty()
                .associateBy { it.userLogin.lowercase() }
            recent = recentItems(entries, positions, live)
            _uiState.update { state -> if (state is HomeUiState.Content) state.copy(recent = recent.toImmutableList()) else state }
        }
    }

    fun onRetry() = load(quietly = false)

    /**
     * Home is on screen (opened, or come back to from a stream or another section): reload if the rows are stale,
     * then keep refreshing while it stays visible.
     */
    fun onVisible() {
        val loadedAt = loadedAtMillis
        if (loadedAt != null && clock.nowMillis() - loadedAt >= HomeStaleMillis) load(quietly = true)
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(HomeAutoRefreshMillis)
                load(quietly = true)
            }
        }
    }

    fun onHidden() {
        autoRefreshJob?.cancel()
    }

    /**
     * [quietly]: a refresh of rows already on screen — no skeleton, and a failure keeps the old rows
     * (the next refresh tries again) instead of replacing them with an error.
     */
    private fun load(quietly: Boolean) {
        if (quietly && loadJob?.isActive == true) return
        loadJob?.cancel()
        val hasContent = _uiState.value is HomeUiState.Content
        if (!quietly || !hasContent) _uiState.value = HomeUiState.Loading
        loadJob = viewModelScope.launch {
            val user = authRepository.authState.filterIsInstance<AuthState.SignedIn>().first().user
            val followed = when (val result = streamsRepository.getFollowedStreams(user.id, FollowedLimit)) {
                is AppResult.Failure -> {
                    if (!(quietly && hasContent)) _uiState.value = HomeUiState.Error(result.error)
                    return@launch
                }
                is AppResult.Success -> result.value
            }
            val language = viewerLanguage(followed)
            val regionalRequest = async { streamsRepository.getTopStreams(language, RowSize) }
            val recommendedRequest = async {
                streamsRepository.getTopStreamsInCategories(topCategories(followed), language, RowSize + followed.size)
            }
            val recommended = recommendedRequest.await()
            val regional = regionalRequest.await()
            val failure = (recommended as? AppResult.Failure)?.error ?: (regional as? AppResult.Failure)?.error
            if (failure != null && quietly && hasContent) return@launch
            loadedAtMillis = clock.nowMillis()
            refreshRecent()
            _uiState.value = when {
                recommended is AppResult.Failure -> HomeUiState.Error(recommended.error)
                regional is AppResult.Failure -> HomeUiState.Error(regional.error)
                else -> buildContent(
                    userName = user.displayName,
                    language = language,
                    followed = followed,
                    recommended = (recommended as AppResult.Success).value,
                    regional = (regional as AppResult.Success).value,
                )
            }
        }
    }

    private fun viewerLanguage(followed: List<Stream>): String =
        followed.map { it.language }
            .filter { it.isNotEmpty() && it != UnknownLanguage }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: deviceLanguage

    private fun topCategories(followed: List<Stream>): List<String> =
        followed.sortedByDescending { it.viewerCount }
            .map { it.categoryId }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(RecommendationCategories)

    private fun buildContent(
        userName: String,
        language: String,
        followed: List<Stream>,
        recommended: List<Stream>,
        regional: List<Stream>,
    ): HomeUiState {
        val followedIds = followed.mapTo(HashSet()) { it.userId }
        // Recommend only channels the user does not follow yet.
        val newChannels = recommended.filterNot { it.userId in followedIds }.take(RowSize)
        if (followed.isEmpty() && newChannels.isEmpty() && regional.isEmpty()) return HomeUiState.Empty(userName)
        return HomeUiState.Content(
            recent = recent.toImmutableList(),
            userName = userName,
            languageCode = language,
            followedLive = followed.toImmutableList(),
            recommended = newChannels.toImmutableList(),
            // Rows do not repeat each other: followed channels are already in the first row.
            regional = regional.filterNot { it.userId in followedIds }.toImmutableList(),
        )
    }
}

/** The "Недавно смотрели" cards: recordings with their strip and time left, channels live or off air. */
internal fun recentItems(
    entries: List<HistoryEntry>,
    positions: Map<String, WatchProgress>,
    liveByLogin: Map<String, Stream>,
): List<RecentItem> = entries.map { entry ->
    val videoId = entry.videoId
    if (videoId != null) {
        val progress = positions[videoId]
        RecentItem.Recording(
            entry = entry,
            watchedFraction = progress?.watchedFraction,
            remainingMs = progress?.takeUnless { it.isFinished }?.let { it.durationMs - it.positionMs },
        )
    } else {
        RecentItem.Channel(entry, liveByLogin[entry.channelLogin.lowercase()])
    }
}
