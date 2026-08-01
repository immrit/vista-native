package ir.coffevista.vista_native.features.search.data

import ir.coffevista.vista_native.core.database.search.SearchHistoryDao
import ir.coffevista.vista_native.core.database.search.SearchHistoryEntity
import ir.coffevista.vista_native.core.network.BackendErrorParser
import ir.coffevista.vista_native.core.network.RemoteFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import javax.inject.Singleton

@Singleton
class DefaultSearchRepository(
    private val api: SearchApi,
    private val historyDao: SearchHistoryDao,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) : SearchRepository {
    override suspend fun searchUsers(query: String, offset: Int): UserSearchPage {
        val normalized = normalizeUserQuery(query)
        if (normalized.isEmpty()) return UserSearchPage(emptyList(), 0)

        val response = api.searchProfiles(
            query = normalized,
            limit = SearchRepository.USER_LIMIT,
            offset = offset.coerceAtLeast(0),
        ).bodyOrThrow("search_profiles_empty")
        val page = response.profiles
            .mapNotNull(SearchUserDto::toDomainOrNull)
            .distinctBy(SearchUser::id)

        val exact = if (
            offset == 0 &&
            USERNAME_PATTERN.matches(normalized)
        ) {
            runCatching {
                api.profileByUsername(normalized.lowercase())
                    .bodyOrThrow("exact_profile_empty")
                    .toDomainOrNull()
            }.getOrNull()
        } else {
            null
        }
        val merged = buildList {
            exact?.let(::add)
            addAll(page.filterNot { it.id == exact?.id })
        }
        return UserSearchPage(
            users = merged,
            rawCount = response.profiles.size,
        )
    }

    override suspend fun searchPostsByHashtag(query: String): PostSearchPage {
        val tag = normalizeHashtag(query)
        if (tag.isEmpty()) return PostSearchPage(emptyList(), false, null)
        val response = api.searchPostsByHashtag(
            tag = tag,
            limit = SearchRepository.POST_LIMIT,
            offset = 0,
        ).bodyOrThrow("search_posts_empty")
        return PostSearchPage(
            posts = response.posts
                .mapNotNull(SearchPostDto::toDomainOrNull)
                .distinctBy(SearchPost::id),
            hasMore = response.hasMore,
            nextCursor = response.nextCursor?.trim()?.takeIf(String::isNotEmpty),
        )
    }

    override suspend fun searchHashtags(query: String): List<HashtagSuggestion> {
        val keyword = normalizeHashtagSuggestion(query)
        if (keyword.isEmpty()) return emptyList()
        return api.searchHashtags(
            keyword = keyword,
            limit = SearchRepository.HASHTAG_SUGGESTION_LIMIT,
        ).bodyOrThrow("search_hashtags_empty")
            .hashtags
            .mapNotNull(HashtagSuggestionDto::toDomainOrNull)
            .distinctBy { it.tag.lowercase() }
    }

    override suspend fun trendingHashtags(): List<HashtagSuggestion> =
        api.trendingHashtags(
            limit = SearchRepository.TRENDING_LIMIT,
            days = SearchRepository.TRENDING_DAYS,
        ).bodyOrThrow("trending_hashtags_empty")
            .hashtags
            .mapNotNull(HashtagSuggestionDto::toDomainOrNull)
            .distinctBy { it.tag.lowercase() }

    override fun observeHistory(accountId: String): Flow<List<SearchHistoryItem>> =
        historyDao.observeRecent(accountId).map { entities ->
            entities.map { entity ->
                SearchHistoryItem(
                    query = entity.query,
                    type = SearchHistoryType.fromWireName(entity.searchType),
                    timestampEpochMillis = entity.timestampEpochMillis,
                )
            }
        }

    override suspend fun addHistory(
        accountId: String,
        query: String,
        type: SearchHistoryType,
    ) {
        val normalized = query.trim()
        if (accountId.isBlank() || normalized.isBlank()) return
        historyDao.addAndTrim(
            SearchHistoryEntity(
                accountId = accountId,
                query = normalized,
                searchType = type.wireName,
                timestampEpochMillis = nowEpochMillis(),
            ),
        )
    }

    override suspend fun deleteHistory(accountId: String, query: String) {
        if (accountId.isBlank()) return
        historyDao.delete(accountId, query.trim())
    }

    override suspend fun clearHistory(accountId: String) {
        if (accountId.isBlank()) return
        historyDao.clearAccount(accountId)
    }

    override suspend fun clearAccount(accountId: String) = clearHistory(accountId)

    private fun <T> Response<T>.bodyOrThrow(emptyCode: String): T {
        if (isSuccessful) {
            return body() ?: throw RemoteFailure(
                statusCode = code(),
                code = emptyCode,
                message = "پاسخ سرور خالی بود",
            )
        }
        val parsed = BackendErrorParser.parse(
            rawBody = errorBody()?.string().orEmpty(),
            retryAfterHeader = headers()["Retry-After"],
        )
        throw RemoteFailure(
            statusCode = code(),
            code = parsed.code,
            message = parsed.message,
            retryAfterSeconds = parsed.retryAfterSeconds,
        )
    }

    companion object {
        private val USERNAME_PATTERN = Regex("^[A-Za-z0-9_.]{2,32}$")
    }
}

internal fun normalizeUserQuery(value: String): String =
    value.trim().replaceFirst(Regex("^@+"), "")

internal fun normalizeHashtag(value: String): String =
    value.trim()
        .replaceFirst(Regex("^#+"), "")
        .replace(Regex("\\s+"), "")
        .lowercase()

internal fun normalizeHashtagSuggestion(value: String): String =
    value.trim().replace("#", "").trim()

internal fun canSearchAsTag(value: String): Boolean {
    val normalized = value.trim()
    if (normalized.isEmpty()) return false
    if (normalized.startsWith("#")) return true
    if (normalized.contains(' ')) return false
    return Regex("^[\\u0600-\\u06FFA-Za-z0-9_]+$").matches(normalized)
}
