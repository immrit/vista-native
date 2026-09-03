package ir.coffevista.vista_native.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import ir.coffevista.vista_native.core.security.BiometricAuthenticator
import ir.coffevista.vista_native.core.security.BiometricAvailability

data class BiometricLoginUiState(
    val isAuthenticating: Boolean = false,
    val error: String? = null,
)

@Composable
fun BiometricLoginScreen(
    onAuthenticated: () -> Unit,
    onUsePasswordOrOtp: () -> Unit,
    biometricAuthenticator: BiometricAuthenticator? = null,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text("ورود امن", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "برای ورود به حساب ویستا، هویت خود را با بیومتریک یا قفل امن دستگاه تایید کنید.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
            Button(
                onClick = {
                    if (activity == null || biometricAuthenticator == null) {
                        onUsePasswordOrOtp()
                        return@Button
                    }
                    if (biometricAuthenticator.availability() != BiometricAvailability.AVAILABLE) {
                        errorMessage = "بیومتریک یا قفل امن دستگاه در دسترس نیست"
                        return@Button
                    }
                    isAuthenticating = true
                    errorMessage = null
                    biometricAuthenticator.authenticate(activity) { authenticated ->
                        isAuthenticating = false
                        if (authenticated) {
                            onAuthenticated()
                        } else {
                            errorMessage = "تایید هویت بیومتریک انجام نشد"
                        }
                    }
                },
                enabled = !isAuthenticating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isAuthenticating) "در حال تایید…" else "تایید با بیومتریک")
            }
            OutlinedButton(
                onClick = onUsePasswordOrOtp,
                enabled = !isAuthenticating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("ورود با رمز یا پیامک")
            }
        }
    }
}
