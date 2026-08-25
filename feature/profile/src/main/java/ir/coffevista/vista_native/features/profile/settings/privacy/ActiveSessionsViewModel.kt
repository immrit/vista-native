package ir.coffevista.vista_native.features.profile.settings.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.fragment.app.FragmentActivity
import ir.coffevista.vista_native.core.security.BiometricAuthenticator
import ir.coffevista.vista_native.core.security.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActiveSessionsUiState(
    val sessions: List<SessionItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val feedbackMessage: String? = null,
)

@HiltViewModel
class ActiveSessionsViewModel @Inject constructor(
    private val api: ActiveSessionsApi,
    private val sessionStore: SessionStore,
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ActiveSessionsUiState())
    val uiState: StateFlow<ActiveSessionsUiState> = mutableUiState.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { api.getActiveSessions() }
            .onSuccess { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    mutableUiState.update {
                        it.copy(sessions = body.sessions.map(::toUi), isLoading = false)
                    }
                } else {
                    mutableUiState.update { it.copy(isLoading = false, errorMessage = "دریافت نشست‌های فعال ناموفق بود") }
                }
            }
            .onFailure {
                mutableUiState.update { it.copy(isLoading = false, errorMessage = "اتصال برای دریافت نشست‌های فعال برقرار نشد") }
            }
    }

    fun terminate(session: SessionItemUiModel) {
        if (session.isCurrent) return
        mutate(listOf(session))
    }

    fun terminateAllOther() {
        val otherSessions = mutableUiState.value.sessions.filterNot(SessionItemUiModel::isCurrent)
        if (otherSessions.isEmpty()) return
        mutate(otherSessions)
    }

    fun authorizeSensitiveAction(activity: FragmentActivity, onApproved: () -> Unit) {
        if (!sessionStore.isBiometricEnabled()) {
            onApproved()
            return
        }
        biometricAuthenticator.authenticate(activity) { authenticated ->
            if (authenticated) {
                onApproved()
            } else {
                mutableUiState.update { it.copy(feedbackMessage = "تایید هویت بیومتریک ناموفق بود") }
            }
        }
    }

    private fun mutate(targets: List<SessionItemUiModel>) = viewModelScope.launch {
        mutableUiState.update { it.copy(isMutating = true, feedbackMessage = null) }
        val terminatedIds = targets.mapNotNull { session ->
            runCatching { api.terminate(TerminateSessionRequestDto(session.id)) }
                .getOrNull()
                ?.takeIf { response -> response.isSuccessful && (response.body()?.success ?: true) }
                ?.let { session.id }
        }.toSet()
        mutableUiState.update { state ->
            val failures = targets.size - terminatedIds.size
            state.copy(
                sessions = state.sessions.filterNot { it.id in terminatedIds },
                isMutating = false,
                feedbackMessage = if (failures == 0) {
                    if (targets.size == 1) "نشست با موفقیت خاتمه یافت" else "تمام نشست‌های دیگر خاتمه یافتند"
                } else {
                    "برخی نشست‌ها خاتمه نیافتند. دوباره تلاش کنید"
                },
            )
        }
    }

    private fun toUi(session: ActiveSessionDto): SessionItemUiModel = SessionItemUiModel(
        id = session.id,
        deviceName = session.deviceName?.takeIf(String::isNotBlank) ?: "دستگاه ناشناس",
        platform = session.platform?.lowercase().orEmpty(),
        ip = session.ipAddress.orEmpty(),
        location = listOfNotNull(session.locationCity, session.locationCountry).joinToString("، ").ifBlank { "مکان نامشخص" },
        lastActive = session.lastActivity?.takeIf(String::isNotBlank) ?: "زمان نامشخص",
        isCurrent = session.isCurrentSession,
    )
}
