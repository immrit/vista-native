package ir.coffevista.vista_native.features.auth

private const val LTR_ISOLATE = '\u2066'
private const val POP_DIRECTIONAL_ISOLATE = '\u2069'

internal fun isolateLtr(value: String): String =
    "$LTR_ISOLATE$value$POP_DIRECTIONAL_ISOLATE"

internal fun AuthUiState.visualPrimaryLabel(): String = when (step) {
    AuthStep.IDENTIFIER -> "ادامه"
    AuthStep.PASSWORD -> if (isRegistering) {
        "تایید و دریافت کد پیامکی"
    } else {
        "ورود"
    }
    AuthStep.OTP -> "تایید"
    AuthStep.SET_PASSWORD -> "ذخیره رمز و ادامه"
}

internal fun AuthUiState.visualOtpTimerText(): String =
    if (resendSeconds > 0) {
        "ارسال مجدد کد در $resendSeconds ثانیه"
    } else {
        "ارسال مجدد کد"
    }
