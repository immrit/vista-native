package ir.coffevista.vista_native.features.profile.settings.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.datastore.PrivacySettingsCache
import ir.coffevista.vista_native.core.datastore.PrivacySettingsSnapshot
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.core.security.BiometricAuthenticator
import ir.coffevista.vista_native.core.security.BiometricAvailability
import ir.coffevista.vista_native.core.security.SessionStore
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacySettingsUiState(
    val settings: PrivacySettingsDto = PrivacySettingsDto(),
    val isLoading: Boolean = true,
    val hasLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val biometricLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val api: PrivacySettingsApi,
    private val cache: PrivacySettingsCache,
    private val authenticationStateProvider: AuthenticationStateProvider,
    private val sessionStore: SessionStore,
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PrivacySettingsUiState())
    val uiState: StateFlow<PrivacySettingsUiState> = mutableUiState.asStateFlow()

    init {
        refreshBiometricPolicy()
        refresh()
    }

    private fun refreshBiometricPolicy() {
        mutableUiState.update {
            it.copy(
                biometricEnabled = sessionStore.isBiometricEnabled(),
                biometricAvailable = biometricAuthenticator.availability() == BiometricAvailability.AVAILABLE,
                biometricLoading = false,
            )
        }
    }

    fun setBiometricEnabled(activity: FragmentActivity, enabled: Boolean) {
        if (!enabled) {
            runCatching { sessionStore.setBiometricEnabled(false) }
                .onSuccess { mutableUiState.update { state -> state.copy(biometricEnabled = false) } }
                .onFailure { mutableUiState.update { state -> state.copy(errorMessage = "غیرفعال‌سازی ورود بیومتریک ناموفق بود") } }
            return
        }
        if (biometricAuthenticator.availability() != BiometricAvailability.AVAILABLE) {
            mutableUiState.update { it.copy(errorMessage = "بیومتریک یا قفل دستگاه در دسترس نیست") }
            return
        }
        mutableUiState.update { it.copy(biometricLoading = true, errorMessage = null) }
        biometricAuthenticator.authenticate(activity) { authenticated ->
            if (authenticated) {
                runCatching { sessionStore.setBiometricEnabled(true) }
                    .onSuccess { mutableUiState.update { state -> state.copy(biometricEnabled = true, biometricLoading = false) } }
                    .onFailure { mutableUiState.update { state -> state.copy(biometricLoading = false, errorMessage = "ذخیرهٔ امن ورود بیومتریک ناموفق بود") } }
            } else {
                mutableUiState.update { state -> state.copy(biometricLoading = false, errorMessage = "تایید هویت بیومتریک ناموفق بود") }
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
        val response = runCatching { api.getSettings() }.getOrNull()
        val settings = response?.body()
        if (response?.isSuccessful == true && settings != null) {
            currentOwnerId()?.let { ownerId ->
                runCatching { cache.write(ownerId, settings.toSnapshot()) }
            }
            mutableUiState.update { it.copy(settings = settings, isLoading = false, hasLoaded = true) }
            return@launch
        }

        val cached = currentOwnerId()?.let { ownerId ->
            runCatching { cache.read(ownerId) }.getOrNull()?.toDto()
        }
        if (cached != null) {
            mutableUiState.update {
                it.copy(
                    settings = cached,
                    isLoading = false,
                    hasLoaded = true,
                    errorMessage = "تنظیمات ذخیره‌شده نمایش داده می‌شوند؛ اتصال برقرار نیست",
                )
            }
        } else {
            mutableUiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = if (response == null) "اتصال برای دریافت تنظیمات حریم خصوصی برقرار نشد" else "دریافت تنظیمات حریم خصوصی ناموفق بود",
                )
            }
        }
    }

    fun update(transform: (PrivacySettingsDto) -> PrivacySettingsDto) = save(transform(mutableUiState.value.settings))

    fun setPrivate(value: Boolean) = save(
        updated = mutableUiState.value.settings.copy(is_private = value),
        updatePrivateAccount = true,
    )

    private fun save(updated: PrivacySettingsDto, updatePrivateAccount: Boolean = false) {
        val previous = mutableUiState.value.settings
        mutableUiState.update { it.copy(settings = updated, isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val success = runCatching {
                api.updateSettings(updated).isSuccessful &&
                    (!updatePrivateAccount || api.updatePrivateAccount(PrivateAccountRequestDto(updated.is_private)).isSuccessful)
            }.getOrDefault(false)
            if (success) {
                currentOwnerId()?.let { ownerId ->
                    runCatching { cache.write(ownerId, updated.toSnapshot()) }
                }
                mutableUiState.update { it.copy(isSaving = false) }
            } else {
                mutableUiState.update {
                    it.copy(
                        settings = previous,
                        isSaving = false,
                        errorMessage = if (updatePrivateAccount) {
                            "تغییر حالت حساب خصوصی ثبت نشد. دوباره تلاش کنید"
                        } else {
                            "ذخیرهٔ تنظیمات حریم خصوصی ناموفق بود"
                        },
                    )
                }
            }
        }
    }

    private fun currentOwnerId(): String? =
        (authenticationStateProvider.state.value as? AuthenticationState.SignedIn)
            ?.context
            ?.userId
            ?.takeIf(String::isNotBlank)
}

private fun PrivacySettingsDto.toSnapshot() = PrivacySettingsSnapshot(
    isPrivate = is_private,
    lastSeenVisibility = last_seen_visibility,
    messagePrivacy = message_privacy,
    groupAddPrivacy = group_add_privacy,
    readReceipts = read_receipts,
    allowProfileZoom = allow_profile_zoom,
)

private fun PrivacySettingsSnapshot.toDto() = PrivacySettingsDto(
    is_private = isPrivate,
    last_seen_visibility = lastSeenVisibility,
    message_privacy = messagePrivacy,
    group_add_privacy = groupAddPrivacy,
    read_receipts = readReceipts,
    allow_profile_zoom = allowProfileZoom,
)
