package app.twicast.tv

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

private const val ImageCrossfadeMillis = 150

@HiltAndroidApp
class TwiCastApplication : Application(), SingletonImageLoader.Factory {

    // Lazy: nothing network-related is built in onCreate; images share the API's connection pool.
    @Inject
    lateinit var okHttpClient: Lazy<OkHttpClient>

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() })) }
            .crossfade(ImageCrossfadeMillis)
            .build()
}
