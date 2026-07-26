package ir.coffevista.vista_native.features.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AuthVisuals(
    val accentColor: Color,
    val personIcon: Painter,
    val lockIcon: Painter,
    val visibilityIcon: Painter,
    val visibilityOffIcon: Painter,
    val brand: @Composable (modifier: Modifier, compact: Boolean) -> Unit,
)

@Composable
fun AuthScreen(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    onAuthenticated: () -> Unit,
    visuals: AuthVisuals,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.step != AuthStep.IDENTIFIER && state.step != AuthStep.SET_PASSWORD) {
        onAction(AuthAction.Back)
    }
    LaunchedEffect(state.completedContext) {
        if (state.completedContext != null) onAuthenticated()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        visuals.accentColor.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (state.step == AuthStep.IDENTIFIER) {
                visuals.brand(Modifier.size(100.dp), false)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    visuals.brand(Modifier.size(52.dp), true)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "VISTA",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.4.sp,
                        )
                        Text(
                            text = "ورود امن به دنیای نزدیک‌تر",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { onAction(AuthAction.Back) },
                        enabled = !state.isLoading,
                    ) {
                        Text("بازگشت")
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 34.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 12.dp,
            ) {
                Column(Modifier.padding(24.dp)) {
                    StepIndicator(step = state.step)
                    AnimatedContent(
                        targetState = state.step,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "authStep",
                    ) { step ->
                        Column {
                            when (step) {
                                AuthStep.IDENTIFIER -> IdentifierStep(state, onAction, visuals)
                                AuthStep.PASSWORD -> PasswordStep(state, onAction, visuals)
                                AuthStep.OTP -> OtpStep(state, onAction)
                                AuthStep.SET_PASSWORD -> PasswordSetupStep(state, onAction, visuals)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = state.infoMessage != null,
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        MessageSurface(
                            message = state.infoMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    AnimatedVisibility(
                        visible = state.errorMessage != null,
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        MessageSurface(
                            message = state.errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Button(
                        onClick = { onAction(AuthAction.Submit) },
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 22.dp)
                            .height(54.dp),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(21.dp),
                                color = Color.White,
                                strokeWidth = 2.3.dp,
                            )
                        } else {
                            Text(
                                text = submitLabel(state),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Text(
                text = "با ادامه، قوانین استفاده و حریم خصوصی ویستا را می‌پذیرید.",
                modifier = Modifier.padding(top = 20.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun IdentifierStep(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    visuals: AuthVisuals,
) {
    StepHeader(
        title = "ورود به ویستا",
        description = "شماره موبایل، ایمیل یا نام کاربری خود را وارد کنید.",
    )
    OutlinedTextField(
        value = state.identifier,
        onValueChange = { onAction(AuthAction.IdentifierChanged(it)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        label = { Text("شناسه ورود") },
        placeholder = { Text("مثلاً 09123456789") },
        singleLine = true,
        enabled = !state.isLoading,
        keyboardActions = KeyboardActions(onDone = { onAction(AuthAction.Submit) }),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        leadingIcon = {
            Icon(
                painter = visuals.personIcon,
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun PasswordStep(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    visuals: AuthVisuals,
) {
    StepHeader(
        title = when {
            state.isTwoFactor -> "تایید دومرحله‌ای"
            state.isRegistering -> "ساخت حساب"
            else -> "رمز عبور"
        },
        description = when {
            state.isTwoFactor -> "برای تکمیل ورود، رمز عبور حساب را وارد کنید."
            state.isRegistering -> "یک رمز امن انتخاب کنید؛ بعد شماره موبایل را تایید می‌کنیم."
            else -> "رمز عبور حساب ${state.identifier} را وارد کنید."
        },
    )
    PasswordField(state, onAction, visuals)
}

@Composable
private fun PasswordSetupStep(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    visuals: AuthVisuals,
) {
    StepHeader(
        title = "تعیین رمز عبور",
        description = "رمز باید حداقل ۸ کاراکتر و ترکیبی از دو گروه کاراکتری باشد.",
    )
    PasswordField(state, onAction, visuals)
}

@Composable
private fun PasswordField(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    visuals: AuthVisuals,
) {
    OutlinedTextField(
        value = state.password,
        onValueChange = { onAction(AuthAction.PasswordChanged(it)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        label = { Text("رمز عبور") },
        singleLine = true,
        enabled = !state.isLoading,
        visualTransformation = if (state.passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        leadingIcon = {
            Icon(
                painter = visuals.lockIcon,
                contentDescription = null,
            )
        },
        trailingIcon = {
            IconButton(onClick = { onAction(AuthAction.TogglePasswordVisibility) }) {
                Icon(
                    painter = if (state.passwordVisible) {
                        visuals.visibilityOffIcon
                    } else {
                        visuals.visibilityIcon
                    },
                    contentDescription = if (state.passwordVisible) {
                        "پنهان کردن رمز"
                    } else {
                        "نمایش رمز"
                    },
                )
            }
        },
        keyboardActions = KeyboardActions(onDone = { onAction(AuthAction.Submit) }),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
    )
}

@Composable
private fun OtpStep(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
) {
    StepHeader(
        title = "تایید شماره موبایل",
        description = "کد پنج‌رقمی ارسال‌شده به ${state.normalizedPhone.orEmpty()} را وارد کنید.",
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value = state.otp,
            onValueChange = { onAction(AuthAction.OtpChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            label = { Text("کد تایید") },
            placeholder = { Text("— — — — —") },
            singleLine = true,
            enabled = !state.isLoading,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                textAlign = TextAlign.Center,
                letterSpacing = 8.sp,
                fontWeight = FontWeight.Bold,
            ),
            keyboardActions = KeyboardActions(onDone = { onAction(AuthAction.Submit) }),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
        )
    }
    OutlinedButton(
        onClick = { onAction(AuthAction.ResendOtp) },
        enabled = state.resendSeconds == 0 && !state.isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
    ) {
        Text(
            if (state.resendSeconds > 0) {
                "ارسال دوباره تا ${state.resendSeconds} ثانیه"
            } else {
                "ارسال دوباره کد"
            },
        )
    }
}

@Composable
private fun StepHeader(
    title: String,
    description: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Black,
    )
    Text(
        text = description,
        modifier = Modifier.padding(top = 8.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        lineHeight = 23.sp,
    )
}

@Composable
private fun StepIndicator(step: AuthStep) {
    val activeIndex = when (step) {
        AuthStep.IDENTIFIER -> 0
        AuthStep.PASSWORD, AuthStep.SET_PASSWORD -> 1
        AuthStep.OTP -> 2
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        repeat(3) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(
                        if (index <= activeIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        },
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun MessageSurface(
    message: String,
    color: Color,
) {
    Surface(
        color = color.copy(alpha = 0.09f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            color = color,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
        )
    }
}

private fun submitLabel(state: AuthUiState): String = when (state.step) {
    AuthStep.IDENTIFIER -> "ادامه"
    AuthStep.PASSWORD -> when {
        state.isTwoFactor -> "تایید و ورود"
        state.isRegistering -> "دریافت کد تایید"
        else -> "ورود به ویستا"
    }
    AuthStep.OTP -> "تایید کد"
    AuthStep.SET_PASSWORD -> "ذخیره رمز و ادامه"
}
