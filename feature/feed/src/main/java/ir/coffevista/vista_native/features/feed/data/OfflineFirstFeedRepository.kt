package ir.coffevista.vista_native.features.feed.data

import ir.coffevista.vista_native.core.database.feed.FeedDao
import ir.coffevista.vista_native.core.database.feed.FeedPageStateEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstFeedRepository @Inject constructor(
    private val feedDao: FeedDao,
    private val feedApi: FeedApi,
    private val postLocationRegistry: PostLocationRegistry? = null,
) : FeedRepository {

    private val exploreLimit = 15
    private val followingLimit = 20
    private val profileLimit = 15
    private val hashtagLimit = 24
    private val appendMutexes = mutableMapOf<String, Mutex>()
    private val pendingEvents = ConcurrentHashMap<String, FeedEventRequestDto>()
    private val pendingBehaviorEvents = ConcurrentLinkedQueue<BehaviorEventDto>()
    private val sentViewEvents = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        repositoryScope.launch {
            while (isActive) {
                delay(2000L)
                flushPendingEvents()
            }
        }
    }

    internal suspend fun flushPendingEvents() {
        if (pendingEvents.isNotEmpty()) {
            val snapshot = ArrayList(pendingEvents.values)
            pendingEvents.clear()
            for (event in snapshot) {
                runCatching { feedApi.trackFeedEvent(event) }
            }
        }
        if (pendingBehaviorEvents.isNotEmpty()) {
            val batch = mutableListOf<BehaviorEventDto>()
            while (batch.size < 25) {
                val item = pendingBehaviorEvents.poll() ?: break
                batch += item
            }
            if (batch.isNotEmpty()) {
                runCatching { feedApi.trackBehaviorEvents(BehaviorEventsBatchRequestDto(batch)) }
            }
        }
    }

    override fun observeFeed(accountId: String, kind: FeedKind): Flow<FeedSnapshot> =
        observePage(storageKey(accountId, kind))

    override fun observeUserPosts(accountId: String, userId: String): Flow<FeedSnapshot> =
        observePage(userPostsStorageKey(accountId, userId))

    private fun observePage(storageAccountId: String): Flow<FeedSnapshot> = combine(
        feedDao.observeFeed(storageAccountId),
        feedDao.observePageState(storageAccountId),
    ) { entities, pageState ->
        FeedSnapshot(
            posts = entities.map { it.asExternalModel() },
            hasMore = pageState?.hasMore ?: true,
            nextOffset = pageState?.nextOffset ?: entities.size,
        )
    }

    override fun getPostById(accountId: String, postId: String): Flow<FeedPost?> = combine(
        feedDao.observePostById(accountId, postId),
        feedDao.observePostById(storageKey(accountId, FeedKind.Following), postId),
        feedDao.observePostById(detailStorageKey(accountId), postId),
        feedDao.observeAnyPostById(postId),
    ) { explore, following, detail, anyPost ->
        (detail ?: explore ?: following ?: anyPost)?.asExternalModel()
    }

    override suspend fun refreshFeed(
        accountId: String,
        kind: FeedKind,
    ): FeedRefreshResult {
        val storageAccountId = storageKey(accountId, kind)
        val response = when (kind) {
            FeedKind.Explore -> {
                if (accountId.isNotBlank() && accountId != "guest") {
                    try {
                        feedApi.getPersonalizedFeed(limit = exploreLimit, offset = 0)
                    } catch (e: Exception) {
                        feedApi.getExploreFeed(limit = exploreLimit, offset = 0)
                    }
                } else {
                    feedApi.getExploreFeed(limit = exploreLimit, offset = 0)
                }
            }
            FeedKind.Following -> feedApi.getFollowingFeed(
                limit = followingLimit,
                offset = 0,
            )
        }
        return replaceFirstPage(storageAccountId, response)
    }

    override suspend fun loadMoreFeed(
        accountId: String,
        kind: FeedKind,
    ): FeedAppendResult {
        val storageAccountId = storageKey(accountId, kind)
        val limit = if (kind == FeedKind.Explore) exploreLimit else followingLimit
        return appendPage(storageAccountId) { _, offset ->
            when (kind) {
                FeedKind.Explore -> {
                    if (accountId.isNotBlank() && accountId != "guest") {
                        try {
                            feedApi.getPersonalizedFeed(limit = limit, offset = offset)
                        } catch (e: Exception) {
                            feedApi.getExploreFeed(limit = limit, offset = offset)
                        }
                    } else {
                        feedApi.getExploreFeed(limit = limit, offset = offset)
                    }
                }
                FeedKind.Following -> feedApi.getFollowingFeed(
                    limit = limit,
                    offset = offset,
                )
            }
        }
    }

    override suspend fun refreshPost(accountId: String, postId: String): FeedPost {
        val post = feedApi.getPost(postId)
        val storageAccountId = detailStorageKey(accountId)
        val existingLocation = if (post.location.isNullOrBlank()) {
            feedDao.findAnyPostById(postId)?.location?.trim()?.takeIf(String::isNotBlank)
        } else {
            null
        }
        val entity = post.asEntityPreservingLocation(
            accountId = storageAccountId,
            sortOrder = 0,
            fallbackLocations = if (existingLocation != null) mapOf(postId to existingLocation) else emptyMap(),
        )
        feedDao.insertPosts(listOf(entity))
        return entity.asExternalModel()
    }

    override suspend fun refreshUserPosts(
        accountId: String,
        userId: String,
    ): FeedRefreshResult {
        val response = feedApi.getUserPosts(userId, profileLimit, 0)
        return replaceFirstPage(userPostsStorageKey(accountId, userId), response)
    }

    override suspend fun loadMoreUserPosts(
        accountId: String,
        userId: String,
    ): FeedAppendResult {
        val storageAccountId = userPostsStorageKey(accountId, userId)
        return appendPage(storageAccountId) { _, offset ->
            feedApi.getUserPosts(userId, profileLimit, offset)
        }
    }

    override suspend fun getHashtagPosts(
        accountId: String,
        hashtag: String,
        offset: Int,
    ): FeedSnapshot {
        val normalized = hashtag.removePrefix("#").trim()
        require(normalized.isNotBlank()) { "هشتگ نامعتبر است" }
        val safeOffset = offset.coerceAtLeast(0)
        val response = feedApi.getHashtagPosts(normalized, hashtagLimit, safeOffset)
        val fallbacks = resolveCachedLocationsForMissing(accountId, response.posts)
        return FeedSnapshot(
            posts = response.posts.mapIndexed { index, post ->
                post.asEntityPreservingLocation(
                    accountId = "$accountId${NAMESPACE_SEPARATOR}hashtag$NAMESPACE_SEPARATOR$normalized",
                    sortOrder = index.toLong(),
                    fallbackLocations = fallbacks,
                ).asExternalModel()
            },
            hasMore = response.hasMore,
            nextOffset = safeOffset + response.posts.size,
        )
    }

    override suspend fun getLocationPosts(
        accountId: String,
        location: String,
        offset: Int,
    ): FeedSnapshot {
        val normalized = location.trim()
        require(normalized.isNotBlank()) { "موقعیت مکانی نامعتبر است" }
        val safeOffset = offset.coerceAtLeast(0)
        val storageKey = "$accountId${NAMESPACE_SEPARATOR}location$NAMESPACE_SEPARATOR${LocationMatcher.normalize(normalized)}"

        val remoteResponse = try {
            feedApi.getLocationPosts(normalized, hashtagLimit, safeOffset)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        if (remoteResponse != null) {
            val fallbacks = resolveCachedLocationsForMissing(accountId, remoteResponse.posts)
            val remotePosts = remoteResponse.posts.mapIndexed { index, post ->
                post.asEntityPreservingLocation(storageKey, index.toLong(), fallbacks)
                    .asExternalModel()
            }
            val mergedPosts = if (safeOffset == 0) {
                val localMatching = feedDao.getPostsWithLocation(accountId, "$accountId$NAMESPACE_SEPARATOR")
                    .asSequence()
                    .map { it.asExternalModel() }
                    .filter { LocationMatcher.matches(it.location, normalized) }
                    .toList()
                (localMatching + remotePosts)
                    .distinctBy(FeedPost::id)
                    .sortedByDescending(FeedPost::createdAt)
            } else {
                remotePosts
            }
            return FeedSnapshot(
                posts = mergedPosts,
                hasMore = remoteResponse.hasMore,
                nextOffset = safeOffset + remoteResponse.posts.size,
            )
        }

        if (safeOffset > 0) {
            return FeedSnapshot(emptyList(), hasMore = false, nextOffset = safeOffset)
        }

        val fallbackPosts = coroutineScope {
            val cachedDeferred = async {
                feedDao.getPostsWithLocation(accountId, "$accountId$NAMESPACE_SEPARATOR")
                    .map { it.asExternalModel() }
            }
            val exploreDeferred = async {
                try {
                    val dtos = feedApi.getExploreFeed(50, 0).posts
                    val fallbacks = resolveCachedLocationsForMissing(accountId, dtos)
                    dtos.mapIndexed { idx, dto ->
                        dto.asEntityPreservingLocation(storageKey, idx.toLong(), fallbacks).asExternalModel()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val followingDeferred = async {
                try {
                    val dtos = feedApi.getFollowingFeed(50, 0).posts
                    val fallbacks = resolveCachedLocationsForMissing(accountId, dtos)
                    dtos.mapIndexed { idx, dto ->
                        dto.asEntityPreservingLocation(storageKey, idx.toLong(), fallbacks).asExternalModel()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyList()
                }
            }

            (exploreDeferred.await() + followingDeferred.await() + cachedDeferred.await())
                .distinctBy(FeedPost::id)
                .filter { post -> LocationMatcher.matches(post.location, normalized) }
                .sortedByDescending(FeedPost::createdAt)
        }

        return FeedSnapshot(
            posts = fallbackPosts,
            hasMore = false,
            nextOffset = fallbackPosts.size,
        )
    }

    private suspend fun replaceFirstPage(
        storageAccountId: String,
        response: FeedResponseDto,
    ): FeedRefreshResult {
        val rootAccountId = storageAccountId.substringBefore(NAMESPACE_SEPARATOR)
        val distinctPosts = response.posts.distinctBy(FeedPostDto::id)
        val fallbacks = resolveCachedLocationsForMissing(rootAccountId, distinctPosts)
        val entities = distinctPosts
            .mapIndexed { index, post ->
                post.asEntityPreservingLocation(
                    accountId = storageAccountId,
                    sortOrder = index.toLong(),
                    fallbackLocations = fallbacks,
                )
            }
        feedDao.replaceFirstPage(
            accountId = storageAccountId,
            posts = entities,
            state = FeedPageStateEntity(
                accountId = storageAccountId,
                nextOffset = response.posts.size,
                hasMore = response.hasMore,
                nextCursor = response.nextCursor,
                lastRefreshEpochMillis = System.currentTimeMillis(),
            ),
        )
        return FeedRefreshResult(entities.size, response.hasMore)
    }

    private suspend fun appendPage(
        storageAccountId: String,
        request: suspend (FeedPageStateEntity?, Int) -> FeedResponseDto,
    ): FeedAppendResult {
        val mutex = synchronized(appendMutexes) {
            appendMutexes.getOrPut(storageAccountId) { Mutex() }
        }
        if (!mutex.tryLock()) return FeedAppendResult.IgnoredAlreadyLoading
        try {
            val currentState = feedDao.getPageState(storageAccountId)
            if (currentState?.hasMore == false) return FeedAppendResult.EndReached
            val offset = currentState?.nextOffset ?: feedDao.getPostCount(storageAccountId)
            val response = request(currentState, offset)
            val distinctPosts = response.posts.distinctBy(FeedPostDto::id)
            val existingIds = if (distinctPosts.isEmpty()) {
                emptySet()
            } else {
                feedDao.getExistingPostIds(
                    accountId = storageAccountId,
                    postIds = distinctPosts.map(FeedPostDto::id),
                ).toSet()
            }
            val firstSortOrder =
                (feedDao.getMaxSortOrder(storageAccountId) ?: -1L) + 1L
            val toInsert = distinctPosts.filterNot { it.id in existingIds }
            val rootAccountId = storageAccountId.substringBefore(NAMESPACE_SEPARATOR)
            val fallbacks = resolveCachedLocationsForMissing(rootAccountId, toInsert)
            val newEntities = toInsert
                .mapIndexed { index, post ->
                    post.asEntityPreservingLocation(
                        accountId = storageAccountId,
                        sortOrder = firstSortOrder + index,
                        fallbackLocations = fallbacks,
                    )
                }
            feedDao.appendPage(
                posts = newEntities,
                state = FeedPageStateEntity(
                    accountId = storageAccountId,
                    nextOffset = offset + response.posts.size,
                    hasMore = response.hasMore,
                    nextCursor = response.nextCursor,
                    lastRefreshEpochMillis = currentState?.lastRefreshEpochMillis
                        ?: System.currentTimeMillis(),
                ),
            )
            return FeedAppendResult.Appended(newEntities.size, response.hasMore)
        } finally {
            mutex.unlock()
        }
    }

    override suspend fun clearAccount(accountId: String) {
        feedDao.clearAccountNamespaces(accountId, "$accountId$NAMESPACE_SEPARATOR")
        synchronized(appendMutexes) {
            appendMutexes.keys.removeAll { key ->
                key == accountId || key.startsWith("$accountId$NAMESPACE_SEPARATOR")
            }
        }
    }

    override suspend fun toggleLike(accountId: String, postId: String, ownerId: String, isLiked: Boolean, newLikeCount: Long) {
        feedDao.updateLikeState(postId, isLiked, newLikeCount)
        try {
            val response = feedApi.toggleLike(postId, LikeRequestDto(ownerId = ownerId))
            feedDao.updateLikeState(postId, response.isLiked, response.likeCount)
        } catch (e: Exception) {
            val rollbackCount = if (isLiked) newLikeCount - 1 else newLikeCount + 1
            feedDao.updateLikeState(postId, !isLiked, maxOf(0L, rollbackCount))
            throw e
        }
    }

    override suspend fun toggleSave(accountId: String, postId: String, isSaved: Boolean) {
        feedDao.updateSaveState(postId, isSaved)
        try {
            val response = feedApi.toggleSave(postId)
            feedDao.updateSaveState(postId, response.isSaved)
        } catch (e: Exception) {
            feedDao.updateSaveState(postId, !isSaved)
            throw e
        }
    }

    override suspend fun updatePost(
        accountId: String,
        postId: String,
        content: String?,
        hideLikeCount: Boolean?,
        hideCommentCount: Boolean?,
        moderationReason: String?,
        clearContent: Boolean?,
        clearImage: Boolean?,
        clearVideo: Boolean?,
        clearMusic: Boolean?,
        notifyUser: Boolean?,
    ): FeedPost {
        val hasChange = content != null || hideLikeCount != null || hideCommentCount != null ||
            moderationReason != null || clearContent == true || clearImage == true || clearVideo == true || clearMusic == true
        require(hasChange) {
            "حداقل یک تغییر برای پست لازم است"
        }
        val response = feedApi.updatePost(
            postId,
            UpdatePostRequestDto(
                content = content,
                hideLikeCount = hideLikeCount,
                hideCommentCount = hideCommentCount,
                moderationReason = moderationReason,
                clearContent = clearContent,
                clearImage = clearImage,
                clearVideo = clearVideo,
                clearMusic = clearMusic,
                notifyUser = notifyUser,
            ),
        )
        feedDao.updatePostFields(postId, content, hideLikeCount, hideCommentCount)
        return response.asEntityPreservingLocation(accountId, sortOrder = 0).asExternalModel()
    }

    override suspend fun deletePost(accountId: String, postId: String) {
        feedApi.deletePost(postId)
        feedDao.deletePostEverywhere(postId)
    }

    override suspend fun reportPost(
        postId: String,
        reportedUserId: String,
        reason: String,
        additionalDetails: String?,
    ) {
        require(reason.isNotBlank()) { "دلیل گزارش را انتخاب کنید" }
        feedApi.reportPost(
            ReportPostRequestDto(postId, reportedUserId, reason.trim(), additionalDetails?.trim()),
        )
    }

    override suspend fun submitAppeal(postId: String, reason: String) {
        require(postId.isNotBlank()) { "شناسه پست نامعتبر است" }
        require(reason.trim().length >= 10) { "توضیح اعتراض باید حداقل ۱۰ نویسه باشد" }
        feedApi.submitAppeal(
            SubmitAppealRequestDto(
                postId = postId,
                reason = reason.trim(),
            ),
        )
    }

    override suspend fun trackFeedEvent(postId: String, eventType: String) {
        trackDetailedFeedEvent(postId = postId, eventType = eventType)
    }

    override suspend fun trackDetailedFeedEvent(
        postId: String,
        eventType: String,
        dwellMs: Long?,
        watchPct: Float?,
        sourceSurface: String?,
    ) {
        if (postId.isBlank() || eventType !in ALLOWED_FEED_EVENTS) return
        val key = "$postId:$eventType"
        if (eventType == "view") {
            if (!sentViewEvents.add(postId)) return
        }
        pendingEvents[key] = FeedEventRequestDto(
            postId = postId,
            eventType = eventType,
            dwellMs = dwellMs?.takeIf { it > 0L },
            watchPct = watchPct?.coerceIn(0f, 1f),
            sourceSurface = sourceSurface?.trim()?.takeIf(String::isNotEmpty),
        )
    }

    override suspend fun trackBehaviorEvent(event: BehaviorEventDto) {
        if (event.eventType.isBlank()) return
        pendingBehaviorEvents.offer(event)
    }

    override suspend fun markPostNotInterestedLocal(postId: String) {
        if (postId.isBlank()) return
        feedDao.deletePostEverywhere(postId)
    }

    override suspend fun updateAuthorFollowStatus(authorId: String, followStatus: String) {
        feedDao.updateAuthorFollowStatus(authorId, followStatus)
    }

    private suspend fun resolveCachedLocationsForMissing(
        rootAccountId: String,
        posts: List<FeedPostDto>,
    ): Map<String, String> {
        if (rootAccountId.isBlank() || posts.none { it.location.isNullOrBlank() }) {
            return emptyMap()
        }
        val unresolvedIds = posts
            .asSequence()
            .filter { it.location.isNullOrBlank() && postLocationRegistry?.get(it.id) == null }
            .mapTo(HashSet()) { it.id }
        if (unresolvedIds.isEmpty()) return emptyMap()
        val dbPosts = runCatching {
            feedDao.getPostsWithLocation(rootAccountId, "$rootAccountId$NAMESPACE_SEPARATOR")
        }.getOrDefault(emptyList())
        if (dbPosts.isEmpty()) return emptyMap()
        val resolved = LinkedHashMap<String, String>()
        dbPosts.forEach { entity ->
            if (entity.id in unresolvedIds) {
                val loc = entity.location?.trim()?.takeIf(String::isNotBlank)
                if (loc != null && resolved[entity.id] == null) {
                    resolved[entity.id] = loc
                    postLocationRegistry?.record(entity.id, loc)
                }
            }
        }
        return resolved
    }

    private fun FeedPostDto.asEntityPreservingLocation(
        accountId: String,
        sortOrder: Long,
        fallbackLocations: Map<String, String> = emptyMap(),
    ) = run {
        val remoteLocation = location?.trim()?.takeIf(String::isNotBlank)
        val effectiveLocation = if (remoteLocation != null) {
            postLocationRegistry?.record(id, remoteLocation)
            remoteLocation
        } else {
            postLocationRegistry?.get(id)
                ?: fallbackLocations[id]?.trim()?.takeIf(String::isNotBlank)
        }
        val normalizedDto = if (effectiveLocation != null && remoteLocation == null) {
            copy(location = effectiveLocation)
        } else {
            this
        }
        normalizedDto.asEntity(accountId = accountId, sortOrder = sortOrder)
    }

    private fun storageKey(accountId: String, kind: FeedKind): String = when (kind) {
        FeedKind.Explore -> accountId
        FeedKind.Following -> "$accountId${NAMESPACE_SEPARATOR}following"
    }

    private fun detailStorageKey(accountId: String): String =
        "$accountId${NAMESPACE_SEPARATOR}post-detail"

    private fun userPostsStorageKey(accountId: String, userId: String): String =
        "$accountId${NAMESPACE_SEPARATOR}profile-posts$NAMESPACE_SEPARATOR$userId"

    private companion object {
        const val NAMESPACE_SEPARATOR = "\u001F"
        val ALLOWED_FEED_EVENTS = setOf(
            "view", "dwell", "open", "like", "comment", "save", "share", "skip", "not_interested",
        )
    }
}
