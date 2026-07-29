package ir.coffevista.vista_native.features.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.component.VistaAuthLogo
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaComponentSize
import ir.coffevista.vista_native.core.designsystem.tokens.VistaRadii
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.theme.vistaMotion

data class AuthVisuals(
    val backIcon: Painter,
    val personIcon: Painter,
    val lockIcon: Painter,
    val visibilityIcon: Painter,
    val visibilityOffIcon: Painter,
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
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        val emphasizedMotion = MaterialTheme.vistaMotion.emphasized
        RibbonBackground()
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                fadeIn(tween(emphasizedMotion)) togetherWith
                    fadeOut(tween(emphasizedMotion))
            },
            label = "authStep",
        ) { step ->
            when (step) {
                AuthStep.IDENTIFIER -> IdentifierScreen(state, onAction, visuals)
                AuthStep.PASSWORD -> PasswordScreen(state, onAction, visuals)
                AuthStep.OTP -> OtpScreen(state, onAction, visuals)
                AuthStep.SET_PASSWORD -> PasswordSetupScreen(state, onAction, visuals)
            }
        }
    }
}

@Composable
private fun IdentifierScreen(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    visuals: AuthVisuals,
) {
    Box(Modifier.fillMaxSize()) {
        AuthScrollColumn {
            Spacer(Modifier.height(10.dp))
            VistaAuthLogo(Modifier.size(100.dp))
            Spacer(Modifier.height(44.dp))
            Text(
                text = "ورود به ویستا",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            // Compose and Flutter expose different font-box metrics for the
            // same 22sp Vazirmatn title. This calibrated gap preserves the
            // measured Flutter control position on the reference device.
            Spacer(Modifier.height(29.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                AuthTextField(
                    value = state.identifier,
                    onValueChange = { onAction(AuthAction.IdentifierChanged(it)) },
                    placeholder = "شماره موبایل، ایمیل یا نام کاربری",
                    enabled = true,
                    isError = false,
                    trailingIcon = visuals.personIcon,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onAction(AuthAction.Submit) }),
                    modifier = Modifier.semantics { contentDescription = "شناسه ورود" },
                )
            }
            Spacer(Modifier.height(24.dp))
            AuthPrimaryButton(
                label = state.visualPrimaryLabel(),
                loading = state.isLoading,
                enabled = !state.isLoading,
                onClick = { onAction(AuthAction.Submit) },
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "با ورود به ویستا، قوانین و مقررات را می‌پذیرم.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(y = if (state.isLoading) (-2.5).dp else 0.dp),
            )
        }
        if (state.errorMessage != null) {
            AuthSnackbar(
                text = state.errorMessage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun PasswordScreen(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    visuals: AuthVisuals,
) {
    AuthSecondaryColumn(
        onBack = { onAction(AuthAction.Back) },
        backIcon = visuals.backIcon,
        backContentSpacing = 28.dp,
    ) {
        Text(
            text = if (state.isRegistering) "انتخاب رمز عبور" else "رمز عبور خود را وارد کنید",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        if (state.isRegistering) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "رمز عبوری شامل حداقل ۸ کاراکتر ترکیبی از حروف و اعداد انتخاب کنید.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(32.dp))
        PasswordField(state, onAction, visuals)
        Spacer(Modifier.height(24.dp))
        AuthPrimaryButton(
            label = state.visualPrimaryLabel(),
            loading = state.isLoading,
            enabled = !state.isLoading,
            onClick = { onAction(AuthAction.Submit) },
        )
        state.errorMessage?.let {
            Spacer(Modifier.height(16.dp))
            AuthInlineError(it)
        }
    }
}

@Composable
private fun PasswordSetupScreen(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    visuals: AuthVisuals,
) {
    AuthSecondaryColumn(onBack = null, backIcon = visuals.backIcon) {
        Text(
            text = "تعیین رمز عبور",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "رمز عبوری شامل حداقل ۸ کاراکتر ترکیبی از حروف و اعداد انتخاب کنید.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
        )
        Spacer(Modifier.height(32.dp))
        PasswordField(state, onAction, visuals)
        Spacer(Modifier.height(24.dp))
        AuthPrimaryButton(
            label = state.visualPrimaryLabel(),
            loading = state.isLoading,
            enabled = true,
            onClick = { onAction(AuthAction.Submit) },
        )
        state.errorMessage?.let {
            Spacer(Modifier.height(16.dp))
            AuthInlineError(it)
        }
    }
}

@Composable
private fun PasswordField(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    visuals: AuthVisuals,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        AuthTextField(
            value = state.password,
            onValueChange = { onAction(AuthAction.PasswordChanged(it)) },
            placeholder = "رمز عبور",
            enabled = !state.isLoading,
            isError = state.errorMessage != null,
            leadingIcon = if (state.passwordVisible) visuals.visibilityOffIcon else visuals.visibilityIcon,
            onLeadingClick = { onAction(AuthAction.TogglePasswordVisibility) },
            trailingIcon = visuals.lockIcon,
            visualTransformation = if (state.passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onAction(AuthAction.Submit) }),
        )
    }
}

@Composable
private fun OtpScreen(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    visuals: AuthVisuals,
) {
    AuthSecondaryColumn(
        onBack = { onAction(AuthAction.Back) },
        backIcon = visuals.backIcon,
        backContentSpacing = 21.dp,
    ) {
        Text(
            text = "کد تایید",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "کد ارسال شده به شماره ${isolateLtr(state.normalizedPhone.orEmpty())} را وارد کنید",
            // Flutter and Android shape this mixed Persian/Latin run with
            // slightly different advances. The measured 0.25sp calibration
            // keeps the same line width on the API-33 reference device.
            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.4.sp),
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-3).dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OtpBoxes(
                value = state.otp,
                onValueChange = { onAction(AuthAction.OtpChanged(it)) },
                enabled = true,
                modifier = Modifier.offset(y = (-3).dp),
            )
        }
        state.errorMessage?.let {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.offset(y = (-3).dp)) {
                AuthInlineError(it)
            }
        }
        Spacer(Modifier.height(30.dp))
        if (state.resendSeconds > 0) {
            Text(
                text = state.visualOtpTimerText(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9E9E9E),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-3).dp),
                textAlign = TextAlign.Center,
            )
        } else {
            val resendColor = if (
                MaterialTheme.colorScheme.background == Color(0xFF09090F)
            ) {
                VistaBrandColors.Indigo
            } else {
                VistaBrandColors.IndigoDeep
            }
            TextButton(
                onClick = { onAction(AuthAction.ResendOtp) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .offset(y = (-3).dp),
            ) {
                Text(
                    text = state.visualOtpTimerText(),
                    style = MaterialTheme.typography.labelLarge,
                    color = resendColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(23.dp))
        AuthPrimaryButton(
            label = state.visualPrimaryLabel(),
            loading = state.isLoading,
            enabled = !state.isLoading,
            onClick = { onAction(AuthAction.Submit) },
        )
    }
}

@Composable
private fun AuthScrollColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            // Flutter's scroll view applies 24dp outside a child whose
            // min-height is still the full viewport. That places the centered
            // child 24dp below the viewport center.
            .padding(start = 24.dp, top = 48.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun AuthSecondaryColumn(
    onBack: (() -> Unit)?,
    backIcon: Painter,
    backContentSpacing: androidx.compose.ui.unit.Dp = 24.dp,
    backIconOffset: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 40.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(40.dp))
        if (onBack != null) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = backIcon,
                            contentDescription = "بازگشت",
                            modifier = Modifier.offset(y = backIconOffset),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
            Spacer(Modifier.height(backContentSpacing))
        } else {
            Spacer(Modifier.height(64.dp))
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null,
    onLeadingClick: (() -> Unit)? = null,
    trailingIcon: Painter? = null,
    onTrailingClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF09090F)
    val fieldColor = if (isDark) Color(0xFF1C1C2E) else Color(0xFFF3F4FF)
    val hintColor = if (isDark) Color(0xFF6E6E92) else Color(0xFF707787)
    val shape = RoundedCornerShape(VistaRadii.Medium)
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = fieldColor,
        unfocusedContainerColor = fieldColor,
        disabledContainerColor = fieldColor,
        errorContainerColor = fieldColor,
        focusedBorderColor = VistaBrandColors.Indigo,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        errorBorderColor = MaterialTheme.colorScheme.error,
        cursorColor = VistaBrandColors.Indigo,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(VistaComponentSize.TextField),
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Left,
            letterSpacing = 0.25.sp,
        ),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(VistaBrandColors.Indigo),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = true,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                isError = isError,
                placeholder = {
                    Text(
                        text = placeholder,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = hintColor,
                        textAlign = TextAlign.Left,
                    )
                },
                leadingIcon = if (leadingIcon != null) {
                    {
                        if (onLeadingClick != null) {
                            IconButton(onClick = onLeadingClick) {
                                Icon(
                                    painter = leadingIcon,
                                    contentDescription = null,
                                    tint = hintColor,
                                )
                            }
                        } else {
                            Icon(
                                painter = leadingIcon,
                                contentDescription = null,
                                tint = hintColor,
                            )
                        }
                    }
                } else {
                    null
                },
                trailingIcon = if (trailingIcon != null) {
                    {
                        if (onTrailingClick != null) {
                            IconButton(onClick = onTrailingClick) {
                                Icon(
                                    painter = trailingIcon,
                                    contentDescription = null,
                                    tint = hintColor,
                                )
                            }
                        } else {
                            Icon(
                                painter = trailingIcon,
                                contentDescription = null,
                                tint = hintColor,
                            )
                        }
                    }
                } else {
                    null
                },
                colors = colors,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 14.dp,
                    end = 12.dp,
                    bottom = 14.dp,
                ),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = shape,
                    )
                },
            )
        },
    )
}

