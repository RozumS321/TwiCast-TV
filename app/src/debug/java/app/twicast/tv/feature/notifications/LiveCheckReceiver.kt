package app.twicast.tv.feature.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug builds only: `adb shell am broadcast -n app.twicast.tv.debug/app.twicast.tv.feature.notifications.LiveCheckReceiver`
 * forgets who was live and runs the check now, so every live followed channel is announced — no 15-minute wait.
 */
class LiveCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EntryPointAccessors.fromApplication(app, LiveStartEntryPoint::class.java).state().setLastLive(emptySet())
                WorkManager.getInstance(app).enqueue(OneTimeWorkRequestBuilder<LiveStartWorker>().build())
            } finally {
                pending.finish()
            }
        }
    }
}
