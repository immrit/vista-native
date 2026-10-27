package ir.coffevista.vista_native.features.profile.settings.privacy

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.fragment.app.FragmentActivity
import ir.coffevista.vista_native.core.common.DeviceIdProvider
import ir.coffevista.vista_native.core.security.BiometricAuthenticator
import ir.coffevista.vista_native.core.security.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
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
    @ApplicationContext private val context: Context,
    private val api: ActiveSessionsApi,
    private val sessionStore: SessionStore,
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutableUiState = MutableStateFlow(ActiveSessionsUiState())
    val uiState: StateFlow<ActiveSessionsUiState> = mutableUiState.asStateFlow()

    init { refresh() }

    private suspend fun ensureCurrentSessionRegistered(): String? {
        var sessionId = prefs.getString(KEY_SESSION_ID, null)?.takeIf(String::isNotBlank)
        var sessionToken = prefs.getString(KEY_SESSION_TOKEN, null)?.takeIf(String::isNotBlank)

        if (sessionId != null && sessionToken != null) {
            val validated = runCatching {
                api.validate(ValidateSessionRequestDto(sessionId = sessionId!!, sessionToken = sessionToken!!))
            }.getOrNull()
            if (validated != null && validated.isSuccessful && validated.body()?.valid == true) {
                runCatching {
                    api.touch(TouchSessionRequestDto(sessionId = sessionId!!, sessionToken = sessionToken!!))
                }
                return sessionId
            }
        }

        val newSessionId = UUID.randomUUID().toString()
        val newSessionToken = UUID.randomUUID().toString()
        val deviceId = DeviceIdProvider.current?.deviceId().orEmpty()
        val manufacturer = runCatching { Build.MANUFACTURER }.getOrNull().orEmpty()
        val model = runCatching { Build.MODEL }.getOrNull() ?: "Android"
        val deviceName = listOf(manufacturer, model)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" ")
            .ifBlank { "Android Device" }
        val registered = runCatching {
            api.register(
                RegisterSessionRequestDto(
                    sessionId = newSessionId,
                    sessionToken = newSessionToken,
                    deviceInfo = buildMap {
                        if (deviceId.isNotBlank()) put("device_id", deviceId)
                        put("device_name", deviceName)
                        put("model", model)
                        if (manufacturer.isNotBlank()) put("manufacturer", manufacturer)
                        put("os_version", runCatching { Build.VERSION.RELEASE }.getOrNull() ?: "14")
                        put("platform", "android")
                    },
                    platform = "android",
                ),
            )
        }.getOrNull()

        if (registered != null && registered.isSuccessful) {
            prefs.edit()
                .putString(KEY_SESSION_ID, newSessionId)
                .putString(KEY_SESSION_TOKEN, newSessionToken)
                .apply()
            return newSessionId
        }
        return sessionId
    }

    fun refresh() = viewModelScope.launch {
        mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
        val currentSessionId = ensureCurrentSessionRegistered()
        runCatching { api.getActiveSessions(currentSessionId = currentSessionId) }
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
        val currentSessionId = prefs.getString(KEY_SESSION_ID, null)?.takeIf(String::isNotBlank)
            ?: mutableUiState.value.sessions.firstOrNull(SessionItemUiModel::isCurrent)?.id
        if (!currentSessionId.isNullOrBlank()) {
            viewModelScope.launch {
                mutableUiState.update { it.copy(isMutating = true, feedbackMessage = null) }
                val res = runCatching {
                    api.terminateOthers(TerminateOthersRequestDto(currentSessionId = currentSessionId))
                }.getOrNull()
                if (res != null && res.isSuccessful) {
                    mutableUiState.update { state ->
                        state.copy(
                            sessions = state.sessions.filter(SessionItemUiModel::isCurrent),
                            isMutating = false,
                            feedbackMessage = "تمام نشست‌های دیگر خاتمه یافتند",
                        )
                    }
                } else {
                    mutate(otherSessions)
                }
            }
        } else {
            mutate(otherSessions)
        }
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
        val currentSessionId = prefs.getString(KEY_SESSION_ID, null)?.takeIf(String::isNotBlank)
            ?: mutableUiState.value.sessions.firstOrNull(SessionItemUiModel::isCurrent)?.id
        val terminatedIds = targets.mapNotNull { session ->
            runCatching {
                api.terminate(
                    TerminateSessionRequestDto(
                        targetSessionId = session.id,
                        currentSessionId = currentSessionId,
                    ),
                )
            }
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

    companion object {
        const val PREFS_NAME = "vista_active_session_prefs"
        const val KEY_SESSION_ID = "current_session_id"
        const val KEY_SESSION_TOKEN = "current_session_token"
    }
}
