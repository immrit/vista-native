package ir.coffevista.vista_native.features.stories.ui.editor

import org.junit.Assert.assertTrue
import org.junit.Test

class StoryVideoUploadPolicyTest {

    @Test
    fun `accepts exact byte and duration limits`() {
        StoryVideoUploadPolicy.validateByteCount(StoryVideoUploadPolicy.MaxBytes)
        StoryVideoUploadPolicy.validateDuration(StoryVideoUploadPolicy.MaxDurationMs)
    }

    @Test
    fun `rejects empty and oversized video bytes`() {
        assertFailsWithMessage("فایل ویدیو خالی است") {
            StoryVideoUploadPolicy.validateByteCount(0)
        }
        assertFailsWithMessage("حجم ویدیو بیش از حد مجاز است") {
            StoryVideoUploadPolicy.validateByteCount(StoryVideoUploadPolicy.MaxBytes + 1)
        }
    }

    @Test
    fun `rejects missing or nonpositive duration`() {
        assertFailsWithMessage("مدت ویدیو قابل تشخیص نیست") {
            StoryVideoUploadPolicy.validateDuration(null)
        }
        assertFailsWithMessage("مدت ویدیو قابل تشخیص نیست") {
            StoryVideoUploadPolicy.validateDuration(0)
        }
    }

    @Test
    fun `rejects duration longer than one minute`() {
        assertFailsWithMessage("مدت ویدیو بیش از حد مجاز است") {
            StoryVideoUploadPolicy.validateDuration(StoryVideoUploadPolicy.MaxDurationMs + 1)
        }
    }

    private fun assertFailsWithMessage(expected: String, block: () -> Unit) {
        val error = try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (error: IllegalArgumentException) {
            error
        }
        assertTrue(error.message.orEmpty().contains(expected))
    }
}
