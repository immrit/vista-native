package ir.coffevista.vista_native.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.network.BootstrapApi
import java.io.File
import java.io.FileOutputStream
import java.net.Proxy
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UpdateCheckResult(
    val updateAvailable: Boolean,
    val versionCode: Int = 0,
    val downloadUrl: String? = null,
    val versionName: String? = null,
    val releaseNotes: String? = null,
    val fileSizeBytes: Long = 0L,
    val isMandatory: Boolean = false,
    val minSupportedVersionCode: Int = 1,
    val sha256: String? = null,
)

sealed interface ApkDownloadResult {
    data class ReadyToInstall(val apkFile: File) : ApkDownloadResult
    data class Failed(val message: String) : ApkDownloadResult
    data object Cancelled : ApkDownloadResult
}

@Singleton
class VistaUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @BootstrapApi private val httpClient: OkHttpClient,
) {
    private companion object {
        const val TAG = "VistaUpdateManager"
        const val BAZAAR_PACKAGE = "com.farsitel.bazaar"
        const val MAX_APK_BYTES = 250L * 1024L * 1024L
        const val MAX_REDIRECTS = 3
        val SHA256_REGEX = Regex("^[a-fA-F0-9]{64}$")
        val ALLOWED_UPDATE_HOSTS = setOf(
            "cafevista.ir",
            "www.cafevista.ir",
            "dl.cafevista.ir",
            "coffevista.ir",
            "www.coffevista.ir",
            "api.coffevista.ir",
            "dl.coffevista.ir",
            "vista-media.s3.ir-thr-at1.arvanstorage.ir",
            "vista-storage.s3.ir-thr-at1.arvanstorage.ir",
        )
    }

    private fun isAllowedDownloadUrl(rawUrl: String): Boolean {
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return false
        if (uri.scheme?.lowercase() != "https") return false
        if (!uri.userInfo.isNullOrBlank()) return false
        val host = uri.host?.lowercase() ?: return false
        return host in ALLOWED_UPDATE_HOSTS ||
            host.endsWith(".cafevista.ir") ||
            host.endsWith(".coffevista.ir")
    }

    private fun downloadClient(): OkHttpClient = httpClient.newBuilder()
        .proxy(Proxy.NO_PROXY)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    @Suppress("DEPRECATION")
    private fun extractSigningDigests(packageInfo: PackageInfo): Set<String>? {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return null
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            packageInfo.signatures
        } ?: return null
        if (signatures.isEmpty()) return null
        val md = MessageDigest.getInstance("SHA-256")
        return signatures.map { sig ->
            md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    @Suppress("DEPRECATION")
    fun verifyApkPackageAndSignature(apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() <= 0L || apkFile.length() > MAX_APK_BYTES) return false
        return runCatching {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
            val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            } ?: return false

            if (archiveInfo.packageName != context.packageName) {
                Log.e(TAG, "APK package mismatch: ${archiveInfo.packageName}")
                return false
            }

            val installedInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                pm.getPackageInfo(context.packageName, flags)
            }
            val installedVersionCode = PackageInfoCompat.getLongVersionCode(installedInfo)
            val archiveVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo)
            if (archiveVersionCode <= installedVersionCode) {
                Log.e(TAG, "Refusing APK downgrade or same-version replay: $archiveVersionCode <= $installedVersionCode")
                return false
            }

            val archiveDigests = extractSigningDigests(archiveInfo) ?: return false
            val installedDigests = extractSigningDigests(installedInfo) ?: return false
            if (archiveDigests.intersect(installedDigests).isEmpty()) {
                Log.e(TAG, "Refusing APK: signing certificate digest mismatch")
                return false
            }
            true
        }.getOrElse { e ->
            Log.e(TAG, "APK signature/metadata verification failed", e)
            false
        }
    }

    private fun updatesDir(): File = File(context.cacheDir, "updates").apply { mkdirs() }

    private fun apkFileForVersion(versionCode: Int): File {
        val suffix = if (versionCode > 0) "_$versionCode" else ""
        return File(updatesDir(), "vista_update$suffix.apk")
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { fis ->
            val hashBuf = ByteArray(16 * 1024)
            var read: Int
            while (fis.read(hashBuf).also { read = it } != -1) {
                digest.update(hashBuf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun getCachedValidApk(versionCode: Int, expectedSha256: String?): File? {
        if (versionCode <= 0) return null
        val cleanExpectedSha = expectedSha256?.trim().orEmpty()
        if (!SHA256_REGEX.matches(cleanExpectedSha)) return null
        val file = apkFileForVersion(versionCode)
        if (!file.exists() || file.length() <= 0L) return null
        val actual = runCatching { computeSha256(file) }.getOrNull() ?: return null
        if (!actual.equals(cleanExpectedSha, ignoreCase = true) || !verifyApkPackageAndSignature(file)) {
            file.delete()
            return null
        }
        return file
    }

    suspend fun checkForUpdate(backendBaseUrl: String, currentVersionCode: Long, flavor: String): UpdateCheckResult {
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = "$backendBaseUrl/api/v1/system/check-update?version_code=$currentVersionCode&flavor=$flavor"
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@runCatching UpdateCheckResult(updateAvailable = false)
                    }
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val available = json.optBoolean("update_available", false)
                    if (!available) {
                        return@runCatching UpdateCheckResult(updateAvailable = false)
                    }
                    val v = json.optJSONObject("app_version")
                    UpdateCheckResult(
                        updateAvailable = true,
                        versionCode = v?.optInt("version_code", 0) ?: 0,
                        downloadUrl = v?.optString("download_url")?.takeIf(String::isNotBlank),
                        versionName = v?.optString("version_name")?.takeIf(String::isNotBlank),
                        releaseNotes = v?.optString("release_notes")?.takeIf(String::isNotBlank),
                        fileSizeBytes = v?.optLong("file_size_bytes", 0L) ?: 0L,
                        isMandatory = v?.optBoolean("is_mandatory", false) ?: false,
                        minSupportedVersionCode = v?.optInt("min_supported_version_code", 1) ?: 1,
                        sha256 = v?.optString("sha256")?.takeIf(String::isNotBlank),
                    )
                }
            }.getOrElse { e ->
                Log.w(TAG, "Failed to check for updates", e)
                UpdateCheckResult(updateAvailable = false)
            }
        }
    }

    fun openStoreForUpdate(): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("bazaar://details?id=${context.packageName}")
                setPackage(BAZAAR_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.recoverCatching {
            // Fallback to browser if Bazaar app is not installed
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://cafebazaar.ir/app/${context.packageName}"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            true
        }.getOrDefault(false)
    }

    suspend fun downloadApk(
        update: UpdateCheckResult,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
        onVerifying: () -> Unit = {},
    ): ApkDownloadResult {
        return withContext(Dispatchers.IO) {
            val downloadUrl = update.downloadUrl?.trim().orEmpty()
            val cleanExpectedSha = update.sha256?.trim().orEmpty()

            if (!SHA256_REGEX.matches(cleanExpectedSha)) {
                Log.e(TAG, "Refusing APK update: valid SHA-256 checksum is required")
                return@withContext ApkDownloadResult.Failed("امضای امنیتی نسخه معتبر نیست")
            }
            if (!isAllowedDownloadUrl(downloadUrl)) {
                Log.e(TAG, "Refusing APK update: untrusted or non-HTTPS download URL")
                return@withContext ApkDownloadResult.Failed("آدرس دانلود نسخه معتبر نیست")
            }

            // Return cached verified APK immediately if already downloaded
            getCachedValidApk(update.versionCode, cleanExpectedSha)?.let { cached ->
                return@withContext ApkDownloadResult.ReadyToInstall(cached)
            }

            val dir = updatesDir()
            val targetFile = apkFileForVersion(update.versionCode)
            val tempFile = File(dir, "vista_update_downloading.tmp")
            if (tempFile.exists()) tempFile.delete()

            // Remove stale APK files from previous versions
            dir.listFiles()?.forEach { f ->
                if (f.name != targetFile.name && f.name != tempFile.name && f.name.endsWith(".apk")) {
                    f.delete()
                }
            }

            try {
                val client = downloadClient()
                var activeUrl = downloadUrl
                var redirects = 0
                var response = client.newCall(Request.Builder().url(activeUrl).build()).execute()
                while (response.isRedirect && redirects < MAX_REDIRECTS) {
                    val location = response.header("Location")?.trim().orEmpty()
                    response.close()
                    if (location.isEmpty() || !isAllowedDownloadUrl(location)) {
                        return@withContext ApkDownloadResult.Failed("تغییر مسیر دانلود به نشانی نامعتبر مسدود شد")
                    }
                    activeUrl = location
                    redirects++
                    response = client.newCall(Request.Builder().url(activeUrl).build()).execute()
                }
                response.use { res ->
                    if (!res.isSuccessful) {
                        return@withContext ApkDownloadResult.Failed("خطا در دریافت فایل (کد ${res.code})")
                    }
                    val body = res.body
                        ?: return@withContext ApkDownloadResult.Failed("بدنه پاسخ سرور خالی است")
                    val contentLength = body.contentLength().takeIf { it > 0L } ?: update.fileSizeBytes
                    if (contentLength > MAX_APK_BYTES) {
                        return@withContext ApkDownloadResult.Failed("حجم فایل به‌روزرسانی بیش از حد مجاز است")
                    }
                    var bytesCopied = 0L
                    var lastReportTime = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                currentCoroutineContext().ensureActive()
                                bytesCopied += read
                                if (bytesCopied > MAX_APK_BYTES) {
                                    throw IllegalStateException("APK size exceeded limit")
                                }
                                output.write(buffer, 0, read)
                                val now = System.currentTimeMillis()
                                if (now - lastReportTime >= 90L) {
                                    lastReportTime = now
                                    onProgress(bytesCopied, contentLength)
                                }
                            }
                            output.flush()
                        }
                    }
                    onProgress(bytesCopied, contentLength.coerceAtLeast(bytesCopied))
                }

                currentCoroutineContext().ensureActive()
                onVerifying()

                val actualSha = computeSha256(tempFile)
                if (!actualSha.equals(cleanExpectedSha, ignoreCase = true)) {
                    Log.e(TAG, "SHA256 mismatch! Refusing APK installation.")
                    tempFile.delete()
                    return@withContext ApkDownloadResult.Failed("تایید اصالت فایل APK ناموفق بود")
                }
                if (!verifyApkPackageAndSignature(tempFile)) {
                    tempFile.delete()
                    return@withContext ApkDownloadResult.Failed("امضای بسته APK یا شماره نسخه با ویستا مطابقت ندارد")
                }

                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                ApkDownloadResult.ReadyToInstall(targetFile)
            } catch (ce: CancellationException) {
                tempFile.delete()
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download APK", e)
                tempFile.delete()
                ApkDownloadResult.Failed("خطا در ارتباط با سرور هنگام دانلود")
            }
        }
    }

    suspend fun downloadAndInstallApk(
        downloadUrl: String,
        expectedSha256: String? = null,
        onProgress: (Float) -> Unit = {},
    ): Boolean {
        val result = downloadApk(
            update = UpdateCheckResult(
                updateAvailable = true,
                downloadUrl = downloadUrl,
                sha256 = expectedSha256,
            ),
            onProgress = { copied, total ->
                if (total > 0L) {
                    onProgress((copied.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                }
            },
        )
        return when (result) {
            is ApkDownloadResult.ReadyToInstall -> installApk(result.apkFile)
            else -> false
        }
    }

    fun canInstallPackages(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
    }

    fun openUnknownSourcesSettings(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return runCatching {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun installApk(apkFile: File): Boolean {
        return runCatching {
            if (!apkFile.exists() || !verifyApkPackageAndSignature(apkFile)) return false

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.files",
                apkFile,
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        }.getOrElse { e ->
            Log.e(TAG, "Failed to launch package installer", e)
            false
        }
    }
}
