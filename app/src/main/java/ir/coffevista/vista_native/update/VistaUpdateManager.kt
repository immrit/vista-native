package ir.coffevista.vista_native.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.network.BootstrapApi
import java.io.File
import java.io.FileOutputStream
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
        val SHA256_REGEX = Regex("^[a-fA-F0-9]{64}$")
        val ALLOWED_UPDATE_HOSTS = setOf(
            "cafevista.ir",
            "www.cafevista.ir",
            "dl.cafevista.ir",
            "coffevista.ir",
            "www.coffevista.ir",
            "api.coffevista.ir",
        )
    }

    private fun isAllowedDownloadUrl(rawUrl: String): Boolean {
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return false
        if (uri.scheme?.lowercase() != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return host in ALLOWED_UPDATE_HOSTS ||
            host.endsWith(".cafevista.ir") ||
            host.endsWith(".coffevista.ir") ||
            host.endsWith(".arvanstorage.ir") ||
            host.endsWith(".cdn.ir")
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
        if (!actual.equals(cleanExpectedSha, ignoreCase = true)) {
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
                val request = Request.Builder().url(downloadUrl).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext ApkDownloadResult.Failed("خطا در دریافت فایل (کد ${response.code})")
                    }
                    val body = response.body
                        ?: return@withContext ApkDownloadResult.Failed("بدنه پاسخ سرور خالی است")
                    val contentLength = body.contentLength().takeIf { it > 0L } ?: update.fileSizeBytes
                    var bytesCopied = 0L
                    var lastReportTime = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                currentCoroutineContext().ensureActive()
                                output.write(buffer, 0, read)
                                bytesCopied += read
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
            if (!apkFile.exists()) return false

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
