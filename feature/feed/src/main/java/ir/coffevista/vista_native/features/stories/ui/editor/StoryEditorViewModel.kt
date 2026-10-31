package ir.coffevista.vista_native.features.stories.ui.editor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.PostMediaUploadGateway
import ir.coffevista.vista_native.features.stories.data.StoryDynamicContextService
import ir.coffevista.vista_native.features.stories.data.StoryRepository
import ir.coffevista.vista_native.features.stories.data.StorySecurityGuard
import ir.coffevista.vista_native.features.stories.domain.CreateStoryRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryDurationType
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.domain.StoryLink
import ir.coffevista.vista_native.features.stories.domain.StoryLocation
import ir.coffevista.vista_native.features.stories.domain.StoryMention
import ir.coffevista.vista_native.features.stories.domain.StoryPoll
import ir.coffevista.vista_native.features.stories.domain.StoryPollOption
import ir.coffevista.vista_native.features.stories.domain.StoryPrivacyType
import ir.coffevista.vista_native.features.stories.domain.StoryUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

import ir.coffevista.vista_native.features.feed.music.MusicRepository
import ir.coffevista.vista_native.features.feed.music.MusicTrack
import ir.coffevista.vista_native.features.stories.domain.StoryMusic
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryTransform
import ir.coffevista.vista_native.features.stories.ui.canvas.withTransform
import ir.coffevista.vista_native.features.stories.ui.render.StoryDrawingCodec
import ir.coffevista.vista_native.features.stories.ui.render.StoryRasterizer
import ir.coffevista.vista_native.features.stories.ui.render.StoryStroke
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextSpec
import ir.coffevista.vista_native.features.stories.ui.render.applyTo
import androidx.core.content.FileProvider

internal object StoryVideoUploadPolicy {
    const val MaxBytes: Long = 100L * 1024 * 1024
    const val MaxDurationMs: Long = 60_000L

    fun validateByteCount(byteCount: Long) {
        require(byteCount > 0) { "فایل ویدیو خالی است" }
        require(byteCount <= MaxBytes) { "حجم ویدیو بیش از حد مجاز است (حداکثر ۱۰۰ مگابایت)" }
    }

    fun validateDuration(durationMs: Long?) {
        require(durationMs != null && durationMs > 0) { "مدت ویدیو قابل تشخیص نیست" }
        require(durationMs <= MaxDurationMs) { "مدت ویدیو بیش از حد مجاز است (حداکثر ۶۰ ثانیه)" }
    }
}

internal data class StoryCanvasHistorySnapshot(
    val elements: List<StoryElement>,
    val mediaTransform: StoryTransform,
    val musicTrack: MusicTrack? = null,
    val musicStartMs: Int = 0,
    val musicEndMs: Int = 0,
    val saveMusicToLibrary: Boolean = false,
)

data class StoryEditorUiState(
    val mediaUri: Uri? = null,
    val isVideo: Boolean = false,
    val mediaTransform: StoryTransform = StoryTransform(cx = 0.5f, cy = 0.5f, scale = 1f, rotation = 0f),
    val elements: List<StoryElement> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val musicTrack: MusicTrack? = null,
    val musicStartMs: Int = 0,
    val musicEndMs: Int = 0,
    val saveMusicToLibrary: Boolean = false,
    val durationType: StoryDurationType = StoryDurationType.Hours24,
    val privacyType: StoryPrivacyType = StoryPrivacyType.Everyone,
    val closeFriendCandidates: List<StoryUser> = emptyList(),
    val selectedCloseFriendIds: Set<String> = emptySet(),
    val isLoadingCloseFriends: Boolean = false,
    val closeFriendsError: String? = null,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadStatus: String? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
) {
    val canPublish: Boolean get() = mediaUri != null && !isUploading
}

