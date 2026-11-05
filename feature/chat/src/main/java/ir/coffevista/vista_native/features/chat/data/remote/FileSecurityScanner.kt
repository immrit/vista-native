package ir.coffevista.vista_native.features.chat.data.remote

import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale

/**
 * سامانه بازرسی امنیتی و پایش بدافزار برای فایل‌های پیوست (مشابه معماری امنیتی تلگرام).
 *
 * ۱. محدودسازی اکید به فایل‌های امن:
 *    - اسناد: منحصراً PDF (.pdf / application/pdf)
 *    - صوت و موسیقی: MP3, M4A, AAC, WAV, OGG, FLAC
 *    - تصویر: JPG, JPEG, PNG, WEBP, GIF
 *    - ویدیو: MP4, MOV, WEBM, MKV
 *    - مسدودسازی قطعی فایل‌های اجرایی، اسکریپت‌ها، ماکروها و آرشیوهای حاوی کد (.exe, .apk, .bat, .sh, .vbs, .js, .msi, ...)
 *
 * ۲. بازرسی بایت‌های جادویی (Magic Bytes Anti-Spoofing):
 *    - فایل‌های تغییرنام‌یافته (مثلاً malware.exe -> contract.pdf) بلافاصله کشف و مسدود می‌شوند.
 *
 * ۳. تحلیل اکتشافی بدافزار و کدهای اجرایی (Malware & Executable Heuristics):
 *    - شناسایی هدرهای اجرایی ویندوز (PE / MZ و استاب DOS)
 *    - شناسایی باینری‌های لینوکس و اندروید (ELF)
 *    - شناسایی بایت‌کد دالویک اندروید (DEX / APK)
 *    - شناسایی باینری‌های Mach-O و اسکریپت‌های اجرایی خطرناک
 *
 * ۴. محاسبه چک‌سام امنیتی SHA-256 برای راستی‌آزمایی جامع.
 */
object FileSecurityScanner {

    val ALLOWED_DOCUMENT_EXTENSIONS = setOf("pdf")
    val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
    val ALLOWED_VIDEO_EXTENSIONS = setOf("mp4", "mov", "webm", "mkv", "3gp", "m4v")
    val ALLOWED_AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac")

    val FORBIDDEN_EXECUTABLE_EXTENSIONS = setOf(
        "exe", "dll", "apk", "xapk", "bat", "cmd", "sh", "bash", "vbs", "vbe", "js", "jse",
        "wsf", "wsh", "msc", "msi", "msp", "com", "scr", "hta", "cpl", "jar", "bin", "elf",
        "so", "dylib", "deb", "rpm", "dmg", "iso", "img", "app", "action", "gadget", "inf",
        "ins", "inx", "isu", "job", "pif", "reg", "rgs", "sct", "vb", "ws",
        "docm", "xlsm", "pptm", "dotm", "xltm", "xlam", "ppam", "ppsm", "sldm",
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz"
    )

