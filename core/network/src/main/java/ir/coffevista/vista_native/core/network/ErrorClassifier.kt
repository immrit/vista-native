package ir.coffevista.vista_native.core.network

import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.common.ErrorKind
import org.json.JSONException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

object ErrorClassifier {
    fun classify(throwable: Throwable, contextFa: String): AppError {
        val effectiveThrowable = when (throwable) {
            is RemoteFailure -> throwable
            is HttpException -> RemoteFailure.fromHttpException(throwable)
            else -> throwable
        }

        if (effectiveThrowable is RemoteFailure) {
            val persianMessage = effectiveThrowable.message
                ?.takeIf { message -> message.any(::isPersianCharacter) }
                ?: effectiveThrowable.code
                    ?.takeIf { code -> code.any(::isPersianCharacter) }

            val normalizedCode = effectiveThrowable.code?.lowercase()
                ?: effectiveThrowable.message?.lowercase()?.takeIf {
                    it in setOf("banned", "account_banned", "account_suspended", "account_disabled", "maintenance_mode", "feature_disabled")
                }

            val fallback = when (normalizedCode) {
                "maintenance_mode" -> "ویستا موقتاً در حال بروزرسانی است"
                "feature_disabled" -> featureDisabledMessageFa(effectiveThrowable.feature, contextFa)
                "banned", "account_banned", "account_suspended" -> "حساب کاربری یا دستگاه شما مسدود شده است"
                "account_disabled" -> "حساب کاربری غیرفعال است"
                else -> when (effectiveThrowable.statusCode) {
                    400 -> "اطلاعات ارسالی نامعتبر است"
                    401 -> "نام کاربری یا رمز عبور اشتباه است"
                    403 -> "حساب کاربری غیرفعال است"
                    409 -> "کاربر با این مشخصات قبلاً ثبت شده است"
                    413 -> "حجم فایل انتخابی بیش از حد مجاز است"
                    429 -> "تلاش‌های زیادی انجام شده است. لطفاً چند دقیقه دیگر دوباره تلاش کنید"
                    in 500..599 -> "سرور موقتاً در دسترس نیست. لطفاً کمی بعد دوباره تلاش کنید"
                    else -> "خطا در $contextFa. لطفاً دوباره تلاش کنید"
                }
            }
            val kind = when (effectiveThrowable.statusCode) {
                401 -> ErrorKind.UNAUTHORIZED
                403 -> if (normalizedCode in setOf("account_disabled", "banned", "account_banned", "account_suspended")) {
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
                code = normalizedCode ?: effectiveThrowable.code,
                retryAfterSeconds = effectiveThrowable.retryAfterSeconds,
                causeType = effectiveThrowable.cause?.javaClass?.name,
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

    private fun featureDisabledMessageFa(feature: String?, contextFa: String): String =
        when (feature?.trim()?.lowercase()) {
            "chat" -> "بخش گفتگو موقتاً غیرفعال شده است"
            "posts" -> "ارسال و ویرایش پست موقتاً غیرفعال شده است"
            "feed" -> "نمایش فید موقتاً غیرفعال شده است"
            "comments" -> "ارسال دیدگاه موقتاً غیرفعال شده است"
            "stories" -> "بخش استوری موقتاً غیرفعال شده است"
            "uploads" -> "بارگذاری فایل موقتاً غیرفعال شده است"
            "payments" -> "بخش پرداخت موقتاً غیرفعال شده است"
            "auth" -> "ورود موقتاً غیرفعال شده است"
            else -> if (contextFa.contains("ورود") || contextFa.contains("احراز")) {
                "ورود موقتاً غیرفعال شده است"
            } else {
                "این قابلیت موقتاً غیرفعال شده است"
            }
        }

    private fun isPersianCharacter(char: Char): Boolean = char in '\u0600'..'\u06FF'
}
