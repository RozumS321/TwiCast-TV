package app.twicast.tv.data.update

import app.twicast.tv.data.remote.GitHubAssetDto
import app.twicast.tv.data.remote.GitHubReleaseDto
import app.twicast.tv.data.remote.GitHubReleasesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private fun dto(code: Int, apk: Boolean = true) = GitHubReleaseDto(
    tagName = "v0.1.$code",
    body = "## Новое\n- Версия $code",
    assets = if (apk) listOf(GitHubAssetDto("twicast-$code.apk", 1_000, "https://example.test/$code.apk")) else emptyList(),
)

/** Serves [releases] (newest first) in pages the way GitHub does. */
private class FakeGitHubReleasesApi(private val releases: List<GitHubReleaseDto>) : GitHubReleasesApi {
    val requestedPages = mutableListOf<Int>()

    override suspend fun latestRelease(owner: String, repo: String) = releases.first()

    override suspend fun releases(owner: String, repo: String, perPage: Int, page: Int): List<GitHubReleaseDto> {
        requestedPages += page
        return releases.drop((page - 1) * perPage).take(perPage)
    }
}

class ReleasesBetweenTest {

    @Test
    fun `picks the range newest first, skipping foreign tags and releases without an apk`() = runTest {
        val api = FakeGitHubReleasesApi(
            listOf(dto(9), dto(8), GitHubReleaseDto(tagName = "nightly"), dto(7, apk = false), dto(6), dto(5), dto(4)),
        )
        val releases = api.releasesBetween("owner", "repo", afterVersionCode = 5, upToVersionCode = 8)
        assertEquals(listOf(8, 6), releases.map { it.versionCode })
        assertEquals(listOf("Версия 8"), releases.first().notes.added)
    }

    @Test
    fun `pages back only until the range start`() = runTest {
        val api = FakeGitHubReleasesApi((250 downTo 1).map { dto(it) })

        val recent = api.releasesBetween("owner", "repo", afterVersionCode = 245, upToVersionCode = 250)
        assertEquals((250 downTo 246).toList(), recent.map { it.versionCode })
        assertEquals(listOf(1), api.requestedPages)

        api.requestedPages.clear()
        val longJump = api.releasesBetween("owner", "repo", afterVersionCode = 100, upToVersionCode = 250)
        assertEquals(150, longJump.size)
        assertEquals(listOf(1, 2), api.requestedPages)
    }

    @Test
    fun `a short list is read in one request`() = runTest {
        val api = FakeGitHubReleasesApi(listOf(dto(3), dto(2)))
        assertEquals(listOf(3, 2), api.releasesBetween("owner", "repo", afterVersionCode = 0, upToVersionCode = 3).map { it.versionCode })
        assertEquals(listOf(1), api.requestedPages)
    }
}
