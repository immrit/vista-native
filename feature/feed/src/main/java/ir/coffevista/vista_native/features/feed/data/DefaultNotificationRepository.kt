package ir.coffevista.vista_native.features.feed.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultNotificationRepository @Inject constructor(
    private val api: NotificationApi,
) : NotificationRepository {
    private val mutableNotifications = MutableStateFlow<List<VistaNotification>>(emptyList())
    override val notifications: StateFlow<List<VistaNotification>> = mutableNotifications.asStateFlow()

    private val mutableIsLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = mutableIsLoading.asStateFlow()

    private val mutableHasMore = MutableStateFlow(true)
    override val hasMore: StateFlow<Boolean> = mutableHasMore.asStateFlow()

    override val unreadCount: Flow<Int> = notifications.map { items ->
        items.count { !it.isRead }
    }

    override val unreadCountsByFilter: Flow<Map<String, Int>> = notifications.map { items ->
        buildUnreadCounts(items)
    }

    private var page = 0
    private var nextAllowedFetchAtMillis = 0L

    override suspend fun refresh() {
        fetch(refresh = true)
    }

    override suspend fun fetchMore() {
        if (!mutableHasMore.value || mutableIsLoading.value) return
        fetch(refresh = false)
    }

    private suspend fun fetch(refresh: Boolean) {
        if (mutableIsLoading.value) return
        val now = System.currentTimeMillis()
        if (now < nextAllowedFetchAtMillis) return

        mutableIsLoading.value = true
        val previous = mutableNotifications.value
        if (refresh) {
            page = 0
            mutableHasMore.value = true
        }

        try {
            val response = api.getNotifications(limit = PageSize, offset = page * PageSize)
            val mapped = response.notifications.map(NotificationDto::toDomain)
            mutableNotifications.value = if (refresh) {
                mapped
            } else {
                val existing = previous.map { it.id }.toHashSet()
                previous + mapped.filterNot { it.id in existing }
            }
            mutableHasMore.value = response.hasMore
            if (response.hasMore) page += 1
        } catch (error: HttpException) {
            if (error.code() == 429) {
                nextAllowedFetchAtMillis = System.currentTimeMillis() + RateLimitCooldownMillis
            }
            if (refresh) mutableNotifications.value = previous
        } catch (_: Exception) {
            if (refresh) mutableNotifications.value = previous
        } finally {
            mutableIsLoading.value = false
        }
    }

    override suspend fun markAllAsRead() {
        val previous = mutableNotifications.value
        mutableNotifications.value = previous.map { it.copy(isRead = true) }
        runCatching { api.markAllAsRead() }.onFailure {
            mutableNotifications.value = previous
        }
    }

    override suspend fun markAsRead(notificationId: String) {
        if (notificationId.isBlank()) return
        markAsRead(listOf(notificationId))
    }

    override suspend fun markAsRead(notificationIds: List<String>) {
        val ids = notificationIds.filter(String::isNotBlank).toSet()
        if (ids.isEmpty()) return
        val previous = mutableNotifications.value
        mutableNotifications.value = previous.map { notification ->
            if (notification.id in ids) notification.copy(isRead = true) else notification
        }
        ids.forEach { id ->
            runCatching { api.markAsRead(id) }.onFailure {
                mutableNotifications.value = previous
                return
            }
        }
    }

    override suspend fun deleteAll() {
        val previous = mutableNotifications.value
        mutableNotifications.value = emptyList()
        runCatching { api.deleteAll() }.onFailure {
            mutableNotifications.value = previous
        }
    }

    override suspend fun deleteNotification(notificationId: String) {
        if (notificationId.isBlank()) return
        val previous = mutableNotifications.value
        mutableNotifications.value = previous.filterNot { it.id == notificationId }
        runCatching { api.deleteNotification(notificationId) }.onFailure {
            mutableNotifications.value = previous
        }
    }

    override suspend fun respondToFollowRequest(
        requesterId: String,
        accept: Boolean,
        notificationId: String?,
    ): FollowRequestActionResult {
        if (requesterId.isBlank()) {
            return FollowRequestActionResult(
                FollowRequestActionState.Failed,
                "شناسه درخواست نامعتبر است",
            )
        }
        return try {
            val response = api.respondToFollowRequest(
                FollowRequestRespondDto(requesterId = requesterId, accept = accept),
            )
            if (response.isSuccessful) {
                removeFollowRequest(requesterId, notificationId)
                FollowRequestActionResult(
                    FollowRequestActionState.Success,
                    response.body()?.message ?: if (accept) {
                        "درخواست با موفقیت پذیرفته شد"
                    } else {
                        "درخواست رد شد"
                    },
                )
            } else if (response.code() == 404) {
                removeFollowRequest(requesterId, notificationId)
                FollowRequestActionResult(
                    FollowRequestActionState.AlreadyHandled,
                    "این درخواست قبلا مدیریت شده است",
                )
            } else {
                FollowRequestActionResult(
                    FollowRequestActionState.Failed,
                    "خطا در پردازش درخواست: ${response.code()}",
                )
            }
        } catch (error: HttpException) {
            if (error.code() == 404) {
                removeFollowRequest(requesterId, notificationId)
                FollowRequestActionResult(
                    FollowRequestActionState.AlreadyHandled,
                    "این درخواست قبلا مدیریت شده است",
                )
            } else {
                FollowRequestActionResult(
                    FollowRequestActionState.Failed,
                    "خطا در پردازش درخواست: ${error.message()}",
                )
            }
        } catch (error: Exception) {
            FollowRequestActionResult(
                FollowRequestActionState.Failed,
                "خطا در پردازش درخواست: ${error.message.orEmpty()}",
            )
        }
    }

    private fun removeFollowRequest(requesterId: String, notificationId: String?) {
        mutableNotifications.value = mutableNotifications.value.filterNot { notification ->
            notification.id == notificationId ||
                (notification.canonicalType == "follow_request" && notification.senderId == requesterId)
        }
    }

    private fun buildUnreadCounts(items: List<VistaNotification>): Map<String, Int> {
        val counts = NotificationFilter.entries.associate { it.type to 0 }.toMutableMap()
        items.forEach { notification ->
            if (notification.isRead) return@forEach
            counts[NotificationFilter.All.type] = counts.getValue(NotificationFilter.All.type) + 1
            NotificationFilter.entries
                .filterNot { it == NotificationFilter.All }
                .forEach { filter ->
                    if (notificationTypeMatchesFilter(notification.type, filter.type)) {
                        counts[filter.type] = counts.getValue(filter.type) + 1
                    }
                }
        }
        return counts
    }

    private companion object {
        const val PageSize = 20
        const val RateLimitCooldownMillis = 30_000L
    }
}
