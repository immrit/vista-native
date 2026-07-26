package ir.coffevista.vista_native.core.network

import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.common.ErrorKind
import org.json.JSONException
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

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
                403 -> if (throwable.code == "account_disabled") {
                    ErrorKind.ACCOUNT_DISABLED
                } else {
                    ErrorKind.FORBIDDEN
                }
                429 -> ErrorKind.RATE_LIMITED
                409 -> ErrorKind.CONFLICT
                in 500..599 -> ErrorKind.SERVER
                else -> ErrorKind.VALIDATION
            }
            return AppError(
                kind = kind,
                messageFa = persianMessage ?: fallback,
                code = throwable.code,
                retryAfterSeconds = throwable.retryAfterSeconds,
                causeType = throwable.cause?.javaClass?.name,
            )
        }

        return when (throwable) {
            is SocketTimeoutException -> AppError(
                ErrorKind.TIMEOUT,
                "اتصال به سرور برقرار نشد. لطفاً اینترنت خود را بررسی کنید",
                causeType = throwable.javaClass.name,
            )
            is SSLException -> AppError(
                ErrorKind.TLS,
                "برقراری ارتباط امن با سرور ممکن نشد",
                causeType = throwable.javaClass.name,
            )
            is IOException -> AppError(
                ErrorKind.NETWORK,
                "خطا در اتصال به سرور. لطفاً اینترنت خود را بررسی کنید",
                causeType = throwable.javaClass.name,
            )
            is JSONException,
            is SerializationException -> AppError(
                ErrorKind.MALFORMED_RESPONSE,
                "پاسخ سرور قابل پردازش نبود. لطفاً دوباره تلاش کنید",
                causeType = throwable.javaClass.name,
            )
            else -> AppError(
                ErrorKind.UNKNOWN,
                "خطای غیرمنتظره‌ای رخ داد. لطفاً دوباره تلاش کنید",
                causeType = throwable.javaClass.name,
            )
        }
    }

    private fun isPersianCharacter(char: Char): Boolean = char in '\u0600'..'\u06FF'
}
