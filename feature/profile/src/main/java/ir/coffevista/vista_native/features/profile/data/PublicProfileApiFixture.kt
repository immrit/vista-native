package ir.coffevista.vista_native.features.profile.data

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Debug/androidTest extension point. Release contributes no implementation.
 */
interface PublicProfileApiFixture {
    fun configure(rawScenario: String?)
    suspend fun profileOrNull(userId: String): PublicProfileDto?
    suspend fun followOrNull(targetUserId: String): FollowActionResponseDto?
    suspend fun unfollowOrNull(targetUserId: String): UnfollowResponseDto?
}

@Module
@InstallIn(SingletonComponent::class)
interface PublicProfileApiFixtureMultibindings {
    @Multibinds
    fun fixtures(): Set<PublicProfileApiFixture>
}
