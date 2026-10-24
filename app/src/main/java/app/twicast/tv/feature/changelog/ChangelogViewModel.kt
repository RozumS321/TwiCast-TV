package app.twicast.tv.feature.changelog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.update.AppRelease
import app.twicast.tv.data.update.UpdateRepository
import app.twicast.tv.feature.update.AppVersion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@Serializable
data object ChangelogDestination

/** The releases of one day, newest first. [date] is null for releases without a publish time. */
@Immutable
data class ChangelogDay(val date: LocalDate?, val releases: List<AppRelease>)

sealed interface ChangelogUiState {
    data object Loading : ChangelogUiState
    data class Error(val error: AppError) : ChangelogUiState

    /** Mockup "Что нового · история обновлений"; [newer]: a release above the installed one, offered for update. */
    @Immutable
    data class Content(val installedVersion: String, val installedCode: Int, val days: List<ChangelogDay>, val newer: AppRelease?) :
        ChangelogUiState
}

/**
 * Releases grouped by the day they came out in [zone], newest day first. Releases with nothing to tell (only
 * tooling changes) are left out.
 */
fun changelogDays(releases: List<AppRelease>, zone: ZoneId): List<ChangelogDay> =
    releases.filterNot { it.notes.isEmpty }
        .sortedByDescending { it.versionCode }
        .groupBy { release -> release.publishedAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } }
        .map { (date, dayReleases) -> ChangelogDay(date, dayReleases) }
        .sortedWith(compareByDescending<ChangelogDay> { it.date ?: LocalDate.MIN })

/** "Что нового" in the sidebar: every update from GitHub Releases, by date. */
@HiltViewModel
class ChangelogViewModel @Inject constructor(
    private val repository: UpdateRepository,
    private val version: AppVersion,
) : ViewModel() {

    /** Tests pin the time zone; the app uses the TV's. */
    internal var zone: ZoneId = ZoneId.systemDefault()

    private val _uiState = MutableStateFlow<ChangelogUiState>(ChangelogUiState.Loading)
    val uiState: StateFlow<ChangelogUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        _uiState.value = ChangelogUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = repository.allReleases()) {
                is AppResult.Failure -> ChangelogUiState.Error(result.error)
                is AppResult.Success -> ChangelogUiState.Content(
                    installedVersion = version.name,
                    installedCode = version.code,
                    days = changelogDays(result.value, zone),
                    // Only builds that update themselves offer it (a debug build is never "behind").
                    newer = result.value.maxByOrNull { it.versionCode }?.takeIf { version.updatesEnabled && it.versionCode > version.code },
                )
            }
        }
    }
}
