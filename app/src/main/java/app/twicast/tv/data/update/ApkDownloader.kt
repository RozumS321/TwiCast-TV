package app.twicast.tv.data.update

import android.content.Context
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.runCatchingRemote
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

interface ApkDownloader {
    /** Downloads the release APK; [onProgress] gets (downloaded, total) bytes. Cancellable. */
    suspend fun download(release: AppRelease, onProgress: (Long, Long) -> Unit): AppResult<File>
}

/** Directory shared with the installer through FileProvider (res/xml/update_paths.xml). */
const val UpdatesDirectory = "updates"
private const val BufferSize = 64 * 1024

@Singleton
class OkHttpApkDownloader @Inject constructor(
    private val client: OkHttpClient,
    @ApplicationContext private val context: Context,
) : ApkDownloader {

    override suspend fun download(release: AppRelease, onProgress: (Long, Long) -> Unit): AppResult<File> =
        withContext(Dispatchers.IO) {
            runCatchingRemote {
                val directory = File(context.cacheDir, UpdatesDirectory).apply { mkdirs() }
                // Old APKs are useless once a newer one is downloaded.
                directory.listFiles()?.forEach { it.delete() }
                val target = File(directory, "twicast-tv-${release.versionName}.apk")
                val partial = File(directory, target.name + ".part")
                client.newCall(Request.Builder().url(release.apkUrl).build()).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val body = response.body
                    val total = body.contentLength().takeIf { it > 0 } ?: release.apkSizeBytes
                    body.byteStream().use { input ->
                        partial.outputStream().use { output ->
                            val buffer = ByteArray(BufferSize)
                            var downloaded = 0L
                            while (true) {
                                coroutineContext.ensureActive()
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                onProgress(downloaded, total)
                            }
                        }
                    }
                }
                if (!partial.renameTo(target)) throw IOException("Could not save the update")
                target
            }
        }
}
