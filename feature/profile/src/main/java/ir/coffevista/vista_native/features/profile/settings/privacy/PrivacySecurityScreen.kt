package ir.coffevista.vista_native.features.profile.settings.privacy

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.PhotoSizeSelectLarge
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import ir.coffevista.vista_native.core.common.toPersianDigits
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
    onDeleteAccountClick: () -> Unit = {},
    onTwoFactorClick: () -> Unit = {},
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

    var showEmailVerificationDialog by rememberSaveable { mutableStateOf(false) }
    var emailInput by rememberSaveable { mutableStateOf(profile?.email.orEmpty()) }
    var emailCodeInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.verifiedEmail) {
        if (emailInput.isBlank() && !uiState.verifiedEmail.isNullOrBlank()) {
            emailInput = uiState.verifiedEmail.orEmpty()
        }
    }

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
                        title = "تایید دو مرحله‌ای (2FA)",
                        subtitle = "تنظیم رمز ابری، ایمیل بازیابی و راهنمای رمز",
                        onClick = onTwoFactorClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.MarkEmailRead,
                        title = "تأیید ایمیل حساب کاربری",
                        subtitle = if (uiState.isEmailVerified && !uiState.verifiedEmail.isNullOrBlank()) {
                            uiState.verifiedEmail
                        } else {
                            "ارسال کد تأیید برای ایمن‌سازی و بازیابی حساب"
                        },
                        trailing = {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (uiState.isEmailVerified) {
                                    Color(0xFF16A34A).copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                },
                            ) {
                                Text(
                                    text = if (uiState.isEmailVerified) "تأیید شده" else "تأیید",
                                    color = if (uiState.isEmailVerified) {
                                        Color(0xFF16A34A)
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        },
                        onClick = {
                            viewModel.prepareEmailVerificationSheet()
                            showEmailVerificationDialog = true
                        },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Account Deletion
                VistaSettingsSection(title = "مدیریت حساب")
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.AutoDelete,
                        title = "حذف خودکار در صورت عدم فعالیت",
                        subtitle = "در صورت عدم حضور و آنلاین نشدن در این بازه، حساب، چت‌ها و تمام محتوا حذف می‌شوند",
                        trailing = {
                            Text(
                                text = when (settings.inactive_account_ttl) {
                                    "1_month" -> "۱ ماه"
                                    "3_months" -> "۳ ماه"
                                    "1_year" -> "۱ سال"
                                    else -> "۶ ماه"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (canEdit) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                ),
                            )
                        },
                        enabled = canEdit,
                        onClick = { activeSheet = "inactiveAccountTtl" },
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.DeleteForever,
                        title = "حذف حساب کاربری",
                        subtitle = "انتخاب علت و ثبت درخواست با دوره انتظار ۱۴ روزه",
                        onClick = onDeleteAccountClick,
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
            "inactiveAccountTtl" -> {
                VistaSettingsChoiceBottomSheet(
                    title = "در صورت عدم حضور تا این مدت",
                    selectedValue = settings.inactive_account_ttl.ifBlank { "6_months" },
                    options = listOf(
                        VistaChoiceOption("1_month", "۱ ماه"),
                        VistaChoiceOption("3_months", "۳ ماه"),
                        VistaChoiceOption("6_months", "۶ ماه (پیش‌فرض)"),
                        VistaChoiceOption("1_year", "۱ سال"),
                    ),
                    onOptionSelected = {
                        viewModel.update { current -> current.copy(inactive_account_ttl = it) }
                        activeSheet = null
                    },
                    onDismissRequest = { activeSheet = null },
                )
            }
        }

        if (showEmailVerificationDialog) {
            EmailVerificationModalSheet(
                uiState = uiState,
                emailInput = emailInput,
                emailCodeInput = emailCodeInput,
                onEmailChange = { emailInput = it },
                onCodeChange = { raw ->
                    emailCodeInput = PrivacySettingsViewModel.normalizeOtpDigits(raw)
                },
                onSendCode = {
                    emailCodeInput = ""
                    viewModel.sendEmailVerification(emailInput)
                },
                onResendCode = {
                    emailCodeInput = ""
                    viewModel.resendEmailVerification()
                },
                onVerifyCode = { viewModel.verifyEmail(emailCodeInput) },
                onChangeEmailStep = {
                    emailCodeInput = ""
                    viewModel.resetEmailVerificationToEmailStep()
                },
                onDismiss = { showEmailVerificationDialog = false },
            )
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailVerificationModalSheet(
    uiState: PrivacySettingsUiState,
    emailInput: String,
    emailCodeInput: String,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onResendCode: () -> Unit,
    onVerifyCode: () -> Unit,
    onChangeEmailStep: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val emerald = Color(0xFF16A34A)
    val stepKey = when {
        uiState.emailVerificationCompleted -> 2
        !uiState.emailVerificationSessionId.isNullOrBlank() -> 1
        else -> 0
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val heroBg = when (stepKey) {
                2 -> emerald.copy(alpha = 0.14f)
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            }
            val heroTint = when (stepKey) {
                2 -> emerald
                else -> MaterialTheme.colorScheme.primary
            }
            val heroIcon = when (stepKey) {
                2 -> Icons.Filled.CheckCircle
                1 -> Icons.Outlined.MarkEmailRead
                else -> Icons.Outlined.AlternateEmail
            }

            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(heroBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = heroIcon,
                    contentDescription = null,
                    tint = heroTint,
                    modifier = Modifier.size(30.dp),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = when (stepKey) {
                    2 -> "ایمیل با موفقیت تأیید شد"
                    1 -> "کد تأیید را وارد کنید"
                    else -> "تأیید ایمیل حساب کاربری"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (stepKey) {
                    2 -> "ایمیل شما به حساب ویستا متصل شد و برای بازیابی و امنیت حساب فعال است."
                    1 -> "کد ۶ رقمی ارسال‌شده به صندوق ایمیل (یا پوشه Spam) خود را در کادر زیر وارد کنید."
                    else -> "با تأیید ایمیل، می‌توانید امنیت حساب و بازیابی رمز عبور دومرحله‌ای را فعال نگه دارید."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = !uiState.emailVerificationErrorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = uiState.emailVerificationErrorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !uiState.emailVerificationSuccessMessage.isNullOrBlank() && stepKey != 2,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MarkEmailUnread,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = uiState.emailVerificationSuccessMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = stepKey,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "email-verification-step",
            ) { step ->
                when (step) {
                    2 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val confirmedEmail = uiState.verifiedEmail?.takeIf { it.isNotBlank() }
                                ?: uiState.emailVerificationTargetEmail
                            if (confirmedEmail.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = emerald.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = emerald.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(14.dp),
                                        ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.MarkEmailRead,
                                            contentDescription = null,
                                            tint = emerald,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = confirmedEmail,
                                            style = TextStyle(
                                                color = emerald,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textDirection = TextDirection.Ltr,
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                            }

                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            ) {
                                Text("تأیید و بستن", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            TextButton(onClick = onChangeEmailStep) {
                                Text(
                                    text = "تغییر و تأیید ایمیل جدید",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }

                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = onChangeEmailStep),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "تغییر ایمیل",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = uiState.emailVerificationTargetEmail.ifBlank { emailInput },
                                        style = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            textDirection = TextDirection.Ltr,
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            EmailOtpCodeBoxes(
                                code = emailCodeInput,
                                onCodeChange = onCodeChange,
                                onDone = {
                                    if (emailCodeInput.length == 6 && !uiState.isVerifyingEmail) {
                                        onVerifyCode()
                                    }
                                },
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (uiState.emailVerificationResendSeconds > 0) {
                                val secText = uiState.emailVerificationResendSeconds
                                    .toString()
                                    .padStart(2, '0')
                                    .toPersianDigits()
                                Text(
                                    text = "امکان ارسال مجدد تا ۰۰:$secText ثانیه دیگر",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.5.sp,
                                    ),
                                )
                            } else {
                                TextButton(
                                    onClick = onResendCode,
                                    enabled = !uiState.isSendingEmailVerification && !uiState.isVerifyingEmail,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ارسال مجدد کد تأیید", fontWeight = FontWeight.Medium)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onVerifyCode,
                                enabled = !uiState.isVerifyingEmail && emailCodeInput.length == 6,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            ) {
                                if (uiState.isVerifyingEmail) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.2.dp,
                                    )
                                } else {
                                    Text("تأیید و ثبت ایمیل", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = onDismiss,
                                enabled = !uiState.isVerifyingEmail,
                            ) {
                                Text("بستن", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (uiState.isEmailVerified && !uiState.verifiedEmail.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = emerald.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = emerald,
                                            modifier = Modifier.size(17.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ایمیل فعلی تأیید شده: ${uiState.verifiedEmail}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = emerald,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium,
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = onEmailChange,
                                label = { Text("آدرس ایمیل") },
                                placeholder = {
                                    Text(
                                        "name@example.com",
                                        style = TextStyle(textDirection = TextDirection.Ltr),
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Email,
                                        contentDescription = null,
                                    )
                                },
                                trailingIcon = {
                                    if (emailInput.isNotEmpty()) {
                                        IconButton(onClick = { onEmailChange("") }) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "پاک کردن",
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.Ltr,
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Send,
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (!uiState.isSendingEmailVerification && emailInput.isNotBlank()) {
                                            onSendCode()
                                        }
                                    },
                                ),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = onSendCode,
                                enabled = !uiState.isSendingEmailVerification && emailInput.trim().contains("@"),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            ) {
                                if (uiState.isSendingEmailVerification) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.2.dp,
                                    )
                                } else {
                                    Text("ارسال کد تأیید", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                            ) {
                                Text("انصراف", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EmailOtpCodeBoxes(
    code: String,
    onCodeChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { runCatching { focusRequester.requestFocus() } },
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = code,
                onValueChange = { onCodeChange(it) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                singleLine = true,
                cursorBrush = SolidColor(Color.Transparent),
                textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                decorationBox = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    ) {
                        for (index in 0 until 6) {
                            val char = code.getOrNull(index)
                            val isCurrent = index == code.length && code.length < 6
                            val isFilled = char != null
                            val borderColor = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                isFilled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                            val bgColor = when {
                                isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                isFilled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bgColor)
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.2.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = char?.toString() ?: "",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}
