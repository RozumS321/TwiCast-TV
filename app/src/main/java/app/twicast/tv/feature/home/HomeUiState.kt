package app.twicast.tv.feature.home

import androidx.compose.runtime.Immutable
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.Stream
import kotlinx.collections.immutable.ImmutableList

sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** Nothing live: no followed channels, no recommendations, nothing in the region. */
    data class Empty(val userName: String) : HomeUiState

    data class Error(val error: AppError) : HomeUiState

    /** Mockup "Главная · авторизован". Empty rows are not shown. */
    @Immutable
    data class Content(
        val userName: String,
        /** Viewer language (ISO 639-1) used for recommendations and "popular". */
        val languageCode: String,
        val followedLive: ImmutableList<Stream>,
        val recommended: ImmutableList<Stream>,
        val regional: ImmutableList<Stream>,
    ) : HomeUiState
}
