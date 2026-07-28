package ir.coffevista.vista_native.debug

import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import ir.coffevista.vista_native.core.database.profile.PublicProfileDao
import ir.coffevista.vista_native.features.profile.data.FollowActionResponseDto
import ir.coffevista.vista_native.features.profile.data.PublicProfileApiFixture
import ir.coffevista.vista_native.features.profile.data.PublicProfileDto
import ir.coffevista.vista_native.features.profile.data.UnfollowResponseDto
import ir.coffevista.vista_native.features.profile.data.UserProfileRepository
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugPublicProfileApiFixture @Inject constructor() : PublicProfileApiFixture {
    @Volatile
    private var scenario: String? = null
    private val relationship = ConcurrentHashMap<String, String>()
    private val failNextMutation = AtomicBoolean(false)

    override fun configure(rawScenario: String?) {
        if (scenario != rawScenario) {
            relationship.clear()
            failNextMutation.set(false)
        }
        scenario = rawScenario
    }

    override suspend fun profileOrNull(userId: String): PublicProfileDto? = when (scenario) {
        "valid-session" -> profile(userId)
        "offline-valid-session" -> throw IOException("debug public profile offline")
        else -> null
    }

    override suspend fun followOrNull(targetUserId: String): FollowActionResponseDto? {
        if (scenario != "valid-session") return null
        failIfRequested()
        val status = if (targetUserId == PRIVATE_PROFILE_ID) "requested" else "following"
        relationship[targetUserId] = status
        return FollowActionResponseDto(
            status = status,
            message = if (status == "requested") {
                "درخواست دنبال کردن ارسال شد"
            } else {
                "با موفقیت دنبال کردید"
            },
        )
    }

    override suspend fun unfollowOrNull(targetUserId: String): UnfollowResponseDto? {
        if (scenario != "valid-session") return null
        failIfRequested()
        relationship[targetUserId] = "none"
        return UnfollowResponseDto(status = "unfollowed")
    }

    fun failNextMutation() {
        failNextMutation.set(true)
    }

    private fun failIfRequested() {
        if (failNextMutation.compareAndSet(true, false)) {
            throw IOException("debug follow mutation offline")
        }
    }

    private fun profile(userId: String): PublicProfileDto {
        val suffix = userId.substringAfterLast('-').toIntOrNull() ?: 1
        val private = userId == PRIVATE_PROFILE_ID
        return PublicProfileDto(
            userId = userId,
            username = "fixture$suffix",
            fullName = "کاربر آزمایشی $suffix",
            bio = if (private) "نمایه خصوصی آزمایشی" else "نمایه عمومی آزمایشی",
            avatarUrl = null,
            isVerified = suffix == 3,
            verificationType = if (suffix == 3) "blueTick" else null,
            isPrivate = private,
            isBlocked = false,
            subscriptionPlan = if (suffix == 2) "premium" else null,
            premiumDaysRemaining = if (suffix == 2) 30 else null,
            postCount = suffix.toLong() * 5,
            followerCount = 40L + suffix,
            followingCount = 10L + suffix,
            followStatus = relationship[userId] ?: "none",
            updatedAt = "2026-07-28T09:30:00Z",
        )
    }

    private companion object {
        const val PRIVATE_PROFILE_ID = "fixture-author-3"
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DebugPublicProfileApiFixtureModule {
    @Provides
    @IntoSet
    fun provideDebugPublicProfileApiFixture(
        fixture: DebugPublicProfileApiFixture,
    ): PublicProfileApiFixture = fixture
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DebugPublicProfileRuntimeEntryPoint {
    fun userProfileRepository(): UserProfileRepository
    fun publicProfileDao(): PublicProfileDao
    fun debugPublicProfileApiFixture(): DebugPublicProfileApiFixture
}
