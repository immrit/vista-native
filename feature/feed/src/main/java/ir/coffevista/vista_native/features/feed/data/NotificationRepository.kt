package ir.coffevista.vista_native.features.feed.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface NotificationRepository {
    val notifications: StateFlow<List<VistaNotification>>
    val isLoading: StateFlow<Boolean>
    val hasMore: StateFlow<Boolean>
    val unreadCount: Flow<Int>
    val unreadCountsByFilter: Flow<Map<String, Int>>

    suspend fun refresh()
    suspend fun fetchMore()
    suspend fun markAllAsRead()
    suspend fun markAsRead(notificationId: String)
    suspend fun markAsRead(notificationIds: List<String>)
    suspend fun deleteAll()
    suspend fun deleteNotification(notificationId: String)
    suspend fun respondToFollowRequest(
        requesterId: String,
        accept: Boolean,
        notificationId: String?,
    ): FollowRequestActionResult
}
