package app.twicast.tv.data.repository

import app.twicast.tv.core.network.Clock
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.Category
import app.twicast.tv.data.model.Page
import app.twicast.tv.data.model.runCatchingRemote
import app.twicast.tv.data.remote.HelixApi
import javax.inject.Inject
import javax.inject.Singleton

interface CategoriesRepository {
    /**
     * Categories ordered by current viewers, a page at a time ([cursor] null = first page).
     * Helix does not return the viewer count itself.
     */
    suspend fun getTopCategories(limit: Int, cursor: String? = null): AppResult<Page<Category>>
}

private const val CacheTtlMillis = 60_000L

@Singleton
class HelixCategoriesRepository @Inject constructor(
    private val api: HelixApi,
    clock: Clock,
) : CategoriesRepository {

    /** Only the first page is cached: that is what a returning viewer sees. */
    private val firstPageCache = TtlCache<Int, Page<Category>>(clock, CacheTtlMillis)

    override suspend fun getTopCategories(limit: Int, cursor: String?): AppResult<Page<Category>> {
        val load: suspend () -> AppResult<Page<Category>> = {
            runCatchingRemote {
                val page = api.getTopGames(first = limit, after = cursor)
                Page(page.data.map { Category(it.id, it.name, it.boxArtUrl) }, page.pagination?.cursor)
            }
        }
        return if (cursor == null) firstPageCache.getOrLoad(limit, load) else load()
    }
}
