package ir.coffevista.vista_native.features.services.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.features.services.data.nearby.NearbyCandidate
import ir.coffevista.vista_native.features.services.data.nearby.NearbyLikeResult
import ir.coffevista.vista_native.features.services.data.nearby.NearbyMatch
import ir.coffevista.vista_native.features.services.data.nearby.NearbyPreferences
import ir.coffevista.vista_native.features.services.data.nearby.NearbyReceivedLike
import ir.coffevista.vista_native.features.services.data.nearby.NearbyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NearbyUiState(
    val cards: List<NearbyCandidate> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val isLocating: Boolean = false,
    val locationError: String? = null,
    val isDisabled: Boolean = false,
    val isRandomOnline: Boolean = false,
    val receivedLikesCount: Int = 0,
    val preferences: NearbyPreferences = NearbyPreferences(),
    val isPreferencesSheetVisible: Boolean = false,
    val matchedResult: NearbyLikeResult? = null,
    val matchedCandidate: NearbyCandidate? = null,
    val zoneTransition: String? = null,
    val isLiking: Boolean = false,
    val errorMessage: String? = null,
    val receivedLikes: List<NearbyReceivedLike> = emptyList(),
    val matches: List<NearbyMatch> = emptyList(),
    val isLoadingLikesMatches: Boolean = false,
)

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val repository: NearbyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyUiState())
    val uiState: StateFlow<NearbyUiState> = _uiState.asStateFlow()

    private var currentZone: String? = null

    init {
        initBootstrap()
    }

    fun initBootstrap() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true, locationError = null, isDisabled = false) }
            runCatching {
                val prefs = repository.getPreferences()
                _uiState.update { it.copy(preferences = prefs) }
                if (prefs.hasLocation && !prefs.isEnabled) {
                    _uiState.update { it.copy(isLocating = false, isDisabled = true) }
                    return@launch
                }
            }
            loadCards(reset = true)
            loadReceivedLikesBadge()
        }
    }

    fun loadCards(reset: Boolean = false, setRandomOnline: Boolean? = null) {
        viewModelScope.launch {
            val isRandom = setRandomOnline ?: _uiState.value.isRandomOnline
            _uiState.update {
                it.copy(
                    isLoading = reset || it.cards.isEmpty(),
                    isRandomOnline = isRandom,
                    isLocating = false,
                    locationError = null,
                )
            }

            runCatching {
                if (isRandom) {
                    repository.discoverRandomOnline(limit = 20)
                } else {
                    val offset = if (reset) 0 else _uiState.value.cards.size
                    repository.discover(limit = 20, offset = offset)
                }
            }.onSuccess { candidates ->
                val newCards = if (reset) candidates else _uiState.value.cards + candidates
                _uiState.update {
                    it.copy(
                        cards = newCards,
                        currentIndex = if (reset) 0 else it.currentIndex,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (reset && it.cards.isEmpty()) "خطا در دریافت افراد نزدیک" else null,
                    )
                }
            }
        }
    }

    fun loadReceivedLikesBadge() {
        viewModelScope.launch {
            runCatching { repository.getLikesReceived(limit = 1) }
                .onSuccess { result ->
                    _uiState.update { it.copy(receivedLikesCount = result.count) }
                }

        }
    }

    fun loadLikesAndMatches() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLikesMatches = true) }
            val likesResult = runCatching { repository.getLikesReceived(limit = 50) }
            val matchesResult = runCatching { repository.getMatches() }

            val likes = likesResult.getOrNull()?.likes.orEmpty()
            val count = likesResult.getOrNull()?.count ?: 0
            val matches = matchesResult.getOrNull().orEmpty()

            _uiState.update {
                it.copy(
                    receivedLikes = likes,
                    matches = matches,
                    receivedLikesCount = count,
                    isLoadingLikesMatches = false,
                )
            }
        }
    }

    fun likeCurrent() {
        val state = _uiState.value
        if (state.isLiking || state.currentIndex < 0 || state.currentIndex >= state.cards.size) return
        val card = state.cards[state.currentIndex]

        viewModelScope.launch {
            _uiState.update { it.copy(isLiking = true) }
            runCatching { repository.like(card.userId, action = "like") }
                .onSuccess { result ->
                    _uiState.update { it.copy(isLiking = false) }
                    if (result.matched) {
                        _uiState.update {
                            it.copy(
                                matchedResult = result,
                                matchedCandidate = card,
                            )
                        }
                    } else {
                        advance(forward = true)
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLiking = false, errorMessage = "ثبت نشد، دوباره تلاش کن") }
                }
        }
    }

    fun passCurrent() {
        val state = _uiState.value
        if (state.currentIndex < 0 || state.currentIndex >= state.cards.size) return
        val card = state.cards[state.currentIndex]

        viewModelScope.launch {
            runCatching { repository.like(card.userId, action = "pass") }
            advance(forward = true)
        }
    }

    fun advance(forward: Boolean) {
        val state = _uiState.value
        val cards = state.cards
        if (cards.isEmpty()) return

        if (forward) {
            if (state.currentIndex < cards.size - 1) {
                val newIndex = state.currentIndex + 1
                val transition = detectZoneChange(cards[newIndex])
                _uiState.update {
                    it.copy(
                        currentIndex = newIndex,
                        zoneTransition = transition,
                    )
                }
                if (newIndex >= cards.size - 5) {
                    loadCards(reset = false)
                }
            }
        } else {
            if (state.currentIndex > 0) {
                val newIndex = state.currentIndex - 1
                val transition = detectZoneChange(cards[newIndex])
                _uiState.update {
                    it.copy(
                        currentIndex = newIndex,
                        zoneTransition = transition,
                    )
                }
            }
        }
    }

    private fun detectZoneChange(card: NearbyCandidate): String? {
        val key = if (card.cityLabel.isNotEmpty()) card.cityLabel else card.zoneType
        if (currentZone == null) {
            currentZone = key
            return null
        }
        if (currentZone == key) return null
        currentZone = key
        return if (card.cityLabel.isNotEmpty()) "افراد ${card.cityLabel}" else "افراد دورتر"
    }

    fun clearMatchDialog() {
        _uiState.update { it.copy(matchedResult = null, matchedCandidate = null) }
        advance(forward = true)
    }

    fun clearZoneTransition() {
        _uiState.update { it.copy(zoneTransition = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun setPreferencesSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isPreferencesSheetVisible = visible) }
    }

    fun updatePreferences(prefs: NearbyPreferences) {
        viewModelScope.launch {
            _uiState.update { it.copy(preferences = prefs, isPreferencesSheetVisible = false) }
            runCatching { repository.updatePreferences(prefs) }
            loadCards(reset = true)
        }
    }

    fun disableDiscovery() {
        viewModelScope.launch {
            runCatching { repository.disableLocation() }
            _uiState.update { it.copy(isDisabled = true, isPreferencesSheetVisible = false) }
        }
    }

    fun reportUser(targetId: String, reason: String) {
        viewModelScope.launch {
            runCatching { repository.report(targetId, reason) }
            advance(forward = true)
        }
    }

    fun unmatch(matchId: String) {
        viewModelScope.launch {
            runCatching { repository.unmatch(matchId) }
            loadLikesAndMatches()
        }
    }
}
