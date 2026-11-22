package app.twicast.tv

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

// Images fade in softly over their skeleton instead of popping in.
private const val ImageCrossfadeMillis = 300

/**
 * Decoded images kept in memory. Coil's default (a share of the app's heap) kept growing with every row scrolled;
 * 32 MB held barely one screen, so coming back to a row decoded its previews again right while it scrolled in.
 * 48 MB keeps a few screens; the rest comes back from the disk cache.
 */
private const val ImageMemoryCacheBytes = 48L * 1024 * 1024

@HiltAndroidApp
class TwiCastApplication : Application(), SingletonImageLoader.Factory {

    // Lazy: nothing network-related is built in onCreate; images share the API's connection pool.
    @Inject
    lateinit var okHttpClient: Lazy<OkHttpClient>

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() })) }
            .crossfade(ImageCrossfadeMillis)
            .memoryCache { MemoryCache.Builder().maxSizeBytes(ImageMemoryCacheBytes).build() }
            .build()
}
