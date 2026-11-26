package ir.coffevista.vista_native.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.network.BootstrapApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
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

@Singleton
class VistaUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @BootstrapApi private val httpClient: OkHttpClient,
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
}
