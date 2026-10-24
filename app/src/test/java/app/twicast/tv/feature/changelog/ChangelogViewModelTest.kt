package app.twicast.tv.feature.changelog

import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.update.AppRelease
import app.twicast.tv.data.update.ReleaseNotes
import app.twicast.tv.data.update.UpdateRepository
import app.twicast.tv.feature.update.AppVersion
import app.twicast.tv.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val kyiv: ZoneId = ZoneId.of("Europe/Kyiv")

private fun release(code: Int, published: String?, added: List<String> = listOf("пункт $code")) = AppRelease(
    versionCode = code,
    versionName = "0.1.$code",
    notes = ReleaseNotes(added, emptyList(), emptyList()),
    apkUrl = "https://example.test/$code.apk",
    apkSizeBytes = 1,
    publishedAtMillis = published?.let { Instant.parse(it).toEpochMilli() },
)

private class FakeReleases(var result: AppResult<List<AppRelease>>) : UpdateRepository {
    override suspend fun latestRelease(): AppResult<AppRelease?> = AppResult.Success(null)
    override suspend fun releasesBetween(afterVersionCode: Int, upToVersionCode: Int): AppResult<List<AppRelease>> = result
    override suspend fun allReleases(): AppResult<List<AppRelease>> = result
    override suspend fun lastSeenVersionCode(): Int? = null
    override suspend fun setLastSeenVersionCode(versionCode: Int) = Unit
}

class ChangelogViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val releases = listOf(
        release(23, "2026-10-24T18:13:40Z"),
        release(22, "2026-10-24T13:42:29Z"),
        // 22:30 UTC is already the 24th in Kyiv.
        release(21, "2026-10-23T22:30:00Z"),
        release(20, "2026-10-23T19:01:02Z"),
        release(19, "2026-10-23T18:53:06Z", added = emptyList()),
    )

    @Test
    fun `releases are grouped by day in the viewer's time zone, newest first, empty ones left out`() {
        val days = changelogDays(releases, kyiv)
        assertEquals(listOf(LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 23)), days.map { it.date })
        assertEquals(listOf(23, 22, 21), days[0].releases.map { it.versionCode })
        assertEquals(listOf(20), days[1].releases.map { it.versionCode })
    }

    @Test
    fun `a release newer than the installed one is offered`() {
        val viewModel = ChangelogViewModel(FakeReleases(AppResult.Success(releases)), AppVersion(21, "0.1.21", updatesEnabled = true))
        val content = viewModel.uiState.value as ChangelogUiState.Content
        assertEquals("0.1.21", content.installedVersion)
        assertEquals(23, content.newer?.versionCode)
    }

    @Test
    fun `nothing is offered on the latest version or in a debug build`() {
        val latest = ChangelogViewModel(FakeReleases(AppResult.Success(releases)), AppVersion(23, "0.1.23", updatesEnabled = true))
        assertNull((latest.uiState.value as ChangelogUiState.Content).newer)
        val debug = ChangelogViewModel(FakeReleases(AppResult.Success(releases)), AppVersion(1, "0.1.1", updatesEnabled = false))
        assertNull((debug.uiState.value as ChangelogUiState.Content).newer)
    }

    @Test
    fun `no network shows an error and retry loads again`() {
        val repository = FakeReleases(AppResult.Failure(AppError.Network))
        val viewModel = ChangelogViewModel(repository, AppVersion(23, "0.1.23", updatesEnabled = true))
        assertEquals(ChangelogUiState.Error(AppError.Network), viewModel.uiState.value)

        repository.result = AppResult.Success(releases)
        viewModel.onRetry()
        assertEquals(true, viewModel.uiState.value is ChangelogUiState.Content)
    }
}
