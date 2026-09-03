package ir.coffevista.vista_native.features.feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.features.feed.data.FollowRequestActionResult
import ir.coffevista.vista_native.features.feed.data.NotificationRepository
import ir.coffevista.vista_native.features.feed.data.VistaNotification
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<VistaNotification> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val unreadCountsByFilter: Map<String, Int> = emptyMap(),
    val actionLoadingIds: Set<String> = emptySet(),
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {
    private val actionLoadingIds = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())

    private val baseUiState = combine(
        repository.notifications,
        repository.isLoading,
        repository.hasMore,
        repository.unreadCountsByFilter,
    ) { notifications, isLoading, hasMore, unreadCounts ->
        NotificationsUiState(
            notifications = notifications,
            isLoading = isLoading,
            hasMore = hasMore,
            unreadCountsByFilter = unreadCounts,
        )
    }

    val uiState: StateFlow<NotificationsUiState> = combine(
        baseUiState,
        actionLoadingIds,
    ) { base, loadingIds ->
        base.copy(actionLoadingIds = loadingIds)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationsUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refresh()
        }
    }

    fun fetchMore() {
        viewModelScope.launch {
            repository.fetchMore()
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun markAsRead(notificationIds: List<String>) {
        viewModelScope.launch {
            repository.markAsRead(notificationIds)
        }
    }

    fun respondToFollowRequest(
        notification: VistaNotification,
        accept: Boolean,
        onResult: (FollowRequestActionResult) -> Unit,
    ) {
        if (notification.id in actionLoadingIds.value) return
        viewModelScope.launch {
            actionLoadingIds.value = actionLoadingIds.value + notification.id
            val result = repository.respondToFollowRequest(
                requesterId = notification.senderId.ifBlank {
                    notification.followerId ?: notification.metadata["follower_id"].orEmpty()
                },
                accept = accept,
                notificationId = notification.id,
            )
            actionLoadingIds.value = actionLoadingIds.value - notification.id
            onResult(result)
        }
    }
}