@Composable
private fun OtpBoxes(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF09090F)
    val fieldColor = if (isDark) Color(0xFF1C1C2E) else Color(0xFFF3F4FF)
    val cursorColor = if (isDark) Color(0xFF6E6E92) else Color(0xFF9CA3AF)
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(Char::isDigit).take(5)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .semantics { contentDescription = "کد تایید" },
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(color = Color.Transparent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = Color.Transparent,
            ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .width(312.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(5) { index ->
                val focused = index == value.length && value.length < 5
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(VistaRadii.Medium))
                        .background(fieldColor)
                        .then(
                            if (focused) {
                                Modifier.border(
                                    width = 1.5.dp,
                                    color = VistaBrandColors.Indigo,
                                    shape = RoundedCornerShape(VistaRadii.Medium),
                                )
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = value.getOrNull(index)?.toString().orEmpty(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (focused) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(cursorColor),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthPrimaryButton(
    label: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(VistaComponentSize.Button),
        shape = RoundedCornerShape(VistaRadii.Medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4F46E5),
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = Color.White,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 24.dp,
            vertical = 14.dp,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Composable
private fun AuthInlineError(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFFE53935),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AuthSnackbar(
    text: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .shadow(6.dp, shape)
            .clip(shape)
            .background(Color(0xFF13131E))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Right,
        )
    }
}

@Composable
private fun RibbonBackground() {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF09090F)
    val base = if (isDark) Color.Black else Color.White
    val orb1 = if (isDark) Color(0xFF13131E) else Color(0xFFF5F5F7)
    val orb2 = if (isDark) Color(0xFF121212) else Color(0xFFEBEBF0)
    val orb3 = if (isDark) Color(0xFF222222) else Color(0xFFEEEEEE)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(base),
        ) {
            val shortest = maxWidth.coerceAtMost(maxHeight)
            val orb1Size = (shortest * 0.9f).coerceIn(260.dp, 420.dp)
            val orb2Size = (shortest * 1.1f).coerceIn(320.dp, 540.dp)
            val orb3Size = (shortest * 0.7f).coerceIn(220.dp, 360.dp)
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = orb1Size * -0.25f, y = orb1Size * -0.25f)
                    .size(orb1Size)
                    .blur(60.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(orb1, CircleShape),
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = orb2Size * 0.25f, y = orb2Size * 0.2f)
                    .size(orb2Size)
                    .blur(60.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(orb2, CircleShape),
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = orb3Size * 0.2f, y = shortest * 0.35f)
                    .size(orb3Size)
                    .blur(60.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(orb3, CircleShape),
            )
        }
    }
}
