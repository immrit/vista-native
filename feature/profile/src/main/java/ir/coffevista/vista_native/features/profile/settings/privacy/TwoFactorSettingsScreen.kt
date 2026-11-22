package ir.coffevista.vista_native.features.profile.settings.privacy

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.core.designsystem.component.rememberBufferedTextState
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsDivider
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsGroup
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsSection
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TwoFactorSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDisableDialog by remember { mutableStateOf(false) }
    var disablePassword by remember { mutableStateOf("") }

    val handleBack: () -> Unit = {
        when (uiState.step) {
            TwoFactorWizardStep.LOADING,
            TwoFactorWizardStep.INTRO_AND_PREREQS,
            TwoFactorWizardStep.ACTIVE_MENU -> onBack()
            else -> viewModel.backToMenuOrIntro()
        }
    }
    BackHandler(onBack = handleBack)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "تایید دو مرحله‌ای (2FA)",
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = handleBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                uiState.infoMessage?.let { msg ->
                    StatusNoticeCard(
                        text = msg,
                        isError = false,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                uiState.errorMessage?.let { err ->
                    StatusNoticeCard(
                        text = err,
                        isError = true,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                when (uiState.step) {
                    TwoFactorWizardStep.LOADING -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    TwoFactorWizardStep.INTRO_AND_PREREQS -> {
                        TelegramTwoFactorIntroAndPrereqs(
                            uiState = uiState,
                            onEmailChange = viewModel::updateAccountEmailInput,
                            onCodeChange = viewModel::updateAccountEmailCodeInput,
                            onSendAccountEmail = viewModel::sendAccountEmailVerification,
                            onVerifyAccountEmail = viewModel::verifyAccountEmailCode,
                            onBeginSetup = viewModel::beginEnableWizard,
                        )
                    }

                    TwoFactorWizardStep.ENTER_CURRENT_PASSWORD -> {
                        WizardStepCard(
                            icon = Icons.Outlined.Key,
                            title = "رمز عبور دومرحله‌ای فعلی",
                            description = "برای تغییر رمز، ابتدا رمز عبور دومرحله‌ای فعلی خود را وارد کنید.",
                        ) {
                            val (bufferedCurrentPassword, onCurrentPasswordChange) = rememberBufferedTextState(
                                externalValue = uiState.currentPasswordInput,
                                onValueChange = viewModel::updateCurrentPassword,
                            )
                            OutlinedTextField(
                                value = bufferedCurrentPassword,
                                onValueChange = onCurrentPasswordChange,
                                label = { Text("رمز عبور دومرحله‌ای فعلی") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::submitCurrentPasswordForChange,
                                enabled = !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("ادامه")
                            }
                        }
                    }

                    TwoFactorWizardStep.ENTER_NEW_PASSWORD -> {
                        WizardStepCard(
                            icon = Icons.Outlined.Password,
                            title = "انتخاب رمز عبور دومرحله‌ای",
                            description = "این رمز هنگام ورود از دستگاه جدید پس از کد تایید پیامکی یا رمز عادی از شما پرسیده می‌شود (حداقل ۶ کاراکتر).",
                        ) {
                            val (bufferedNewPassword, onNewPasswordChange) = rememberBufferedTextState(
                                externalValue = uiState.newPasswordInput,
                                onValueChange = viewModel::updateNewPassword,
                            )
                            OutlinedTextField(
                                value = bufferedNewPassword,
                                onValueChange = onNewPasswordChange,
                                label = { Text("رمز عبور دومرحله‌ای") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::submitNewPassword,
                                enabled = !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("ادامه")
                            }
                        }
                    }

                    TwoFactorWizardStep.CONFIRM_NEW_PASSWORD -> {
                        WizardStepCard(
                            icon = Icons.Outlined.VerifiedUser,
                            title = "تکرار رمز عبور دومرحله‌ای",
                            description = "برای اطمینان از صحت رمز، دوباره آن را وارد کنید.",
                        ) {
                            val (bufferedConfirmPassword, onConfirmPasswordChange) = rememberBufferedTextState(
                                externalValue = uiState.confirmPasswordInput,
                                onValueChange = viewModel::updateConfirmPassword,
                            )
                            OutlinedTextField(
                                value = bufferedConfirmPassword,
                                onValueChange = onConfirmPasswordChange,
                                label = { Text("تکرار رمز عبور دومرحله‌ای") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::submitConfirmPassword,
                                enabled = !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("ادامه")
                            }
                        }
                    }

                    TwoFactorWizardStep.ENTER_HINT -> {
                        WizardStepCard(
                            icon = Icons.Outlined.HelpOutline,
                            title = "راهنمای رمز (اختیاری)",
                            description = "یک راهنما برای به خاطر آوردن رمز بسازید. راهنما نباید با خود رمز عبور یکسان باشد.",
                        ) {
                            val (bufferedHint, onHintChange) = rememberBufferedTextState(
                                externalValue = uiState.hintInput,
                                onValueChange = viewModel::updateHintInput,
                            )
                            OutlinedTextField(
                                value = bufferedHint,
                                onValueChange = onHintChange,
                                label = { Text("راهنمای رمز") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.submitHintAndProceed(skipHint = false) },
                                enabled = !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (uiState.isUpdatingExisting) "ذخیره رمز جدید" else "ادامه")
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.submitHintAndProceed(skipHint = true) },
                                enabled = !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("رد کردن (بدون راهنما)")
                            }
                        }
                    }

                    TwoFactorWizardStep.ENTER_RECOVERY_EMAIL -> {
                        WizardStepCard(
                            icon = Icons.Outlined.AlternateEmail,
                            title = "ایمیل بازیابی دومرحله‌ای",
                            description = "یک کد تایید ۶ رقمی به این ایمیل ارسال می‌شود تا در صورت فراموشی رمز دومرحله‌ای بتوانید حساب خود را بازیابی کنید.",
                        ) {
                            val (bufferedRecoveryEmail, onRecoveryEmailChange) = rememberBufferedTextState(
                                externalValue = uiState.recoveryEmailInput,
                                onValueChange = viewModel::updateRecoveryEmailInput,
                            )
                            OutlinedTextField(
                                value = bufferedRecoveryEmail,
                                onValueChange = onRecoveryEmailChange,
                                label = { Text("ایمیل بازیابی") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.submitSetupRequest(keepRecoveryEmail = false) },
                                enabled = !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (uiState.isBusy) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("ارسال کد تایید به ایمیل")
                                }
                            }
                        }
                    }

                    TwoFactorWizardStep.VERIFY_RECOVERY_CODE -> {
                        WizardStepCard(
                            icon = Icons.Outlined.MarkEmailRead,
                            title = "تایید ایمیل بازیابی",
                            description = "کد ۶ رقمی ارسال‌شده به ${uiState.pendingEmailPattern ?: "ایمیل بازیابی"} را وارد کنید تا تایید دو مرحله‌ای نهایی شود.",
                        ) {
                            val (bufferedConfirmCode, onConfirmCodeChange) = rememberBufferedTextState(
                                externalValue = uiState.confirmCodeInput,
                                onValueChange = viewModel::updateConfirmCodeInput,
                            )
                            OutlinedTextField(
                                value = bufferedConfirmCode,
                                onValueChange = onConfirmCodeChange,
                                label = { Text("کد ۶ رقمی") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::confirmRecoveryEmailCode,
                                enabled = !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (uiState.isBusy) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("تایید و فعال‌سازی")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                TextButton(
                                    onClick = viewModel::resendRecoveryConfirmCode,
                                    enabled = !uiState.isBusy && uiState.resendSeconds == 0,
                                ) {
                                    Text(
                                        if (uiState.resendSeconds > 0) "ارسال مجدد (${uiState.resendSeconds})" else "ارسال مجدد کد",
                                    )
                                }
                                TextButton(
                                    onClick = viewModel::abortPendingSetup,
                                    enabled = !uiState.isBusy,
                                ) {
                                    Text("لغو", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    TwoFactorWizardStep.ACTIVE_MENU -> {
                        TelegramTwoFactorActiveMenu(
                            status = uiState.status,
                            isBusy = uiState.isBusy,
                            onChangePassword = viewModel::beginChangePasswordWizard,
                            onChangeHint = viewModel::beginChangeHint,
                            onChangeRecoveryEmail = viewModel::beginChangeRecoveryEmail,
                            onDisableClick = { showDisableDialog = true },
                            onCancelPendingReset = viewModel::cancelPendingReset,
                        )
                    }

                    TwoFactorWizardStep.CHANGE_HINT -> {
                        WizardStepCard(
                            icon = Icons.Outlined.HelpOutline,
                            title = "تغییر راهنمای رمز",
                            description = "رمز عبور دومرحله‌ای فعلی و راهنمای جدید را وارد کنید.",
                        ) {
                            val (bufferedCurrentPassword, onCurrentPasswordChange) = rememberBufferedTextState(
                                externalValue = uiState.currentPasswordInput,
                                onValueChange = viewModel::updateCurrentPassword,
                            )
                            OutlinedTextField(
                                value = bufferedCurrentPassword,
                                onValueChange = onCurrentPasswordChange,
                                label = { Text("رمز عبور دومرحله‌ای فعلی") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            val (bufferedHint, onHintChange) = rememberBufferedTextState(
                                externalValue = uiState.hintInput,
                                onValueChange = viewModel::updateHintInput,
                            )
                            OutlinedTextField(
                                value = bufferedHint,
                                onValueChange = onHintChange,
                                label = { Text("راهنمای جدید (خالی برای حذف)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::submitChangeHint,
                                enabled = !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("ذخیره راهنما")
                            }
                        }
                    }

                    TwoFactorWizardStep.CHANGE_EMAIL -> {
                        WizardStepCard(
                            icon = Icons.Outlined.AlternateEmail,
                            title = "تغییر ایمیل بازیابی",
                            description = "پس از وارد کردن رمز فعلی، کد تایید ۶ رقمی به ایمیل بازیابی جدید ارسال خواهد شد.",
                        ) {
                            val (bufferedCurrentPassword, onCurrentPasswordChange) = rememberBufferedTextState(
                                externalValue = uiState.currentPasswordInput,
                                onValueChange = viewModel::updateCurrentPassword,
                            )
                            OutlinedTextField(
                                value = bufferedCurrentPassword,
                                onValueChange = onCurrentPasswordChange,
                                label = { Text("رمز عبور دومرحله‌ای فعلی") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            val (bufferedRecoveryEmail, onRecoveryEmailChange) = rememberBufferedTextState(
                                externalValue = uiState.recoveryEmailInput,
                                onValueChange = viewModel::updateRecoveryEmailInput,
                            )
                            OutlinedTextField(
                                value = bufferedRecoveryEmail,
                                onValueChange = onRecoveryEmailChange,
                                label = { Text("ایمیل بازیابی جدید") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::submitChangeRecoveryEmail,
                                enabled = !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("ارسال کد تایید")
                            }
                        }
                    }
                }
            }

            if (showDisableDialog) {
                AlertDialog(
                    onDismissRequest = { showDisableDialog = false },
                    title = { Text("غیرفعال‌سازی تایید دو مرحله‌ای") },
                    text = {
                        Column {
                            Text(
                                text = "برای غیرفعال کردن تایید دو مرحله‌ای، رمز عبور دومرحله‌ای فعلی خود را وارد کنید:",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = disablePassword,
                                onValueChange = { disablePassword = it },
                                label = { Text("رمز عبور دومرحله‌ای فعلی") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.disableTwoFactor(disablePassword) {
                                    disablePassword = ""
                                    showDisableDialog = false
                                    Toast.makeText(context, "تایید دو مرحله‌ای غیرفعال شد", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text("غیرفعال‌سازی")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDisableDialog = false }) {
                            Text("انصراف")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TelegramTwoFactorIntroAndPrereqs(
    uiState: TwoFactorUiState,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSendAccountEmail: () -> Unit,
    onVerifyAccountEmail: () -> Unit,
    onBeginSetup: () -> Unit,
) {
    val status = uiState.status
    WizardStepCard(
        icon = Icons.Outlined.Shield,
        title = "امنیت چندلایه حساب ویستا",
        description = "با فعال‌سازی تایید دو مرحله‌ای، برای ورود از هر دستگاه جدید علاوه بر کد تایید یا رمز حساب، رمز عبور دومرحله‌ای نیز لازم است.",
    ) {
        VistaSettingsSection(title = "پیش‌نیاز فعال‌سازی (تایید شماره موبایل یا ایمیل)")
        VistaSettingsGroup {
            PrerequisiteRow(
                icon = Icons.Outlined.PhoneAndroid,
                label = "شماره موبایل تایید شده",
                detail = status.verifiedPhone ?: "تایید نشده",
                verified = status.phoneVerified,
            )
            VistaSettingsDivider()
            PrerequisiteRow(
                icon = Icons.Outlined.AlternateEmail,
                label = "ایمیل تایید شده",
                detail = status.verifiedEmail ?: "تایید نشده",
                verified = status.emailVerified,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "تایید تنها یکی از موارد بالا (شماره موبایل یا ایمیل) برای فعال‌سازی کافی است.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!status.canEnable) {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "برای فعال‌سازی 2FA، ایمیل یا شماره موبایل خود را تایید کنید:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(10.dp))
                    if (uiState.accountEmailSessionId == null) {
                        val (bufferedEmail, onBufferedEmailChange) = rememberBufferedTextState(
                            externalValue = uiState.accountEmailInput,
                            onValueChange = onEmailChange,
                        )
                        OutlinedTextField(
                            value = bufferedEmail,
                            onValueChange = onBufferedEmailChange,
                            label = { Text("آدرس ایمیل") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onSendAccountEmail,
                            enabled = !uiState.isSendingAccountEmail,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (uiState.isSendingAccountEmail) "در حال ارسال..." else "ارسال کد تایید ایمیل")
                        }
                    } else {
                        val (bufferedCode, onBufferedCodeChange) = rememberBufferedTextState(
                            externalValue = uiState.accountEmailCodeInput,
                            onValueChange = onCodeChange,
                        )
                        OutlinedTextField(
                            value = bufferedCode,
                            onValueChange = onBufferedCodeChange,
                            label = { Text("کد تایید ایمیل") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onVerifyAccountEmail,
                            enabled = !uiState.isVerifyingAccountEmail,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (uiState.isVerifyingAccountEmail) "در حال بررسی..." else "تایید ایمیل")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onBeginSetup,
            enabled = status.canEnable && !uiState.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("تنظیم رمز عبور دومرحله‌ای")
        }
    }
}

@Composable
private fun TelegramTwoFactorActiveMenu(
    status: TwoFAStatusDto,
    isBusy: Boolean,
    onChangePassword: () -> Unit,
    onChangeHint: () -> Unit,
    onChangeRecoveryEmail: () -> Unit,
    onDisableClick: () -> Unit,
    onCancelPendingReset: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1B5E20).copy(alpha = 0.10f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E7D32)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "تایید دو مرحله‌ای فعال است",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (status.hint.isNotBlank()) {
                    Text(
                        text = "راهنمای رمز: ${status.hint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!status.recoveryEmailPattern.isNullOrBlank()) {
                    Text(
                        text = "ایمیل بازیابی: ${status.recoveryEmailPattern}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (status.pendingResetDate > 0L) {
        val nowSec = System.currentTimeMillis() / 1000L
        val remHours = ((status.pendingResetDate - nowSec).coerceAtLeast(0L) + 3599L) / 3600L
        val days = remHours / 24L
        val hours = remHours % 24L
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "درخواست بازنشانی ۷ روزه فعال است ($days روز و $hours ساعت باقی‌مانده)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "اگر شما درخواست بازنشانی رمز دومرحله‌ای را ثبت نکرده‌اید، همین حالا آن را لغو کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onCancelPendingReset,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("لغو درخواست بازنشانی")
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    VistaSettingsSection(title = "مدیریت تایید دو مرحله‌ای")
    VistaSettingsGroup {
        VistaSettingsTile(
            icon = Icons.Outlined.Password,
            title = "تغییر رمز عبور دومرحله‌ای",
            subtitle = "تعیین رمز جدید با تایید رمز فعلی",
            onClick = onChangePassword,
        )
        VistaSettingsDivider()
        VistaSettingsTile(
            icon = Icons.Outlined.HelpOutline,
            title = "تغییر راهنمای رمز",
            subtitle = status.hint.ifBlank { "بدون راهنما" },
            onClick = onChangeHint,
        )
        VistaSettingsDivider()
        VistaSettingsTile(
            icon = Icons.Outlined.AlternateEmail,
            title = "تغییر ایمیل بازیابی",
            subtitle = status.recoveryEmailPattern ?: "تنظیم نشده",
            onClick = onChangeRecoveryEmail,
        )
        VistaSettingsDivider()
        VistaSettingsTile(
            icon = Icons.Outlined.LockOpen,
            title = "غیرفعال‌سازی تایید دو مرحله‌ای",
            subtitle = "حذف رمز دومرحله‌ای حساب",
            onClick = onDisableClick,
        )
    }
}

@Composable
private fun PrerequisiteRow(
    icon: ImageVector,
    label: String,
    detail: String,
    verified: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = if (verified) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = if (verified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun WizardStepCard(
    icon: ImageVector,
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

@Composable
private fun StatusNoticeCard(
    text: String,
    isError: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            modifier = Modifier.padding(12.dp),
        )
    }
}
