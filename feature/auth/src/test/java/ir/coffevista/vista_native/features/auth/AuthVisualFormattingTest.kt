package ir.coffevista.vista_native.features.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthVisualFormattingTest {
    @Test
    fun `phone and username are isolated as ltr inside rtl copy`() {
        assertEquals("\u206609123456789\u2069", isolateLtr("09123456789"))
        assertEquals("\u2066@vista_user\u2069", isolateLtr("@vista_user"))
    }

    @Test
    fun `visual labels map every login state`() {
        assertEquals("ادامه", AuthUiState().visualPrimaryLabel())
        assertEquals(
            "تایید و دریافت کد پیامکی",
            AuthUiState(step = AuthStep.PASSWORD, isRegistering = true).visualPrimaryLabel(),
        )
        assertEquals("ورود", AuthUiState(step = AuthStep.PASSWORD).visualPrimaryLabel())
        assertEquals("تایید", AuthUiState(step = AuthStep.OTP).visualPrimaryLabel())
        assertEquals(
            "ذخیره رمز و ادامه",
            AuthUiState(step = AuthStep.SET_PASSWORD).visualPrimaryLabel(),
        )
    }

    @Test
    fun `otp resend copy maps timer and available states`() {
        assertEquals(
            "ارسال مجدد کد در 42 ثانیه",
            AuthUiState(step = AuthStep.OTP, resendSeconds = 42).visualOtpTimerText(),
        )
        assertEquals(
            "ارسال مجدد کد",
            AuthUiState(step = AuthStep.OTP, resendSeconds = 0).visualOtpTimerText(),
        )
    }
}
