package ir.coffevista.vista_native.features.profile.settings.notifications

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import kotlinx.serialization.Serializable

/** Mirrors Flutter's authenticated `/me/notification-settings` contract. */
interface NotificationSettingsApi {
    @GET("v1/me/notification-settings")
    suspend fun getSettings(): Response<NotificationSettingsDto>

    @POST("v1/me/notification-settings")
    suspend fun updateSettings(
        @Body settings: NotificationSettingsDto,
    ): Response<Unit>
}

@Serializable
data class NotificationSettingsDto(
    val push_notifications: Boolean = true,
    val message_notifications: Boolean = true,
    val mention_notifications: Boolean = true,
    val like_notifications: Boolean = true,
    val comment_notifications: Boolean = true,
    val follow_notifications: Boolean = true,
    val story_notifications: Boolean = true,
    val suggest_notifications: Boolean = true,
    val show_message_preview: Boolean = true,
    val in_app_chat_sounds: Boolean = true,
    val sound_enabled: Boolean = true,
    val vibration_enabled: Boolean = true,
    val quiet_hours_enabled: Boolean = false,
    val quiet_hours_start: String = "22:00",
    val quiet_hours_end: String = "08:00",
) {
    val socialEnabled: Boolean
        get() = like_notifications && comment_notifications && follow_notifications && story_notifications

    fun withSocialEnabled(value: Boolean): NotificationSettingsDto = copy(
        like_notifications = value,
        comment_notifications = value,
        follow_notifications = value,
        story_notifications = value,
    )
}
