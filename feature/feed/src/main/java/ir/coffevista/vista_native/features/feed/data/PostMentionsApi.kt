package ir.coffevista.vista_native.features.feed.data

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.Locale
import javax.inject.Inject

interface PostMentionsApi {
    @GET("v1/profiles/by-username/{username}")
    suspend fun profile(@Path("username") username: String): PostMentionProfileDto

    @POST("v1/posts/{postId}/mentions")
    suspend fun add(@Path("postId") postId: String, @Body request: PostMentionsRequestDto)
}

@Serializable
data class PostMentionProfileDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("id") val id: String? = null,
)

@Serializable
data class PostMentionsRequestDto(@SerialName("user_ids") val userIds: List<String>)

class PostMentions @Inject constructor(private val api: PostMentionsApi) {
    suspend fun resolve(usernames: List<String>): List<String> {
        val ids = linkedSetOf<String>()
        for (username in usernames.map { it.lowercase(Locale.ROOT) }.distinct()) {
            try {
                val profile = api.profile(username)
                (profile.userId ?: profile.id)?.trim()?.takeIf { it.isNotEmpty() }?.let(ids::add)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Flutter drops unknown/unavailable profiles; never log their identities.
            }
        }
        return ids.toList()
    }

    suspend fun attach(postId: String, userIds: List<String>) {
        if (postId.isBlank() || userIds.isEmpty()) return
        try {
            api.add(postId, PostMentionsRequestDto(userIds))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Creation already succeeded. Do not invite a retry that duplicates the post.
        }
    }
}
