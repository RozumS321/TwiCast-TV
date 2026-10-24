package app.twicast.tv.data.remote

import kotlinx.serialization.Serializable

/*
 * Guest browsing over gql.twitch.tv: the queries twitch.tv itself runs for visitors who are not signed in.
 * Images come at a fixed size; the repositories turn them back into "{width}x{height}" templates.
 */

private const val StreamFieldsWithoutBroadcaster =
    "id title viewersCount createdAt language previewImageURL(width: 440, height: 248) game { id name }"

private const val StreamFields =
    "$StreamFieldsWithoutBroadcaster broadcaster { id login displayName profileImageURL(width: 70) }"

/** Live streams by viewers. [languages]: Twitch's Language enum, upper case ("RU"). */
const val GqlTopStreamsQuery = "query(\$first: Int, \$after: Cursor, \$languages: [Language!]) { " +
    "streams(first: \$first, after: \$after, options: {languages: \$languages}) { " +
    "edges { cursor node { $StreamFields } } pageInfo { hasNextPage } } }"

/** One category's live streams. Here [languages] is lower case ("ru"). */
const val GqlCategoryStreamsQuery = "query(\$id: ID, \$first: Int, \$after: Cursor, \$languages: [String!]) { " +
    "game(id: \$id) { streams(first: \$first, after: \$after, options: {languages: \$languages}) { " +
    "edges { cursor node { $StreamFields } } pageInfo { hasNextPage } } } }"

const val GqlTopCategoriesQuery = "query(\$first: Int, \$after: Cursor) { games(first: \$first, after: \$after) { " +
    "edges { cursor node { id name boxArtURL(width: 188, height: 250) } } pageInfo { hasNextPage } } }"

/** Who of [logins] is live now (stream null when not). */
const val GqlUsersQuery = "query(\$logins: [String!]) { users(logins: \$logins) { login stream { $StreamFieldsWithoutBroadcaster } " +
    "id displayName profileImageURL(width: 70) } }"

const val GqlSearchQuery = "query(\$query: String!, \$channels: Int, \$categories: Int) { " +
    "searchFor(userQuery: \$query, platform: \"web\", options: {targets: [{index: CHANNEL, limit: \$channels}]}) { " +
    "channels { edges { item { ... on User { id login displayName profileImageURL(width: 70) " +
    "stream { title game { name } } broadcastSettings { game { name } } } } } } } " +
    "searchCategories(query: \$query, first: \$categories) { edges { node { id name boxArtURL(width: 188, height: 250) } } } }"

const val GqlChannelQuery = "query(\$login: String!, \$videos: Int, \$clips: Int) { user(login: \$login) { " +
    "id login displayName description profileImageURL(width: 300) offlineImageURL followers { totalCount } " +
    "stream { $StreamFieldsWithoutBroadcaster } " +
    "videos(first: \$videos, type: ARCHIVE, sort: TIME) { edges { node { id title createdAt lengthSeconds viewCount " +
    "previewThumbnailURL(width: 320, height: 180) } } } " +
    "clips(first: \$clips, criteria: {period: LAST_MONTH, sort: VIEWS_DESC}) { edges { node { slug title viewCount durationSeconds " +
    "thumbnailURL(width: 480, height: 272) } } } } }"

@Serializable
data class GqlPageInfo(val hasNextPage: Boolean = false)

@Serializable
data class GqlGame(val id: String = "", val name: String? = null, val boxArtURL: String? = null)

@Serializable
data class GqlBroadcaster(val id: String = "", val login: String = "", val displayName: String? = null, val profileImageURL: String? = null)

@Serializable
data class GqlStream(
    val id: String = "",
    val title: String? = null,
    val viewersCount: Int = 0,
    val createdAt: String? = null,
    val language: String? = null,
    val previewImageURL: String? = null,
    val game: GqlGame? = null,
    val broadcaster: GqlBroadcaster? = null,
)

@Serializable
data class GqlStreamEdge(val cursor: String? = null, val node: GqlStream)

@Serializable
data class GqlStreamConnection(val edges: List<GqlStreamEdge> = emptyList(), val pageInfo: GqlPageInfo = GqlPageInfo())

@Serializable
data class GqlTopStreamsData(val streams: GqlStreamConnection? = null)

@Serializable
data class GqlGameWithStreams(val streams: GqlStreamConnection? = null)

@Serializable
data class GqlCategoryStreamsData(val game: GqlGameWithStreams? = null)

@Serializable
data class GqlGameEdge(val cursor: String? = null, val node: GqlGame)

@Serializable
data class GqlGameConnection(val edges: List<GqlGameEdge> = emptyList(), val pageInfo: GqlPageInfo = GqlPageInfo())

@Serializable
data class GqlTopCategoriesData(val games: GqlGameConnection? = null)

@Serializable
data class GqlUser(
    val id: String = "",
    val login: String = "",
    val displayName: String = "",
    val profileImageURL: String? = null,
    val stream: GqlStream? = null,
)

@Serializable
data class GqlUsersData(val users: List<GqlUser?> = emptyList())

@Serializable
data class GqlSearchStream(val title: String? = null, val game: GqlGame? = null)

@Serializable
data class GqlBroadcastSettings(val game: GqlGame? = null)

@Serializable
data class GqlSearchChannel(
    val id: String = "",
    val login: String = "",
    val displayName: String = "",
    val profileImageURL: String? = null,
    val stream: GqlSearchStream? = null,
    val broadcastSettings: GqlBroadcastSettings? = null,
)

@Serializable
data class GqlSearchItem(val item: GqlSearchChannel? = null)

@Serializable
data class GqlSearchChannels(val edges: List<GqlSearchItem> = emptyList())

@Serializable
data class GqlSearchFor(val channels: GqlSearchChannels? = null)

@Serializable
data class GqlCategoryNode(val node: GqlGame)

@Serializable
data class GqlCategoryEdges(val edges: List<GqlCategoryNode> = emptyList())

@Serializable
data class GqlSearchData(val searchFor: GqlSearchFor? = null, val searchCategories: GqlCategoryEdges? = null)

@Serializable
data class GqlFollowers(val totalCount: Int? = null)

@Serializable
data class GqlVideo(
    val id: String = "",
    val title: String? = null,
    val createdAt: String? = null,
    val lengthSeconds: Long = 0,
    val viewCount: Int = 0,
    val previewThumbnailURL: String? = null,
)

@Serializable
data class GqlVideoEdge(val node: GqlVideo)

@Serializable
data class GqlVideoConnection(val edges: List<GqlVideoEdge> = emptyList())

@Serializable
data class GqlClip(
    val slug: String = "",
    val title: String? = null,
    val viewCount: Int = 0,
    val durationSeconds: Long = 0,
    val thumbnailURL: String? = null,
)

@Serializable
data class GqlClipEdge(val node: GqlClip)

@Serializable
data class GqlClipConnection(val edges: List<GqlClipEdge> = emptyList())

@Serializable
data class GqlChannel(
    val id: String = "",
    val login: String = "",
    val displayName: String = "",
    val description: String? = null,
    val profileImageURL: String? = null,
    val offlineImageURL: String? = null,
    val followers: GqlFollowers? = null,
    val stream: GqlStream? = null,
    val videos: GqlVideoConnection? = null,
    val clips: GqlClipConnection? = null,
)

@Serializable
data class GqlChannelData(val user: GqlChannel? = null)