@HiltViewModel
class StoryEditorViewModel @Inject constructor(
    private val repository: StoryRepository,
    val musicRepository: MusicRepository,
    val contextService: StoryDynamicContextService,
    private val uploader: PostMediaUploadGateway,
    private val authStateProvider: AuthenticationStateProvider,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryEditorUiState())
    val uiState: StateFlow<StoryEditorUiState> = _uiState.asStateFlow()

    // Transactional UndoStore stacks (lightweight structural references, zero bitmap duplication)
    private val undoStack = ArrayDeque<StoryCanvasHistorySnapshot>()
    private val redoStack = ArrayDeque<StoryCanvasHistorySnapshot>()

    private val currentUserId: String
        get() = (authStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId.orEmpty()

    val signedInUserId: String
        get() = currentUserId

    private fun snapshotOf(state: StoryEditorUiState) = StoryCanvasHistorySnapshot(
        elements = state.elements,
        mediaTransform = state.mediaTransform,
        musicTrack = state.musicTrack,
        musicStartMs = state.musicStartMs,
        musicEndMs = state.musicEndMs,
        saveMusicToLibrary = state.saveMusicToLibrary,
    )

    private fun pushHistoryBeforeMutation(current: StoryEditorUiState) {
        val snapshot = snapshotOf(current)
        if (undoStack.lastOrNull() != snapshot) {
            undoStack.addLast(snapshot)
            if (undoStack.size > 60) undoStack.removeFirst()
        }
        redoStack.clear()
    }

    private fun mutateCanvas(block: (StoryEditorUiState) -> StoryEditorUiState) {
        _uiState.update { current ->
            val next = block(current)
            if (snapshotOf(next) == snapshotOf(current)) {
                return@update next
            }
            pushHistoryBeforeMutation(current)
            next.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
            )
        }
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        _uiState.update { current ->
            redoStack.addLast(snapshotOf(current))
            current.copy(
                elements = prev.elements,
                mediaTransform = prev.mediaTransform,
                musicTrack = prev.musicTrack,
                musicStartMs = prev.musicStartMs,
                musicEndMs = prev.musicEndMs,
                saveMusicToLibrary = prev.saveMusicToLibrary,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
            )
        }
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        _uiState.update { current ->
            undoStack.addLast(snapshotOf(current))
            current.copy(
                elements = next.elements,
                mediaTransform = next.mediaTransform,
                musicTrack = next.musicTrack,
                musicStartMs = next.musicStartMs,
                musicEndMs = next.musicEndMs,
                saveMusicToLibrary = next.saveMusicToLibrary,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
            )
        }
    }

    fun onMediaSelected(uri: Uri, isVideo: Boolean = false) {
        if (!StorySecurityGuard.isSafeReadableMediaUri(context, uri)) {
            _uiState.update { it.copy(errorMessage = "دسترسی به این فایل مجاز نیست") }
            return
        }
        undoStack.clear()
        redoStack.clear()
        _uiState.update {
            it.copy(
                mediaUri = uri,
                isVideo = isVideo,
                mediaTransform = StoryTransform(cx = 0.5f, cy = 0.5f, scale = 1f, rotation = 0f),
                canUndo = false,
                canRedo = false,
                errorMessage = null,
            )
        }
    }

    internal fun updateMediaTransform(transform: StoryTransform) {
        mutateCanvas { state ->
            state.copy(mediaTransform = transform)
        }
    }

    fun resetMediaTransform() {
        mutateCanvas { state ->
            state.copy(mediaTransform = StoryTransform(cx = 0.5f, cy = 0.5f, scale = 1f, rotation = 0f))
        }
    }

    fun addTextElement(text: String, colorHex: String = "#FFFFFF", bgHex: String? = null) {
        if (text.isBlank()) return
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "text",
            content = text,
            color = colorHex,
            backgroundColor = bgHex,
            y = 0f,
        )
        mutateCanvas { it.copy(elements = it.elements + element) }
    }

    fun addPollElement(question: String, option1: String, option2: String, variant: Int = 0) {
        if (question.isBlank() || option1.isBlank() || option2.isBlank()) return
        val poll = StoryPoll(
            id = UUID.randomUUID().toString(),
            question = question,
            options = listOf(
                StoryPollOption(id = "opt_1", text = option1),
                StoryPollOption(id = "opt_2", text = option2),
            ),
        )
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "poll",
            poll = poll,
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.48f,
            variant = variant,
        )
        mutateCanvas { it.copy(elements = it.elements + element) }
    }

    fun addLocationElement(
        name: String,
        latitude: Double? = null,
        longitude: Double? = null,
        address: String? = null,
        variant: Int = 1,
    ) {
        if (name.isBlank()) return
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "location",
            location = StoryLocation(
                name = name.trim(),
                latitude = latitude,
                longitude = longitude,
                address = address?.trim()?.takeIf { it.isNotBlank() },
            ),
            y = -0.3f,
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.32f,
            variant = variant,
        )
        mutateCanvas { it.copy(elements = it.elements + element) }
    }

    fun addLinkElement(url: String, title: String?, variant: Int = 0): Boolean {
        val normalizedUrl = StorySecurityGuard.normalizeSafeHttpUrl(url)
        if (normalizedUrl == null) {
            _uiState.update { it.copy(errorMessage = "نشانی وب وارد شده معتبر نیست") }
            return false
        }
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "link",
            link = StoryLink(url = normalizedUrl, title = title?.trim()?.takeIf { it.isNotBlank() }),
            y = 0.3f,
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.65f,
            variant = variant,
        )
        mutateCanvas { it.copy(elements = it.elements + element, errorMessage = null) }
        return true
    }

    fun addMentionElement(username: String, userId: String? = null, variant: Int = 1) {
        if (username.isBlank()) return
        val clean = username.removePrefix("@").trim()
        if (clean.isBlank()) return
        val resolvedUserId = userId?.trim()?.takeIf { it.isNotBlank() } ?: clean
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "mention",
            mention = StoryMention(userId = resolvedUserId, username = clean),
            y = -0.2f,
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.38f,
            variant = variant,
        )
        mutateCanvas { it.copy(elements = it.elements + element) }
    }

    fun addMusicElement(
        track: MusicTrack,
        startMs: Int = 0,
        endMs: Int = 0,
        saveToLibrary: Boolean = false,
    ) {
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "music",
            music = StoryMusic(
                musicUrl = track.musicUrl,
                title = track.title,
                artist = track.artist,
                coverUrl = track.coverUrl,
                startMs = startMs.coerceAtLeast(0),
                endMs = endMs.coerceAtLeast(0),
            ),
            y = 0.35f,
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.72f,
            variant = 1,
        )
        mutateCanvas {
            it.copy(
                musicTrack = track,
                musicStartMs = startMs.coerceAtLeast(0),
                musicEndMs = endMs.coerceAtLeast(0),
                saveMusicToLibrary = saveToLibrary,
                elements = it.elements.filterNot { el -> el.type == "music" } + element,
            )
        }
    }

    fun onMusicConfigured(result: SelectedStoryMusicResult) {
        addMusicElement(
            track = result.track,
            startMs = result.startMs,
            endMs = result.endMs,
            saveToLibrary = result.saveToVistaLibrary,
        )
    }

    fun addQuestionElement(question: String, variant: Int = 0) {
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "question",
            content = question.ifBlank { "از من بپرسید" },
            y = 0f,
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.5f,
            variant = variant,
        )
        mutateCanvas { it.copy(elements = it.elements + element) }
    }

    fun addCountdownElement(title: String, targetTimestampMs: Long, variant: Int = 1) {
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "countdown",
            content = "$title|$targetTimestampMs",
            y = -0.1f,
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.42f,
            variant = variant,
        )
        mutateCanvas { it.copy(elements = it.elements + element) }
    }

    fun addWeatherElement(
        city: String,
        temperature: String,
        conditionEmoji: String? = null,
        variant: Int = 1,
    ) {
        val cleanEmoji = conditionEmoji?.trim().orEmpty()
        val encodedContent = if (cleanEmoji.isNotBlank()) {
            "${city.trim()}|${temperature.trim()}|$cleanEmoji"
        } else {
            "${city.trim()}|${temperature.trim()}"
        }
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "weather",
            content = encodedContent,
            y = -0.25f,
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.28f,
            variant = variant,
        )
        mutateCanvas { it.copy(elements = it.elements + element) }
    }

    fun addDateElement(dateText: String) {
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "date",
            content = dateText,
            y = -0.3f,
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.25f,
            variant = 1,
        )
        mutateCanvas { it.copy(elements = it.elements + element) }
    }

    fun addHashtagElement(tag: String, variant: Int = 2) {
        val clean = tag.trim().removePrefix("#")
        if (clean.isBlank()) return
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "hashtag",
            content = clean,
            y = 0.1f,
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.55f,
            variant = variant,
        )
        mutateCanvas { it.copy(elements = it.elements + element) }
    }

    fun addGifElement(gifUrl: String): Boolean {
        val safeUrl = StorySecurityGuard.normalizeSafeHttpUrl(gifUrl)
        if (safeUrl == null) {
            _uiState.update { it.copy(errorMessage = "نشانی تصویر متحرک معتبر نیست") }
            return false
        }
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "gif",
            content = safeUrl,
            y = 0f,
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.5f,
        )
        mutateCanvas { it.copy(elements = it.elements + element, errorMessage = null) }
        return true
    }

    fun addReactionElement(emoji: String = "❤️") {
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "reaction",
            content = emoji.ifBlank { "❤️" },
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.58f,
            variant = 0,
        )
        mutateCanvas { it.copy(elements = it.elements + element) }
    }

    fun updateReactionEmoji(id: String, emoji: String) {
        mutateCanvas { state ->
            state.copy(
                elements = state.elements.map { el ->
                    if (el.id == id && el.type == "reaction") el.copy(content = emoji) else el
                },
            )
        }
    }

    fun addPhotoCutoutElement(photoUri: Uri) {
        if (!StorySecurityGuard.isSafeReadableMediaUri(context, photoUri)) {
            _uiState.update { it.copy(errorMessage = "دسترسی به این تصویر مجاز نیست") }
            return
        }
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "photo",
            content = photoUri.toString(),
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.5f,
            variant = 0, // 0 = Squircle, 1 = Circle, 2 = Star, 3 = Heart
        )
        mutateCanvas { it.copy(elements = it.elements + element) }
    }

    fun addEmojiStickerElement(emoji: String) {
        if (emoji.isBlank()) return
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "emoji",
            content = emoji,
            schema = 2,
            xNorm = 0.5f,
            yNorm = 0.48f,
            variant = 0,
        )
        mutateCanvas { it.copy(elements = it.elements + element) }
    }

    fun addDrawingElement(drawingJson: String) {
        if (drawingJson.isBlank()) return
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "drawing",
            content = drawingJson,
        )
        mutateCanvas { it.copy(elements = it.elements + element) }
    }

    internal fun addOrUpdateTextElement(spec: StoryTextSpec, existingId: String? = null) {
        if (spec.text.isBlank()) {
            if (existingId != null) removeElement(existingId)
            return
        }
        mutateCanvas { state ->
            val existing = if (existingId != null) state.elements.firstOrNull { it.id == existingId } else null
            if (existing != null) {
                state.copy(
                    elements = state.elements.map { el ->
                        if (el.id == existingId) spec.applyTo(el) else el
                    },
                )
            } else {
                val newElem = spec.applyTo(
                    StoryElement(
                        id = UUID.randomUUID().toString(),
                        type = "text",
                    ),
                ).withTransform(StoryTransform(cx = 0.5f, cy = 0.45f, scale = 1f, rotation = 0f))
                state.copy(elements = state.elements + newElem)
            }
        }
    }

    internal fun updateElementTransform(id: String, transform: StoryTransform) {
        mutateCanvas { state ->
            state.copy(
                elements = state.elements.map { el ->
                    if (el.id == id) el.withTransform(transform) else el
                },
            )
        }
    }

    fun bringToFront(id: String) {
        _uiState.update { state ->
            val target = state.elements.firstOrNull { it.id == id } ?: return@update state
            if (state.elements.lastOrNull()?.id == id) return@update state
            state.copy(elements = state.elements.filterNot { it.id == id } + target)
        }
    }

    fun cycleWidgetVariant(id: String) {
        mutateCanvas { state ->
            state.copy(
                elements = state.elements.map { el ->
                    if (el.id == id) el.copy(schema = 2, variant = (el.variant + 1) % 4) else el
                },
            )
        }
    }

    internal fun setDrawingStrokes(strokes: List<StoryStroke>) {
        val encoded = if (strokes.isNotEmpty()) StoryDrawingCodec.encode(strokes) else ""
        mutateCanvas { state ->
            val withoutDrawing = state.elements.filterNot { it.type == "drawing" }
            if (encoded.isBlank()) {
                state.copy(elements = withoutDrawing)
            } else {
                val drawingElem = StoryElement(
                    id = state.elements.firstOrNull { it.type == "drawing" }?.id ?: UUID.randomUUID().toString(),
                    type = "drawing",
                    content = encoded,
                )
                // Drawing always placed first (bottom-most layer)
                state.copy(elements = listOf(drawingElem) + withoutDrawing)
            }
        }
    }

    fun updateElementPosition(id: String, deltaX: Float, deltaY: Float) {
        mutateCanvas { state ->
            state.copy(
                elements = state.elements.map { el ->
                    if (el.id == id) el.copy(x = (el.x + deltaX).coerceIn(-1.5f, 1.5f), y = (el.y + deltaY).coerceIn(-1.5f, 1.5f))
                    else el
                },
            )
        }
    }

    fun removeElement(id: String) {
        mutateCanvas { state ->
            val removed = state.elements.firstOrNull { it.id == id }
            val nextElements = state.elements.filterNot { el -> el.id == id }
            if (removed?.type == "music") {
                state.copy(
                    elements = nextElements,
                    musicTrack = null,
                    musicStartMs = 0,
                    musicEndMs = 0,
                    saveMusicToLibrary = false,
                )
            } else {
                state.copy(elements = nextElements)
            }
        }
    }

    fun setDuration(duration: StoryDurationType) {
        _uiState.update { it.copy(durationType = duration) }
    }

    fun setPrivacy(privacy: StoryPrivacyType) {
        _uiState.update { it.copy(privacyType = privacy) }
        if (privacy == StoryPrivacyType.CloseFriends && _uiState.value.closeFriendCandidates.isEmpty()) {
            loadCloseFriends()
        }
    }

    fun loadCloseFriends() {
        val userId = currentUserId
        if (userId.isBlank() || _uiState.value.isLoadingCloseFriends) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCloseFriends = true, closeFriendsError = null) }
            val users = repository.getFollowingUsers(userId)
            val selected = repository.getCloseFriendIds()
            _uiState.update { current ->
                current.copy(
                    closeFriendCandidates = users.getOrDefault(emptyList()),
                    selectedCloseFriendIds = selected.getOrDefault(current.selectedCloseFriendIds.toList()).toSet(),
                    isLoadingCloseFriends = false,
                    closeFriendsError = users.exceptionOrNull()?.message ?: selected.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun toggleCloseFriend(userId: String) {
        _uiState.update { current ->
            val selected = current.selectedCloseFriendIds.toMutableSet()
            if (!selected.add(userId)) selected.remove(userId)
            current.copy(selectedCloseFriendIds = selected, closeFriendsError = null)
        }
    }

    fun saveCloseFriends() {
        if (_uiState.value.isLoadingCloseFriends) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCloseFriends = true, closeFriendsError = null) }
            repository.updateCloseFriends(_uiState.value.selectedCloseFriendIds.toList())
                .onSuccess { _uiState.update { it.copy(isLoadingCloseFriends = false) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingCloseFriends = false, closeFriendsError = error.message ?: "ذخیره دوستان نزدیک ناموفق بود") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun publishStory() {
        val state = _uiState.value
        if (state.isUploading || state.isSuccess) return
        val uri = state.mediaUri ?: return
        val userId = currentUserId
        if (userId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "کاربر وارد نشده است") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploading = true,
                    uploadProgress = 0.1f,
                    uploadStatus = "در حال آماده‌سازی رسانه…",
                    errorMessage = null,
                )
            }

            try {
                // 1. Upload any local device music track (plus embedded cover art) so all viewers hear it
                var uploadedMusicRemoteUrl: String? = null
                var uploadedMusicCoverUrl: String? = null
                var effectiveMusicStartMs = state.musicStartMs
                var effectiveMusicEndMs = state.musicEndMs

                val activeMusicElem = state.elements.firstOrNull { it.type == "music" }?.music
                val rawMusicUrl = state.musicTrack?.musicUrl ?: activeMusicElem?.musicUrl
                val rawMusicTitle = state.musicTrack?.title ?: activeMusicElem?.title ?: "موزیک"
                val rawMusicArtist = state.musicTrack?.artist ?: activeMusicElem?.artist ?: "موزیک انتخابی"
                val rawMusicCover = state.musicTrack?.coverUrl ?: activeMusicElem?.coverUrl

                if (!rawMusicUrl.isNullOrBlank() &&
                    (rawMusicUrl.startsWith("content://", ignoreCase = true) ||
                        rawMusicUrl.startsWith("file://", ignoreCase = true))
                ) {
                    _uiState.update {
                        it.copy(
                            uploadProgress = 0.12f,
                            uploadStatus = "در حال پردازش و آپلود موزیک انتخابی…",
                        )
                    }
                    if (!rawMusicCover.isNullOrBlank() &&
                        (rawMusicCover.startsWith("content://", ignoreCase = true) ||
                            rawMusicCover.startsWith("file://", ignoreCase = true))
                    ) {
                        uploadedMusicCoverUrl = runCatching {
                            uploader.uploadImage(userId, Uri.parse(rawMusicCover)).url
                        }.getOrNull()
                    }
                    val audioUploaded = uploader.uploadAudio(
                        userId = userId,
                        uri = Uri.parse(rawMusicUrl),
                        maxBytes = 30L * 1024 * 1024,
                        startMs = state.musicStartMs,
                        endMs = state.musicEndMs,
                    ) { p ->
                        _uiState.update { it.copy(uploadProgress = 0.12f + p * 0.25f) }
                    }
                    uploadedMusicRemoteUrl = audioUploaded.url
                    if (audioUploaded.wasTrimmed) {
                        effectiveMusicStartMs = 0
                        effectiveMusicEndMs = 0
                    }
                    if (state.saveMusicToLibrary) {
                        runCatching {
                            musicRepository.publishMusic(
                                title = rawMusicTitle,
                                artist = rawMusicArtist,
                                musicUrl = audioUploaded.url,
                                coverUrl = uploadedMusicCoverUrl,
                            )
                        }
                    }
                }

                // 2. Upload any local photo-cutout stickers & bind uploaded music CDN URLs
                val uploadedElements = state.elements.map { element ->
                    when (element.type) {
                        "photo" -> {
                            val rawUriStr = element.content.orEmpty()
                            if (rawUriStr.startsWith("content://", ignoreCase = true) ||
                                rawUriStr.startsWith("file://", ignoreCase = true)
                            ) {
                                _uiState.update { it.copy(uploadStatus = "در حال آپلود برش تصویر…") }
                                val cutoutUploaded = uploader.uploadImage(userId, Uri.parse(rawUriStr))
                                element.copy(content = cutoutUploaded.url)
                            } else {
                                element
                            }
                        }
                        "music" -> {
                            val currentMusic = element.music
                            if (currentMusic != null && uploadedMusicRemoteUrl != null) {
                                element.copy(
                                    music = currentMusic.copy(
                                        musicUrl = uploadedMusicRemoteUrl,
                                        coverUrl = uploadedMusicCoverUrl ?: currentMusic.coverUrl?.takeIf {
                                            it.startsWith("http://", ignoreCase = true) ||
                                                it.startsWith("https://", ignoreCase = true)
                                        },
                                        startMs = effectiveMusicStartMs,
                                        endMs = effectiveMusicEndMs,
                                    ),
                                )
                            } else {
                                element
                            }
                        }
                        else -> element
                    }
                }

                // 3. Upload Primary Media (for images, export 9:16 base canvas with dominant gradient + mediaTransform)
                val baseProgressOffset = if (uploadedMusicRemoteUrl != null) 0.40f else 0.15f
                val baseProgressSpan = if (uploadedMusicRemoteUrl != null) 0.48f else 0.65f
                val uploadResult = if (state.isVideo) {
                    _uiState.update { it.copy(uploadStatus = "در حال آپلود ویدیو…") }
                    uploadStoryVideo(userId, uri) { p ->
                        _uiState.update { it.copy(uploadProgress = baseProgressOffset + p * baseProgressSpan) }
                    }
                } else {
                    _uiState.update { it.copy(uploadStatus = "در حال پردازش و آپلود تصویر…") }
                    val exportDir = File(context.cacheDir, "camera").apply { mkdirs() }
                    val tempBaseFile = File(exportDir, "story_export_${System.currentTimeMillis()}.jpg")
                    try {
                        val exported = runCatching {
                            StoryRasterizer.exportBaseCanvasToFile(
                                context = context,
                                mediaUri = uri,
                                mediaTransform = state.mediaTransform,
                                outputFile = tempBaseFile,
                            )
                        }.getOrNull()
                        val effectiveUri = if (exported != null && exported.exists() && exported.length() > 0L) {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.files",
                                exported,
                            )
                        } else {
                            uri
                        }
                        uploader.uploadImage(userId, effectiveUri) { p ->
                            _uiState.update { it.copy(uploadProgress = baseProgressOffset + p * baseProgressSpan) }
                        }
                    } finally {
                        tempBaseFile.delete()
                    }
                }

                // 4. Create Story via API
                _uiState.update {
                    it.copy(
                        uploadProgress = 0.92f,
                        uploadStatus = "در حال انتشار استوری…",
                    )
                }

                val resolvedMusicUrl = uploadedMusicRemoteUrl
                    ?: uploadedElements.firstOrNull { it.type == "music" }?.music?.musicUrl
                    ?: state.musicTrack?.musicUrl?.takeIf {
                        it.startsWith("http://", ignoreCase = true) ||
                            it.startsWith("https://", ignoreCase = true)
                    }
                val resolvedMusicTitle = state.musicTrack?.let { "${it.title} — ${it.artist}" }
                    ?: uploadedElements.firstOrNull { it.type == "music" }?.music?.let { "${it.title} — ${it.artist}" }

                val finalInteractiveElements = if (state.isVideo) {
                    val t = state.mediaTransform
                    val hasVideoTransform = kotlin.math.abs(t.cx - 0.5f) > 0.004f ||
                        kotlin.math.abs(t.cy - 0.5f) > 0.004f ||
                        kotlin.math.abs(t.scale - 1f) > 0.008f ||
                        kotlin.math.abs(t.rotation) > 0.15f
                    val base = uploadedElements.filterNot { it.type == "media_transform" }
                    if (hasVideoTransform) {
                        base + StoryElement(
                            id = "__media_transform__",
                            type = "media_transform",
                            schema = 2,
                            xNorm = t.cx,
                            yNorm = t.cy,
                            scale = t.scale,
                            rotation = t.rotation,
                        )
                    } else {
                        base
                    }
                } else {
                    uploadedElements.filterNot { it.type == "media_transform" }
                }

                val createReq = CreateStoryRequestDto(
                    mediaUrl = uploadResult.url,
                    mediaType = if (state.isVideo) "video" else "image",
                    thumbnailUrl = uploadResult.thumbnailUrl,
                    durationType = if (state.durationType == StoryDurationType.Hours48) "hours48" else "hours24",
                    privacyType = when (state.privacyType) {
                        StoryPrivacyType.CloseFriends -> "close_friends"
                        StoryPrivacyType.Followers -> "followers"
                        StoryPrivacyType.Custom -> "custom"
                        else -> "everyone"
                    },
                    allowedUserIds = state.selectedCloseFriendIds.toList(),
                    interactiveElements = finalInteractiveElements,
                    musicUrl = resolvedMusicUrl,
                    musicTitle = resolvedMusicTitle,
                )

                val result = repository.createStory(createReq)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadProgress = 1f,
                            isSuccess = true,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = result.exceptionOrNull()?.message ?: "خطا در ایجاد استوری",
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        errorMessage = e.message ?: "خطا در آپلود استوری",
                    )
                }
            }
        }
    }

    private suspend fun uploadStoryVideo(
        userId: String,
        uri: Uri,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val maxBytes = StoryVideoUploadPolicy.MaxBytes
        val temporaryVideo = File.createTempFile("story-video-", ".mp4", context.cacheDir)
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw java.io.IOException("فایل ویدیو قابل خواندن نیست")
            input.use { source ->
                temporaryVideo.outputStream().use { target ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        StoryVideoUploadPolicy.validateByteCount(total)
                        target.write(buffer, 0, count)
                    }
                    StoryVideoUploadPolicy.validateByteCount(total)
                }
            }
            val retriever = MediaMetadataRetriever()
            val durationMs = try {
                retriever.setDataSource(temporaryVideo.absolutePath)
                require(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes") {
                    "فایل انتخاب‌شده ویدیوی معتبری نیست"
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                retriever.release()
            }
            StoryVideoUploadPolicy.validateDuration(durationMs)
            currentCoroutineContext().ensureActive()
            uploader.uploadVideo(userId, temporaryVideo, maxBytes, onProgress)
        } finally {
            temporaryVideo.delete()
        }
    }
}
