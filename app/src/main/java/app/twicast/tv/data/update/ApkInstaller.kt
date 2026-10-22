package app.twicast.tv.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface ApkInstaller {
    /** "Install unknown apps" granted for TwiCast (Android 8+ asks once per app). */
    fun canInstall(): Boolean

    fun openInstallPermissionSettings()

    /** Hands the APK to the system installer; it installs over the current version (same signing key). */
    fun install(apk: File)
}

private const val ApkMimeType = "application/vnd.android.package-archive"

@Singleton
class SystemApkInstaller @Inject constructor(@ApplicationContext private val context: Context) : ApkInstaller {

    override fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    override fun openInstallPermissionSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun install(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, ApkMimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
