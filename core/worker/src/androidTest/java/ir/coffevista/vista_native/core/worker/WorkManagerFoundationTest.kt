package ir.coffevista.vista_native.core.worker

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WorkManagerFoundationTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        val configuration = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.ERROR)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        workManager = WorkManager.getInstance(context)
    }

    @After
    fun tearDown() {
        workManager.cancelAllWork().result.get(10, TimeUnit.SECONDS)
    }

    @Test
    fun uniqueKeepDeduplicatesAndTestDriverIsAvailable() {
        val identity = identity(accountId = "account-42", operationId = OPERATION_ID)
        val first = request(identity)
        val duplicate = request(identity)

        workManager.enqueueUniqueWork(
            identity.uniqueName,
            ExistingWorkPolicy.KEEP,
            first,
        ).result.get(10, TimeUnit.SECONDS)
        workManager.enqueueUniqueWork(
            identity.uniqueName,
            ExistingWorkPolicy.KEEP,
            duplicate,
        ).result.get(10, TimeUnit.SECONDS)

        val infos = workManager.getWorkInfosForUniqueWork(identity.uniqueName)
            .get(10, TimeUnit.SECONDS)
        assertEquals(1, infos.size)
        assertNotNull(WorkManagerTestInitHelper.getTestDriver(context))
    }

    @Test
    fun accountTagCancellationCancelsOnlyScopedWork() {
        val accountA = identity(accountId = "account-a", operationId = UUID.randomUUID())
        val accountB = identity(accountId = "account-b", operationId = UUID.randomUUID())
        val requestA = request(accountA)
        val requestB = request(accountB)
        workManager.enqueue(listOf(requestA, requestB)).result.get(10, TimeUnit.SECONDS)

        workManager.cancelAllWorkByTag(accountA.accountTag).result.get(10, TimeUnit.SECONDS)

        assertEquals(
            WorkInfo.State.CANCELLED,
            requireNotNull(
                workManager.getWorkInfoById(requestA.id).get(10, TimeUnit.SECONDS),
            ).state,
        )
        assertEquals(
            WorkInfo.State.ENQUEUED,
            requireNotNull(
                workManager.getWorkInfoById(requestB.id).get(10, TimeUnit.SECONDS),
            ).state,
        )
    }

    private fun identity(
        accountId: String,
        operationId: UUID,
    ) = WorkIdentity(
        accountId = accountId,
        operation = "foundation_probe",
        operationId = operationId,
    )

    private fun request(identity: WorkIdentity) =
        OneTimeWorkRequestBuilder<FoundationProbeWorker>()
            .setInitialDelay(1, TimeUnit.HOURS)
            .setConstraints(WorkRequirements().toConstraints())
            .addTag(identity.accountTag)
            .addTag(identity.operationTag)
            .build()

    private companion object {
        val OPERATION_ID: UUID =
            UUID.fromString("f8a39560-09e3-48f0-90e2-9ff16e1e14b7")
    }
}

class FoundationProbeWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : Worker(appContext, workerParameters) {
    override fun doWork(): Result = Result.success()
}
