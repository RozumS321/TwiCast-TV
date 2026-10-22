package app.twicast.tv.core.di

import app.twicast.tv.BuildConfig
import app.twicast.tv.core.network.Clock
import app.twicast.tv.core.network.HelixAuthInterceptor
import app.twicast.tv.core.network.HelixTokenAuthenticator
import app.twicast.tv.core.network.TwitchClientId
import app.twicast.tv.data.remote.HelixApi
import app.twicast.tv.data.remote.TwitchOAuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val HelixBaseUrl = "https://api.twitch.tv/helix/"
private const val OAuthBaseUrl = "https://id.twitch.tv/oauth2/"
private const val TimeoutSeconds = 15L

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @TwitchClientId
    fun provideClientId(): String = BuildConfig.TWITCH_CLIENT_ID

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock { System.currentTimeMillis() }

    /** Base client shared by Retrofit services and Coil (one connection pool). */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(TimeoutSeconds, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                // BASIC: never log headers or bodies — they carry tokens.
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideOAuthApi(client: OkHttpClient, json: Json): TwitchOAuthApi = Retrofit.Builder()
        .baseUrl(OAuthBaseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TwitchOAuthApi::class.java)

    @Provides
    @Singleton
    fun provideHelixApi(
        client: OkHttpClient,
        json: Json,
        authInterceptor: HelixAuthInterceptor,
        authenticator: HelixTokenAuthenticator,
    ): HelixApi = Retrofit.Builder()
        .baseUrl(HelixBaseUrl)
        .client(client.newBuilder().addInterceptor(authInterceptor).authenticator(authenticator).build())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(HelixApi::class.java)
}
