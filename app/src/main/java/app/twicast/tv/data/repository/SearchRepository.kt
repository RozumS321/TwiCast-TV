package app.twicast.tv.data.repository

import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.Category
import app.twicast.tv.data.model.ChannelSearchResult
import app.twicast.tv.data.model.runCatchingRemote
import app.twicast.tv.data.remote.HelixApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

data class SearchResults(
    val channels: List<ChannelSearchResult>,
    val categories: List<Category>,
)

interface SearchRepository {
    /** Channels (live first) and categories matching [query]; both requests run in parallel. */
    suspend fun search(query: String, channelLimit: Int, categoryLimit: Int): AppResult<SearchResults>
}

@Singleton
class HelixSearchRepository @Inject constructor(private val api: HelixApi) : SearchRepository {

    override suspend fun search(query: String, channelLimit: Int, categoryLimit: Int): AppResult<SearchResults> =
        runCatchingRemote {
            coroutineScope {
                val channels = async { api.searchChannels(query, channelLimit).data }
                val categories = async { api.searchCategories(query, categoryLimit).data }
                SearchResults(
                    channels = channels.await()
                        .map {
                            ChannelSearchResult(
                                id = it.id,
                                login = it.login,
                                displayName = it.displayName,
                                avatarUrl = it.avatarUrl.ifEmpty { null },
                                isLive = it.isLive,
                                categoryName = it.gameName,
                                title = it.title,
                            )
                        }
                        // Stable sort: Twitch relevance order is kept within live and offline.
                        .sortedByDescending { it.isLive },
                    categories = categories.await().map { Category(it.id, it.name, it.boxArtUrl) },
                )
            }
        }
}
