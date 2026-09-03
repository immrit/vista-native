package ir.coffevista.vista_native.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UpdateCheckResult(
    val updateAvailable: Boolean,
    val downloadUrl: String? = null,
    val versionName: String? = null,
    val isMandatory: Boolean = false,
    val minSupportedVersionCode: Int = 1,
    val sha256: String? = null,
)

@Singleton
class VistaUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
) {
    private companion object {
        const val TAG = "VistaUpdateManager"
        const val BAZAAR_PACKAGE = "com.farsitel.bazaar"
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
                        downloadUrl = v?.optString("download_url"),
                        versionName = v?.optString("version_name"),
                        isMandatory = v?.optBoolean("is_mandatory", false) ?: false,
                        minSupportedVersionCode = v?.optInt("min_supported_version_code", 1) ?: 1,
                        sha256 = v?.optString("sha256"),
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

    suspend fun downloadAndInstallApk(
        downloadUrl: String,
        expectedSha256: String? = null,
        onProgress: (Float) -> Unit = {},
    ): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val targetFile = File(updatesDir, "vista_update.apk")
                if (targetFile.exists()) targetFile.delete()

                val request = Request.Builder().url(downloadUrl).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching false
                    val body = response.body ?: return@runCatching false
                    val contentLength = body.contentLength()
                    var bytesCopied = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(targetFile).use { output ->
                            val buffer = ByteArray(8 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                bytesCopied += read
                                if (contentLength > 0) {
                                    onProgress(bytesCopied.toFloat() / contentLength)
                                }
                            }
                            output.flush()
                        }
                    }
                }

                if (!expectedSha256.isNullOrBlank()) {
                    val digest = MessageDigest.getInstance("SHA-256")
                    val fileBytes = targetFile.readBytes()
                    val actualSha = digest.digest(fileBytes).joinToString("") { "%02x".format(it) }
                    if (!actualSha.equals(expectedSha256, ignoreCase = true)) {
                        Log.e(TAG, "SHA256 mismatch! Expected: $expectedSha256, actual: $actualSha")
                        targetFile.delete()
                        return@runCatching false
                    }
                }

                installApk(targetFile)
            }.getOrElse { e ->
                Log.e(TAG, "Failed to download and install APK", e)
                false
            }
        }
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
