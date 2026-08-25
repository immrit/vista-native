package ir.coffevista.vista_native.features.chat.data.remote

import ir.coffevista.vista_native.core.network.ExternalMedia
import ir.coffevista.vista_native.features.chat.domain.model.GifItem
import ir.coffevista.vista_native.features.chat.domain.repository.GifCatalog
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class TenorGifCatalog @Inject constructor(
    @ExternalMedia private val client: OkHttpClient,
) : GifCatalog {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun trending(limit: Int): List<GifItem> = request("trending", null, limit)

    override suspend fun search(query: String, limit: Int): List<GifItem> =
        query.trim().takeIf(String::isNotEmpty)?.let { request("search", it, limit) }
            ?: trending(limit)

    private suspend fun request(path: String, query: String?, limit: Int): List<GifItem> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/$path".toHttpUrl().newBuilder()
            .addQueryParameter("key", API_KEY)
            .addQueryParameter("limit", limit.coerceIn(1, 50).toString())
            .apply { query?.let { addQueryParameter("q", it) } }
            .build()
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            if (!response.isSuccessful) throw java.io.IOException("GIF HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            parse(body)
        }
    }

    internal fun parse(body: String): List<GifItem> = runCatching {
        json.parseToJsonElement(body).jsonObject["results"]?.jsonArray.orEmpty().mapNotNull { element ->
            val item = element.jsonObject
            val media = item["media"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@mapNotNull null
            val main = media.variant("gif") ?: media.variant("mediumgif")
                ?: media.variant("tinygif") ?: media.variant("nanogif") ?: return@mapNotNull null
            val preview = media.variant("nanogif") ?: media.variant("tinygif") ?: main
            val url = main.string("url")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val dimensions = main["dims"]?.jsonArray
            GifItem(
                id = item.string("id").orEmpty(),
                url = url,
                previewUrl = preview.string("url").orEmpty().ifBlank { url },
                width = dimensions?.getOrNull(0)?.jsonPrimitive?.intOrNull ?: 0,
                height = dimensions?.getOrNull(1)?.jsonPrimitive?.intOrNull ?: 0,
            )
        }
    }.getOrDefault(emptyList())

    private fun JsonObject.variant(name: String): JsonObject? = get(name)?.let { runCatching { it.jsonObject }.getOrNull() }
    private fun JsonObject.string(name: String): String? = get(name)?.jsonPrimitive?.contentOrNull

    private companion object {
        const val BASE_URL = "https://g.tenor.com/v1"
        const val API_KEY = "LIVDSRZULELA"
    }
}
