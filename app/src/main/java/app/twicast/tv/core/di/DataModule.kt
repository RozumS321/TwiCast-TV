package app.twicast.tv.core.di

import app.twicast.tv.core.device.AndroidDeviceProfile
import app.twicast.tv.core.device.DeviceProfile
import app.twicast.tv.data.repository.AuthRepository
import app.twicast.tv.data.repository.CategoriesRepository
import app.twicast.tv.data.repository.ChannelRepository
import app.twicast.tv.data.repository.DataStorePlayerSettingsRepository
import app.twicast.tv.data.repository.FollowsRepository
import app.twicast.tv.data.repository.HelixCategoriesRepository
import app.twicast.tv.data.repository.HelixChannelRepository
import app.twicast.tv.data.repository.HelixFollowsRepository
import app.twicast.tv.data.repository.HelixSearchRepository
import app.twicast.tv.data.repository.HelixStreamsRepository
import app.twicast.tv.data.repository.GqlPlaybackRepository
import app.twicast.tv.data.repository.PlaybackRepository
import app.twicast.tv.data.repository.PlayerSettingsRepository
import app.twicast.tv.data.repository.SearchRepository
import app.twicast.tv.data.repository.StreamsRepository
import app.twicast.tv.data.repository.TwitchAuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Locale

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindStreamsRepository(impl: HelixStreamsRepository): StreamsRepository

    @Binds
    abstract fun bindCategoriesRepository(impl: HelixCategoriesRepository): CategoriesRepository

    @Binds
    abstract fun bindAuthRepository(impl: TwitchAuthRepository): AuthRepository

    @Binds
    abstract fun bindChannelRepository(impl: HelixChannelRepository): ChannelRepository

    @Binds
    abstract fun bindFollowsRepository(impl: HelixFollowsRepository): FollowsRepository

    @Binds
    abstract fun bindSearchRepository(impl: HelixSearchRepository): SearchRepository

    @Binds
    abstract fun bindPlayerSettingsRepository(impl: DataStorePlayerSettingsRepository): PlayerSettingsRepository

    @Binds
    abstract fun bindPlaybackRepository(impl: GqlPlaybackRepository): PlaybackRepository

    @Binds
    abstract fun bindDeviceProfile(impl: AndroidDeviceProfile): DeviceProfile

    companion object {
        /** Device locale: picks the "popular in your region" language. */
        @Provides
        fun provideLocale(): Locale = Locale.getDefault()
    }
}
