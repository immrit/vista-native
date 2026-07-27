package ir.coffevista.vista_native.features.profile.data

import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.database.profile.OwnProfileDao
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class OfflineFirstOwnProfileRepositoryTest {

    private val fakeDao = FakeOwnProfileDao()
    private val fakeApi = FakeProfileApi()
    
    private val repository = OfflineFirstOwnProfileRepository(api = fakeApi, dao = fakeDao)

    @Test
    fun fetchAndCacheOwnProfile_success_updatesDatabase() = runTest {
        val dto = ProfileDto(
            userId = "test-user-id",
            fullName = "کاربر تستی",
            username = "test_user",
            bio = "این یک تست است",
            postCount = 5,
            followerCount = 10,
            followingCount = 2,
            isVerified = true
        )
        fakeApi.response = Response.success(dto)

        val result = repository.fetchAndCacheOwnProfile("test-user-id")
        
        assertTrue(result is Outcome.Success)
        val cachedEntity = fakeDao.getOwnProfile("test-user-id").first()
        assertEquals("test-user-id", cachedEntity?.userId)
        assertEquals("کاربر تستی", cachedEntity?.fullName)
        assertEquals("test_user", cachedEntity?.username)
        assertEquals("این یک تست است", cachedEntity?.bio)
        assertEquals(5L, cachedEntity?.postCount)
        assertEquals(10L, cachedEntity?.followerCount)
        assertEquals(2L, cachedEntity?.followingCount)
        assertEquals(true, cachedEntity?.isVerified)
    }

    @Test
    fun fetchAndCacheOwnProfile_networkError_returnsFailure_doesNotClearCache() = runTest {
        // Pre-populate cache
        fakeDao.insertOrUpdate(OwnProfileEntity(
            userId = "test-user-id",
            username = "old_user",
            fullName = "Old Name",
            bio = null,
            avatarUrl = null,
            isVerified = false,
            accountType = null,
            postCount = 0,
            followerCount = 0,
            followingCount = 0,
            updatedAt = null
        ))
        
        fakeApi.response = Response.error(500, "Server Error".toResponseBody(null))

        val result = repository.fetchAndCacheOwnProfile("test-user-id")
        
        assertTrue(result is Outcome.Failure)
        
        // Cache must still exist
        val cachedEntity = fakeDao.getOwnProfile("test-user-id").first()
        assertEquals("Old Name", cachedEntity?.fullName)
    }

    @Test
    fun clearProfileData_clearsCache() = runTest {
        fakeDao.insertOrUpdate(OwnProfileEntity(
            userId = "test-user-id",
            username = "name_user",
            fullName = "Name",
            bio = null,
            avatarUrl = null,
            isVerified = false,
            accountType = null,
            postCount = 0,
            followerCount = 0,
            followingCount = 0,
            updatedAt = null
        ))
        
        repository.clearProfileData()
        
        val cachedEntity = fakeDao.getOwnProfile("test-user-id").first()
        assertEquals(null, cachedEntity)
    }
}

class FakeOwnProfileDao : OwnProfileDao {
    private val _db = MutableStateFlow<Map<String, OwnProfileEntity>>(emptyMap())

    override fun getOwnProfile(userId: String): Flow<OwnProfileEntity?> {
        return _db.map { it[userId] }
    }

    override suspend fun insertOrUpdate(profile: OwnProfileEntity) {
        _db.update { it + (profile.userId to profile) }
    }

    override suspend fun deleteProfile(userId: String) {
        _db.update { it - userId }
    }

    override suspend fun deleteAll() {
        _db.value = emptyMap()
    }
}

class FakeProfileApi : ProfileApi {
    var response: Response<ProfileDto>? = null
    var shouldThrow: Exception? = null

    override suspend fun fetchOwnProfile(): Response<ProfileDto> {
        if (shouldThrow != null) throw shouldThrow!!
        return response ?: error("Response not setup")
    }
}
