package app.twicast.tv.core.di

import android.content.res.Resources
import app.twicast.tv.core.device.AndroidDeviceProfile
import app.twicast.tv.core.device.DeviceProfile
import app.twicast.tv.data.repository.AppearanceSettingsRepository
import app.twicast.tv.data.repository.AuthRepository
import app.twicast.tv.data.repository.CategoriesRepository
import app.twicast.tv.data.repository.ChatRepository
import app.twicast.tv.data.repository.TwitchIrcChatRepository
import app.twicast.tv.data.repository.ChannelRepository
import app.twicast.tv.data.repository.DataStoreAppearanceSettingsRepository
import app.twicast.tv.data.repository.DataStoreOptimizationSettingsRepository
import app.twicast.tv.data.repository.DataStoreLiveNotificationStateRepository
import app.twicast.tv.data.repository.DataStoreSearchHistoryRepository
import app.twicast.tv.data.repository.DataStoreWatchHistoryRepository
import app.twicast.tv.data.repository.DataStoreWatchProgressRepository
import app.twicast.tv.data.repository.DataStorePlayerSettingsRepository
import app.twicast.tv.data.repository.FollowsRepository
import app.twicast.tv.data.repository.HelixCategoriesRepository
import app.twicast.tv.data.repository.HelixChannelRepository
import app.twicast.tv.data.repository.HelixFollowsRepository
import app.twicast.tv.data.repository.HelixSearchRepository
import app.twicast.tv.data.repository.HelixStreamsRepository
import app.twicast.tv.data.repository.LanguageSettingsRepository
import app.twicast.tv.data.repository.OptimizationSettingsRepository
import app.twicast.tv.data.repository.LiveNotificationStateRepository
import app.twicast.tv.data.repository.SearchHistoryRepository
import app.twicast.tv.data.repository.WatchHistoryRepository
import app.twicast.tv.data.repository.WatchProgressRepository
import app.twicast.tv.data.repository.GqlPlaybackRepository
import app.twicast.tv.data.repository.PlaybackRepository
import app.twicast.tv.data.repository.PlayerSettingsRepository
import app.twicast.tv.data.repository.SearchRepository
import app.twicast.tv.data.repository.SharedPreferencesLanguageSettingsRepository
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
    abstract fun bindAppearanceSettingsRepository(impl: DataStoreAppearanceSettingsRepository): AppearanceSettingsRepository

    @Binds
    abstract fun bindPlaybackRepository(impl: GqlPlaybackRepository): PlaybackRepository

    @Binds
    abstract fun bindChatRepository(impl: TwitchIrcChatRepository): ChatRepository

    @Binds
    abstract fun bindLiveNotificationStateRepository(impl: DataStoreLiveNotificationStateRepository): LiveNotificationStateRepository

    @Binds
    abstract fun bindSearchHistoryRepository(impl: DataStoreSearchHistoryRepository): SearchHistoryRepository

    @Binds
    abstract fun bindWatchHistoryRepository(impl: DataStoreWatchHistoryRepository): WatchHistoryRepository

    @Binds
    abstract fun bindWatchProgressRepository(impl: DataStoreWatchProgressRepository): WatchProgressRepository

    @Binds
    abstract fun bindOptimizationSettingsRepository(impl: DataStoreOptimizationSettingsRepository): OptimizationSettingsRepository

    @Binds
    abstract fun bindLanguageSettingsRepository(impl: SharedPreferencesLanguageSettingsRepository): LanguageSettingsRepository

    @Binds
    abstract fun bindDeviceProfile(impl: AndroidDeviceProfile): DeviceProfile

    companion object {
        /**
         * The TV's own locale: picks the "popular in your region" language. Not Locale.getDefault(), which
         * follows the interface language chosen in Settings.
         */
        @Provides
        fun provideLocale(): Locale = Resources.getSystem().configuration.locales[0]
    }
}
