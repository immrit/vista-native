package ir.coffevista.vista_native.debug

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import ir.coffevista.vista_native.features.profile.data.OwnProfileApiFixture
import ir.coffevista.vista_native.features.profile.data.ProfileDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugOwnProfileApiFixture @Inject constructor() : OwnProfileApiFixture {
    private var scenario: String? = null

    override fun configure(rawScenario: String?) {
        scenario = rawScenario
    }

    override suspend fun profileOrNull(): ProfileDto? = when (scenario) {
        "valid-session" -> ProfileDto(
            userId = "fnd-debug-user",
            username = "fixture",
            fullName = "کاربر تست Foundation",
            bio = "نمایه آزمایشی برای سنجش تطابق دیداری و رفتار آفلاین ویستا",
            avatarUrl = null,
            isVerified = true,
            verificationType = "identity",
            accountType = "premium",
            isPrivate = false,
            postCount = 1,
            followerCount = 18,
            followingCount = 7,
            joinOrder = 128,
            subscriptionPlan = "premium",
            premiumDaysRemaining = 30,
            messagePrivacy = "everyone",
            allowProfileZoom = true,
            updatedAt = "2026-07-29T10:00:00Z",
        )
        "offline-valid-session", "feed-error" ->
            throw IOException("debug own profile offline")
        else -> null
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DebugOwnProfileApiFixtureModule {
    @Provides
    @IntoSet
    fun provideDebugOwnProfileApiFixture(
        fixture: DebugOwnProfileApiFixture,
    ): OwnProfileApiFixture = fixture
}
