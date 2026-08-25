package ir.coffevista.vista_native.features.profile.settings.datastorage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Measures and clears only Android's app-scoped temporary cache directories. */
@Singleton
class AppCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        cacheRoots().sumOf(::sizeOf)
    }

    suspend fun clear(): Long = withContext(Dispatchers.IO) {
        val before = cacheRoots().sumOf(::sizeOf)
        cacheRoots().forEach { root ->
            root.listFiles()?.forEach { child -> child.deleteRecursively() }
        }
        before
    }

    private fun cacheRoots(): List<File> = buildList {
        add(context.cacheDir)
        context.externalCacheDir?.let(::add)
    }

    private fun sizeOf(file: File): Long = when {
        !file.exists() -> 0L
        file.isFile -> file.length()
        else -> file.listFiles()?.sumOf(::sizeOf) ?: 0L
    }
}
