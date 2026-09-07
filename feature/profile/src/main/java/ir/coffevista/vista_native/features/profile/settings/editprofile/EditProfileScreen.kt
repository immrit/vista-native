package ir.coffevista.vista_native.features.profile.settings.editprofile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import ir.coffevista.vista_native.features.profile.ui.components.JalaliDatePickerDialog
import ir.coffevista.vista_native.features.profile.ui.components.gregorianIsoToJalaliDisplay
import ir.coffevista.vista_native.features.profile.ui.components.jalaliToGregorianIso
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.features.profile.data.ProfileUpdateRequestDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profile: OwnProfileEntity?,
    onBack: () -> Unit,
    onSave: suspend (ProfileUpdateRequestDto) -> Outcome<Unit>,
    onAvatarSelected: suspend (Uri) -> Outcome<Unit>,
    onAvatarRemoved: suspend () -> Outcome<Unit>,
    modifier: Modifier = Modifier,
) {
    var username by rememberSaveable { mutableStateOf(profile?.username.orEmpty()) }
    var fullName by rememberSaveable { mutableStateOf(profile?.fullName ?: "") }
    var bio by rememberSaveable { mutableStateOf(profile?.bio ?: "") }
    var email by rememberSaveable { mutableStateOf(profile?.email.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(profile?.phoneNumber.orEmpty()) }
    var website by rememberSaveable { mutableStateOf(profile?.websiteUrl.orEmpty()) }
    var birthDate by rememberSaveable {
        mutableStateOf(
            profile?.birthDate?.let(::gregorianIsoToJalaliDisplay) ?: profile?.birthDate.orEmpty()
        )
    }
    var showBirthDatePicker by rememberSaveable { mutableStateOf(false) }
    var gender by rememberSaveable { mutableStateOf(profile?.gender.orEmpty()) }
    var maritalStatus by rememberSaveable { mutableStateOf(profile?.maritalStatus.orEmpty()) }
    var expandedGender by remember { mutableStateOf(false) }
    var expandedMaritalStatus by remember { mutableStateOf(false) }
    var showEmail by rememberSaveable { mutableStateOf(profile?.showEmail ?: false) }
    var showBirthDate by rememberSaveable { mutableStateOf(profile?.showBirthDate ?: false) }
    var showGender by rememberSaveable { mutableStateOf(profile?.showGender ?: false) }
    var showMaritalStatus by rememberSaveable { mutableStateOf(profile?.showMaritalStatus ?: false) }

    var hasInitialized by rememberSaveable { mutableStateOf(profile != null) }

    LaunchedEffect(profile) {
        if (profile != null && !hasInitialized) {
            username = profile.username.orEmpty()
            fullName = profile.fullName
            bio = profile.bio.orEmpty()
            email = profile.email.orEmpty()
            phone = profile.phoneNumber.orEmpty()
            website = profile.websiteUrl.orEmpty()
            birthDate = profile.birthDate?.let(::gregorianIsoToJalaliDisplay) ?: profile.birthDate.orEmpty()
            gender = profile.gender.orEmpty()
            maritalStatus = profile.maritalStatus.orEmpty()
            showEmail = profile.showEmail
            showBirthDate = profile.showBirthDate
            showGender = profile.showGender
            showMaritalStatus = profile.showMaritalStatus
            hasInitialized = true
        }
    }

    var isSaving by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var showAvatarOptions by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val genderOptions = listOf("male" to "مرد", "female" to "زن", "prefer_not_to_say" to "ترجیح می‌دهم نگویم")
    val maritalStatusOptions = listOf("single" to "مجرد", "married" to "متأهل", "prefer_not_to_say" to "ترجیح می‌دهم نگویم")
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            isUploadingAvatar = true
            when (val result = onAvatarSelected(uri)) {
                is Outcome.Success -> snackbarHostState.showSnackbar("تصویر پروفایل با موفقیت به‌روزرسانی شد")
                is Outcome.Failure -> snackbarHostState.showSnackbar(result.error.messageFa)
            }
            isUploadingAvatar = false
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LocalLayoutDirection.current) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "ویرایش پروفایل",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Avatar with Camera Icon Overlay
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !isUploadingAvatar) { showAvatarOptions = true },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!profile?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile?.avatarUrl,
                            contentDescription = "تصویر پروفایل",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(54.dp),
                        )
                    }

                    // Camera badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = "تغییر تصویر",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    if (isUploadingAvatar) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(34.dp),
                            strokeWidth = 3.dp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Username — Flutter places this before the display name.
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("نام کاربری") },
                    leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("نام و نام خانوادگی") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ایمیل") },
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("شماره تلفن") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Gender Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedGender,
                    onExpandedChange = { expandedGender = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = genderOptions.firstOrNull { it.first == gender }?.second.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("جنسیت") },
                        leadingIcon = { Icon(Icons.Outlined.Wc, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGender) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                    ExposedDropdownMenu(
                        expanded = expandedGender,
                        onDismissRequest = { expandedGender = false },
                    ) {
                        genderOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.second) },
                                onClick = {
                                    gender = option.first
                                    expandedGender = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Birth date (Jalali)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBirthDatePicker = true },
                ) {
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("تاریخ تولد (شمسی)") },
                        leadingIcon = { Icon(Icons.Outlined.Cake, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        singleLine = true,
                        enabled = true,
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showBirthDatePicker = true },
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedMaritalStatus,
                    onExpandedChange = { expandedMaritalStatus = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = maritalStatusOptions.firstOrNull { it.first == maritalStatus }?.second.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("وضعیت تأهل") },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMaritalStatus) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMaritalStatus,
                        onDismissRequest = { expandedMaritalStatus = false },
                    ) {
                        maritalStatusOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.second) },
                                onClick = {
                                    maritalStatus = option.first
                                    expandedMaritalStatus = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "نمایش در جزئیات اکانت",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Start,
                )
                VisibilityOption("نمایش ایمیل در صفحه جزییات اکانت", showEmail) { showEmail = it }
                VisibilityOption("نمایش تاریخ تولد در صفحه جزییات اکانت", showBirthDate) { showBirthDate = it }
                VisibilityOption("نمایش جنسیت در صفحه جزییات اکانت", showGender) { showGender = it }
                VisibilityOption("نمایش وضعیت تأهل در صفحه جزییات اکانت", showMaritalStatus) { showMaritalStatus = it }

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = {
                        scope.launch {
                            if (username.isBlank()) {
                                snackbarHostState.showSnackbar("نام کاربری نمی‌تواند خالی باشد")
                                return@launch
                            }
                            if (fullName.isBlank()) {
                                snackbarHostState.showSnackbar("نام و نام خانوادگی نمی‌تواند خالی باشد")
                                return@launch
                            }
                            if (birthDate.isBlank()) {
                                snackbarHostState.showSnackbar("تاریخ تولد را وارد کنید")
                                return@launch
                            }
                            if (gender.isBlank()) {
                                snackbarHostState.showSnackbar("جنسیت را انتخاب کنید")
                                return@launch
                            }
                            isSaving = true
                            val result = onSave(
                                ProfileUpdateRequestDto(
                                    username = username.trim(),
                                    fullName = fullName.trim(),
                                    bio = bio.trim(),
                                    email = email.trim(),
                                    phoneNumber = phone.trim(),
                                    websiteUrl = website.trim(),
                                    birthDate = (jalaliToGregorianIso(birthDate) ?: birthDate).trim(),
                                    gender = gender,
                                    maritalStatus = maritalStatus,
                                    showEmail = showEmail,
                                    showBirthDate = showBirthDate,
                                    showGender = showGender,
                                    showMaritalStatus = showMaritalStatus,
                                ),
                            )
                            isSaving = false
                            when (result) {
                                is Outcome.Success -> {
                                    snackbarHostState.showSnackbar("تغییرات پروفایل با موفقیت ذخیره شد")
                                    onBack()
                                }
                                is Outcome.Failure -> snackbarHostState.showSnackbar(result.error.messageFa)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = "ذخیره تغییرات",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
        if (showAvatarOptions) {
            ModalBottomSheet(onDismissRequest = { showAvatarOptions = false }) {
                ListItem(
                    headlineContent = { Text("افزودن تصویر جدید") },
                    leadingContent = { Icon(Icons.Outlined.AddAPhoto, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showAvatarOptions = false
                        avatarPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
                ListItem(
                    headlineContent = { Text("حذف عکس پروفایل", color = MaterialTheme.colorScheme.error) },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier = Modifier.clickable(enabled = !profile?.avatarUrl.isNullOrBlank()) {
                        showAvatarOptions = false
                        scope.launch {
                            isUploadingAvatar = true
                            when (val result = onAvatarRemoved()) {
                                is Outcome.Success -> snackbarHostState.showSnackbar("عکس پروفایل حذف شد")
                                is Outcome.Failure -> snackbarHostState.showSnackbar(result.error.messageFa)
                            }
                            isUploadingAvatar = false
                        }
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        if (showBirthDatePicker) {
            JalaliDatePickerDialog(
                initialValue = birthDate,
                onDismissRequest = { showBirthDatePicker = false },
                onDateSelected = { date ->
                    birthDate = date.displayValue
                    showBirthDatePicker = false
                },
            )
        }
    }
}

@Composable
private fun VisibilityOption(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        ir.coffevista.vista_native.core.designsystem.component.VistaSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
