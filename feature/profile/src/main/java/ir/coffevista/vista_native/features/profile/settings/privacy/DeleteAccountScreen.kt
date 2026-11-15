package ir.coffevista.vista_native.features.profile.settings.privacy

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity
import kotlinx.coroutines.delay

private data class DeletionReasonOption(
    val key: String,
    val title: String,
)

private val deletionReasons = listOf(
    DeletionReasonOption("not_using", "دیگر از برنامه استفاده نمی‌کنم"),
    DeletionReasonOption("privacy", "نگرانی درباره حریم خصوصی"),
    DeletionReasonOption("notifications", "اعلان‌های زیاد"),
    DeletionReasonOption("alternative", "جایگزین دیگری پیدا کردم"),
    DeletionReasonOption("content_quality", "کیفیت محتوا رضایت‌بخش نیست"),
    DeletionReasonOption("technical_issues", "مشکلات فنی و خطاها"),
    DeletionReasonOption("other", "سایر موارد"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    profile: OwnProfileEntity?,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrivacySettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var step by remember { mutableIntStateOf(1) }
    var selectedReason by remember { mutableStateOf("not_using") }
    var acknowledgedPolicy by remember { mutableStateOf(false) }

    var otpCode by remember { mutableStateOf("") }
    var accountPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var otpChannel by remember { mutableStateOf<String?>(null) }
    var accountHasPassword by remember { mutableStateOf(true) }
    var resendRemainingSeconds by remember { mutableIntStateOf(0) }
    var gracePeriodDays by remember { mutableIntStateOf(14) }
    var inlineError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(step, resendRemainingSeconds) {
        if (step == 2 && resendRemainingSeconds > 0) {
            delay(1000L)
            resendRemainingSeconds = (resendRemainingSeconds - 1).coerceAtLeast(0)
        }
    }

    BackHandler(enabled = step == 2 || step == 3) {
        if (step == 3) {
            onAccountDeleted()
        } else {
            inlineError = null
            step = 1
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "حذف حساب کاربری",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Surface(
                                color = if (step == 3) {
                                    Color(0xFF10B981).copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                },
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(
                                    text = when (step) {
                                        1 -> "مرحله ۱ از ۲"
                                        2 -> "مرحله ۲ از ۲"
                                        else -> "ثبت شد"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (step == 3) Color(0xFF047857) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (step != 3) {
                            IconButton(
                                onClick = {
                                    if (step == 2) {
                                        inlineError = null
                                        step = 1
                                    } else {
                                        onBack()
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "بازگشت",
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            AnimatedContent(
                targetState = step,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                label = "delete_account_step",
            ) { currentStep ->
                when (currentStep) {
                    1 -> DeleteAccountStepOne(
                        profile = profile,
                        selectedReason = selectedReason,
                        acknowledgedPolicy = acknowledgedPolicy,
                        isSaving = uiState.isSaving,
                        inlineError = inlineError,
                        onSelectReason = {
                            selectedReason = it
                            inlineError = null
                        },
                        onToggleAcknowledged = {
                            acknowledgedPolicy = it
                            inlineError = null
                        },
                        onRequestOtp = {
                            inlineError = null
                            viewModel.requestAccountDeletionOtp(
                                reason = selectedReason,
                                onSent = { channel, expiresIn, hasPassword ->
                                    otpChannel = channel
                                    accountHasPassword = hasPassword
                                    resendRemainingSeconds = expiresIn.toInt().coerceIn(30, 300)
                                    otpCode = ""
                                    accountPassword = ""
                                    step = 2
                                },
                                onError = { msg ->
                                    inlineError = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                            )
                        },
                        onCancel = onBack,
                    )

                    2 -> DeleteAccountStepTwo(
                        selectedReasonLabel = deletionReasons.firstOrNull { it.key == selectedReason }?.title
                            ?: "سایر موارد",
                        otpChannel = otpChannel,
                        accountHasPassword = accountHasPassword,
                        otpCode = otpCode,
                        accountPassword = accountPassword,
                        passwordVisible = passwordVisible,
                        resendRemainingSeconds = resendRemainingSeconds,
                        isSaving = uiState.isSaving,
                        inlineError = inlineError,
                        onOtpChange = { value ->
                            otpCode = value.filter { it.isDigit() || it.isLetter() }.take(12)
                            inlineError = null
                        },
                        onPasswordChange = {
                            accountPassword = it
                            inlineError = null
                        },
                        onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                        onResendOtp = {
                            inlineError = null
                            viewModel.requestAccountDeletionOtp(
                                reason = selectedReason,
                                onSent = { channel, expiresIn, hasPassword ->
                                    otpChannel = channel
                                    accountHasPassword = hasPassword
                                    resendRemainingSeconds = expiresIn.toInt().coerceIn(30, 300)
                                    Toast.makeText(context, "کد تایید مجدداً ارسال شد", Toast.LENGTH_SHORT).show()
                                },
                                onError = { msg ->
                                    inlineError = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                            )
                        },
                        onConfirm = {
                            if (otpCode.isBlank()) {
                                inlineError = "کد تایید ارسال‌شده را وارد کنید"
                                return@DeleteAccountStepTwo
                            }
                            if (accountHasPassword && accountPassword.isBlank()) {
                                inlineError = "رمز عبور حساب خود را وارد کنید"
                                return@DeleteAccountStepTwo
                            }
                            inlineError = null
                            viewModel.confirmAccountDeletion(
                                otpCode = otpCode.trim(),
                                password = accountPassword,
                                reason = selectedReason,
                                onSuccess = { grace ->
                                    gracePeriodDays = grace
                                    step = 3
                                },
                                onError = { msg ->
                                    inlineError = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                            )
                        },
                        onBackStep = {
                            inlineError = null
                            step = 1
                        },
                    )

                    else -> DeleteAccountCompletedStep(
                        gracePeriodDays = gracePeriodDays,
                        onFinishLogout = onAccountDeleted,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteAccountStepOne(
    profile: OwnProfileEntity?,
    selectedReason: String,
    acknowledgedPolicy: Boolean,
    isSaving: Boolean,
    inlineError: String?,
    onSelectReason: (String) -> Unit,
    onToggleAcknowledged: (Boolean) -> Unit,
    onRequestOtp: () -> Unit,
    onCancel: () -> Unit,
) {
    val errorColor = MaterialTheme.colorScheme.error
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Compact warning summary card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = errorColor.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, errorColor.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(errorColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = errorColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "نکات مهم پیش از حذف حساب",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        )
                        val username = profile?.username?.takeIf { it.isNotBlank() }
                        if (username != null) {
                            Text(
                                text = "@$username",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Text(
                    text = "• دوره انتظار ۱۴ روزه است؛ طی این مدت پروفایل شما مخفی و نشست‌ها بسته می‌شوند.\n" +
                        "• در طول ۱۴ روز، ثبت‌نام مجدد با شماره موبایل یا ایمیل فعلی امکان‌پذیر نیست.\n" +
                        "• سوابق امنیتی در موارد قانونی مطابق مقررات نگهداری می‌شود.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Reason selector (compact card group)
        Text(
            text = "علت حذف حساب را انتخاب کنید",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                deletionReasons.forEachIndexed { index, option ->
                    val selected = selectedReason == option.key
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectReason(option.key) }
                            .background(
                                if (selected) errorColor.copy(alpha = 0.06f) else Color.Transparent,
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectReason(option.key) },
                            colors = RadioButtonDefaults.colors(selectedColor = errorColor),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (index < deletionReasons.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    }
                }
            }
        }

        // Compact acknowledgment checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .clickable { onToggleAcknowledged(!acknowledgedPolicy) }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = acknowledgedPolicy,
                onCheckedChange = onToggleAcknowledged,
                colors = CheckboxDefaults.colors(checkedColor = errorColor),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "شرایط حذف حساب و دوره انتظار ۱۴ روزه را می‌پذیرم.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (!inlineError.isNullOrBlank()) {
            InlineErrorBanner(message = inlineError)
        }

        Button(
            onClick = onRequestOtp,
            enabled = acknowledgedPolicy && !isSaving,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = errorColor,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("در حال ارسال کد...")
            } else {
                Text(
                    text = "ادامه و دریافت کد تایید",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        OutlinedButton(
            onClick = onCancel,
            enabled = !isSaving,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        ) {
            Text("انصراف")
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DeleteAccountStepTwo(
    selectedReasonLabel: String,
    otpChannel: String?,
    accountHasPassword: Boolean,
    otpCode: String,
    accountPassword: String,
    passwordVisible: Boolean,
    resendRemainingSeconds: Int,
    isSaving: Boolean,
    inlineError: String?,
    onOtpChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onResendOtp: () -> Unit,
    onConfirm: () -> Unit,
    onBackStep: () -> Unit,
) {
    val errorColor = MaterialTheme.colorScheme.error
    val channelLabel = when (otpChannel?.lowercase()) {
        "sms" -> "شماره موبایل شما (پیامک)"
        "email" -> "ایمیل شما"
        else -> "شماره موبایل یا ایمیل ثبت‌شده"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MarkEmailRead,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "کد تایید به $channelLabel ارسال شد",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "علت: $selectedReasonLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        OutlinedTextField(
            value = otpCode,
            onValueChange = onOtpChange,
            label = { Text("کد تایید") },
            placeholder = { Text("کد ۶ رقمی") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(imageVector = Icons.Outlined.Shield, contentDescription = null) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (resendRemainingSeconds > 0) {
                    val mins = resendRemainingSeconds / 60
                    val secs = resendRemainingSeconds % 60
                    "ارسال مجدد تا ${"%02d:%02d".format(mins, secs)}"
                } else {
                    "کد را دریافت نکردید؟"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onResendOtp,
                enabled = resendRemainingSeconds == 0 && !isSaving,
            ) {
                Text("ارسال مجدد")
            }
        }

        if (accountHasPassword) {
            OutlinedTextField(
                value = accountPassword,
                onValueChange = onPasswordChange,
                label = { Text("رمز عبور حساب کاربری") },
                placeholder = { Text("رمز عبور اکانت خود را وارد کنید") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = { Icon(imageVector = Icons.Outlined.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisible) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "مخفی کردن" else "نمایش",
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!inlineError.isNullOrBlank()) {
            InlineErrorBanner(message = inlineError)
        }

        val canConfirm = otpCode.isNotBlank() && (!accountHasPassword || accountPassword.isNotBlank()) && !isSaving

        Button(
            onClick = onConfirm,
            enabled = canConfirm,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = errorColor, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("در حال ثبت...")
            } else {
                Text(
                    text = "تایید و حذف حساب",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        OutlinedButton(
            onClick = onBackStep,
            enabled = !isSaving,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        ) {
            Text("بازگشت")
        }
    }
}

@Composable
private fun DeleteAccountCompletedStep(
    gracePeriodDays: Int,
    onFinishLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "درخواست حذف حساب ثبت شد",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "حساب شما از دید عموم مخفی شد و دوره انتظار $gracePeriodDays روزه آغاز گردید. در این مدت امکان ثبت‌نام مجدد با این شماره/ایمیل وجود ندارد.",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onFinishLogout,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = "خروج از حساب",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun InlineErrorBanner(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
