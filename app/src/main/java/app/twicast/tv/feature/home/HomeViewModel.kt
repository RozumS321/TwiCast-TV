package app.twicast.tv.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.model.Stream
import app.twicast.tv.data.repository.AuthRepository
import app.twicast.tv.data.repository.StreamsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    locale: Locale,
) : ViewModel() {

    private val deviceLanguage = locale.language
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        loadJob?.cancel()
        _uiState.value = HomeUiState.Loading
        loadJob = viewModelScope.launch {
            val user = authRepository.authState.filterIsInstance<AuthState.SignedIn>().first().user
            val followed = when (val result = streamsRepository.getFollowedStreams(user.id, FollowedLimit)) {
                is AppResult.Failure -> {
                    _uiState.value = HomeUiState.Error(result.error)
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
            userName = userName,
            languageCode = language,
            followedLive = followed.toImmutableList(),
            recommended = newChannels.toImmutableList(),
            // Rows do not repeat each other: followed channels are already in the first row.
            regional = regional.filterNot { it.userId in followedIds }.toImmutableList(),
        )
    }
}
