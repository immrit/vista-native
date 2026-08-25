package ir.coffevista.vista_native.features.chat.domain.repository

import ir.coffevista.vista_native.features.chat.domain.model.GifItem

interface GifCatalog {
    suspend fun trending(limit: Int = 20): List<GifItem>
    suspend fun search(query: String, limit: Int = 20): List<GifItem>
}
