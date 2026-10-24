package app.twicast.tv.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** https://api.github.com — public releases of the app's own repository (no token: 60 requests/hour per IP). */
interface GitHubReleasesApi {

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(@Path("owner") owner: String, @Path("repo") repo: String): GitHubReleaseDto

    /** Newest first; [page] starts at 1, [perPage] is at most 100. */
    @GET("repos/{owner}/{repo}/releases")
    suspend fun releases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int,
        @Query("page") page: Int,
    ): List<GitHubReleaseDto>
}

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val assets: List<GitHubAssetDto> = emptyList(),
    /** ISO 8601, e.g. "2026-10-24T18:13:40Z"; null for drafts. */
    @SerialName("published_at") val publishedAt: String? = null,
)

@Serializable
data class GitHubAssetDto(
    val name: String,
    val size: Long,
    @SerialName("browser_download_url") val downloadUrl: String,
)
