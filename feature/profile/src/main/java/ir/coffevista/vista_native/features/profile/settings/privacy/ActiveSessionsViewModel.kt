package ir.coffevista.vista_native.features.profile.settings.privacy

import android.content.Context
import android.os.Build
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.common.DeviceMetadataProvider
import ir.coffevista.vista_native.core.common.SessionCredentialsProvider
import ir.coffevista.vista_native.core.security.BiometricAuthenticator
import ir.coffevista.vista_native.core.security.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

data class ActiveSessionsUiState(
    val sessions: List<SessionItemUiModel> = emptyList(),
    val autoTerminateDays: Int = 180,
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val isSavingSettings: Boolean = false,
    val isApprovingQr: Boolean = false,
    val errorMessage: String? = null,
    val feedbackMessage: String? = null,
)

@HiltViewModel
class ActiveSessionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ActiveSessionsApi,
    private val lifecycleManager: ActiveSessionLifecycleManager,
    private val sessionStore: SessionStore,
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {
    private val prefs = context.getSharedPreferences(SessionCredentialsProvider.PREFS_NAME, Context.MODE_PRIVATE)
    private val mutableUiState = MutableStateFlow(ActiveSessionsUiState())
    val uiState: StateFlow<ActiveSessionsUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun clearFeedback() {
        mutableUiState.update { it.copy(feedbackMessage = null) }
    }

    fun refresh() = viewModelScope.launch {
        mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
        val currentSessionId = lifecycleManager.ensureCurrentSessionRegistered(sessionStore.read()?.userId)
            ?: currentSessionIdFallback()
        runCatching { api.getActiveSessions(currentSessionId = currentSessionId) }
            .onSuccess { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    mutableUiState.update {
                        it.copy(
                            sessions = body.sessions.map(::toUi),
                            autoTerminateDays = body.autoTerminateDays.takeIf { d -> d > 0 } ?: 180,
                            isLoading = false,
                        )
                    }
                } else {
                    mutableUiState.update {
                        it.copy(isLoading = false, errorMessage = "دریافت نشست‌های فعال ناموفق بود")
                    }
                }
            }
            .onFailure {
                mutableUiState.update {
                    it.copy(isLoading = false, errorMessage = "اتصال برای دریافت نشست‌های فعال برقرار نشد")
                }
            }
    }

    fun terminate(session: SessionItemUiModel, onDone: () -> Unit = {}) {
        if (session.isCurrent) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isMutating = true, feedbackMessage = null) }
            val currentSessionId = currentSessionIdFallback()
            val response = runCatching {
                api.terminate(
                    TerminateSessionRequestDto(
                        targetSessionId = session.id,
                        currentSessionId = currentSessionId,
                    ),
                )
            }.getOrNull()

            if (response != null && response.isSuccessful && (response.body()?.success ?: true)) {
                mutableUiState.update { state ->
                    state.copy(
                        sessions = state.sessions.filterNot { it.id == session.id },
                        isMutating = false,
                        feedbackMessage = "نشست «${session.deviceName}» با موفقیت خاتمه یافت",
                    )
                }
                onDone()
            } else {
                val errMsg = parseApiErrorMessage(response?.errorBody()?.string())
                    ?: "خاتمه نشست انجام نشد. دوباره تلاش کنید"
                mutableUiState.update { it.copy(isMutating = false, feedbackMessage = errMsg) }
            }
        }
    }

    fun approvePendingSession(session: SessionItemUiModel, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isMutating = true, feedbackMessage = null) }
            val currentSessionId = currentSessionIdFallback()
            val response = runCatching {
                api.alertAction(
                    SessionAlertActionRequestDto(
                        sessionId = session.id,
                        action = "approve",
                        currentSessionId = currentSessionId,
                    ),
                )
            }.getOrNull()

            if (response != null && response.isSuccessful && response.body()?.success == true) {
                mutableUiState.update { state ->
                    state.copy(
                        sessions = state.sessions.map {
                            if (it.id == session.id) it.copy(approvalStatus = "approved") else it
                        },
                        isMutating = false,
                        feedbackMessage = "ورود «${session.deviceName}» تایید شد",
                    )
                }
                onDone()
            } else {
                mutableUiState.update {
                    it.copy(isMutating = false, feedbackMessage = "تایید نشست انجام نشد")
                }
            }
        }
    }

    fun terminateAllOther() {
        val otherSessions = mutableUiState.value.sessions.filterNot(SessionItemUiModel::isCurrent)
        if (otherSessions.isEmpty()) return
        val currentSessionId = currentSessionIdFallback()
        if (currentSessionId.isNullOrBlank()) {
            mutableUiState.update {
                it.copy(feedbackMessage = "شناسه نشست فعلی هنوز ثبت نشده است")
            }
            return
        }
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
                        feedbackMessage = "تمام نشست‌های دیگر بلافاصله خاتمه یافتند",
                    )
                }
            } else {
                val errMsg = parseApiErrorMessage(res?.errorBody()?.string())
                    ?: "خاتمه نشست‌های دیگر انجام نشد"
                mutableUiState.update { it.copy(isMutating = false, feedbackMessage = errMsg) }
            }
        }
    }

    fun updateAutoTerminateDays(days: Int) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSavingSettings = true, feedbackMessage = null) }
            val res = runCatching {
                api.updateSettings(UpdateSessionSettingsRequestDto(autoTerminateDays = days))
            }.getOrNull()
            if (res != null && res.isSuccessful) {
                val saved = res.body()?.autoTerminateDays ?: days
                mutableUiState.update {
                    it.copy(
                        autoTerminateDays = saved,
                        isSavingSettings = false,
                        feedbackMessage = "زمان خاتمه خودکار روی «${formatAutoTerminateLabel(saved)}» تنظیم شد",
                    )
                }
            } else {
                mutableUiState.update {
                    it.copy(isSavingSettings = false, feedbackMessage = "ذخیره تنظیمات انجام نشد")
                }
            }
        }
    }

    fun approveQrToken(rawQrContent: String, onSuccess: () -> Unit = {}) {
        val cleanedToken = extractQrLoginToken(rawQrContent)
        if (cleanedToken.isBlank()) {
            mutableUiState.update { it.copy(feedbackMessage = "کد ورود نامعتبر است") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isApprovingQr = true, feedbackMessage = null) }
            val res = runCatching {
                api.approveQr(ApproveQrLoginRequestDto(token = cleanedToken))
            }.getOrNull()
            if (res != null && res.isSuccessful && res.body()?.success == true) {
                mutableUiState.update {
                    it.copy(
                        isApprovingQr = false,
                        feedbackMessage = "دستگاه جدید با موفقیت متصل شد",
                    )
                }
                onSuccess()
                refresh()
            } else {
                val errMsg = parseApiErrorMessage(res?.errorBody()?.string())
                    ?: "کد QR نامعتبر یا منقضی شده است"
                mutableUiState.update {
                    it.copy(isApprovingQr = false, feedbackMessage = errMsg)
                }
            }
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

    private fun currentSessionIdFallback(): String? =
        SessionCredentialsProvider.current?.currentSessionId()
            ?: prefs.getString(SessionCredentialsProvider.KEY_SESSION_ID, null)?.takeIf(String::isNotBlank)
            ?: mutableUiState.value.sessions.firstOrNull(SessionItemUiModel::isCurrent)?.id

    private fun toUi(session: ActiveSessionDto): SessionItemUiModel {
        val rawPlatform = session.platform?.lowercase()?.trim().orEmpty()
        val osVersion = session.deviceInfo["os_version"]?.jsonPrimitive?.content?.trim().orEmpty()
        val appVer = session.appVersion?.trim().orEmpty()
        val appCode = session.appVersionCode?.takeIf { it > 0 }
        val subtitle = buildClientSubtitle(rawPlatform, appVer, appCode, osVersion)
        val appVersionDisplay = buildAppVersionDisplay(rawPlatform, appVer, appCode)
        val primaryPlace = session.locationCity?.cleanPlaceToken()
            ?: session.locationRegion?.cleanPlaceToken()
        val country = session.locationCountry?.cleanPlaceToken()
        val location = listOfNotNull(primaryPlace, country)
            .distinct()
            .joinToString("، ")
            .ifBlank { "موقعیت نامشخص" }

        return SessionItemUiModel(
            id = session.id,
            deviceName = resolveDisplayDeviceName(session, rawPlatform),
            clientSubtitle = subtitle,
            appVersionDisplay = appVersionDisplay,
            platform = rawPlatform,
            ip = session.ipAddress.orEmpty(),
            location = location,
            lastActiveText = if (session.isCurrentSession) "آنلاین" else formatRelativeOrTime(session.lastActivity),
            createdAtText = formatFullDateTime(session.createdAt),
            approvalStatus = session.approvalStatus.ifBlank { "approved" },
            isCurrent = session.isCurrentSession,
        )
    }

    private fun resolveDisplayDeviceName(session: ActiveSessionDto, platform: String): String {
        val rawName = session.deviceName?.trim().orEmpty()
        if (rawName.isNotBlank() && !isGenericDeviceName(rawName)) {
            return rawName
        }
        val manufacturer = session.deviceInfo["manufacturer"]?.jsonPrimitive?.content?.trim().orEmpty()
        val model = session.deviceInfo["device_model"]?.jsonPrimitive?.content?.trim().orEmpty()
            .ifBlank { session.deviceInfo["model"]?.jsonPrimitive?.content?.trim().orEmpty() }
        val brand = session.deviceInfo["brand"]?.jsonPrimitive?.content?.trim().orEmpty()
        if (model.isNotBlank() && !isGenericDeviceName(model)) {
            return if (platform == "android") {
                DeviceMetadataProvider.formatAndroidDeviceName(manufacturer, model, brand)
            } else {
                model
            }
        }
        val browser = session.deviceInfo["browser"]?.jsonPrimitive?.content?.trim().orEmpty()
        val osName = session.deviceInfo["os_name"]?.jsonPrimitive?.content?.trim().orEmpty()
        if (browser.isNotBlank() && !isGenericDeviceName(browser)) {
            return if (osName.isNotBlank()) "$browser ($osName)" else browser
        }
        if (session.isCurrentSession) {
            return DeviceMetadataProvider.current?.metadata()?.deviceName
                ?.takeIf { it.isNotBlank() && !isGenericDeviceName(it) }
                ?: DeviceMetadataProvider.formatAndroidDeviceName(Build.MANUFACTURER, Build.MODEL, Build.BRAND)
        }
        return rawName.takeIf(String::isNotBlank) ?: "دستگاه ناشناس"
    }

    private fun isGenericDeviceName(value: String): Boolean {
        val normalized = value.trim().lowercase()
        return normalized.isBlank() ||
            normalized == "vista android" ||
            normalized == "vista web" ||
            normalized == "vista (web)" ||
            normalized == "vista (android)" ||
            normalized == "vista desktop" ||
            normalized == "vista ios" ||
            normalized == "vista app" ||
            normalized == "android" ||
            normalized == "web"
    }

    private fun buildAppVersionDisplay(platform: String, appVersion: String, appVersionCode: Int?): String {
        if (platform == "web" && (appVersion.isBlank() || appVersion.equals("web", ignoreCase = true))) {
            return "نسخه وب"
        }
        return when {
            appVersion.isNotBlank() && appVersionCode != null -> "v$appVersion (کد $appVersionCode)"
            appVersion.isNotBlank() -> "v$appVersion"
            appVersionCode != null -> "کد $appVersionCode"
            else -> "نامشخص"
        }
    }

    private fun buildClientSubtitle(platform: String, appVersion: String, appVersionCode: Int?, osVersion: String): String {
        val base = when (platform) {
            "android" -> "Vista Android"
            "ios" -> "Vista iOS"
            "windows", "macos", "linux", "desktop" -> "Vista Desktop"
            "web" -> "Vista Web"
            else -> "Vista App"
        }
        val cleanAppVersion = appVersion.takeUnless { it.equals("web", ignoreCase = true) }.orEmpty()
        val verText = when {
            cleanAppVersion.isNotBlank() && appVersionCode != null -> "$cleanAppVersion ($appVersionCode)"
            cleanAppVersion.isNotBlank() -> cleanAppVersion
            else -> ""
        }
        return when {
            verText.isNotBlank() && osVersion.isNotBlank() -> "$base $verText • OS $osVersion"
            verText.isNotBlank() -> "$base $verText"
            osVersion.isNotBlank() -> "$base • $osVersion"
            else -> base
        }
    }

    private fun parseApiErrorMessage(rawBody: String?): String? {
        if (rawBody.isNullOrBlank()) return null
        return runCatching {
            val obj = JSONObject(rawBody)
            obj.optJSONObject("error")?.optString("message")?.takeIf(String::isNotBlank)
                ?: obj.optString("message").takeIf(String::isNotBlank)
        }.getOrNull()
    }
}

