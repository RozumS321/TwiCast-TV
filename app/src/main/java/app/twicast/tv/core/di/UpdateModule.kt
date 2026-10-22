package app.twicast.tv.core.di

import app.twicast.tv.BuildConfig
import app.twicast.tv.data.remote.GitHubReleasesApi
import app.twicast.tv.data.update.ApkDownloader
import app.twicast.tv.data.update.ApkInstaller
import app.twicast.tv.data.update.GitHubUpdateRepository
import app.twicast.tv.data.update.OkHttpApkDownloader
import app.twicast.tv.data.update.SystemApkInstaller
import app.twicast.tv.data.update.UpdateRepository
import app.twicast.tv.feature.update.AppVersion
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

private const val GitHubApiBaseUrl = "https://api.github.com/"

@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateModule {

    @Binds
    abstract fun bindUpdateRepository(impl: GitHubUpdateRepository): UpdateRepository

    @Binds
    abstract fun bindApkDownloader(impl: OkHttpApkDownloader): ApkDownloader

    @Binds
    abstract fun bindApkInstaller(impl: SystemApkInstaller): ApkInstaller

    companion object {
        @Provides
        @Singleton
        fun provideGitHubReleasesApi(client: OkHttpClient, json: Json): GitHubReleasesApi = Retrofit.Builder()
            .baseUrl(GitHubApiBaseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubReleasesApi::class.java)

        @Provides
        fun provideAppVersion(): AppVersion =
            AppVersion(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, updatesEnabled = BuildConfig.UPDATES_ENABLED)
    }
}
