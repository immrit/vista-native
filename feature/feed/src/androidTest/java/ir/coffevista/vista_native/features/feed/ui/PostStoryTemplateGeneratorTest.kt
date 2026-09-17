package ir.coffevista.vista_native.features.feed.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.coffevista.vista_native.features.feed.data.FeedPost
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PostStoryTemplateGeneratorTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val samplePost = FeedPost(
        id = "test-post-123",
        userId = "user-1",
        content = "این یک متن تستی برای بررسی قالب استوری ویستا است #ویستا",
        imageUrl = null,
        imageUrls = emptyList(),
        videoUrl = null,
        musicUrl = null,
        aspectRatio = "1:1",
        musicTitle = null,
        tags = listOf("ویستا"),
        likeCount = 42,
        commentCount = 7,
        isLiked = true,
        isSaved = false,
        hideLikeCount = false,
        hideCommentCount = false,
        authorUsername = "vista_user",
        authorFullName = "کاربر ویستا",
        authorAvatarUrl = null,
        authorIsVerified = true,
        authorVerificationType = "official",
        createdAt = "2026-09-14T10:00:00Z",
    )

    @Test
    fun renderToBitmap_darkTheme_returnsExactStoryDimensions() {
        val bitmap = PostStoryTemplateGenerator.renderToBitmap(samplePost, "dark")
        try {
            assertEquals(1080, bitmap.width)
            assertEquals(1920, bitmap.height)
            assertNotNull(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun renderToBitmap_lightTheme_returnsExactStoryDimensions() {
        val bitmap = PostStoryTemplateGenerator.renderToBitmap(samplePost, "light")
        try {
            assertEquals(1080, bitmap.width)
            assertEquals(1920, bitmap.height)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun renderToBitmap_vistaTheme_returnsExactStoryDimensions() {
        val bitmap = PostStoryTemplateGenerator.renderToBitmap(samplePost, "vista")
        try {
            assertEquals(1080, bitmap.width)
            assertEquals(1920, bitmap.height)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun generateTemplate_savesPngFileInCacheDir() = runTest {
        val file = PostStoryTemplateGenerator.generateTemplate(context, samplePost, "dark")
        try {
            assertTrue(file.exists())
            assertTrue(file.length() > 1024L) // Non-empty image file
            assertTrue(file.name.startsWith("story_post_test-post-123_dark_"))
            assertTrue(file.name.endsWith(".png"))
        } finally {
            file.delete()
        }
    }

    @Test
    fun cleanOldTempFiles_removesExpiredFilesOnly() {
        val dir = File(context.cacheDir, "story_shares").apply { mkdirs() }
        val oldFile = File(dir, "old_story.png").apply {
            writeText("dummy")
            setLastModified(System.currentTimeMillis() - 7200_000L) // 2 hours old
        }
        val newFile = File(dir, "new_story.png").apply {
            writeText("dummy")
            setLastModified(System.currentTimeMillis())
        }

        PostStoryTemplateGenerator.cleanOldTempFiles(context)

        assertFalse("Old file should be deleted", oldFile.exists())
        assertTrue("Fresh file should be retained", newFile.exists())
        newFile.delete()
    }
}
