package app.twicast.tv.data.sleep

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import app.twicast.tv.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only device-admin right TwiCast asks for: lock the screen ("force-lock"). Locking puts the box to sleep at
 * once, and over HDMI-CEC the TV goes off with it. Without the right the box falls asleep by its own settings.
 */
class SleepAdminReceiver : DeviceAdminReceiver()

@Singleton
class DeviceSleeper @Inject constructor(@ApplicationContext private val context: Context) {
    private val policy = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, SleepAdminReceiver::class.java)
    private val prefs = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)

    val allowed: Boolean get() = policy?.isAdminActive(admin) == true

    /** Whether we already explained why: the card shows once, the setting stays reachable from the timer menu. */
    var explained: Boolean
        get() = prefs.getBoolean(ExplainedKey, false)
        set(value) = prefs.edit().putBoolean(ExplainedKey, value).apply()

    /**
     * Android's own "activate device admin" screen, with our one-line reason. Aimed at one app: TV boxes carry both
     * the phone and the TV settings, and the "choose an app" sheet between them came up black and unusable.
     */
    fun requestIntent(): Intent {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, context.getString(R.string.sleep_admin_explanation))
        val handlers = context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo }
        val target = handlers.firstOrNull { it.packageName == TvSettings } ?: handlers.firstOrNull()
        if (target != null) intent.setClassName(target.packageName, target.name)
        return intent
    }

    /** Puts the box to sleep if allowed; false when it is not (the caller only stops playback then). */
    fun sleepNow(): Boolean {
        if (!allowed) return false
        return runCatching { policy?.lockNow() }.isSuccess
    }

    private companion object {
        const val PrefsName = "sleep_timer"
        const val TvSettings = "com.android.tv.settings"
        const val ExplainedKey = "explained"
    }
}
