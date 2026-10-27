package ir.coffevista.vista_native.features.profile.settings.privacy

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.material3.RadioButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.PhotoSizeSelectLarge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.features.profile.settings.components.VistaChoiceOption
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsChoiceBottomSheet
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsDivider
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsGroup
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsSection
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsSwitch
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsTile
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(
    profile: OwnProfileEntity?,
    onBack: () -> Unit,
    onBlockedUsersClick: () -> Unit,
    onActiveSessionsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrivacySettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val canEdit = uiState.hasLoaded && !uiState.isSaving
    val context = LocalContext.current
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }

    // BottomSheet states
    var activeSheet by remember { mutableStateOf<String?>(null) }
    var showRemove2FADialog by remember { mutableStateOf(false) }
    var remove2FAPassword by remember { mutableStateOf("") }

    var showAccountDeletionDialog by remember { mutableStateOf(false) }
    var deletionStep by remember { mutableStateOf(1) }
    var deletionReason by remember { mutableStateOf("not_using") }
    var deletionOtp by remember { mutableStateOf("") }
    var deletionPassword by remember { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LocalLayoutDirection.current) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "حریم خصوصی و امنیت",
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
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Privacy
                VistaSettingsSection(title = "حریم خصوصی")
                VistaSettingsGroup {
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.Lock,
                        title = "حساب خصوصی",
                        value = settings.is_private,
                        enabled = canEdit,
                        onChanged = viewModel::setPrivate,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.AccessTime,
                        title = "آخرین بازدید",
                        trailing = {
                            Text(
                                text = when (settings.last_seen_visibility) {
                                    "everyone" -> "همه"
                                    "my_contacts" -> "فقط مخاطبین"
                                    else -> "هیچکس"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                ),
                            )
                        },
                        enabled = canEdit,
                        onClick = { activeSheet = "lastSeen" },
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "پیام از طرف",
                        trailing = {
                            Text(
                                text = when (settings.message_privacy) {
                                    "everyone" -> "همه"
                                    "friends" -> "دوستان من"
                                    else -> "هیچکس"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                ),
                            )
                        },
                        enabled = canEdit,
                        onClick = { activeSheet = "messagePrivacy" },
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.GroupAdd,
                        title = "افزودن به گروه",
                        trailing = {
                            Text(
                                text = when (settings.group_add_privacy) {
                                    "everyone" -> "همه"
                                    "following" -> "فقط دنبال‌کننده‌ها"
                                    else -> "هیچکس"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                ),
                            )
                        },
                        enabled = canEdit,
                        onClick = { activeSheet = "groupAddPrivacy" },
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "پاسخ به استوری",
                        trailing = {
                            Text(
                                text = when (uiState.storyReplyPermission) {
                                    "everyone" -> "همه"
                                    "following" -> "فقط دنبال‌کننده‌ها"
                                    else -> "غیرفعال"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                ),
                            )
                        },
                        enabled = canEdit,
                        onClick = { activeSheet = "storyReplies" },
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.DoneAll,
                        title = "تیک خوانده‌شدن",
                        value = settings.read_receipts,
                        enabled = canEdit,
                        onChanged = { value -> viewModel.update { it.copy(read_receipts = value) } },
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.PhotoSizeSelectLarge,
                        title = "بزرگنمایی تصویر پروفایل",
                        subtitle = "اگر غیرفعال باشد، دیگران نمی‌توانند عکس پروفایل شما را بزرگ کنند",
                        value = settings.allow_profile_zoom,
                        enabled = canEdit,
                        onChanged = { value -> viewModel.update { it.copy(allow_profile_zoom = value) } },
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Block,
                        title = "کاربران مسدود شده",
                        onClick = onBlockedUsersClick,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Security
                VistaSettingsSection(title = "امنیت")
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.Devices,
                        title = "نشست‌های فعال",
                        onClick = onActiveSessionsClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.Fingerprint,
                        title = "ورود بیومتریک",
                        subtitle = if (uiState.biometricAvailable) null else "این دستگاه بیومتریک یا قفل امن در دسترس ندارد",
                        value = uiState.biometricEnabled,
                        enabled = !uiState.biometricLoading && (uiState.biometricAvailable || uiState.biometricEnabled),
                        onChanged = { enabled ->
                            val activity = context as? FragmentActivity
                            if (activity == null) {
                                Toast.makeText(context, "امکان نمایش تایید هویت در این صفحه وجود ندارد", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.setBiometricEnabled(activity, enabled)
                            }
                        },
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.LockReset,
                        title = "غیرفعال‌سازی تایید دو مرحله‌ای (2FA)",
                        onClick = { showRemove2FADialog = true },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Account Deletion
                VistaSettingsSection(title = "مدیریت حساب")
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.DeleteForever,
                        title = "حذف حساب کاربری",
                        subtitle = "حذف دائمی حساب با مهلت بازگردانی ۳۰ روزه",
                        onClick = {
                            deletionStep = 1
                            deletionOtp = ""
                            deletionPassword = ""
                            showAccountDeletionDialog = true
                        },
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Choice Bottom Sheets
        when (activeSheet) {
            "lastSeen" -> {
                VistaSettingsChoiceBottomSheet(
                    title = "آخرین بازدید",
                    selectedValue = settings.last_seen_visibility,
                    options = listOf(
                        VistaChoiceOption("everyone", "همه"),
                        VistaChoiceOption("my_contacts", "فقط مخاطبین"),
                        VistaChoiceOption("nobody", "هیچکس"),
                    ),
                    onOptionSelected = {
                        viewModel.update { settings -> settings.copy(last_seen_visibility = it) }
                        activeSheet = null
                    },
                    onDismissRequest = { activeSheet = null },
                )
            }
            "messagePrivacy" -> {
                VistaSettingsChoiceBottomSheet(
                    title = "پیام از طرف",
                    selectedValue = settings.message_privacy,
                    options = listOf(
                        VistaChoiceOption("everyone", "همه"),
                        VistaChoiceOption("friends", "دوستان من"),
                        VistaChoiceOption("nobody", "هیچکس"),
                    ),
                    onOptionSelected = {
                        viewModel.update { settings -> settings.copy(message_privacy = it) }
                        activeSheet = null
                    },
                    onDismissRequest = { activeSheet = null },
                )
            }
            "groupAddPrivacy" -> {
                VistaSettingsChoiceBottomSheet(
                    title = "افزودن به گروه",
                    selectedValue = settings.group_add_privacy,
                    options = listOf(
                        VistaChoiceOption("everyone", "همه"),
                        VistaChoiceOption("following", "فقط دنبال‌کننده‌ها"),
                        VistaChoiceOption("nobody", "هیچکس"),
                    ),
                    onOptionSelected = {
                        viewModel.update { settings -> settings.copy(group_add_privacy = it) }
                        activeSheet = null
                    },
                    onDismissRequest = { activeSheet = null },
                )
            }
            "storyReplies" -> {
                VistaSettingsChoiceBottomSheet(
                    title = "پاسخ به استوری",
                    selectedValue = uiState.storyReplyPermission,
                    options = listOf(
                        VistaChoiceOption("everyone", "همه"),
                        VistaChoiceOption("following", "فقط دنبال‌کننده‌ها"),
                        VistaChoiceOption("off", "غیرفعال"),
                    ),
                    onOptionSelected = {
                        viewModel.setStoryReplyPermission(it)
                        activeSheet = null
                    },
                    onDismissRequest = { activeSheet = null },
                )
            }
        }

        if (showRemove2FADialog) {
            AlertDialog(
                onDismissRequest = { showRemove2FADialog = false },
                title = { Text("غیرفعال‌سازی تایید دو مرحله‌ای") },
                text = {
                    Column {
                        Text(
                            text = "برای حذف رمز تایید دو مرحله‌ای، لطفاً رمز عبور فعلی خود را وارد کنید:",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = remove2FAPassword,
                            onValueChange = { remove2FAPassword = it },
                            label = { Text("رمز عبور فعلی") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (remove2FAPassword.isBlank()) {
                                Toast.makeText(context, "لطفاً رمز عبور را وارد کنید", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.remove2FA(
                                currentPassword = remove2FAPassword,
                                onSuccess = {
                                    showRemove2FADialog = false
                                    remove2FAPassword = ""
                                    Toast.makeText(context, "تایید دو مرحله‌ای با موفقیت غیرفعال شد", Toast.LENGTH_SHORT).show()
                                },
                                onError = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                            )
                        },
                        enabled = !uiState.isSaving,
                    ) {
                        Text("غیرفعال‌سازی")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemove2FADialog = false }) { Text("انصراف") }
                },
            )
        }

        if (showAccountDeletionDialog) {
            AlertDialog(
                onDismissRequest = { showAccountDeletionDialog = false },
                title = { Text("حذف حساب کاربری") },
                text = {
                    Column {
                        if (deletionStep == 1) {
                            Text(
                                text = "حساب شما پس از ثبت درخواست به مدت ۳۰ روز در حالت تعلیق قرار می‌گیرد و پس از آن تمام اطلاعات برای همیشه پاک خواهد شد.\n\nعلت حذف حساب را انتخاب کنید:",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val reasons = listOf(
                                "not_using" to "دیگر از برنامه استفاده نمی‌کنم",
                                "privacy" to "نگرانی حریم خصوصی",
                                "notifications" to "اعلان‌های زیاد",
                                "alternative" to "جایگزین پیدا کردم",
                                "technical_issues" to "مشکلات فنی",
                                "other" to "سایر موارد",
                            )
                            reasons.forEach { (key, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { deletionReason = key }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = deletionReason == key,
                                        onClick = { deletionReason = key },
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        } else {
                            Text(
                                text = "کد تایید ارسال‌شده به شماره همراه / ایمیل خود را وارد نمایید:",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = deletionOtp,
                                onValueChange = { deletionOtp = it },
                                label = { Text("کد تایید پیامک‌شده") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = deletionPassword,
                                onValueChange = { deletionPassword = it },
                                label = { Text("رمز عبور حساب (اختیاری)") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (deletionStep == 1) {
                                viewModel.requestAccountDeletionOtp(
                                    reason = deletionReason,
                                    onSent = { deletionStep = 2 },
                                    onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                                )
                            } else {
                                if (deletionOtp.isBlank()) {
                                    Toast.makeText(context, "لطفاً کد تایید را وارد کنید", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.confirmAccountDeletion(
                                    otpCode = deletionOtp,
                                    password = deletionPassword,
                                    reason = deletionReason,
                                    onSuccess = { graceDays ->
                                        showAccountDeletionDialog = false
                                        Toast.makeText(
                                            context,
                                            "درخواست حذف ثبت شد. مهلت انصراف: $graceDays روز.",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        onBack()
                                    },
                                    onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                        enabled = !uiState.isSaving,
                    ) {
                        Text(if (deletionStep == 1) "دریافت کد تایید" else "تایید نهایی و حذف حساب")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccountDeletionDialog = false }) { Text("انصراف") }
                },
            )
        }
    }
}
