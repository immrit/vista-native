package ir.coffevista.vista_native.features.feed.ui.create

import android.net.Uri
import android.content.Context
import android.media.MediaMetadataRetriever
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.database.feed.FeedDao
import ir.coffevista.vista_native.core.database.profile.OwnProfileDao
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.CreatePostRequestDto
import ir.coffevista.vista_native.features.feed.data.FeedApi
import ir.coffevista.vista_native.features.feed.data.PostMentions
import ir.coffevista.vista_native.features.feed.data.asEntity
import kotlinx.coroutines.CancellationException
import ir.coffevista.vista_native.features.feed.data.HashtagSuggestionDto
import ir.coffevista.vista_native.features.feed.data.PostMediaUploadGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class PostAspectRatio(val label: String, val ratioValue: String, val floatValue: Float) {
    Square("۱:۱", "1:1", 1f),
    Portrait("۴:۵", "4:5", 4f / 5f),
    Landscape("۱۶:۹", "16:9", 16f / 9f),
    Auto("اصلی", "auto", 1f),
}

data class AddPostUiState(
    val content: String = "",
    val authorAvatarUrl: String? = null,
    val authorUsername: String = "",
    val authorFullName: String = "",
    val authorIsVerified: Boolean = false,
    val authorVerificationType: String? = null,
    val selectedImages: List<Uri> = emptyList(),
    val selectedVideo: File? = null,
    val selectedVideoUri: Uri? = null,
    val isVideo: Boolean = false,
    val aspectRatio: PostAspectRatio = PostAspectRatio.Square,
    val hideLikeCount: Boolean = false,
    val hideCommentCount: Boolean = false,
    val commentsDisabled: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadStatusMessage: String? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isPremium: Boolean = false,
    val maxCharLength: Int = 500,
    val selectedLocation: String? = null,
    val selectedMusicTitle: String? = null,
    val selectedMusicUrl: String? = null,
    val selectedMusicUri: Uri? = null,
    val isMusicBackgroundMode: Boolean = false,
    val musicDurationMs: Int = 0,
    val musicStartMs: Int = 0,
    val musicEndMs: Int = 0,
    val hashtagSuggestions: List<HashtagSuggestionDto> = emptyList(),
    val isLoadingHashtagSuggestions: Boolean = false,
    val mentionedUsernames: List<String> = emptyList(),
) {
    val maxGalleryImages: Int get() = if (isPremium) 10 else 3
    val maxUploadBytes: Long get() = if (isPremium) 100L * 1024 * 1024 else 15L * 1024 * 1024
    val maxVideoDurationMs: Long get() = if (isPremium) 120_000L else 60_000L
    val canSubmit: Boolean get() = !isUploading && (
        content.isNotBlank() || selectedImages.isNotEmpty() || selectedVideo != null || selectedVideoUri != null || selectedMusicUri != null || !selectedMusicUrl.isNullOrBlank()
    )
}

