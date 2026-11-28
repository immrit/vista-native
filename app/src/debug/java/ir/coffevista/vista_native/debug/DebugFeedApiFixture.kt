package ir.coffevista.vista_native.debug

import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import ir.coffevista.vista_native.R
import ir.coffevista.vista_native.core.database.profile.OwnProfileDao
import ir.coffevista.vista_native.features.feed.data.AuthorInfoDto
import ir.coffevista.vista_native.features.feed.data.FeedApiFixture
import ir.coffevista.vista_native.features.feed.data.FeedPostDto
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import ir.coffevista.vista_native.features.feed.data.FeedResponseDto
import ir.coffevista.vista_native.features.feed.data.SocialCommentsSummaryDto
import ir.coffevista.vista_native.features.feed.data.SocialPostAnalysisDto
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugFeedApiFixture @Inject constructor() : FeedApiFixture {
    private var scenario: String? = null
    private val firstPageRequests = AtomicInteger(0)

    override fun configure(rawScenario: String?) {
        scenario = rawScenario
    }

    override suspend fun responseOrNull(limit: Int, offset: Int): FeedResponseDto? {
        if (offset == 0) {
            firstPageRequests.incrementAndGet()
        }
        return when (scenario) {
            "valid-session" -> page(limit = limit, offset = offset)
            "offline-valid-session", "feed-error" -> throw IOException("debug feed offline")
            else -> null
        }
    }

    override suspend fun followingResponseOrNull(
        limit: Int,
        offset: Int,
    ): FeedResponseDto? = when (scenario) {
        "valid-session" -> page(limit = limit, offset = offset)
        "offline-valid-session", "feed-error" -> throw IOException("debug feed offline")
        else -> null
    }

    override suspend fun postResponseOrNull(postId: String): FeedPostDto? =
        when (scenario) {
            "valid-session" -> {
                val index = postId
                    .removePrefix("fixture-post-")
                    .toIntOrNull()
                    ?.takeIf { it in 1..22 }
                    ?: 1
                post(index).copy(id = postId)
            }
            "offline-valid-session", "feed-error" -> {
                throw IOException("debug feed offline")
            }
            else -> null
        }

    override suspend fun userPostsResponseOrNull(
        userId: String,
        limit: Int,
        offset: Int,
    ): FeedResponseDto? = when (scenario) {
        "valid-session" -> {
            val matching = (1..22).map(::post).filter { it.userId == userId }
            val posts = matching.drop(offset).take(limit)
            FeedResponseDto(
                posts = posts,
                hasMore = offset + posts.size < matching.size,
                nextCursor = posts.lastOrNull()?.createdAt,
            )
        }
        "offline-valid-session", "feed-error" -> throw IOException("debug feed offline")
        else -> null
    }

    override suspend fun postAnalysisOrNull(postId: String, mode: String): SocialPostAnalysisDto? {
        if (scenario != "valid-session") return null
        return if (mode == "fact_check") {
            SocialPostAnalysisDto(
                postId = postId,
                mode = "fact_check",
                headline = "بررسی ادعا و زمینه محتوایی پست",
                verdict = "context_needed",
                summary = "محتوای این پست بر پایه تجربه کاربری و مشاهده عمومی منتشر شده است؛ ادعای اصلی در فضای فناوری رایج است اما برای نتیجه‌گیری قطعی نیاز به داده تکمیلی دارد.",
                keyPoints = listOf(
                    "ادعای مطرح‌شده با روندهای رایج هم‌خوانی دارد اما منبع آماری مشخصی ذکر نشده است.",
                    "واکنش کاربران در بخش نظرات بیشتر بر تجربه عملی و مقایسه ابزارها تمرکز دارد.",
                    "برای تصمیم‌گیری دقیق‌تر بررسی داده‌های مرجع توصیه می‌شود.",
                ),
                contextNote = "صرفاً بر اساس متن عمومی پست و بدون دسترسی به داده‌های خصوصی تحلیل شد.",
                cached = false,
                providerUsed = "Vista-1 Social",
            )
        } else {
            SocialPostAnalysisDto(
                postId = postId,
                mode = "explain",
                headline = "واکاوی هوشمند پیام و زاویه دید نویسنده",
                verdict = "explained",
                summary = "نویسنده در این پست ساختار تعاملی فید ویستا را با تمرکز بر خوانایی بالا و تجربه کاربری مینیمال به چالش کشیده است تا بازخورد واقعی مخاطبان را دریافت کند.",
                keyPoints = listOf(
                    "تمرکز اصلی بر شفافیت پیام، سادگی بصری و سرعت انتقال مفهوم در فید است.",
                    "لحن نوشتار دعوت‌کننده به گفتگو است و زمینه تعامل در نظرات را فراهم می‌کند.",
                    "استفاده از هشتگ‌های هدفمند به دسته‌بندی دقیق‌تر پست در اکسپلور کمک می‌کند.",
                ),
                contextNote = "صرفاً بر اساس متن عمومی پست و بدون دسترسی به داده‌های خصوصی تحلیل شد.",
                cached = false,
                providerUsed = "Vista-1 Flash",
            )
        }
    }

    override suspend fun commentsSummaryOrNull(postId: String): SocialCommentsSummaryDto? {
        if (scenario != "valid-session") return null
        return SocialCommentsSummaryDto(
            postId = postId,
            commentCount = 5,
            overallMood = "مشارکتی و تحلیلی",
            summary = "بحث کاربران پیرامون نظم زمانی پاسخ‌ها و انسجام ساختار درختی نظرات شکل گرفته و اغلب دیدگاه‌ها با لحنی سازنده همراه است.",
            viewpoints = listOf(
                "گروهی از کاربران خوانایی پاسخ‌های تودرتو و تفکیک بصری رشته‌ها را نقطه قوت می‌دانند.",
                "برخی پیشنهاد کرده‌اند دسته‌بندی نظرات طولانی با خلاصه هوشمند سریع‌تر قابل پیگیری باشد.",
                "اجماع کلی نشان‌دهنده تعامل مثبت و کنجکاوی کاربران درباره قابلیت‌های جدید است.",
            ),
            cached = false,
            providerUsed = "Vista-1 Social",
        )
    }

    fun firstPageRequestCount(): Int = firstPageRequests.get()

    private fun page(limit: Int, offset: Int): FeedResponseDto {
        val allPosts = (1..22).map(::post)
        val posts = allPosts.drop(offset).take(limit)
        val nextOffset = offset + posts.size
        return FeedResponseDto(
            posts = posts,
            hasMore = nextOffset < allPosts.size,
            nextCursor = if (nextOffset < allPosts.size) {
                "$CURSOR_PREFIX$nextOffset"
            } else {
                null
            },
        )
    }

    private fun post(index: Int): FeedPostDto {
        val createdSecond = (60 - index).coerceAtLeast(0).toString().padStart(2, '0')
        val authorId = if (index == 2) {
            "fnd-debug-user"
        } else {
            "fixture-author-${(index % 3) + 1}"
        }
        val authorNumber = (index % 3) + 1
        return FeedPostDto(
            id = "fixture-post-$index",
            userId = authorId,
            content = "پست آزمایشی شماره $index برای بررسی فید فقط‌خواندنی",
            imageUrl = if (index == 1 || index % 4 == 0) {
                "android.resource://ir.coffevista.vista_native/${R.drawable.viu_create}"
            } else {
                null
            },
            imageUrls = emptyList(),
            videoUrl = if (index == 1) "https://fixture.invalid/video.mp4" else null,
            musicUrl = null,
            aspectRatio = if (index == 1 || index % 4 == 0) "1:1" else null,
            musicTitle = null,
            tags = listOf("vista", "fixture"),
            likeCount = index.toLong() * 3,
            commentCount = index.toLong(),
            isLiked = false,
            isSaved = false,
            hideLikeCount = false,
            hideCommentCount = false,
            author = AuthorInfoDto(
                userId = authorId,
                username = if (index == 2) "fixture" else "fixture$authorNumber",
                fullName = if (index == 2) "کاربر آزمایشی" else "کاربر آزمایشی $authorNumber",
                avatarUrl = null,
                isVerified = index % 3 == 0,
                verificationType = if (index % 3 == 0) "blueTick" else null,
            ),
            createdAt = "2026-07-27T17:21:${createdSecond}Z",
            updatedAt = "2026-07-27T17:21:${createdSecond}Z",
        )
    }

    private companion object {
        const val CURSOR_PREFIX = "fixture-offset-"
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DebugFeedApiFixtureModule {
    @Provides
    @IntoSet
    fun provideDebugFeedApiFixture(fixture: DebugFeedApiFixture): FeedApiFixture = fixture
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DebugFeedRuntimeEntryPoint {
    fun feedRepository(): FeedRepository
    fun ownProfileDao(): OwnProfileDao
    fun debugFeedApiFixture(): DebugFeedApiFixture
}
