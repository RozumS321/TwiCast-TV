package app.twicast.tv.feature.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.data.repository.AuthRepository
import app.twicast.tv.data.repository.LiveNotificationStateRepository
import app.twicast.tv.data.repository.PlayerSettingsRepository
import app.twicast.tv.data.repository.StreamsRepository
import app.twicast.tv.data.repository.newlyLive
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Android runs periodic work at most this often: a channel is announced up to 15 minutes after it goes live. */
private const val CheckIntervalMinutes = 15L
private const val WorkName = "live-starts"

/** As home and the sidebar: one request for the whole follow list. */
private const val FollowedLimit = 100

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LiveStartEntryPoint {
    fun auth(): AuthRepository
    fun streams(): StreamsRepository
    fun settings(): PlayerSettingsRepository
    fun state(): LiveNotificationStateRepository
}

/**
 * "Сообщать о начале эфиров" while the app is closed: there is no server to push, so every 15 minutes the follow
 * list is checked and channels live since the last check get a system notification.
 */
class LiveStartWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, LiveStartEntryPoint::class.java)
        if (!deps.settings().notifyLiveStarts.first()) return Result.success()
        val user = (deps.auth().authState.first { it != AuthState.Loading } as? AuthState.SignedIn)?.user
            ?: return Result.success()
        // No network or Twitch hiccup: the next run tries again; nothing is lost but a little delay.
        val live = (deps.streams().getFollowedStreams(user.id, FollowedLimit) as? AppResult.Success)?.value
            ?: return Result.success()
        val announce = newlyLive(deps.state().lastLive(), live)
        deps.state().setLastLive(live.mapTo(HashSet()) { it.userLogin })
        LiveNotifications.show(applicationContext, announce)
        return Result.success()
    }

    companion object {
        /** Starts the checks while the option is on, stops them when it is off. Safe to call on every launch. */
        fun sync(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (!enabled) {
                workManager.cancelUniqueWork(WorkName)
                return
            }
            val request = PeriodicWorkRequestBuilder<LiveStartWorker>(CheckIntervalMinutes, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            workManager.enqueueUniquePeriodicWork(WorkName, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
