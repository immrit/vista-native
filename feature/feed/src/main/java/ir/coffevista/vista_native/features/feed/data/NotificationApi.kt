package ir.coffevista.vista_native.features.feed.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {
    @GET("v1/notifications")
    suspend fun getNotifications(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): NotificationsResponseDto

    @POST("v1/notifications/read-all")
    suspend fun markAllAsRead(): Response<Unit>

    @POST("v1/notifications/{notificationId}/read")
    suspend fun markAsRead(
        @Path("notificationId") notificationId: String,
    ): Response<Unit>

    @DELETE("v1/notifications")
    suspend fun deleteAll(): Response<Unit>

    @DELETE("v1/notifications/{notificationId}")
    suspend fun deleteNotification(
        @Path("notificationId") notificationId: String,
    ): Response<Unit>

    @POST("v1/me/follow-request/respond")
    suspend fun respondToFollowRequest(
        @Body body: FollowRequestRespondDto,
    ): Response<FollowRequestRespondResponseDto>
}
