package ir.coffevista.vista_native.features.chat.data.local

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.features.chat.domain.model.GifItem
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class SavedGifEntity(
    val id: String,
    val url: String,
    val previewUrl: String,
    val width: Int,
    val height: Int,
    val isCustom: Boolean = false,
    val savedAtEpochMillis: Long = System.currentTimeMillis(),
)

/**
 * مدیریت و ذخیره‌سازی محلی گیف‌های مورد علاقه و گیف‌های شخصی کاربران.
 * - امکان ذخیره گیف‌های دریافتی در چت
 * - امکان افزودن گیف‌های دلخواه از گالری و حافظه دستگاه
 * - کپی امن فایل‌های گیف محلی به فضای ذخیره‌سازی برنامه جهت دسترسی پایدار آفلاین
 */
@Singleton
class SavedGifStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private val storeFile = File(context.filesDir, "saved_gifs.json")
    private val gifsDir = File(context.filesDir, "saved_gifs").apply { mkdirs() }

    private val _savedGifs = MutableStateFlow<List<GifItem>>(emptyList())
    val savedGifs: StateFlow<List<GifItem>> = _savedGifs.asStateFlow()

    init {
        scope.launch {
            loadGifs()
        }
    }

    private suspend fun loadGifs() = withContext(Dispatchers.IO) {
        val loaded = if (storeFile.exists()) {
            runCatching {
                val text = storeFile.readText(Charsets.UTF_8)
                val list = json.decodeFromString<List<SavedGifEntity>>(text)
                list.map { it.toGifItem() }
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val initial = if (loaded.isEmpty()) {
            BUILTIN_GIFS
        } else {
            loaded
        }
        _savedGifs.value = initial
    }

    private suspend fun persist() = withContext(Dispatchers.IO) {
        val entities = _savedGifs.value.map { item ->
            SavedGifEntity(
                id = item.id,
                url = item.url,
                previewUrl = item.previewUrl,
                width = item.width,
                height = item.height,
                isCustom = item.url.startsWith("file://"),
            )
        }
        runCatching {
            storeFile.writeText(json.encodeToString(entities), Charsets.UTF_8)
        }
    }

    /**
     * ذخیره یک گیف آنلاین یا دریافتی به عنوان گیف محبوب
     */
    fun saveGif(url: String, previewUrl: String? = null, width: Int = 300, height: Int = 300): GifItem {
        val cleanUrl = url.trim()
        val existing = _savedGifs.value.firstOrNull { it.url == cleanUrl }
        if (existing != null) return existing

        val newItem = GifItem(
            id = "saved_${UUID.randomUUID().toString().take(8)}",
            url = cleanUrl,
            previewUrl = previewUrl?.trim()?.ifBlank { cleanUrl } ?: cleanUrl,
            width = width,
            height = height,
        )
        _savedGifs.update { listOf(newItem) + it }
        scope.launch { persist() }
        return newItem
    }

    /**
     * افزودن گیف دلخواه از گالری/دستگاه کاربر و کپی به دایرکتوری اختصاصی
     */
    suspend fun importFromDevice(uri: Uri, resolver: ContentResolver): GifItem = withContext(Dispatchers.IO) {
        val gifFile = File(gifsDir, "gif_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.gif")
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(gifFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw java.io.IOException("عدم امکان خواندن فایل گیف از دستگاه")

        val fileUri = Uri.fromFile(gifFile).toString()
        val item = GifItem(
            id = "custom_${System.currentTimeMillis()}",
            url = fileUri,
            previewUrl = fileUri,
            width = 300,
            height = 300,
        )
        _savedGifs.update { listOf(item) + it }
        persist()
        item
    }

    /**
     * حذف یک گیف از لیست ذخیره‌شده‌ها
     */
    fun removeGif(urlOrId: String) {
        val target = urlOrId.trim()
        val item = _savedGifs.value.firstOrNull { it.id == target || it.url == target } ?: return
        if (item.url.startsWith("file://")) {
            runCatching { File(Uri.parse(item.url).path ?: "").delete() }
        }
        _savedGifs.update { current -> current.filterNot { it.id == item.id || it.url == item.url } }
        scope.launch { persist() }
    }

    /**
     * بررسی اینکه آیا گیف ذخیره شده است یا خیر
     */
    fun isSaved(url: String): Boolean {
        val clean = url.trim()
        return _savedGifs.value.any { it.url == clean }
    }

    private fun SavedGifEntity.toGifItem(): GifItem = GifItem(
        id = id,
        url = url,
        previewUrl = previewUrl,
        width = width,
        height = height,
    )

    private companion object {
        // نمونه‌های استاندارد گیف با قابلیت کارکرد پایدار
        val BUILTIN_GIFS = listOf(
            GifItem(
                id = "builtin_cat_space",
                url = "https://c.tenor.com/2roX3uxz_68AAAAM/cat-space.gif",
                previewUrl = "https://c.tenor.com/2roX3uxz_68AAAAM/cat-space.gif",
                width = 300,
                height = 300,
            ),
            GifItem(
                id = "builtin_thumbs_up",
                url = "https://c.tenor.com/7p4r-1eL4LAAAAAC/thumbs-up-computer.gif",
                previewUrl = "https://c.tenor.com/7p4r-1eL4LAAAAAC/thumbs-up-computer.gif",
                width = 300,
                height = 200,
            ),
            GifItem(
                id = "builtin_laugh",
                url = "https://c.tenor.com/CqXk1qW0n8EAAAAC/laughing-laugh.gif",
                previewUrl = "https://c.tenor.com/CqXk1qW0n8EAAAAC/laughing-laugh.gif",
                width = 300,
                height = 250,
            ),
            GifItem(
                id = "builtin_heart",
                url = "https://c.tenor.com/Oxkw_Nq_xQcAAAAC/heart-love.gif",
                previewUrl = "https://c.tenor.com/Oxkw_Nq_xQcAAAAC/heart-love.gif",
                width = 300,
                height = 300,
            ),
            GifItem(
                id = "builtin_applause",
                url = "https://c.tenor.com/4g_f5j_N1q8AAAAC/applause-clapping.gif",
                previewUrl = "https://c.tenor.com/4g_f5j_N1q8AAAAC/applause-clapping.gif",
                width = 300,
                height = 220,
            ),
        )
    }
}
