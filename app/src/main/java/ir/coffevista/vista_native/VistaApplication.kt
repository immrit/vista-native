package ir.coffevista.vista_native

import android.app.Application
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import dagger.hilt.android.HiltAndroidApp
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
        ir.coffevista.vista_native.crash.VistaCrashReporter.install(this)
        ir.coffevista.vista_native.worker.MediaRetentionWorker.schedule(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(externalMediaClient)
        .components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .crossfade(true)
        .build()
}
