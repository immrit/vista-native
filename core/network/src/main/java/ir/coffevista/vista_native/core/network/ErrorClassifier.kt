package ir.coffevista.vista_native.core.network

import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.common.ErrorKind
import org.json.JSONException
import java.io.IOException
import java.net.SocketTimeoutException

object ErrorClassifier {
    fun classify(throwable: Throwable, contextFa: String): AppError {
        if (throwable is RemoteFailure) {
            val persianMessage = throwable.message
                ?.takeIf { message -> message.any(::isPersianCharacter) }
            val fallback = when (throwable.code?.lowercase()) {
                "maintenance_mode" -> "ویستا موقتاً در حال بروزرسانی است"
                "feature_disabled" -> "ورود موقتاً غیرفعال شده است"
                else -> when (throwable.statusCode) {
                    400 -> "اطلاعات ارسالی نامعتبر است"
                    401 -> "نام کاربری یا رمز عبور اشتباه است"
                    403 -> "حساب کاربری غیرفعال است"
                    409 -> "کاربر با این مشخصات قبلاً ثبت شده است"
                    429 -> "تلاش‌های زیادی انجام شده است. لطفاً چند دقیقه دیگر دوباره تلاش کنید"
                    in 500..599 -> "سرور موقتاً در دسترس نیست. لطفاً کمی بعد دوباره تلاش کنید"
                    else -> "خطا در $contextFa. لطفاً دوباره تلاش کنید"
                }
            }
            val kind = when (throwable.statusCode) {
                401 -> ErrorKind.UNAUTHORIZED
                403 -> ErrorKind.ACCOUNT_DISABLED
                429 -> ErrorKind.RATE_LIMITED
                in 500..599 -> ErrorKind.SERVER
                else -> ErrorKind.UNKNOWN
            }
            return AppError(
                kind = kind,
                messageFa = persianMessage ?: fallback,
                code = throwable.code,
                retryAfterSeconds = throwable.retryAfterSeconds,
            )
        }

        return when (throwable) {
            is SocketTimeoutException -> AppError(
                ErrorKind.NETWORK,
                "اتصال به سرور برقرار نشد. لطفاً اینترنت خود را بررسی کنید",
            )
            is IOException -> AppError(
                ErrorKind.NETWORK,
                "خطا در اتصال به سرور. لطفاً اینترنت خود را بررسی کنید",
            )
            is JSONException -> AppError(
                ErrorKind.MALFORMED_RESPONSE,
                "پاسخ سرور قابل پردازش نبود. لطفاً دوباره تلاش کنید",
            )
            else -> AppError(
                ErrorKind.UNKNOWN,
                "خطای غیرمنتظره‌ای رخ داد. لطفاً دوباره تلاش کنید",
            )
        }
    }

    private fun isPersianCharacter(char: Char): Boolean = char in '\u0600'..'\u06FF'
}