    // Magic Signatures
    private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF
    private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val GIF_MAGIC = byteArrayOf(0x47, 0x49, 0x46) // GIF
    private val ID3_MAGIC = byteArrayOf(0x49, 0x44, 0x33) // ID3
    private val OGG_MAGIC = byteArrayOf(0x4F, 0x67, 0x67, 0x53) // OggS
    private val FLAC_MAGIC = byteArrayOf(0x66, 0x4C, 0x61, 0x43) // fLaC
    private val EBML_MAGIC = byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte())

    // Known Executable / Malware Headers
    private val PE_MZ_MAGIC = byteArrayOf(0x4D, 0x5A) // MZ (Windows Portable Executable / DOS)
    private val ELF_MAGIC = byteArrayOf(0x7F, 0x45, 0x4C, 0x46) // \x7fELF (Linux/Android binary)
    private val DEX_MAGIC = byteArrayOf(0x64, 0x65, 0x78, 0x0A) // dex\n (Android Dalvik bytecode)
    private val MACHO_32 = byteArrayOf(0xFE.toByte(), 0xED.toByte(), 0xFA.toByte(), 0xCE.toByte())
    private val MACHO_64 = byteArrayOf(0xFE.toByte(), 0xED.toByte(), 0xFA.toByte(), 0xCF.toByte())
    private val MACHO_FAT = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())
    private val SHEBANG = byteArrayOf(0x23, 0x21) // #! (Unix shell script)

    /**
     * بررسی جامع امنیتی بایت‌های اولیه فایل.
     * در صورت وجود کوچکترین تهدید یا عدم تطابق، خطا می‌دهد.
     */
    fun verifySafety(fileName: String, header: ByteArray) {
        val extension = fileName.substringAfterLast('.', "")
            .lowercase(Locale.US)
            .filter(Char::isLetterOrDigit)

        // ۱. بررسی لیست سیاه پسوندها
        if (extension in FORBIDDEN_EXECUTABLE_EXTENSIONS) {
            throw SecurityException("ارسال فایل‌های اجرایی، نصبی یا فشرده ($extension) به دلایل امنیتی مجاز نیست.")
        }

        // ۲. بازرسی امضاهای کدهای اجرایی و بدافزار در هدر (حتی اگر پسوند تغییر کرده باشد)
        if (containsExecutableSignature(header)) {
            throw SecurityException("فایل مشکوک به بدافزار یا کد اجرایی شناسایی شد و به دلایل امنیتی مسدود گردید.")
        }

        // ۳. بررسی نوع مجاز
        val isAllowed = extension in ALLOWED_DOCUMENT_EXTENSIONS ||
            extension in ALLOWED_IMAGE_EXTENSIONS ||
            extension in ALLOWED_AUDIO_EXTENSIONS ||
            extension in ALLOWED_VIDEO_EXTENSIONS

        if (!isAllowed) {
            throw SecurityException("فرمت فایل ($extension) پشتیبانی نمی‌شود. تنها اسناد PDF، صوت و موسیقی، تصاویر و ویدیوها مجاز هستند.")
        }

        // ۴. صحت‌سنجی بایت‌های جادویی برای اسناد PDF (جلوگیری از تغییر پسوند)
        if (extension == "pdf" && header.isNotEmpty()) {
            if (!header.startsWithBytes(PDF_MAGIC)) {
                throw SecurityException("محتوای فایل PDF معتبر نیست یا فایل جعلی است.")
            }
        }
    }

    /**
     * کشف امضاهای اجرایی و بدافزار در بافر ابتدایی
     */
    fun containsExecutableSignature(header: ByteArray): Boolean {
        if (header.size < 2) return false

        // Windows PE (MZ)
        if (header.startsWithBytes(PE_MZ_MAGIC)) {
            // Check for DOS stub message in first bytes or PE signature
            val asString = runCatching { header.decodeToString() }.getOrDefault("")
            if (asString.contains("This program cannot be run in DOS mode", ignoreCase = true) ||
                asString.contains("PE\u0000\u0000") ||
                header.size >= 4
            ) {
                return true
            }
        }

        // Linux/Android ELF
        if (header.startsWithBytes(ELF_MAGIC)) return true

        // Android Dalvik DEX
        if (header.startsWithBytes(DEX_MAGIC)) return true

        // Mach-O binaries
        if (header.startsWithBytes(MACHO_32) ||
            header.startsWithBytes(MACHO_64) ||
            header.startsWithBytes(MACHO_FAT)
        ) {
            return true
        }

        // Shell script shebang
        if (header.startsWithBytes(SHEBANG)) {
            val preview = runCatching { header.take(128).toByteArray().decodeToString() }.getOrDefault("")
            if (preview.contains("/bin/sh") || preview.contains("/bin/bash") || preview.contains("powershell")) {
                return true
            }
        }

        // Suspicious script tags
        val textSnippet = runCatching { header.take(256).toByteArray().decodeToString() }.getOrDefault("")
        if (textSnippet.contains("<script", ignoreCase = true) ||
            textSnippet.contains("WScript.Shell", ignoreCase = true)
        ) {
            return true
        }

        return false
    }

    /**
     * محاسبه چک‌سام امنیتی SHA-256
     */
    fun computeSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun ByteArray.startsWithBytes(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        for (i in prefix.indices) {
            if (this[i] != prefix[i]) return false
        }
        return true
    }
}
