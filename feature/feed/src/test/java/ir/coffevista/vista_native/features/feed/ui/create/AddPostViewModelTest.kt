package ir.coffevista.vista_native.features.feed.ui.create

import android.net.Uri
import ir.coffevista.vista_native.core.database.profile.OwnProfileDao
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity
import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.AuthorInfoDto
import ir.coffevista.vista_native.features.feed.data.CreatePostRequestDto
import ir.coffevista.vista_native.features.feed.data.CreatePostResponseDto
import ir.coffevista.vista_native.features.feed.data.FeedApi
import ir.coffevista.vista_native.features.feed.data.FeedPostDto
import ir.coffevista.vista_native.features.feed.data.PostDraft
import ir.coffevista.vista_native.features.feed.data.PostDraftCodec
import ir.coffevista.vista_native.features.feed.data.PostDraftStorage
import ir.coffevista.vista_native.features.feed.data.PostMediaUploadGateway
import ir.coffevista.vista_native.features.feed.data.UploadedMediaResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AddPostViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val authStateFlow = MutableStateFlow<AuthenticationState>(
        AuthenticationState.SignedIn(
            AuthenticatedContext(
                userId = "test-user-123",
                profileCompleted = true,
                passwordRequired = false,
                offline = false,
                displayName = "testuser",
            ),
        ),
    )

    private val authStateProvider = object : AuthenticationStateProvider {
        override val state: StateFlow<AuthenticationState> = authStateFlow
    }

    private class FakeFeedApi : FeedApi {
        var lastCreateRequest: CreatePostRequestDto? = null
        var shouldSucceed: Boolean = true

        override suspend fun createPost(request: CreatePostRequestDto): FeedPostDto {
            lastCreateRequest = request
            if (!shouldSucceed) {
                throw java.io.IOException("سرور در دسترس نیست")
            }
            return FeedPostDto(
                id = "created-post",
                userId = "user-test",
                content = request.content,
                imageUrl = request.imageUrl,
                imageUrls = request.imageUrls,
                videoUrl = request.videoUrl,
                author = AuthorInfoDto(userId = "user-test", fullName = "Test User"),
                createdAt = "2026-07-27T17:21:05Z",
                updatedAt = "2026-07-27T17:21:05Z",
            )
        }

        override suspend fun getExploreFeed(limit: Int, offset: Int) = throw NotImplementedError()
        override suspend fun getPersonalizedFeed(limit: Int, offset: Int) = throw NotImplementedError()
        override suspend fun getFollowingFeed(limit: Int, offset: Int) = throw NotImplementedError()
        override suspend fun getPost(postId: String) = throw NotImplementedError()
        override suspend fun getUserPosts(userId: String, limit: Int, offset: Int) = throw NotImplementedError()
        override suspend fun getHashtagPosts(tag: String, limit: Int, offset: Int) = throw NotImplementedError()
        override suspend fun getTrendingHashtags(limit: Int, days: Int) =
            ir.coffevista.vista_native.features.feed.data.HashtagSuggestionsResponseDto()
        override suspend fun searchHashtags(query: String, limit: Int) =
            ir.coffevista.vista_native.features.feed.data.HashtagSuggestionsResponseDto()
        override suspend fun toggleLike(postId: String, body: ir.coffevista.vista_native.features.feed.data.LikeRequestDto) = throw NotImplementedError()
        override suspend fun toggleSave(postId: String) = throw NotImplementedError()
        override suspend fun updatePost(postId: String, body: ir.coffevista.vista_native.features.feed.data.UpdatePostRequestDto) = throw NotImplementedError()
        override suspend fun deletePost(postId: String) = throw NotImplementedError()
        override suspend fun reportPost(body: ir.coffevista.vista_native.features.feed.data.ReportPostRequestDto) = throw NotImplementedError()
        override suspend fun submitAppeal(body: ir.coffevista.vista_native.features.feed.data.SubmitAppealRequestDto) = throw NotImplementedError()
        override suspend fun trackFeedEvent(body: ir.coffevista.vista_native.features.feed.data.FeedEventRequestDto) = throw NotImplementedError()
        override suspend fun trackBehaviorEvents(body: ir.coffevista.vista_native.features.feed.data.BehaviorEventsBatchRequestDto) = Unit
        override suspend fun presignUpload(request: ir.coffevista.vista_native.features.feed.data.PostPresignRequestDto) = throw NotImplementedError()
        override suspend fun deleteUpload(request: ir.coffevista.vista_native.features.feed.data.DeleteUploadRequestDto) = ir.coffevista.vista_native.features.feed.data.DeleteUploadResponseDto(true)
        override suspend fun follow(request: ir.coffevista.vista_native.features.feed.data.FollowActionRequestDto) = throw NotImplementedError()
        override suspend fun unfollow(request: ir.coffevista.vista_native.features.feed.data.FollowActionRequestDto) = throw NotImplementedError()
    }

    private lateinit var fakeApi: FakeFeedApi
    private val attachedMentions = mutableListOf<Pair<String, List<String>>>()
    private val fakeUploader = object : PostMediaUploadGateway {
        override suspend fun uploadImage(
            userId: String,
            uri: Uri,
            maxBytes: Long,
            onProgress: (Float) -> Unit,
        ): UploadedMediaResult = error("Media upload is not used by these tests")

        override suspend fun uploadVideo(
            userId: String,
            videoFile: File,
            maxBytes: Long,
            onProgress: (Float) -> Unit,
        ): UploadedMediaResult = error("Media upload is not used by these tests")

        override suspend fun uploadAudio(
            userId: String,
            uri: Uri,
            maxBytes: Long,
            startMs: Int,
            endMs: Int,
            onProgress: (Float) -> Unit,
        ): UploadedMediaResult = error("Media upload is not used by these tests")

        override suspend fun deleteUpload(objectKey: String): Boolean = true
    }
    private class FakeOwnProfileDao : OwnProfileDao {
        override fun getOwnProfile(userId: String) = MutableStateFlow<OwnProfileEntity?>(null)
        override suspend fun insertOrUpdate(profile: OwnProfileEntity) = Unit
        override suspend fun deleteProfile(userId: String) = Unit
        override suspend fun deleteAll() = Unit
    }

    private lateinit var viewModel: AddPostViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeApi = FakeFeedApi()
        viewModel = AddPostViewModel(
            api = fakeApi,
            uploader = fakeUploader,
            authStateProvider = authStateProvider,
            ownProfileDao = FakeOwnProfileDao(),
            postMentions = ir.coffevista.vista_native.features.feed.data.PostMentions(
                object : ir.coffevista.vista_native.features.feed.data.PostMentionsApi {
                    override suspend fun profile(username: String) =
                        ir.coffevista.vista_native.features.feed.data.PostMentionProfileDto(userId = "mentioned-id")
                    override suspend fun add(postId: String, request: ir.coffevista.vista_native.features.feed.data.PostMentionsRequestDto) {
                        attachedMentions += postId to request.userIds
                    }
                },
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty and cannot submit`() {
        val state = viewModel.uiState.value
        assertEquals("", state.content)
        assertTrue(state.selectedImages.isEmpty())
        assertFalse(state.isVideo)
        assertFalse(state.canSubmit)
    }

    @Test
    fun `successful flat create response attaches mentions to its id`() = runTest {
        viewModel.onContentChanged("Hello @Alice @ALICE email@example.com")
        viewModel.submitPost()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSuccess)
        assertEquals(listOf("created-post" to listOf("mentioned-id")), attachedMentions)
    }

    @Test
    fun `failed creation never attaches mentions`() = runTest {
        fakeApi.shouldSucceed = false
        viewModel.onContentChanged("Hello @Alice")
        viewModel.submitPost()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSuccess)
        assertTrue(attachedMentions.isEmpty())
    }

    @Test
    fun `music-only draft can be submitted after audio selection`() {
        val state = AddPostUiState(
            selectedMusicUrl = "https://example.com/audio.mp3",
            selectedMusicTitle = "موسیقی تست",
            musicDurationMs = 30_000,
            musicEndMs = 15_000,
        )

        assertTrue(state.canSubmit)
    }

    @Test
    fun `content change updates state and enforces character limit`() {
        viewModel.onContentChanged("سلام به همه #ویستا")
        assertEquals("سلام به همه #ویستا", viewModel.uiState.value.content)
        assertTrue(viewModel.uiState.value.canSubmit)

        val longText = "A".repeat(501)
        viewModel.onContentChanged(longText)
        assertEquals("سلام به همه #ویستا", viewModel.uiState.value.content) // rejected because exceeds 500
    }

    @Test
    fun `aspect ratio changes correctly`() {
        viewModel.onAspectRatioChanged(PostAspectRatio.Portrait)
        assertEquals(PostAspectRatio.Portrait, viewModel.uiState.value.aspectRatio)
        assertEquals("4:5", viewModel.uiState.value.aspectRatio.ratioValue)
    }

    @Test
    fun `toggle privacy settings updates state`() {
        viewModel.toggleHideLikeCount(true)
        viewModel.toggleCommentsDisabled(true)

        val state = viewModel.uiState.value
        assertTrue(state.hideLikeCount)
        assertTrue(state.commentsDisabled)
    }

    @Test
    fun `submit text-only post succeeds and parses hashtags`() = runTest(testDispatcher) {
        viewModel.onContentChanged("پست تستی جدید #خبر #فوری")
        viewModel.submitPost()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isUploading)

        val request = fakeApi.lastCreateRequest
        assertNotNull(request)
        assertEquals("پست تستی جدید #خبر #فوری", request?.content)
        assertEquals(listOf("خبر", "فوری"), request?.tags)
    }

    @Test
    fun `video draft can be submitted after video selection`() {
        val state = AddPostUiState(
            selectedVideo = File("fake_video.mp4"),
            isVideo = true,
        )

        assertTrue(state.canSubmit)
    }

    @Test
    fun `trimmed video updates state and allows submission`() {
        val testFile = File("fake_trimmed_video.mp4")
        viewModel.onVideoTrimmed(testFile)

        val state = viewModel.uiState.value
        assertTrue(state.isVideo)
        assertEquals(testFile, state.selectedVideo)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `submit post failure updates error message`() = runTest(testDispatcher) {
        fakeApi.shouldSucceed = false
        viewModel.onContentChanged("پست ناموفق")
        viewModel.submitPost()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertFalse(state.isUploading)
        assertEquals("سرور در دسترس نیست", state.errorMessage)
    }

    @Test
    fun `post draft codec round-trips full draft including media metadata`() {
        val original = PostDraft(
            content = "پیش‌نویس تستی",
            location = "تهران",
            aspectRatio = "4:5",
            hideLikeCount = true,
            hideCommentCount = true,
            commentsDisabled = true,
            imagePaths = listOf("/tmp/img_0.jpg", "/tmp/img_1.jpg"),
            videoPath = "/tmp/draft_video.mp4",
            videoSourceUri = "content://media/12",
            videoDurationMs = 15_000L,
            musicPath = "/tmp/draft_music.mp3",
            musicUrl = "https://cdn.example.com/song.mp3",
            musicTitle = "آهنگ نمونه",
            musicStartMs = 3_000,
            musicEndMs = 18_000,
            musicDurationMs = 90_000,
            isMusicBackgroundMode = true,
        )
        assertTrue(original.hasContent())
        val decoded = PostDraftCodec.decode(PostDraftCodec.encode(original))
        assertEquals(original, decoded)
        assertFalse(PostDraft().hasContent())
    }

    @Test
    fun `draft save restore discard and submit-copy preserve media safely`() = runTest(testDispatcher) {
        val tempRoot = createTempDir(prefix = "draft-vm-test-")
        try {
            val draftDir = File(tempRoot, "post_drafts/test-user-123").apply { mkdirs() }
            val cacheDir = File(tempRoot, "cache").apply { mkdirs() }
            val sourceVideo = File(tempRoot, "trimmed.mp4").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
            val fakeStorage = object : PostDraftStorage {
                private val saved = mutableMapOf<String, PostDraft>()
                override fun saveDraft(userId: String, draft: PostDraft) {
                    saved[userId] = draft
                }
                override fun getDraft(userId: String): PostDraft? = saved[userId]
                override fun clearDraft(userId: String) {
                    saved.remove(userId)
                    draftDir.deleteRecursively()
                }
                override fun persistImages(userId: String, uris: List<Uri>) = emptyList<String>()
                override fun persistVideo(userId: String, videoFile: File?, videoUri: Uri?): String? {
                    if (videoFile == null || !videoFile.exists()) return null
                    draftDir.mkdirs()
                    val dest = File(draftDir, "draft_video.mp4")
                    if (videoFile.canonicalPath != dest.canonicalPath) {
                        videoFile.copyTo(dest, overwrite = true)
                    }
                    return dest.absolutePath
                }
                override fun persistMusic(userId: String, musicUri: Uri?): String? = null
                override fun isDraftFile(file: File): Boolean =
                    file.canonicalPath.startsWith(draftDir.canonicalPath)
                override fun copyDraftVideoForUpload(videoFile: File): File =
                    copyDraftFileToCache(videoFile, "upload-video-", ".mp4")
                override fun copyDraftFileToCache(file: File, prefix: String, suffix: String): File {
                    val out = File.createTempFile(prefix, suffix, cacheDir)
                    file.copyTo(out, overwrite = true)
                    return out
                }
            }

            var uploadedVideoExistsAtUploadTime = false
            val recordingUploader = object : PostMediaUploadGateway {
                override suspend fun uploadImage(userId: String, uri: Uri, maxBytes: Long, onProgress: (Float) -> Unit) =
                    error("unused")
                override suspend fun uploadVideo(userId: String, videoFile: File, maxBytes: Long, onProgress: (Float) -> Unit): UploadedMediaResult {
                    uploadedVideoExistsAtUploadTime = videoFile.exists() && videoFile.length() == 4L
                    return UploadedMediaResult(url = "https://cdn.test/video.mp4", objectKey = "k1", isVideo = true)
                }
                override suspend fun uploadAudio(userId: String, uri: Uri, maxBytes: Long, startMs: Int, endMs: Int, onProgress: (Float) -> Unit) =
                    error("unused")
                override suspend fun deleteUpload(objectKey: String) = true
            }

            fun newVm() = AddPostViewModel(
                api = fakeApi,
                uploader = recordingUploader,
                authStateProvider = authStateProvider,
                ownProfileDao = FakeOwnProfileDao(),
                postMentions = ir.coffevista.vista_native.features.feed.data.PostMentions(
                    object : ir.coffevista.vista_native.features.feed.data.PostMentionsApi {
                        override suspend fun profile(username: String) =
                            ir.coffevista.vista_native.features.feed.data.PostMentionProfileDto(userId = "id")
                        override suspend fun add(postId: String, request: ir.coffevista.vista_native.features.feed.data.PostMentionsRequestDto) = Unit
                    },
                ),
                draftStore = fakeStorage,
            )

            // 1. Compose with video + location + aspect ratio (even with empty caption!) and save draft
            val vm1 = newVm()
            vm1.onLocationSelected("شیراز")
            vm1.onAspectRatioChanged(PostAspectRatio.Portrait)
            vm1.toggleHideLikeCount(true)
            vm1.onVideoTrimmed(sourceVideo)
            assertTrue(vm1.saveDraftNow())

            // 2. Reopen ViewModel and verify full draft restoration
            val vm2 = newVm()
            val restored = vm2.uiState.value
            assertTrue(restored.isDraftRestored)
            assertEquals("شیراز", restored.selectedLocation)
            assertEquals(PostAspectRatio.Portrait, restored.aspectRatio)
            assertTrue(restored.hideLikeCount)
            assertTrue(restored.isVideo)
            assertNotNull(restored.selectedVideo)
            assertTrue(restored.selectedVideo!!.exists())

            // 3. Submit restored draft and ensure video copy stays intact even after draft directory is cleared
            vm2.submitPost()
            advanceUntilIdle()
            assertTrue(vm2.uiState.value.isSuccess)
            assertTrue(uploadedVideoExistsAtUploadTime)
            assertEquals(null, fakeStorage.getDraft("test-user-123"))
        } finally {
            tempRoot.deleteRecursively()
        }
    }
}