@HiltViewModel
class AddPostViewModel @Inject constructor(
    private val api: FeedApi,
    private val uploader: PostMediaUploadGateway,
    private val authStateProvider: AuthenticationStateProvider,
    private val ownProfileDao: OwnProfileDao,
    private val postMentions: PostMentions,
    private val feedDao: FeedDao? = null,
    @ApplicationContext private val appContext: Context? = null,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle? = null,
    private val postUploadManager: ir.coffevista.vista_native.features.feed.data.PostUploadManager? = null,
    private val draftStore: ir.coffevista.vista_native.features.feed.data.AddPostDraftStore? = null,
) : ViewModel() {

    constructor(
        api: FeedApi,
        uploader: PostMediaUploadGateway,
        authStateProvider: AuthenticationStateProvider,
        ownProfileDao: OwnProfileDao,
        postMentions: PostMentions,
    ) : this(api, uploader, authStateProvider, ownProfileDao, postMentions, null, null, null, null, null)

    private var hashtagSearchJob: Job? = null

    private val _uiState = MutableStateFlow(AddPostUiState())
    val uiState: StateFlow<AddPostUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = (authStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId.orEmpty()

    init {
        draftStore?.getDraft()?.let { draft ->
            _uiState.update { current ->
                if (current.content.isBlank()) {
                    current.copy(
                        content = draft.content,
                        selectedLocation = draft.location,
                        hideLikeCount = draft.hideLikeCount,
                        hideCommentCount = draft.hideCommentCount,
                        commentsDisabled = draft.commentsDisabled,
                    )
                } else current
            }
        }
        savedStateHandle?.getStateFlow<String?>("trimmed_video_path", null)?.let { flow ->
            viewModelScope.launch {
                flow.collect { path ->
                    if (!path.isNullOrBlank()) {
                        val file = File(path)
                        if (file.exists()) {
                            onVideoTrimmed(file)
                            savedStateHandle["trimmed_video_path"] = null
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            val userId = currentUserId
            if (userId.isNotBlank()) {
                ownProfileDao.getOwnProfile(userId).collect { profile ->
                    if (profile != null) {
                        val days = profile.premiumDaysRemaining
                        val isPrem = profile.subscriptionPlan != null || (days != null && days > 0)
                        val maxChars = if (profile.verificationType?.lowercase() == "blue") 10000
                        else if (isPrem || profile.verificationType?.lowercase() in listOf("gold", "black")) 1000
                        else 500
                        _uiState.update { current ->
                            current.copy(
                                authorAvatarUrl = profile.avatarUrl,
                                authorUsername = profile.username.orEmpty(),
                                authorFullName = profile.fullName,
                                authorIsVerified = profile.isVerified,
                                authorVerificationType = profile.verificationType,
                                isPremium = isPrem,
                                maxCharLength = maxChars,
                            )
                        }
                    }
                }
            }
        }
    }

    fun initPreloaded(text: String?, mediaUris: List<Uri> = emptyList()) {
        if (!text.isNullOrBlank() && _uiState.value.content.isBlank()) {
            onContentChanged(text)
        }
        if (mediaUris.isNotEmpty() && _uiState.value.selectedImages.isEmpty()) {
            onImagesSelected(mediaUris)
        }
    }

    fun onContentChanged(newContent: String) {
        if (newContent.length <= _uiState.value.maxCharLength) {
            val mentions = extractMentions(newContent)
            _uiState.update { it.copy(content = newContent, mentionedUsernames = mentions, errorMessage = null) }
            loadHashtagSuggestions(activeHashtagQuery(newContent))
        }
    }

    fun selectHashtagSuggestion(tag: String) {
        val normalized = tag.removePrefix("#").trim()
        if (normalized.isBlank()) return
        _uiState.update { state ->
            val replaced = ACTIVE_HASHTAG.replace(state.content) { match ->
                "${match.groupValues[1]}#$normalized "
            }
            state.copy(content = replaced, hashtagSuggestions = emptyList())
        }
    }

    private fun loadHashtagSuggestions(query: String?) {
        hashtagSearchJob?.cancel()
        if (query == null) {
            _uiState.update { it.copy(hashtagSuggestions = emptyList(), isLoadingHashtagSuggestions = false) }
            return
        }
        hashtagSearchJob = viewModelScope.launch {
            delay(280)
            _uiState.update { it.copy(isLoadingHashtagSuggestions = true) }
            val suggestions = runCatching {
                if (query.isBlank()) api.getTrendingHashtags() else api.searchHashtags(query)
            }.getOrNull()?.hashtags.orEmpty()
            _uiState.update { current ->
                current.copy(
                    hashtagSuggestions = suggestions.distinctBy { it.tag.lowercase() },
                    isLoadingHashtagSuggestions = false,
                )
            }
        }
    }

    private fun activeHashtagQuery(content: String): String? =
        ACTIVE_HASHTAG.find(content)?.groupValues?.getOrNull(2)

    fun onImagesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val currentMax = _uiState.value.maxGalleryImages
        _uiState.update { current ->
            val combined = (current.selectedImages + uris).distinct().take(currentMax)
            current.copy(
                selectedImages = combined,
                selectedVideo = null,
                selectedVideoUri = null,
                isVideo = false,
                errorMessage = null,
            )
        }
    }

    fun onImageCaptured(uri: Uri) {
        val current = _uiState.value.selectedImages
        if (current.size < _uiState.value.maxGalleryImages) {
            _uiState.update {
                it.copy(
                    selectedImages = current + uri,
                    selectedVideo = null,
                    selectedVideoUri = null,
                    isVideo = false,
                    errorMessage = null,
                )
            }
        }
    }

    fun removeImage(uri: Uri) {
        _uiState.update {
            val updated = it.selectedImages.filterNot { item -> item == uri }
            it.copy(selectedImages = updated)
        }
    }

    fun onVideoSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedVideoUri = uri,
                selectedImages = emptyList(),
                isVideo = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (appContext != null) {
                try {
                    val tempFile = File(appContext.cacheDir, "post_video_${System.currentTimeMillis()}.mp4")
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 0) {
                        _uiState.update { current ->
                            if (current.selectedVideo == null) {
                                current.copy(selectedVideo = tempFile)
                            } else current
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun onVideoTrimmed(trimmedFile: File) {
        val oldFile = _uiState.value.selectedVideo
        if (oldFile != null && oldFile.absolutePath != trimmedFile.absolutePath) {
            runCatching { oldFile.delete() }
        }
        _uiState.update {
            it.copy(
                selectedVideo = trimmedFile,
                selectedImages = emptyList(),
                isVideo = true,
                errorMessage = null,
            )
        }
    }

    fun removeVideo() {
        val oldFile = _uiState.value.selectedVideo
        runCatching { oldFile?.delete() }
        _uiState.update {
            it.copy(
                selectedVideo = null,
                selectedVideoUri = null,
                isVideo = false,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching {
            _uiState.value.selectedVideo?.delete()
        }
    }

    fun onAspectRatioChanged(ratio: PostAspectRatio) {
        _uiState.update { it.copy(aspectRatio = ratio) }
    }

    fun toggleHideLikeCount(hide: Boolean) {
        _uiState.update { it.copy(hideLikeCount = hide) }
    }

    fun toggleHideCommentCount(hide: Boolean) {
        _uiState.update { it.copy(hideCommentCount = hide) }
    }

    fun toggleCommentsDisabled(disabled: Boolean) {
        _uiState.update { it.copy(commentsDisabled = disabled) }
    }

    fun onLocationSelected(location: String?) {
        _uiState.update { it.copy(selectedLocation = location) }
    }

    fun saveDraft() {
        val state = _uiState.value
        if (state.content.isNotBlank() || state.selectedLocation != null) {
            draftStore?.saveDraft(
                ir.coffevista.vista_native.features.feed.data.PostDraft(
                    content = state.content,
                    location = state.selectedLocation,
                    aspectRatio = state.aspectRatio.ratioValue,
                    hideLikeCount = state.hideLikeCount,
                    hideCommentCount = state.hideCommentCount,
                    commentsDisabled = state.commentsDisabled,
                )
            )
        }
    }

    fun discardDraft() {
        draftStore?.clearDraft()
    }

    fun onMusicSelected(title: String?, url: String?) {
        _uiState.update {
            if (title == null && url == null) {
                it.copy(
                    selectedMusicTitle = null,
                    selectedMusicUrl = null,
                    selectedMusicUri = null,
                    musicDurationMs = 0,
                    musicStartMs = 0,
                    musicEndMs = 0,
                )
            } else {
                it.copy(selectedMusicTitle = title, selectedMusicUrl = url)
            }
        }
    }

    fun onMusicFileSelected(uri: Uri) {
        viewModelScope.launch {
            val (durationMs, extractedTitle) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                var dur = 0
                var title: String? = null
                if (appContext != null) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(appContext, uri)
                        dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull() ?: 0
                        val titleMeta = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
                        val artistMeta = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
                        if (!artistMeta.isNullOrBlank() && !titleMeta.isNullOrBlank()) {
                            title = "$artistMeta - $titleMeta"
                        } else if (!titleMeta.isNullOrBlank()) {
                            title = titleMeta
                        }
                    } catch (_: Exception) {
                    } finally {
                        runCatching { retriever.release() }
                    }

                    if (title.isNullOrBlank()) {
                        runCatching {
                            appContext.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                    if (idx >= 0) {
                                        val displayName = cursor.getString(idx)
                                        if (!displayName.isNullOrBlank()) {
                                            title = displayName.substringBeforeLast('.').trim()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Pair(dur, title)
            }
            val finalDurationMs = if (durationMs > 0) durationMs else 30_000
            val finalTitle = extractedTitle ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.ifBlank { "موسیقی" } ?: "موسیقی"
            _uiState.update {
                it.copy(
                    selectedMusicUri = uri,
                    selectedMusicUrl = null,
                    selectedMusicTitle = finalTitle,
                    musicDurationMs = finalDurationMs,
                    musicStartMs = 0,
                    musicEndMs = minOf(finalDurationMs, 15_000),
                )
            }
        }
    }

    fun onMusicTrimChanged(startMs: Int, endMs: Int) {
        val duration = _uiState.value.musicDurationMs
        val maxClipMs = if (_uiState.value.isPremium) 60_000 else 15_000
        if (duration <= 0 || startMs !in 0 until duration || endMs !in 1..duration || endMs <= startMs || endMs - startMs > maxClipMs) return
        _uiState.update { it.copy(musicStartMs = startMs, musicEndMs = endMs) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun submitPost() {
        val state = _uiState.value
        val userId = currentUserId
        if (userId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "کاربر احراز هویت نشده است") }
            return
        }
        if (!state.canSubmit) {
            _uiState.update { it.copy(errorMessage = "لطفاً متن یا تصویری برای پست انتخاب کنید") }
            return
        }

        draftStore?.clearDraft()

        if (postUploadManager != null) {
            postUploadManager.startUpload(
                ir.coffevista.vista_native.features.feed.data.PendingPostUpload(
                    userId = userId,
                    content = state.content,
                    images = state.selectedImages,
                    videoFile = state.selectedVideo,
                    videoUri = state.selectedVideoUri,
                    isVideo = state.isVideo,
                    aspectRatio = state.aspectRatio.ratioValue,
                    location = state.selectedLocation,
                    musicUri = state.selectedMusicUri,
                    musicUrl = state.selectedMusicUrl,
                    musicTitle = state.selectedMusicTitle,
                    musicStartMs = state.musicStartMs,
                    musicEndMs = state.musicEndMs,
                    hideLikeCount = state.hideLikeCount,
                    hideCommentCount = state.hideCommentCount,
                    commentsDisabled = state.commentsDisabled,
                    maxUploadBytes = state.maxUploadBytes,
                )
            )
            _uiState.update { it.copy(isSuccess = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploading = true,
                    uploadProgress = 0.05f,
                    uploadStatusMessage = "در حال آماده‌سازی فایل‌ها…",
                    errorMessage = null,
                )
            }

            try {
                var finalVideoUrl: String? = null
                val mentionIds = postMentions.resolve(extractMentions(state.content))
                var finalThumbUrl: String? = null
                val finalImageUrls = mutableListOf<String>()
                val finalMusicUrl = state.selectedMusicUri?.let { audioUri ->
                    _uiState.update { it.copy(uploadStatusMessage = "در حال آپلود موسیقی…") }
                    uploader.uploadAudio(
                        userId = userId,
                        uri = audioUri,
                        maxBytes = state.maxUploadBytes,
                        startMs = state.musicStartMs,
                        endMs = state.musicEndMs,
                    ) { progress ->
                        _uiState.update { it.copy(uploadProgress = 0.05f + progress * 0.25f) }
                    }.url
                } ?: state.selectedMusicUrl

                // 1. Upload Video if present
                if (state.isVideo && (state.selectedVideo != null || state.selectedVideoUri != null)) {
                    _uiState.update { it.copy(uploadStatusMessage = "در حال آپلود ویدیو…") }
                    val videoFile = state.selectedVideo ?: run {
                        val uri = state.selectedVideoUri ?: throw java.io.IOException("فایل ویدیو یافت نشد")
                        val temp = File(appContext?.cacheDir ?: File.createTempFile("post-vid-", ".mp4").parentFile, "post_video_${System.currentTimeMillis()}.mp4")
                        appContext?.contentResolver?.openInputStream(uri)?.use { input ->
                            temp.outputStream().use { output -> input.copyTo(output) }
                        } ?: throw java.io.IOException("امکان خواندن فایل ویدیو وجود ندارد")
                        temp
                    }
                    val result = uploader.uploadVideo(
                        userId = userId,
                        videoFile = videoFile,
                        maxBytes = state.maxUploadBytes,
                        onProgress = { progress ->
                            _uiState.update { it.copy(uploadProgress = 0.1f + progress * 0.7f) }
                        },
                    )
                    finalVideoUrl = result.url
                    finalThumbUrl = result.thumbnailUrl
                }
                // 2. Upload Images if present
                else if (state.selectedImages.isNotEmpty()) {
                    val totalImages = state.selectedImages.size
                    state.selectedImages.forEachIndexed { index, imageUri ->
                        _uiState.update {
                            it.copy(uploadStatusMessage = "در حال آپلود تصویر (${index + 1} از $totalImages)…")
                        }
                        val result = uploader.uploadImage(
                            userId = userId,
                            uri = imageUri,
                            maxBytes = state.maxUploadBytes,
                            onProgress = { p ->
                                val baseProgress = 0.1f + (index.toFloat() / totalImages) * 0.7f
                                val currentItemProgress = (p / totalImages) * 0.7f
                                _uiState.update { it.copy(uploadProgress = baseProgress + currentItemProgress) }
                            },
                        )
                        finalImageUrls.add(result.url)
                    }
                }

                // 3. Extract Hashtags from content
                val hashtags = extractHashtags(state.content)

                // 4. Create Post on Server
                _uiState.update {
                    it.copy(
                        uploadProgress = 0.9f,
                        uploadStatusMessage = "در حال انتشار پست…",
                    )
                }

                val createReq = CreatePostRequestDto(
                    content = state.content.trim().ifBlank { null },
                    imageUrl = finalImageUrls.firstOrNull() ?: finalThumbUrl,
                    imageUrls = finalImageUrls,
                    videoUrl = finalVideoUrl,
                    musicUrl = finalMusicUrl,
                    musicTitle = state.selectedMusicTitle,
                    musicStartMs = state.musicStartMs.takeIf { finalMusicUrl != null },
                    musicEndMs = state.musicEndMs.takeIf { finalMusicUrl != null },
                    aspectRatio = state.aspectRatio.ratioValue,
                    tags = hashtags,
                    location = state.selectedLocation,
                    hideLikeCount = state.hideLikeCount,
                    hideCommentCount = state.hideCommentCount,
                    commentsDisabled = state.commentsDisabled,
                )

                val postDto = api.createPost(createReq)
                postMentions.attach(postDto.id, mentionIds)

                val userId = currentUserId
                if (userId.isNotBlank() && feedDao != null) {
                    runCatching {
                        val exploreEntity = postDto.asEntity(accountId = userId, sortOrder = -1L)
                        val followingEntity = postDto.asEntity(accountId = "$userId\u001Ffollowing", sortOrder = -1L)
                        feedDao.insertPosts(listOf(exploreEntity, followingEntity))
                    }
                }

                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadProgress = 1f,
                        isSuccess = true,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        errorMessage = e.message ?: "خطا در آپلود یا ثبت پست",
                    )
                }
            }
        }
    }

    fun toggleMusicBackgroundMode(background: Boolean) {
        _uiState.update { it.copy(isMusicBackgroundMode = background) }
    }

    private fun extractHashtags(text: String): List<String> {
        val regex = Regex("#[\\p{L}\\p{N}_]+")
        return regex.findAll(text).map { it.value.removePrefix("#") }.toList()
    }

    private fun extractMentions(text: String): List<String> {
        val regex = Regex("(?<![^\\s\\n])@([\\p{L}\\p{N}_]+)")
        return regex.findAll(text).map { it.groupValues[1] }.distinct().toList()
    }

    private companion object {
        val ACTIVE_HASHTAG = Regex("(^|\\s)#([\\p{L}\\p{N}_]*)$")
    }
}
