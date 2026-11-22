package ir.coffevista.vista_native

import android.app.Application
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.DataSource
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.pxOrElse
import dagger.hilt.android.HiltAndroidApp
import ir.coffevista.vista_native.core.designsystem.component.VistaMediaBrowserHelpers
import ir.coffevista.vista_native.core.designsystem.component.loadVistaLocalMediaThumbnail
import ir.coffevista.vista_native.core.network.ExternalMedia
import javax.inject.Inject
import okhttp3.OkHttpClient

@HiltAndroidApp
class VistaApplication : Application(), Configuration.Provider, ImageLoaderFactory {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    @ExternalMedia
    lateinit var externalMediaClient: OkHttpClient

    @Inject
    lateinit var directShareGateway: ir.coffevista.vista_native.share.AppDirectShareGateway

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences(
            ir.coffevista.vista_native.core.common.DeviceIdProvider.PREFERENCES_NAME,
            MODE_PRIVATE,
        )
        val deviceId = prefs.getString(
            ir.coffevista.vista_native.core.common.DeviceIdProvider.KEY_DEVICE_ID,
            null,
        )?.takeIf(String::isNotBlank) ?: java.util.UUID.randomUUID().toString().also { generated ->
            prefs.edit()
                .putString(ir.coffevista.vista_native.core.common.DeviceIdProvider.KEY_DEVICE_ID, generated)
                .apply()
        }
        ir.coffevista.vista_native.core.common.DeviceIdProvider.current =
            ir.coffevista.vista_native.core.common.DeviceIdProvider { deviceId }
        val appVerName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull()?.takeIf(String::isNotBlank) ?: "1.0.0"
        val deviceDisplayName = ir.coffevista.vista_native.core.common.DeviceMetadataProvider.formatAndroidDeviceName(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            brand = Build.BRAND,
        )
        val deviceModelName = Build.MODEL?.trim().orEmpty().ifBlank { "Android" }
        val osRelease = Build.VERSION.RELEASE?.trim().orEmpty().ifBlank { "14" }
        val vistaUa = "VistaAndroid/$appVerName (Android $osRelease; $deviceDisplayName)"
        val cachedDeviceMetadata = ir.coffevista.vista_native.core.common.DeviceMetadata(
            deviceName = deviceDisplayName,
            deviceModel = deviceModelName,
            osVersion = "Android $osRelease",
            appVersion = appVerName,
            userAgent = vistaUa,
        )
        ir.coffevista.vista_native.core.common.DeviceMetadataProvider.current =
            ir.coffevista.vista_native.core.common.DeviceMetadataProvider { cachedDeviceMetadata }
        val sessionPrefs = getSharedPreferences(
            ir.coffevista.vista_native.core.common.SessionCredentialsProvider.PREFS_NAME,
            MODE_PRIVATE,
        )
        ir.coffevista.vista_native.core.common.SessionCredentialsProvider.current =
            object : ir.coffevista.vista_native.core.common.SessionCredentialsProvider {
                override fun currentSessionId(): String? = sessionPrefs.getString(
                    ir.coffevista.vista_native.core.common.SessionCredentialsProvider.KEY_SESSION_ID,
                    null,
                )?.takeIf(String::isNotBlank)

                override fun currentSessionToken(): String? = sessionPrefs.getString(
                    ir.coffevista.vista_native.core.common.SessionCredentialsProvider.KEY_SESSION_TOKEN,
                    null,
                )?.takeIf(String::isNotBlank)

                override fun currentUserId(): String? = sessionPrefs.getString(
                    ir.coffevista.vista_native.core.common.SessionCredentialsProvider.KEY_SESSION_USER_ID,
                    null,
                )?.takeIf(String::isNotBlank)

                override fun saveSessionCredentials(sessionId: String, sessionToken: String, userId: String?) {
                    sessionPrefs.edit()
                        .putString(ir.coffevista.vista_native.core.common.SessionCredentialsProvider.KEY_SESSION_ID, sessionId)
                        .putString(ir.coffevista.vista_native.core.common.SessionCredentialsProvider.KEY_SESSION_TOKEN, sessionToken)
                        .apply {
                            if (!userId.isNullOrBlank()) {
                                putString(ir.coffevista.vista_native.core.common.SessionCredentialsProvider.KEY_SESSION_USER_ID, userId)
                            }
                        }
                        .apply()
                }

                override fun clear() {
                    sessionPrefs.edit()
                        .remove(ir.coffevista.vista_native.core.common.SessionCredentialsProvider.KEY_SESSION_ID)
                        .remove(ir.coffevista.vista_native.core.common.SessionCredentialsProvider.KEY_SESSION_TOKEN)
                        .remove(ir.coffevista.vista_native.core.common.SessionCredentialsProvider.KEY_SESSION_USER_ID)
                        .apply()
                }
            }
        ir.coffevista.vista_native.core.designsystem.component.VistaDirectShareRegistry.current =
            directShareGateway
        ir.coffevista.vista_native.features.feed.ui.components.FeedVideoPlaybackCoordinator
            .configureHttpClient(externalMediaClient)
        ir.coffevista.vista_native.crash.VistaCrashReporter.install(this)
        ir.coffevista.vista_native.worker.MediaRetentionWorker.schedule(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(externalMediaClient)
        .components {
            add(LocalVideoThumbnailFetcher.Factory(this@VistaApplication))
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .crossfade(true)
        .build()
}

private class LocalVideoThumbnailFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val widthPx = options.size.width.pxOrElse { 320 }
        val heightPx = options.size.height.pxOrElse { 320 }
        val targetPx = minOf(widthPx, heightPx).coerceIn(128, 720)
        val bitmap = loadVistaLocalMediaThumbnail(
            context = context,
            uri = uri,
            isVideo = true,
            targetSizePx = targetPx,
        ) ?: return null
        return DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(
        private val context: Context,
    ) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!isLocalVideoUri(data)) return null
            return LocalVideoThumbnailFetcher(context, data, options)
        }

        private fun isLocalVideoUri(uri: Uri): Boolean {
            val scheme = uri.scheme?.lowercase() ?: return false
            if (scheme == "content") {
                val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
                if (mime.startsWith("video/", ignoreCase = true)) return true
                return uri.toString().contains("/video/", ignoreCase = true)
            }
            if (scheme == "file") {
                val ext = VistaMediaBrowserHelpers.extensionFromFileName(uri.path.orEmpty())
                return ext in VistaMediaBrowserHelpers.CHAT_ALLOWED_VIDEO_EXTENSIONS ||
                    ext in setOf("avi", "flv", "wmv", "ts")
            }
            return false
        }
    }
}
