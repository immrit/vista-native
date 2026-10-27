package ir.coffevista.vista_native.features.feed.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class PostDraft(
    val content: String,
    val location: String? = null,
    val aspectRatio: String? = null,
    val hideLikeCount: Boolean = false,
    val hideCommentCount: Boolean = false,
    val commentsDisabled: Boolean = false,
    val savedAtEpochMs: Long = System.currentTimeMillis(),
)

@Singleton
class AddPostDraftStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveDraft(draft: PostDraft) {
        prefs.edit()
            .putString(KEY_CONTENT, draft.content)
            .putString(KEY_LOCATION, draft.location)
            .putString(KEY_ASPECT_RATIO, draft.aspectRatio)
            .putBoolean(KEY_HIDE_LIKES, draft.hideLikeCount)
            .putBoolean(KEY_HIDE_COMMENTS, draft.hideCommentCount)
            .putBoolean(KEY_COMMENTS_DISABLED, draft.commentsDisabled)
            .putLong(KEY_SAVED_AT, draft.savedAtEpochMs)
            .apply()
    }

    fun getDraft(): PostDraft? {
        val content = prefs.getString(KEY_CONTENT, null)
        if (content.isNullOrBlank()) return null
        return PostDraft(
            content = content,
            location = prefs.getString(KEY_LOCATION, null),
            aspectRatio = prefs.getString(KEY_ASPECT_RATIO, null),
            hideLikeCount = prefs.getBoolean(KEY_HIDE_LIKES, false),
            hideCommentCount = prefs.getBoolean(KEY_HIDE_COMMENTS, false),
            commentsDisabled = prefs.getBoolean(KEY_COMMENTS_DISABLED, false),
            savedAtEpochMs = prefs.getLong(KEY_SAVED_AT, 0L),
        )
    }

    fun clearDraft() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "vista_post_draft_store"
        const val KEY_CONTENT = "draft_content"
        const val KEY_LOCATION = "draft_location"
        const val KEY_ASPECT_RATIO = "draft_aspect_ratio"
        const val KEY_HIDE_LIKES = "draft_hide_likes"
        const val KEY_HIDE_COMMENTS = "draft_hide_comments"
        const val KEY_COMMENTS_DISABLED = "draft_comments_disabled"
        const val KEY_SAVED_AT = "draft_saved_at"
    }
}