internal fun formatAutoTerminateLabel(days: Int): String = when (days) {
    7 -> "۱ هفته"
    30 -> "۱ ماه"
    90 -> "۳ ماه"
    180 -> "۶ ماه"
    365 -> "۱ سال"
    else -> "$days روز"
}

private fun formatRelativeOrTime(rawIso: String?): String {
    if (rawIso.isNullOrBlank()) return "نامشخص"
    return runCatching {
        val instant = Instant.parse(rawIso)
        val now = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(instant, now).coerceAtLeast(0)
        when {
            minutes < 2 -> "آنلاین"
            minutes < 60 -> "$minutes دقیقه پیش"
            minutes < 24 * 60 -> {
                val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
                DateTimeFormatter.ofPattern("HH:mm", Locale.US).format(localTime)
            }
            else -> {
                val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.US).format(localDate)
            }
        }
    }.getOrElse { rawIso }
}

private fun formatFullDateTime(rawIso: String?): String {
    if (rawIso.isNullOrBlank()) return "نامشخص"
    return runCatching {
        val instant = Instant.parse(rawIso)
        val zoned = instant.atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("HH:mm • yyyy/MM/dd", Locale.US).format(zoned)
    }.getOrElse { rawIso }
}

private val IPV4_PATTERN = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
private val IPV6_PATTERN = Regex("""^[0-9a-fA-F:.\[\]]+$""")

private fun String.cleanPlaceToken(): String? {
    val trimmed = trim()
    if (trimmed.isEmpty()) return null
    if (IPV4_PATTERN.matches(trimmed) || (trimmed.contains(':') && IPV6_PATTERN.matches(trimmed))) {
        return null
    }
    return trimmed
}

private val QR_TOKEN_PARAM_REGEX = Regex("""(?:^|[?&])token=([^&\s]+)""", RegexOption.IGNORE_CASE)

private fun extractQrLoginToken(raw: String): String {
    val cleaned = raw.trim().trim('"', '\'').trim()
    if (cleaned.isEmpty()) return ""
    val match = QR_TOKEN_PARAM_REGEX.find(cleaned) ?: return cleaned
    val encoded = match.groupValues.getOrNull(1)?.trim().orEmpty()
    if (encoded.isEmpty()) return cleaned
    return runCatching {
        java.net.URLDecoder.decode(encoded, "UTF-8").trim()
    }.getOrDefault(encoded)
}
