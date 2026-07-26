package ir.coffevista.vista_native.core.worker

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

interface AccountWorkController {
    fun cancelAccountWork(accountId: String)
}

object NoOpAccountWorkController : AccountWorkController {
    override fun cancelAccountWork(accountId: String) = Unit
}

class WorkManagerAccountWorkController @Inject constructor(
    private val workManager: WorkManager,
) : AccountWorkController {
    override fun cancelAccountWork(accountId: String) {
        workManager.cancelAllWorkByTag(WorkIdentity.accountTag(accountId))
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerBindingsModule {
    @Binds
    @Singleton
    abstract fun bindAccountWorkController(
        implementation: WorkManagerAccountWorkController,
    ): AccountWorkController
}

@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)
}
