package ir.coffevista.vista_native.features.search.data

import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun searchUsers(query: String, offset: Int): UserSearchPage
    suspend fun searchPostsByHashtag(query: String): PostSearchPage = searchPostsByHashtag(query, 0)
    suspend fun searchPostsByHashtag(query: String, offset: Int): PostSearchPage = PostSearchPage(emptyList(), false, null)
    suspend fun searchHashtags(query: String): List<HashtagSuggestion>
    suspend fun trendingHashtags(): List<HashtagSuggestion>
    suspend fun getExploreGrid(limit: Int = 30, offset: Int = 0): List<SearchPost> = emptyList()

    fun observeHistory(accountId: String): Flow<List<SearchHistoryItem>>
    suspend fun addHistory(
        accountId: String,
        query: String,
        type: SearchHistoryType,
    )
    suspend fun deleteHistory(accountId: String, query: String)
    suspend fun clearHistory(accountId: String)
    suspend fun clearAccount(accountId: String)

    companion object {
        const val USER_LIMIT = 20
        const val POST_LIMIT = 15
        const val HASHTAG_SUGGESTION_LIMIT = 10
        const val TRENDING_LIMIT = 12
        const val TRENDING_DAYS = 30
    }
}
