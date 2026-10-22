package app.twicast.tv.core.device

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Below this much RAM the Twitch embed at 1080p runs the WebView renderer out of memory (seen on Mi TV boxes). */
private const val LowMemoryBytes = 3L * 1024 * 1024 * 1024

/** What the hardware can take; lets weak TVs start the player at a lighter quality. */
interface DeviceProfile {
    val isLowMemory: Boolean
}

@Singleton
class AndroidDeviceProfile @Inject constructor(@ApplicationContext context: Context) : DeviceProfile {
    override val isLowMemory: Boolean = run {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        activityManager.isLowRamDevice || memory.totalMem < LowMemoryBytes
    }
}
