package ir.coffevista.vista_native.features.profile.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.coffevista.vista_native.core.database.profile.OwnProfileDao
import ir.coffevista.vista_native.core.network.InternalApi
import ir.coffevista.vista_native.features.profile.settings.notifications.NotificationSettingsApi
import ir.coffevista.vista_native.features.profile.settings.privacy.PrivacySettingsApi
import ir.coffevista.vista_native.features.profile.settings.privacy.BlockedUsersApi
import ir.coffevista.vista_native.features.profile.settings.privacy.ActiveSessionsApi
import ir.coffevista.vista_native.features.profile.settings.password.ChangePasswordApi
import ir.coffevista.vista_native.features.profile.settings.saved.SavedPostsApi
import ir.coffevista.vista_native.features.profile.settings.about.ContactRequestsApi
import ir.coffevista.vista_native.features.profile.settings.pricing.PricingPlansApi
import ir.coffevista.vista_native.features.profile.settings.about.SupportConversationApi
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProfileDataModule {
    @Provides
    @Singleton
    fun provideNotificationSettingsApi(
        @InternalApi retrofit: Retrofit,
    ): NotificationSettingsApi = retrofit.create(NotificationSettingsApi::class.java)

    @Provides
    @Singleton
    fun providePrivacySettingsApi(
        @InternalApi retrofit: Retrofit,
    ): PrivacySettingsApi = retrofit.create(PrivacySettingsApi::class.java)

    @Provides
    @Singleton
    fun provideBlockedUsersApi(
        @InternalApi retrofit: Retrofit,
    ): BlockedUsersApi = retrofit.create(BlockedUsersApi::class.java)

    @Provides
    @Singleton
    fun provideActiveSessionsApi(
        @InternalApi retrofit: Retrofit,
    ): ActiveSessionsApi = retrofit.create(ActiveSessionsApi::class.java)

    @Provides
    @Singleton
    fun provideChangePasswordApi(
        @InternalApi retrofit: Retrofit,
    ): ChangePasswordApi = retrofit.create(ChangePasswordApi::class.java)

    @Provides
    @Singleton
    fun provideSavedPostsApi(
        @InternalApi retrofit: Retrofit,
    ): SavedPostsApi = retrofit.create(SavedPostsApi::class.java)

    @Provides
    @Singleton
    fun provideContactRequestsApi(
        @InternalApi retrofit: Retrofit,
    ): ContactRequestsApi = retrofit.create(ContactRequestsApi::class.java)

    @Provides
    @Singleton
    fun providePricingPlansApi(
        @InternalApi retrofit: Retrofit,
    ): PricingPlansApi = retrofit.create(PricingPlansApi::class.java)

    @Provides
    @Singleton
    fun provideSupportConversationApi(
        @InternalApi retrofit: Retrofit,
    ): SupportConversationApi = retrofit.create(SupportConversationApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(
        @InternalApi retrofit: Retrofit,
        fixtures: Set<@JvmSuppressWildcards OwnProfileApiFixture>,
    ): ProfileApi {
        val remote = retrofit.create(ProfileApi::class.java)
        return object : ProfileApi {
            override suspend fun fetchOwnProfile(): retrofit2.Response<ProfileDto> {
                fixtures.forEach { fixture ->
                    fixture.profileOrNull()?.let {
                        return retrofit2.Response.success(it)
                    }
                }
                return remote.fetchOwnProfile()
            }

            override suspend fun updateOwnProfile(
                request: ProfileUpdateRequestDto,
            ): retrofit2.Response<ProfileDto> = remote.updateOwnProfile(request)

            override suspend fun updateAvatar(request: ProfileAvatarUpdateRequestDto): retrofit2.Response<ProfileDto> = remote.updateAvatar(request)
            override suspend fun presignUpload(request: ProfileMediaPresignRequestDto): ProfileMediaPresignResponseDto = remote.presignUpload(request)
            override suspend fun deleteUpload(request: ProfileMediaDeleteRequestDto): ProfileMediaDeleteResponseDto = remote.deleteUpload(request)
        }
    }

    @Provides
    @Singleton
    fun provideOwnProfileRepository(
        api: ProfileApi,
        dao: OwnProfileDao
    ): OwnProfileRepository = OfflineFirstOwnProfileRepository(api, dao)

    @Provides
    @Singleton
    fun providePublicProfileApi(
        @InternalApi retrofit: Retrofit,
        fixtures: Set<@JvmSuppressWildcards PublicProfileApiFixture>,
    ): PublicProfileApi {
        val remote = retrofit.create(PublicProfileApi::class.java)
        return object : PublicProfileApi {
            override suspend fun fetchPublicProfile(
                userId: String,
            ): retrofit2.Response<PublicProfileDto> {
                fixtures.forEach { fixture ->
                    fixture.profileOrNull(userId)?.let {
                        return retrofit2.Response.success(it)
                    }
                }
                return remote.fetchPublicProfile(userId)
            }

            override suspend fun follow(
                request: FollowActionRequestDto,
            ): retrofit2.Response<FollowActionResponseDto> {
                fixtures.forEach { fixture ->
                    fixture.followOrNull(request.targetUserId)?.let {
                        return retrofit2.Response.success(it)
                    }
                }
                return remote.follow(request)
            }

            override suspend fun unfollow(
                request: FollowActionRequestDto,
            ): retrofit2.Response<UnfollowResponseDto> {
                fixtures.forEach { fixture ->
                    fixture.unfollowOrNull(request.targetUserId)?.let {
                        return retrofit2.Response.success(it)
                    }
                }
                return remote.unfollow(request)
            }

            override suspend fun getFollowers(
                userId: String,
                limit: Int,
                offset: Int,
            ): retrofit2.Response<FollowListResponseDto> = remote.getFollowers(userId, limit, offset)

            override suspend fun getFollowing(
                userId: String,
                limit: Int,
                offset: Int,
            ): retrofit2.Response<FollowListResponseDto> = remote.getFollowing(userId, limit, offset)
        }
    }

    @Provides
    @Singleton
    fun provideUserProfileRepository(
        api: PublicProfileApi,
        dao: ir.coffevista.vista_native.core.database.profile.PublicProfileDao,
    ): UserProfileRepository = OfflineFirstUserProfileRepository(api, dao)
}
