package app.twicast.tv.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

/** https://api.github.com — public releases of the app's own repository (no token: 60 requests/hour per IP). */
interface GitHubReleasesApi {

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(@Path("owner") owner: String, @Path("repo") repo: String): GitHubReleaseDto

    @GET("repos/{owner}/{repo}/releases/tags/{tag}")
    suspend fun releaseByTag(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("tag") tag: String,
    ): GitHubReleaseDto
}

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
data class GitHubAssetDto(
    val name: String,
    val size: Long,
    @SerialName("browser_download_url") val downloadUrl: String,
)
