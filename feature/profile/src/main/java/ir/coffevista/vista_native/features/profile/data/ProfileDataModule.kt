package ir.coffevista.vista_native.features.profile.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.coffevista.vista_native.core.database.profile.OwnProfileDao
import ir.coffevista.vista_native.core.network.InternalApi
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProfileDataModule {
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
        }
    }

    @Provides
    @Singleton
    fun provideUserProfileRepository(
        api: PublicProfileApi,
        dao: ir.coffevista.vista_native.core.database.profile.PublicProfileDao,
    ): UserProfileRepository = OfflineFirstUserProfileRepository(api, dao)
}
