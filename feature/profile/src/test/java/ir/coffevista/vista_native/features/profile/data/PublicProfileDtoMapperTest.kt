package ir.coffevista.vista_native.features.profile.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicProfileDtoMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun successResponseMapsVerifiedPremiumAndRelationshipFields() {
        val dto = json.decodeFromString<PublicProfileDto>(
            """
            {
              "user_id":"user-b",
              "username":"vista",
              "full_name":"Vista User",
              "bio":"hello",
              "avatar_url":"https://example.test/avatar.jpg",
              "is_verified":true,
              "verification_type":"blueTick",
              "is_private":false,
              "is_blocked":false,
              "subscription_plan":"premium",
              "premium_days_remaining":20,
              "post_count":3,
              "follower_count":4,
              "following_count":5,
              "follow_status":"following",
              "updated_at":"2026-07-28T09:30:00.123456Z",
              "ignored_backend_field":"safe"
            }
            """.trimIndent(),
        )

        val profile = dto.toEntity("viewer-a", 99).toDomain()

        assertEquals("user-b", profile.userId)
        assertEquals(FollowState.Following, profile.followState)
        assertTrue(profile.isVerified)
        assertTrue(profile.isPremium)
        assertEquals(99, profile.lastSyncedEpochMillis)
    }

    @Test
    fun nullableFieldsRemainNullableAndAnonymousRelationshipDefaultsToNone() {
        val dto = json.decodeFromString<PublicProfileDto>(
            """
            {
              "user_id":"user-b",
              "full_name":"Vista User",
              "is_verified":false,
              "is_private":false,
              "is_blocked":false,
              "post_count":0,
              "follower_count":0,
              "following_count":0,
              "updated_at":"2026-07-28T09:30:00Z"
            }
            """.trimIndent(),
        )

        val profile = dto.toEntity("viewer-a", 1).toDomain()

        assertNull(profile.username)
        assertNull(profile.bio)
        assertNull(profile.avatarUrl)
        assertNull(profile.verificationType)
        assertEquals(FollowState.NotFollowing, profile.followState)
        assertFalse(profile.isPremium)
    }

    @Test
    fun privateRequestedStateMapsExactly() {
        val profile = dto(
            isPrivate = true,
            followStatus = "requested",
        ).toEntity("viewer-a", 1).toDomain()

        assertTrue(profile.isPrivate)
        assertEquals(FollowState.Requested, profile.followState)
    }

    @Test
    fun blockedProfileMakesRelationshipUnavailable() {
        val profile = dto(
            isBlocked = true,
            followStatus = "following",
        ).toEntity("viewer-a", 1).toDomain()

        assertEquals(FollowState.Unavailable, profile.followState)
    }

    @Test(expected = SerializationException::class)
    fun malformedRelationshipFieldIsRejected() {
        dto(followStatus = "guess").toEntity("viewer-a", 1)
    }

    @Test(expected = SerializationException::class)
    fun malformedEnvelopeMissingRequiredFieldsIsRejected() {
        json.decodeFromString<PublicProfileDto>(
            """{"code":"PROFILE_NOT_FOUND","message":"not found"}""",
        )
    }

    private fun dto(
        isPrivate: Boolean = false,
        isBlocked: Boolean = false,
        followStatus: String = "none",
    ) = PublicProfileDto(
        userId = "user-b",
        username = null,
        fullName = "Vista User",
        bio = null,
        avatarUrl = null,
        isVerified = false,
        verificationType = null,
        isPrivate = isPrivate,
        isBlocked = isBlocked,
        subscriptionPlan = null,
        premiumDaysRemaining = null,
        postCount = 0,
        followerCount = 0,
        followingCount = 0,
        followStatus = followStatus,
        updatedAt = "2026-07-28T09:30:00Z",
    )
}
