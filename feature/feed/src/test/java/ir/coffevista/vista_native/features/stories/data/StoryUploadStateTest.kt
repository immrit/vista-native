package ir.coffevista.vista_native.features.stories.data

import ir.coffevista.vista_native.features.stories.domain.StoryDurationType
import ir.coffevista.vista_native.features.stories.domain.StoryPrivacyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryUploadStateTest {

    @Test
    fun `default StoryUploadState is idle`() {
        val state = StoryUploadState()
        assertFalse(state.isUploading)
        assertFalse(state.isSuccess)
        assertEquals(0f, state.progress, 0.001f)
        assertNull(state.errorMessage)
        assertNull(state.statusMessage)
        assertNull(state.pendingUpload)
    }

    @Test
    fun `uploading state updates progress and status`() {
        val pending = PendingStoryUpload(
            userId = "user_123",
            isVideo = false,
            durationType = StoryDurationType.Hours24,
            privacyType = StoryPrivacyType.Everyone,
        )

        val state = StoryUploadState(
            isUploading = true,
            progress = 0.55f,
            statusMessage = "در حال انتشار استوری…",
            pendingUpload = pending,
        )

        assertTrue(state.isUploading)
        assertEquals(0.55f, state.progress, 0.001f)
        assertEquals("در حال انتشار استوری…", state.statusMessage)
        assertEquals("user_123", state.pendingUpload?.userId)
    }

    @Test
    fun `error state captures failure and allows dismiss`() {
        val state = StoryUploadState(
            isUploading = false,
            errorMessage = "خطا در اتصال به سرور",
        )

        assertFalse(state.isUploading)
        assertEquals("خطا در اتصال به سرور", state.errorMessage)

        val dismissed = state.copy(errorMessage = null)
        assertNull(dismissed.errorMessage)
    }
}
