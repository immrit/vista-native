package ir.coffevista.vista_native

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.coffevista.vista_native.core.designsystem.theme.VistaTheme
import ir.coffevista.vista_native.features.auth.AuthAction
import ir.coffevista.vista_native.features.auth.AuthScreen
import ir.coffevista.vista_native.features.auth.AuthStep
import ir.coffevista.vista_native.features.auth.AuthUiState
import ir.coffevista.vista_native.features.auth.AuthVisuals
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthVisualParityInstrumentationTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun capturesNineFlutterParityStatesFromRealComposeRuntime() {
        lateinit var state: MutableState<AuthUiState>
        launchHarness { state = it }.use {
            composeRule.onNodeWithText("ورود به ویستا").assertIsDisplayed()
            settleTransition()
            capture("native-01-login-initial.png")

            composeRule.onNodeWithContentDescription("شناسه ورود").performClick()
            composeRule.onNodeWithContentDescription("شناسه ورود").performTextInput("0")
            composeRule.onNodeWithContentDescription("شناسه ورود").performTextClearance()
            waitForImeAnimation()
            capture("native-02-login-focused.png")

            composeRule.onNodeWithContentDescription("شناسه ورود")
                .performTextInput("09123456789")
            composeRule.runOnIdle {
                state.value = state.value.copy(identifier = "09123456789")
            }
            composeRule.onNodeWithContentDescription("شناسه ورود")
                .assertTextEquals("09123456789")
            capture("native-03-login-filled.png")

            closeKeyboard()
            composeRule.runOnIdle {
                state.value = state.value.copy(
                    identifier = "",
                    errorMessage = "لطفاً ورودی را کامل کنید",
                )
            }
            composeRule.onNodeWithText("لطفاً ورودی را کامل کنید").assertIsDisplayed()
            SystemClock.sleep(500)
            composeRule.waitForIdle()
            capture("native-04-login-error.png")

            composeRule.runOnIdle {
                state.value = state.value.copy(
                    identifier = "09123456789",
                    errorMessage = null,
                    isLoading = true,
                )
            }
            composeRule.onNodeWithContentDescription("شناسه ورود")
                .assertTextEquals("09123456789")
            composeRule.onNodeWithText("لطفاً ورودی را کامل کنید").assertDoesNotExist()
            composeRule.onNodeWithText("ادامه").assertDoesNotExist()
            SystemClock.sleep(500)
            capture("native-05-login-loading.png")

            composeRule.runOnIdle {
                state.value = AuthUiState(
                    step = AuthStep.OTP,
                    normalizedPhone = "09123456789",
                    resendSeconds = 56,
                )
            }
            settleTransition()
            composeRule.onNodeWithContentDescription("کد تایید").performClick()
            waitForImeAnimation()
            capture("native-06-otp-initial.png")

            composeRule.onNodeWithContentDescription("کد تایید").performTextInput("12")
            composeRule.runOnIdle { state.value = state.value.copy(resendSeconds = 25) }
            composeRule.onNodeWithText("ارسال مجدد کد در 25 ثانیه").assertIsDisplayed()
            SystemClock.sleep(500)
            composeRule.waitForIdle()
            capture("native-07-otp-partial.png")

            closeKeyboard()
            composeRule.runOnIdle {
                state.value = state.value.copy(
                    otp = "12345",
                    resendSeconds = 20,
                    errorMessage = "کد تایید نامعتبر است",
                )
            }
            composeRule.onNodeWithText("کد تایید نامعتبر است").assertIsDisplayed()
            capture("native-08-otp-error.png")

            composeRule.runOnIdle {
                state.value = state.value.copy(resendSeconds = 0)
            }
            composeRule.onNodeWithText("ارسال مجدد کد").assertIsDisplayed()
            capture("native-09-otp-resend.png")
        }
    }

    @Test
    fun rtlPersianCopySurvivesActivityRecreation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("vista.foundation.fixture", "offline-no-session")
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            composeRule.onNodeWithText("ورود به ویستا").assertIsDisplayed()
            scenario.recreate()
            composeRule.onNodeWithText("ورود به ویستا").assertIsDisplayed()
        }
    }

    private fun launchHarness(
        onState: (MutableState<AuthUiState>) -> Unit,
    ): ActivityScenario<MainActivity> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val scenario = ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java),
        )
        scenario.onActivity { activity ->
            val state = mutableStateOf(AuthUiState())
            onState(state)
            activity.setContent {
                VistaTheme(darkTheme = false) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        CompositionLocalProvider(
                            LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl,
                        ) {
                            AuthScreen(
                                state = state.value,
                                onAction = { action ->
                                    when (action) {
                                        is AuthAction.IdentifierChanged -> {
                                            state.value = state.value.copy(identifier = action.value)
                                        }
                                        is AuthAction.OtpChanged -> {
                                            state.value = state.value.copy(otp = action.value)
                                        }
                                        else -> Unit
                                    }
                                },
                                onAuthenticated = {},
                                visuals = AuthVisuals(
                                    backIcon = painterResource(R.drawable.ic_auth_arrow_back),
                                    personIcon = painterResource(R.drawable.ic_person_outline),
                                    lockIcon = painterResource(R.drawable.ic_lock_outline),
                                    visibilityIcon = painterResource(R.drawable.ic_visibility),
                                    visibilityOffIcon = painterResource(R.drawable.ic_visibility_off),
                                ),
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        return scenario
    }

    private fun closeKeyboard() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("input keyevent 4")
            .close()
        waitForImeAnimation()
    }

    private fun waitForImeAnimation() {
        SystemClock.sleep(900)
        composeRule.waitForIdle()
    }

    private fun settleTransition() {
        SystemClock.sleep(900)
        composeRule.waitForIdle()
    }

    private fun capture(filename: String) {
        // Semantics can reach idle one frame before the platform screenshot
        // surface is updated. Allow the rendered frame to settle so evidence
        // always reflects the asserted state.
        SystemClock.sleep(300)
        composeRule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(
            instrumentation.targetContext.getExternalFilesDir(null),
            "visual-parity",
        ).apply { mkdirs() }
        val scopedFile = File(directory, filename)
        FileOutputStream(scopedFile).use { output ->
            check(instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        shell("mkdir -p /sdcard/Download/vista-login-parity")
        shell("cp ${scopedFile.absolutePath} /sdcard/Download/vista-login-parity/$filename")
    }

    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
            .use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
            }
    }
}
