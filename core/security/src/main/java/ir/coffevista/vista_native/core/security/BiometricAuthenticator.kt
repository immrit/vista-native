package ir.coffevista.vista_native.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    NOT_ENROLLED,
    UNAVAILABLE,
}

interface BiometricAuthenticator {
    fun availability(): BiometricAvailability
    fun authenticate(activity: FragmentActivity, onResult: (Boolean) -> Unit)
}

/** AndroidX prompt adapter. It never treats cancellation/error as authentication success. */
@Singleton
class AndroidBiometricAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context,
) : BiometricAuthenticator {
    override fun availability(): BiometricAvailability = when (
        BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS)
    ) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
        else -> BiometricAvailability.UNAVAILABLE
    }

    override fun authenticate(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
        if (availability() != BiometricAvailability.AVAILABLE) {
            onResult(false)
            return
        }
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(false)
            }

            override fun onAuthenticationFailed() = Unit
        }
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("تایید هویت")
                .setSubtitle("برای ادامه، هویت خود را تایید کنید")
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build(),
        )
    }
}

private const val ALLOWED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
