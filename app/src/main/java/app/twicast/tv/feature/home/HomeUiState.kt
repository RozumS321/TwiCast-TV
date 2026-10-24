package app.twicast.tv.feature.home

import androidx.compose.runtime.Immutable
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.Stream
import app.twicast.tv.data.repository.HistoryEntry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** A card of "Недавно смотрели" (mockup "Главная · недавно смотрели"). */
@Immutable
sealed interface RecentItem {
    val entry: HistoryEntry

    /** A recording: how much is watched (strip) and what is left; null fraction when never got past 30 s. */
    data class Recording(override val entry: HistoryEntry, val watchedFraction: Float?, val remainingMs: Long?) : RecentItem

    /** A channel watched live: its stream when it broadcasts now, else null (off air). */
    data class Channel(override val entry: HistoryEntry, val stream: Stream?) : RecentItem
}

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
        /** "Недавно смотрели", newest first; empty: no row. */
        val recent: ImmutableList<RecentItem> = persistentListOf(),
        /** Not signed in (mockup "Главная · гость"): no greeting and no followed rows, the sign-in banner instead. */
        val guest: Boolean = false,
        /** Guests: the banner is up until «Позже» (for this run of the app). */
        val signInBanner: Boolean = false,
        /** Guests: popular in the whole world, after popular in their language. */
        val worldwide: ImmutableList<Stream> = persistentListOf(),
    ) : HomeUiState
}
