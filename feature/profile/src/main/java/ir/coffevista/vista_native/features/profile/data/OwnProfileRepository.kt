package ir.coffevista.vista_native.features.profile.data

import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity
import kotlinx.coroutines.flow.Flow

interface OwnProfileRepository {
    fun getOwnProfileFlow(userId: String): Flow<OwnProfileEntity?>
suspend fun fetchAndCacheOwnProfile(userId: String): Outcome<Unit>
suspend fun updateOwnProfile(request: ProfileUpdateRequestDto): Outcome<Unit>
suspend fun updateAvatar(avatarUrl: String): Outcome<Unit>
suspend fun clearProfileData()
}
