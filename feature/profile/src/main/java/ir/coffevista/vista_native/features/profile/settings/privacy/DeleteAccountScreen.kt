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
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.PersonOff
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    val description: String,
)

private val deletionReasons = listOf(
    DeletionReasonOption(
        key = "not_using",
        title = "دیگر از ویستا استفاده نمی‌کنم",
        description = "فعلاً نیازی به این حساب کاربری ندارم",
    ),
    DeletionReasonOption(
        key = "privacy",
        title = "نگرانی درباره حریم خصوصی",
        description = "می‌خواهم اطلاعات عمومی و پروفایلم از دسترس خارج شود",
    ),
    DeletionReasonOption(
        key = "notifications",
        title = "اعلان‌ها و پیام‌های زیاد",
        description = "حجم اعلان‌ها یا گفتگوها بیش از انتظار من است",
    ),
    DeletionReasonOption(
        key = "alternative",
        title = "جایگزین دیگری پیدا کردم",
        description = "قصد مهاجرت به پلتفرم یا حساب دیگری را دارم",
    ),
    DeletionReasonOption(
        key = "technical_issues",
        title = "مشکلات فنی و خطاهای برنامه",
        description = "باگ یا کندی مانع استفاده راحت از برنامه شده است",
    ),
    DeletionReasonOption(
        key = "other",
        title = "سایر موارد",
        description = "دلیل شخصی یا خارج از گزینه‌های بالا",
    ),
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
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var otpChannel by remember { mutableStateOf<String?>(null) }
    var resendRemainingSeconds by remember { mutableIntStateOf(0) }
    var gracePeriodDays by remember { mutableIntStateOf(30) }
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
                                        else -> "ثبت نهایی"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = if (step == 3) {
                                        Color(0xFF047857)
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
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
                                onSent = { channel, expiresIn ->
                                    otpChannel = channel
                                    resendRemainingSeconds = expiresIn.toInt().coerceIn(30, 300)
                                    otpCode = ""
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
                        otpCode = otpCode,
                        password = password,
                        passwordVisible = passwordVisible,
                        resendRemainingSeconds = resendRemainingSeconds,
                        isSaving = uiState.isSaving,
                        inlineError = inlineError,
                        onOtpChange = { value ->
                            otpCode = value.filter { it.isDigit() || it.isLetter() }.take(12)
                            inlineError = null
                        },
                        onPasswordChange = {
                            password = it
                            inlineError = null
                        },
                        onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                        onResendOtp = {
                            inlineError = null
                            viewModel.requestAccountDeletionOtp(
                                reason = selectedReason,
                                onSent = { channel, expiresIn ->
                                    otpChannel = channel
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
                                inlineError = "لطفاً کد تایید ارسال‌شده را وارد کنید"
                                return@DeleteAccountStepTwo
                            }
                            inlineError = null
                            viewModel.confirmAccountDeletion(
                                otpCode = otpCode.trim(),
                                password = password,
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Hero warning card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = errorColor.copy(alpha = 0.08f),
            ),
            border = BorderStroke(1.dp, errorColor.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(errorColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = errorColor,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "حذف حساب ویستا",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val identityText = buildString {
                            val name = profile?.fullName?.takeIf { it.isNotBlank() }
                            val username = profile?.username?.takeIf { it.isNotBlank() }
                            if (name != null) append(name)
                            if (username != null) {
                                if (isNotEmpty()) append(" • ")
                                append("@$username")
                            }
                        }
                        if (identityText.isNotEmpty()) {
                            Text(
                                text = identityText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Text(
                    text = "با ثبت این درخواست، حساب شما بلافاصله غیرفعال می‌شود و وارد دوره انتظار ۳۰ روزه خواهد شد. در این صفحه می‌توانید پیامدها و علت ترک خود را مشخص کنید.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Consequence & Policy Summary
        Text(
            text = "سیاست حذف و نگهداری داده‌ها",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PolicyInfoRow(
                    icon = Icons.Outlined.HourglassBottom,
                    iconTint = Color(0xFFD97706),
                    title = "دوره انتظار ۳۰ روزه",
                    body = "تا ۳۰ روز امکان لغو درخواست و بازگرداندن حساب از طریق پشتیبانی وجود دارد. پس از پایان این بازه، پاک‌سازی خودکار اجرا می‌شود.",
                )
                PolicyInfoRow(
                    icon = Icons.Outlined.PersonOff,
                    iconTint = errorColor,
                    title = "خروج از دسترسی عمومی و بسته شدن نشست‌ها",
                    body = "بلافاصله پس از تایید، پروفایل، پست‌ها و حساب شما از دید کاربران و جستجو پنهان شده و تمامی نشست‌های فعال قطع می‌شوند.",
                )
                PolicyInfoRow(
                    icon = Icons.Outlined.Gavel,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "سوابق امنیتی و الزامات قانونی",
                    body = "مطابق قوانین، در صورت وجود گزارش تخلف یا دستور مراجع قضایی، داده‌های ضروری و تاریخچه گفتگوها می‌تواند در بازه قانونی نگهداری شود.",
                )
            }
        }

        // Reason selector
        Text(
            text = "علت حذف حساب خود را انتخاب کنید",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            deletionReasons.forEach { option ->
                val selected = selectedReason == option.key
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            errorColor.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) {
                            errorColor.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectReason(option.key) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectReason(option.key) },
                            colors = RadioButtonDefaults.colors(selectedColor = errorColor),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Policy acknowledgment checkbox
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onToggleAcknowledged(!acknowledgedPolicy) },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = acknowledgedPolicy,
                    onCheckedChange = onToggleAcknowledged,
                    colors = CheckboxDefaults.colors(checkedColor = errorColor),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "شرایط حذف حساب کاربری، دوره انتظار ۳۰ روزه و بسته شدن فوری نشست‌های فعال خود را مطالعه کرده و می‌پذیرم.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (!inlineError.isNullOrBlank()) {
            InlineErrorBanner(message = inlineError)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    .height(50.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("در حال ارسال کد تایید...")
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
                    .height(48.dp),
            ) {
                Text("انصراف و بازگشت")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DeleteAccountStepTwo(
    selectedReasonLabel: String,
    otpChannel: String?,
    otpCode: String,
    password: String,
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
        "sms" -> "شماره موبایل ثبت‌شده (پیامک)"
        "email" -> "آدرس ایمیل ثبت‌شده"
        else -> "شماره موبایل یا ایمیل ثبت‌شده"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MarkEmailRead,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تایید هویت امنیتی",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "کد تایید یک‌بارمصرف به $channelLabel ارسال شد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "علت انتخاب‌شده: $selectedReasonLabel",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = otpCode,
            onValueChange = onOtpChange,
            label = { Text("کد تایید") },
            placeholder = { Text("کد ارسال‌شده را وارد کنید") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Shield, contentDescription = null)
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (resendRemainingSeconds > 0) {
                val mins = resendRemainingSeconds / 60
                val secs = resendRemainingSeconds % 60
                Text(
                    text = "ارسال مجدد تا ${"%02d:%02d".format(mins, secs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "کد را دریافت نکردید؟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onResendOtp,
                enabled = resendRemainingSeconds == 0 && !isSaving,
            ) {
                Text("ارسال مجدد کد")
            }
        }

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("رمز تایید دو مرحله‌ای (در صورت فعال بودن)") },
            placeholder = { Text("اگر رمز تایید دو مرحله‌ای ندارید خالی بگذارید") },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisible) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = if (passwordVisible) "مخفی کردن رمز" else "نمایش رمز",
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        if (!inlineError.isNullOrBlank()) {
            InlineErrorBanner(message = inlineError)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = onConfirm,
            enabled = otpCode.isNotBlank() && !isSaving,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = errorColor,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("در حال ثبت درخواست...")
            } else {
                Text(
                    text = "تایید نهایی و ثبت حذف حساب",
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
                .height(48.dp),
        ) {
            Text("بازگشت به مرحله قبل")
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
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(42.dp),
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "درخواست حذف حساب ثبت شد",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "حساب شما بلافاصله از حالت عمومی خارج شد و وارد دوره انتظار $gracePeriodDays روزه شده است. تمام نشست‌های فعال شما بسته شدند. اگر پیش از پایان این مهلت منصرف شدید، می‌توانید با پشتیبانی ویستا تماس بگیرید تا درخواست لغو شود.",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onFinishLogout,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Text(
                text = "خروج امن از حساب",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun PolicyInfoRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    body: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
