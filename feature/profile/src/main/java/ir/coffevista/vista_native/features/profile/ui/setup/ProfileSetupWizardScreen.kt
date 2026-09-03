package ir.coffevista.vista_native.features.profile.ui.setup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.features.profile.data.OwnProfileRepository
import ir.coffevista.vista_native.features.profile.data.ProfileAvatarUploader
import ir.coffevista.vista_native.features.profile.data.ProfileUpdateRequestDto
import ir.coffevista.vista_native.features.profile.ui.components.JalaliDatePickerDialog
import ir.coffevista.vista_native.features.profile.ui.components.gregorianIsoToJalaliDisplay
import ir.coffevista.vista_native.features.profile.ui.components.jalaliToGregorianIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val username: String = "",
    val fullName: String = "",
    val bio: String = "",
    val birthDate: String = "",
    val gender: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val websiteUrl: String = "",
    val maritalStatus: String = "prefer_not_to_say",
    val avatarUri: Uri? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val ownProfileRepository: OwnProfileRepository,
    private val avatarUploader: ProfileAvatarUploader,
    private val authenticationStateProvider: AuthenticationStateProvider,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = (authenticationStateProvider.state.value as? AuthenticationState.SignedIn)
                ?.context
                ?.userId
                ?: return@launch
            ownProfileRepository.fetchAndCacheOwnProfile(userId)
            ownProfileRepository.getOwnProfileFlow(userId).first()?.let { profile ->
                mutableState.update {
                    it.copy(
                        username = profile.username.orEmpty(),
                        fullName = profile.fullName,
                        bio = profile.bio.orEmpty(),
                        birthDate = profile.birthDate
                            ?.let(::gregorianIsoToJalaliDisplay)
                            .orEmpty(),
                        gender = profile.gender.orEmpty(),
                        email = profile.email.orEmpty(),
                        phoneNumber = profile.phoneNumber.orEmpty(),
                        websiteUrl = profile.websiteUrl.orEmpty(),
                        maritalStatus = profile.maritalStatus.orEmpty().ifBlank { "prefer_not_to_say" },
                    )
                }
            }
        }
    }

    fun change(transform: (SetupUiState) -> SetupUiState) = mutableState.update { transform(it).copy(error = null) }

    fun complete(onCompleted: () -> Unit) {
        val draft = state.value
        val userId = (authenticationStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId
        val username = draft.username.trim().lowercase()
        val birthDate = jalaliToGregorianIso(draft.birthDate)
        if (!USERNAME.matches(username) || draft.fullName.trim().length < 2 || draft.gender.isBlank() || birthDate == null) {
            mutableState.update { it.copy(error = "نام کاربری، نام، تاریخ تولد و جنسیت را کامل کنید") }
            return
        }
        if (userId.isNullOrBlank() || draft.isSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true, error = null) }
            val avatarResult = draft.avatarUri?.let { uri -> runCatching { avatarUploader.upload(userId, uri) } }
            val avatarUrl = avatarResult?.getOrNull()?.url
            if (avatarResult?.isFailure == true) {
                mutableState.update { it.copy(isSaving = false, error = "آپلود آواتار ناموفق بود") }
                return@launch
            }
            when (val result = ownProfileRepository.updateOwnProfile(
                ProfileUpdateRequestDto(
                    username = username,
                    fullName = draft.fullName.trim(),
                    bio = draft.bio.trim(),
                    email = draft.email,
                    phoneNumber = draft.phoneNumber,
                    websiteUrl = draft.websiteUrl,
                    birthDate = birthDate,
                    gender = draft.gender,
                    maritalStatus = draft.maritalStatus,
                    showEmail = false,
                    showBirthDate = false,
                    showGender = false,
                    showMaritalStatus = false,
                ),
            )) {
                is Outcome.Failure -> mutableState.update { it.copy(isSaving = false, error = result.error.messageFa) }
                is Outcome.Success -> {
                    if (avatarUrl != null) {
                        when (val avatarUpdate = ownProfileRepository.updateAvatar(avatarUrl)) {
                            is Outcome.Failure -> {
                                mutableState.update { it.copy(isSaving = false, error = avatarUpdate.error.messageFa) }
                                return@launch
                            }
                            is Outcome.Success -> Unit
                        }
                    }
                    sessionStore.markProfileCompleted()
                    mutableState.update { it.copy(isSaving = false) }
                    onCompleted()
                }
            }
        }
    }

    private companion object {
        val USERNAME = Regex("[a-zA-Z0-9_]{3,30}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupWizardScreen(onCompleted: () -> Unit) {
    val viewModel: ProfileSetupViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableIntStateOf(0) }
    var isBirthDatePickerVisible by rememberSaveable { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.change { it.copy(avatarUri = uri) }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(topBar = { TopAppBar(title = { Text("تکمیل ثبت‌نام") }) }) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("مرحله ${page + 1} از ۲", color = MaterialTheme.colorScheme.primary)
                if (page == 0) {
                    Text("خودتان را معرفی کنید", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = { value -> viewModel.change { it.copy(username = value) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("نام کاربری") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.fullName,
                        onValueChange = { value -> viewModel.change { it.copy(fullName = value) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("نام و نام خانوادگی") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.bio,
                        onValueChange = { value -> viewModel.change { it.copy(bio = value) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("بیو") },
                        minLines = 3,
                    )
                    Button(onClick = { page = 1 }, modifier = Modifier.fillMaxWidth()) { Text("ادامه") }
                } else {
                    Text("اطلاعات پایه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = { isBirthDatePickerVisible = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = state.birthDate.ifBlank { "انتخاب تاریخ تولد شمسی" },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    listOf("male" to "مرد", "female" to "زن", "prefer_not_to_say" to "ترجیح می‌دهم نگویم").forEach { (value, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.gender == value, onClick = { viewModel.change { it.copy(gender = value) } })
                            Text(label)
                        }
                    }
                    state.avatarUri?.let {
                        AsyncImage(model = it, contentDescription = "آواتار انتخاب‌شده", modifier = Modifier.height(120.dp).fillMaxWidth())
                    }
                    OutlinedButton(
                        onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("انتخاب آواتار")
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = { viewModel.complete(onCompleted) },
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("تکمیل ثبت‌نام")
                        }
                    }
                    OutlinedButton(onClick = { page = 0 }, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                        Text("مرحله قبل")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        if (isBirthDatePickerVisible) {
            JalaliDatePickerDialog(
                initialValue = state.birthDate,
                onDismissRequest = { isBirthDatePickerVisible = false },
                onDateSelected = { date ->
                    viewModel.change { it.copy(birthDate = date.displayValue) }
                    isBirthDatePickerVisible = false
                },
            )
        }
    }
}
