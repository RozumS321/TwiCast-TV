package app.twicast.tv.feature.update

import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.update.ApkDownloader
import app.twicast.tv.data.update.ApkInstaller
import app.twicast.tv.data.update.AppRelease
import app.twicast.tv.data.update.ReleaseNotes
import app.twicast.tv.data.update.UpdateRepository
import app.twicast.tv.testing.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

private fun release(code: Int, notes: ReleaseNotes = ReleaseNotes(listOf("Чат"), emptyList(), emptyList())) =
    AppRelease(code, "0.1.$code", notes, "https://example.test/$code.apk", apkSizeBytes = 1_000)

private class FakeUpdateRepository : UpdateRepository {
    var latest: AppResult<AppRelease?> = AppResult.Success(null)
    var byVersion: Map<String, AppRelease> = emptyMap()
    var lastSeen: Int? = null

    override suspend fun latestRelease() = latest
    override suspend fun release(versionName: String): AppResult<AppRelease?> = AppResult.Success(byVersion[versionName])
    override suspend fun lastSeenVersionCode() = lastSeen
    override suspend fun setLastSeenVersionCode(versionCode: Int) {
        lastSeen = versionCode
    }
}

private class FakeDownloader : ApkDownloader {
    var result: AppResult<File> = AppResult.Success(File("update.apk"))
    override suspend fun download(release: AppRelease, onProgress: (Long, Long) -> Unit): AppResult<File> {
        onProgress(release.apkSizeBytes / 2, release.apkSizeBytes)
        return result
    }
}

private class FakeInstaller : ApkInstaller {
    var permitted = true
    val installed = mutableListOf<File>()
    override fun canInstall() = permitted
    override fun openInstallPermissionSettings() = Unit
    override fun install(apk: File) {
        installed += apk
    }
}

class UpdateViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val repository = FakeUpdateRepository()
    private val downloader = FakeDownloader()
    private val installer = FakeInstaller()

    private fun kotlinx.coroutines.test.TestScope.viewModel(code: Int = 10, enabled: Boolean = true) =
        UpdateViewModel(repository, downloader, installer, AppVersion(code, "0.1.$code", enabled)).also { runCurrent() }

    @Test
    fun `a newer release is offered`() = runTest(mainRule.dispatcher) {
        repository.lastSeen = 10
        repository.latest = AppResult.Success(release(12))
        assertEquals(UpdateUiState.Available(release(12), "0.1.10"), viewModel().uiState.value)
    }

    @Test
    fun `same or older release, failures and local builds stay quiet`() = runTest(mainRule.dispatcher) {
        repository.lastSeen = 10
        repository.latest = AppResult.Success(release(10))
        assertEquals(UpdateUiState.Hidden, viewModel().uiState.value)

        repository.latest = AppResult.Failure(AppError.Network)
        assertEquals(UpdateUiState.Hidden, viewModel().uiState.value)

        repository.latest = AppResult.Success(release(99))
        assertEquals(UpdateUiState.Hidden, viewModel(enabled = false).uiState.value)
    }

    @Test
    fun `first start after an update shows what's new once`() = runTest(mainRule.dispatcher) {
        repository.lastSeen = 9
        repository.byVersion = mapOf("0.1.10" to release(10))
        assertEquals(UpdateUiState.WhatsNew(release(10)), viewModel().uiState.value)
        assertEquals(10, repository.lastSeen)
        assertEquals("not again on the next start", UpdateUiState.Hidden, viewModel().uiState.value)
    }

    @Test
    fun `the very first install shows nothing`() = runTest(mainRule.dispatcher) {
        repository.byVersion = mapOf("0.1.10" to release(10))
        assertEquals(UpdateUiState.Hidden, viewModel().uiState.value)
    }

    @Test
    fun `update downloads and hands the apk to the installer`() = runTest(mainRule.dispatcher) {
        repository.lastSeen = 10
        repository.latest = AppResult.Success(release(12))
        val viewModel = viewModel()

        viewModel.onUpdate()
        runCurrent()

        assertEquals(listOf(File("update.apk")), installer.installed)
    }

    @Test
    fun `without the install permission it asks, then installs on return`() = runTest(mainRule.dispatcher) {
        repository.lastSeen = 10
        repository.latest = AppResult.Success(release(12))
        installer.permitted = false
        val viewModel = viewModel()

        viewModel.onUpdate()
        runCurrent()
        assertTrue(viewModel.uiState.value is UpdateUiState.NeedsPermission)

        installer.permitted = true
        viewModel.onResume()
        assertEquals(1, installer.installed.size)
    }

    @Test
    fun `a failed download can be retried`() = runTest(mainRule.dispatcher) {
        repository.lastSeen = 10
        repository.latest = AppResult.Success(release(12))
        downloader.result = AppResult.Failure(AppError.Network)
        val viewModel = viewModel()

        viewModel.onUpdate()
        runCurrent()
        assertEquals(UpdateUiState.Failed(release(12), "0.1.10"), viewModel.uiState.value)
    }
}

class ReleaseNotesTest {

    @Test
    fun `parses the sections the release workflow writes`() {
        val notes = ReleaseNotes.parse(
            """
            ## Новое
            - Подписки в эфире
            - Голосовой поиск

            ## Исправлено
            - Качество после рекламы

            ## Что-то ещё
            - игнорируется
            """.trimIndent(),
        )
        assertEquals(listOf("Подписки в эфире", "Голосовой поиск"), notes.added)
        assertEquals(listOf("Качество после рекламы"), notes.fixed)
        assertEquals(emptyList<String>(), notes.improved)
    }

    @Test
    fun `tags carry the version code`() {
        assertEquals(42 to "0.1.42", AppRelease.parseTag("v0.1.42"))
        assertEquals(null, AppRelease.parseTag("nightly"))
    }
}
