package ir.coffevista.vista_native.features.feed.ui.create

import android.net.Uri
import ir.coffevista.vista_native.core.database.profile.OwnProfileDao
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity
import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.CreatePostRequestDto
import ir.coffevista.vista_native.features.feed.data.CreatePostResponseDto
import ir.coffevista.vista_native.features.feed.data.FeedApi
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

        override suspend fun createPost(request: CreatePostRequestDto): CreatePostResponseDto {
            lastCreateRequest = request
            return if (shouldSucceed) {
                CreatePostResponseDto(success = true, id = "created-post")
            } else {
                CreatePostResponseDto(success = false, message = "سرور در دسترس نیست")
            }
        }

        override suspend fun getExploreFeed(limit: Int, offset: Int) = throw NotImplementedError()
        override suspend fun getFollowingFeed(limit: Int, cursor: String?) = throw NotImplementedError()
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
        override suspend fun presignUpload(request: ir.coffevista.vista_native.features.feed.data.PostPresignRequestDto) = throw NotImplementedError()
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
            onProgress: (Float) -> Unit,
        ): UploadedMediaResult = error("Media upload is not used by these tests")
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
}
