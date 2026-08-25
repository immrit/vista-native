package ir.coffevista.vista_native

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(externalMediaClient)
        .crossfade(true)
        .build()
}
