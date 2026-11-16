package ir.coffevista.vista_native.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.coffevista.vista_native.features.profile.settings.datastorage.AppCacheManager
import java.util.concurrent.TimeUnit

@HiltWorker
class MediaRetentionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val cacheManager: AppCacheManager,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        cacheManager.pruneExpiredMedia()
        Result.success()
    }.getOrElse {
        Result.retry()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "vista_media_retention_prune"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MediaRetentionWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
