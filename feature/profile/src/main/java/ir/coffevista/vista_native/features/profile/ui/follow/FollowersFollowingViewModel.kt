package ir.coffevista.vista_native.features.profile.ui.follow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.features.profile.data.FollowActionRequestDto
import ir.coffevista.vista_native.features.profile.data.FollowUserDto
import ir.coffevista.vista_native.features.profile.data.PublicProfileApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FollowUiState(
    val selectedTab: Int = 0, // 0 = Followers, 1 = Following
    val followers: List<FollowUserDto> = emptyList(),
    val following: List<FollowUserDto> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val filteredFollowers: List<FollowUserDto>
        get() = if (searchQuery.isBlank()) followers
        else followers.filter {
            it.username.contains(searchQuery, ignoreCase = true) ||
                it.fullName?.contains(searchQuery, ignoreCase = true) == true
        }

    val filteredFollowing: List<FollowUserDto>
        get() = if (searchQuery.isBlank()) following
        else following.filter {
            it.username.contains(searchQuery, ignoreCase = true) ||
                it.fullName?.contains(searchQuery, ignoreCase = true) == true
        }
}

@HiltViewModel
class FollowersFollowingViewModel @Inject constructor(
    private val api: PublicProfileApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowUiState())
    val uiState: StateFlow<FollowUiState> = _uiState.asStateFlow()

    private var targetUserId: String = ""

    fun bind(userId: String, initialTab: Int = 0) {
        targetUserId = userId
        _uiState.update { it.copy(selectedTab = initialTab) }
        loadData()
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun loadData() {
        if (targetUserId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val followersResp = api.getFollowers(targetUserId)
                val followingResp = api.getFollowing(targetUserId)

                val followers = followersResp.body()?.users ?: emptyList()
                val following = followingResp.body()?.users ?: emptyList()

                _uiState.update {
                    it.copy(
                        followers = followers,
                        following = following,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "خطا در دریافت لیست",
                    )
                }
            }
        }
    }

    fun toggleFollow(user: FollowUserDto) {
        viewModelScope.launch {
            val isFollowing = user.isFollowing
            val newStatus = !isFollowing

            // Optimistic update
            updateUserFollowState(user.userId, newStatus)

            try {
                if (newStatus) {
                    api.follow(FollowActionRequestDto(targetUserId = user.userId))
                } else {
                    api.unfollow(FollowActionRequestDto(targetUserId = user.userId))
                }
            } catch (_: Exception) {
                // Revert on error
                updateUserFollowState(user.userId, isFollowing)
            }
        }
    }

    private fun updateUserFollowState(userId: String, isFollowing: Boolean) {
        _uiState.update { state ->
            val updatedFollowers = state.followers.map {
                if (it.userId == userId) it.copy(isFollowing = isFollowing) else it
            }
            val updatedFollowing = state.following.map {
                if (it.userId == userId) it.copy(isFollowing = isFollowing) else it
            }
            state.copy(followers = updatedFollowers, following = updatedFollowing)
        }
    }
}
