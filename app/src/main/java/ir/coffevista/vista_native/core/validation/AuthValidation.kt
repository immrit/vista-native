package ir.coffevista.vista_native.core.validation

private val iranPhonePattern = Regex("^09\\d{9}$")

fun normalizeDigits(input: String): String = buildString(input.length) {
    input.forEach { char ->
        append(
            when (char) {
                in '\u06F0'..'\u06F9' -> '0' + (char - '\u06F0')
                in '\u0660'..'\u0669' -> '0' + (char - '\u0660')
                else -> char
            },
        )
    }
}

fun normalizeIranPhone(input: String): String? {
    val compact = normalizeDigits(input).trim()
        .replace(" ", "")
        .replace("-", "")
        .replace("(", "")
        .replace(")", "")
    val normalized = when {
        compact.startsWith("+98") -> "0${compact.drop(3)}"
        compact.startsWith("0098") -> "0${compact.drop(4)}"
        compact.startsWith("98") && compact.length == 12 -> "0${compact.drop(2)}"
        compact.startsWith("9") && compact.length == 10 -> "0$compact"
        else -> compact
    }
    return normalized.takeIf(iranPhonePattern::matches)
}

fun validatePassword(password: String, phone: String? = null): String? {
    if (password.length < 8) return "رمز عبور باید حداقل ۸ کاراکتر باشد"
    if (password.any { it.isWhitespace() || it.isISOControl() }) {
        return "رمز عبور شامل کاراکتر نامعتبر است"
    }
    val categories = listOf(
        password.any { it in 'a'..'z' },
        password.any { it in 'A'..'Z' },
        password.any(Char::isDigit),
        password.any { !it.isLetterOrDigit() || it.code > 127 },
    ).count { it }
    if (categories < 2) return "رمز عبور باید ترکیبی از حروف و اعداد باشد"
    if (password.lowercase() in setOf(
            "password",
            "password123",
            "qwerty123",
            "12345678",
            "11111111",
            "00000000",
        )
    ) {
        return "رمز عبور انتخاب‌شده ضعیف است"
    }
    if (phone != null && password.contains(phone.takeLast(4))) {
        return "رمز عبور نباید شامل بخش پایانی شماره موبایل باشد"
    }
    return null
}
