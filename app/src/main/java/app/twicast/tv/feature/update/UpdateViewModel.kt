package app.twicast.tv.feature.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.update.ApkDownloader
import app.twicast.tv.data.update.ApkInstaller
import app.twicast.tv.data.update.AppRelease
import app.twicast.tv.data.update.ReleaseNotes
import app.twicast.tv.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** The running build. [updatesEnabled] is true only for CI builds (they carry a real versionCode). */
data class AppVersion(val code: Int, val name: String, val updatesEnabled: Boolean)

sealed interface UpdateUiState {
    data object Hidden : UpdateUiState

    /** Mockup "Обновление · доступно". The release's notes cover every version since [installedVersion]. */
    data class Available(val release: AppRelease, val installedVersion: String) : UpdateUiState

    /** Mockup "Обновление · загрузка". */
    data class Downloading(val release: AppRelease, val installedVersion: String, val downloadedBytes: Long, val totalBytes: Long) :
        UpdateUiState

    /** The APK is ready but "Install unknown apps" is off for TwiCast. */
    data class NeedsPermission(val release: AppRelease, val installedVersion: String) : UpdateUiState

    data class Failed(val release: AppRelease, val installedVersion: String) : UpdateUiState

    /** Mockup "Обновление · что нового": shown once after the update installed, notes of every version skipped. */
    data class WhatsNew(val release: AppRelease) : UpdateUiState
}

/**
 * Self-update from GitHub Releases. On start: "what's new" if this version is new to the device,
 * otherwise a check for a newer release. The system installer still asks the user to confirm.
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateRepository,
    private val downloader: ApkDownloader,
    private val installer: ApkInstaller,
    private val version: AppVersion,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Hidden)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null
    private var downloadedApk: File? = null

    init {
        if (version.updatesEnabled) viewModelScope.launch { onStart() }
    }

    private suspend fun onStart() {
        val lastSeen = repository.lastSeenVersionCode()
        repository.setLastSeenVersionCode(version.code)
        if (lastSeen != null && version.code > lastSeen) {
            val installed = (repository.releasesBetween(lastSeen, version.code) as? AppResult.Success)?.value.orEmpty()
            val current = installed.firstOrNull { it.versionCode == version.code }
            if (current != null) {
                val whatsNew = withNotesOf(current, installed)
                if (!whatsNew.notes.isEmpty) {
                    _uiState.value = UpdateUiState.WhatsNew(whatsNew)
                    return
                }
            }
        }
        // Failures stay silent: the update is offered next time the app starts.
        val latest = (repository.latestRelease() as? AppResult.Success)?.value ?: return
        offer(latest)
    }

    private suspend fun offer(latest: AppRelease) {
        if (latest.versionCode <= version.code) return
        // Without the list the offer still shows the latest release's own notes.
        val skipped = (repository.releasesBetween(version.code, latest.versionCode) as? AppResult.Success)?.value.orEmpty()
        _uiState.value = UpdateUiState.Available(withNotesOf(latest, skipped), version.name)
    }

    /** «Обновить до …» in "Что нового": the same offer as at start, for a release the viewer passed on then. */
    fun onShowUpdate(latest: AppRelease) {
        if (_uiState.value is UpdateUiState.Downloading) return
        viewModelScope.launch { offer(latest) }
    }

    /** [newest] with its own notes followed by those of the older [releases] (duplicates dropped). */
    private fun withNotesOf(newest: AppRelease, releases: List<AppRelease>): AppRelease {
        val older = releases.filter { it.versionCode < newest.versionCode }.sortedByDescending { it.versionCode }
        return newest.copy(notes = ReleaseNotes.merge(listOf(newest.notes) + older.map { it.notes }))
    }

    fun onUpdate() {
        val release = currentRelease() ?: return
        downloadJob?.cancel()
        _uiState.value = UpdateUiState.Downloading(release, version.name, 0, release.apkSizeBytes)
        downloadJob = viewModelScope.launch {
            val result = downloader.download(release) { downloaded, total ->
                _uiState.update { state ->
                    if (state is UpdateUiState.Downloading) state.copy(downloadedBytes = downloaded, totalBytes = total) else state
                }
            }
            when (result) {
                is AppResult.Failure -> _uiState.value = UpdateUiState.Failed(release, version.name)
                is AppResult.Success -> {
                    downloadedApk = result.value
                    installOrAskPermission(release)
                }
            }
        }
    }

    fun onCancelDownload() {
        downloadJob?.cancel()
        currentRelease()?.let { _uiState.value = UpdateUiState.Available(it, version.name) }
    }

    fun onOpenPermissionSettings() = installer.openInstallPermissionSettings()

    /** Back from system settings (or from the installer): retry if the permission is now granted. */
    fun onResume() {
        val state = _uiState.value
        if (state is UpdateUiState.NeedsPermission) installOrAskPermission(state.release)
    }

    fun onDismiss() {
        downloadJob?.cancel()
        _uiState.value = UpdateUiState.Hidden
    }

    private fun installOrAskPermission(release: AppRelease) {
        val apk = downloadedApk ?: return
        if (installer.canInstall()) {
            installer.install(apk)
            // The installer takes over the screen; if the user backs out, the offer stays.
            _uiState.value = UpdateUiState.Available(release, version.name)
        } else {
            _uiState.value = UpdateUiState.NeedsPermission(release, version.name)
        }
    }

    private fun currentRelease(): AppRelease? = when (val state = _uiState.value) {
        is UpdateUiState.Available -> state.release
        is UpdateUiState.Downloading -> state.release
        is UpdateUiState.NeedsPermission -> state.release
        is UpdateUiState.Failed -> state.release
        is UpdateUiState.WhatsNew, UpdateUiState.Hidden -> null
    }
}
