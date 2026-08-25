package ir.coffevista.vista_native.debug

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import ir.coffevista.vista_native.features.feed.data.CommentApiFixture
import ir.coffevista.vista_native.features.feed.data.CommentAuthorProfileDto
import ir.coffevista.vista_native.features.feed.data.CommentListResponseDto
import ir.coffevista.vista_native.features.feed.data.CommentResponseDto
import javax.inject.Inject
import javax.inject.Singleton

/** A deterministic multi-level thread used only by the debug `valid-session` fixture. */
@Singleton
class DebugCommentApiFixture @Inject constructor() : CommentApiFixture {
    private var scenario: String? = null

    override fun configure(rawScenario: String?) {
        scenario = rawScenario
    }

    override suspend fun responseOrNull(
        postId: String,
        limit: Int,
        offset: Int,
    ): CommentListResponseDto? {
        if (scenario != "valid-session" || postId != FIXTURE_POST_ID) return null
        val page = comments.drop(offset).take(limit)
        return CommentListResponseDto(
            comments = page,
            hasMore = offset + page.size < comments.size,
        )
    }

    private companion object {
        const val FIXTURE_POST_ID = "fixture-post-1"

        val author = CommentAuthorProfileDto(
            username = "fixture_author",
            fullName = "کاربر آزمایشی",
        )
        val responder = CommentAuthorProfileDto(
            username = "fixture_reply",
            fullName = "پاسخ‌دهنده آزمایشی",
        )
        val comments = listOf(
            CommentResponseDto(
                id = "fixture-root",
                postId = FIXTURE_POST_ID,
                userId = "fixture-author-1",
                content = "این دیدگاه اصلی برای بررسی خط زمان است.",
                createdAt = "2026-07-27T17:21:01Z",
                profiles = author,
            ),
            CommentResponseDto(
                id = "fixture-reply-1",
                postId = FIXTURE_POST_ID,
                userId = "fixture-user-1",
                content = "پاسخ اول به دیدگاه اصلی.",
                createdAt = "2026-07-27T17:22:01Z",
                parentCommentId = "fixture-root",
                profiles = responder,
            ),
            CommentResponseDto(
                id = "fixture-reply-2",
                postId = FIXTURE_POST_ID,
                userId = "fixture-user-2",
                content = "پاسخ دوم به همان دیدگاه.",
                createdAt = "2026-07-27T17:23:01Z",
                parentCommentId = "fixture-root",
                profiles = author,
            ),
            CommentResponseDto(
                id = "fixture-nested-1",
                postId = FIXTURE_POST_ID,
                userId = "fixture-user-3",
                content = "پاسخ تو در تو برای آزمون درخت کامل.",
                createdAt = "2026-07-27T17:24:01Z",
                parentCommentId = "fixture-reply-1",
                profiles = responder,
            ),
            CommentResponseDto(
                id = "fixture-reply-3",
                postId = FIXTURE_POST_ID,
                userId = "fixture-user-4",
                content = "پاسخ سوم؛ باید انتهای ریل باشد.",
                createdAt = "2026-07-27T17:25:01Z",
                parentCommentId = "fixture-root",
                profiles = author,
            ),
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DebugCommentApiFixtureModule {
    @Provides
    @IntoSet
    fun provideDebugCommentApiFixture(fixture: DebugCommentApiFixture): CommentApiFixture = fixture
}
