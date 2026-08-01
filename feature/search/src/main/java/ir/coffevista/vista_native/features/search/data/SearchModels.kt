package ir.coffevista.vista_native.features.search.data

enum class SearchHistoryType(val wireName: String) {
    Hashtag("hashtag"),
    User("user");

    companion object {
        fun fromWireName(value: String): SearchHistoryType =
            entries.firstOrNull { it.wireName == value } ?: User
    }
}

data class SearchUser(
    val id: String,
    val username: String,
    val fullName: String,
    val avatarUrl: String?,
    val isVerified: Boolean,
    val verificationType: String?,
    val role: String?,
)

data class SearchPost(
    val id: String,
    val imageUrl: String?,
    val videoUrl: String?,
)

data class HashtagSuggestion(
    val tag: String,
    val usageCount: Int,
)

data class SearchHistoryItem(
    val query: String,
    val type: SearchHistoryType,
    val timestampEpochMillis: Long,
)

data class UserSearchPage(
    val users: List<SearchUser>,
    val rawCount: Int,
)

data class PostSearchPage(
    val posts: List<SearchPost>,
    val hasMore: Boolean,
    val nextCursor: String?,
)
