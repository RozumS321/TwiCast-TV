package app.twicast.tv.data.repository

import app.twicast.tv.data.local.SessionStore
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.Category
import app.twicast.tv.data.model.ChannelPage
import app.twicast.tv.data.model.Page
import app.twicast.tv.data.model.Stream
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Signed in, the app browses through Helix (official, with the viewer's token); as a guest through
 * gql.twitch.tv like the site does for visitors. Each call asks which one applies, so signing in or out
 * switches at once.
 */

/** Whether a Twitch session is stored; repositories use it to pick Helix or the guest source. */
fun interface SignedInCheck {
    suspend fun signedIn(): Boolean
}

@Singleton
class SessionSignedInCheck @Inject constructor(private val sessions: SessionStore) : SignedInCheck {
    override suspend fun signedIn(): Boolean = sessions.current() != null
}

@Singleton
class GuestAwareStreamsRepository @Inject constructor(
    private val helix: HelixStreamsRepository,
    private val guest: GqlBrowseRepository,
    private val session: SignedInCheck,
) : StreamsRepository {

    override suspend fun getTopStreams(language: String?, limit: Int): AppResult<List<Stream>> =
        if (session.signedIn()) helix.getTopStreams(language, limit) else guest.topStreams(language, limit)

    override suspend fun isLive(login: String): AppResult<Boolean> =
        if (session.signedIn()) helix.isLive(login) else guest.isLive(login)

    override suspend fun getLiveStreams(logins: List<String>): AppResult<List<Stream>> =
        if (session.signedIn()) helix.getLiveStreams(logins) else guest.liveStreams(logins)

    /** A guest follows nobody. */
    override suspend fun getFollowedStreams(userId: String, limit: Int): AppResult<List<Stream>> =
        if (session.signedIn()) helix.getFollowedStreams(userId, limit) else AppResult.Failure(AppError.Unauthorized)

    override suspend fun getCategoryStreams(categoryId: String, language: String?, limit: Int, cursor: String?): AppResult<Page<Stream>> =
        if (session.signedIn()) helix.getCategoryStreams(categoryId, language, limit, cursor) else guest.categoryStreams(categoryId, language, limit, cursor)

    /** Recommendations come from followed channels: none for a guest. */
    override suspend fun getTopStreamsInCategories(categoryIds: List<String>, language: String?, limit: Int): AppResult<List<Stream>> =
        if (session.signedIn()) helix.getTopStreamsInCategories(categoryIds, language, limit) else AppResult.Success(emptyList())
}

@Singleton
class GuestAwareCategoriesRepository @Inject constructor(
    private val helix: HelixCategoriesRepository,
    private val guest: GqlBrowseRepository,
    private val session: SignedInCheck,
) : CategoriesRepository {
    override suspend fun getTopCategories(limit: Int, cursor: String?): AppResult<Page<Category>> =
        if (session.signedIn()) helix.getTopCategories(limit, cursor) else guest.topCategories(limit, cursor)
}

@Singleton
class GuestAwareSearchRepository @Inject constructor(
    private val helix: HelixSearchRepository,
    private val guest: GqlBrowseRepository,
    private val session: SignedInCheck,
) : SearchRepository {
    override suspend fun search(query: String, channelLimit: Int, categoryLimit: Int): AppResult<SearchResults> =
        if (session.signedIn()) helix.search(query, channelLimit, categoryLimit) else guest.search(query, channelLimit, categoryLimit)
}

@Singleton
class GuestAwareChannelRepository @Inject constructor(
    private val helix: HelixChannelRepository,
    private val guest: GqlBrowseRepository,
) : ChannelRepository {
    /** No viewer: a guest; their page comes without "В подписках" and without the schedule. */
    override suspend fun getChannel(login: String, viewerId: String?): AppResult<ChannelPage> =
        if (viewerId != null) helix.getChannel(login, viewerId) else guest.channel(login)
}
