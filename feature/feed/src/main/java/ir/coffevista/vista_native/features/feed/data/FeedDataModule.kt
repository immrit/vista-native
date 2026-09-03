package ir.coffevista.vista_native.features.feed.data

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.coffevista.vista_native.core.network.InternalApi
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FeedDataModule {
    @Binds
    @Singleton
    abstract fun bindPostMediaUploadGateway(
        impl: PostMediaUploader,
    ): PostMediaUploadGateway

    @Binds
    @Singleton
    abstract fun bindFeedRepository(
        impl: OfflineFirstFeedRepository
    ): FeedRepository

    @Binds
    @Singleton
    abstract fun bindCommentRepository(
        impl: DefaultCommentRepository
    ): CommentRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: DefaultNotificationRepository
    ): NotificationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object FeedApiModule {
    @Provides
    @Singleton
    fun provideFeedApi(
        @InternalApi retrofit: Retrofit,
        fixtures: Set<@JvmSuppressWildcards FeedApiFixture>,
    ): FeedApi {
        val remote = retrofit.create(FeedApi::class.java)
        return object : FeedApi {
            override suspend fun getExploreFeed(limit: Int, offset: Int): FeedResponseDto {
                fixtures.forEach { fixture ->
                    fixture.responseOrNull(limit, offset)?.let { return it }
                }
                return remote.getExploreFeed(limit, offset)
            }

            override suspend fun getFollowingFeed(
                limit: Int,
                cursor: String?,
            ): FeedResponseDto {
                fixtures.forEach { fixture ->
                    fixture.followingResponseOrNull(limit, cursor)?.let { return it }
                }
                return remote.getFollowingFeed(limit, cursor)
            }

            override suspend fun getPost(postId: String): FeedPostDto {
                fixtures.forEach { fixture ->
                    fixture.postResponseOrNull(postId)?.let { return it }
                }
                return remote.getPost(postId)
            }

            override suspend fun getUserPosts(
                userId: String,
                limit: Int,
                offset: Int,
            ): FeedResponseDto {
                fixtures.forEach { fixture ->
                    fixture.userPostsResponseOrNull(userId, limit, offset)
                        ?.let { return it }
                }
                return remote.getUserPosts(userId, limit, offset)
            }

            override suspend fun getHashtagPosts(tag: String, limit: Int, offset: Int): FeedResponseDto =
                remote.getHashtagPosts(tag, limit, offset)

            override suspend fun getTrendingHashtags(limit: Int, days: Int): HashtagSuggestionsResponseDto =
                remote.getTrendingHashtags(limit, days)

            override suspend fun searchHashtags(query: String, limit: Int): HashtagSuggestionsResponseDto =
                remote.searchHashtags(query, limit)

            override suspend fun toggleLike(postId: String, body: LikeRequestDto): LikeResponseDto {
                return remote.toggleLike(postId, body)
            }

            override suspend fun toggleSave(postId: String): SaveResponseDto {
                return remote.toggleSave(postId)
            }

            override suspend fun updatePost(postId: String, body: UpdatePostRequestDto): FeedPostDto =
                remote.updatePost(postId, body)

            override suspend fun deletePost(postId: String) = remote.deletePost(postId)

            override suspend fun reportPost(body: ReportPostRequestDto) = remote.reportPost(body)

            override suspend fun submitAppeal(body: SubmitAppealRequestDto) =
                remote.submitAppeal(body)

            override suspend fun trackFeedEvent(body: FeedEventRequestDto) = remote.trackFeedEvent(body)

            override suspend fun presignUpload(request: PostPresignRequestDto): PostPresignResponseDto =
                remote.presignUpload(request)

            override suspend fun createPost(request: CreatePostRequestDto): CreatePostResponseDto =
                remote.createPost(request)
        }
    }

    @Provides
    @Singleton
    fun provideCommentApi(
        @InternalApi retrofit: Retrofit,
        fixtures: Set<@JvmSuppressWildcards CommentApiFixture>,
    ): CommentApi {
        val remote = retrofit.create(CommentApi::class.java)
        return object : CommentApi {
            override suspend fun getComments(
                postId: String,
                limit: Int,
                offset: Int,
            ): CommentListResponseDto {
                fixtures.forEach { fixture ->
                    fixture.responseOrNull(postId, limit, offset)?.let { return it }
                }
                return remote.getComments(postId, limit, offset)
            }

            override suspend fun createComment(request: CreateCommentRequestDto): CommentResponseDto =
                remote.createComment(request)

            override suspend fun deleteComment(commentId: String) = remote.deleteComment(commentId)

            override suspend fun updateComment(
                commentId: String,
                request: UpdateCommentRequestDto,
            ): CommentResponseDto = remote.updateComment(commentId, request)

            override suspend fun reportComment(
                commentId: String,
                request: ReportCommentRequestDto,
            ) = remote.reportComment(commentId, request)

            override suspend fun addMentions(
                commentId: String,
                request: CommentMentionsRequestDto,
            ) = remote.addMentions(commentId, request)
        }
    }

    @Provides
    @Singleton
    fun provideStoryApi(
        @InternalApi retrofit: Retrofit,
    ): ir.coffevista.vista_native.features.stories.data.StoryApi {
        return retrofit.create(ir.coffevista.vista_native.features.stories.data.StoryApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationApi(
        @InternalApi retrofit: Retrofit,
    ): NotificationApi = retrofit.create(NotificationApi::class.java)
}
