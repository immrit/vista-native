package ir.coffevista.vista_native.features.feed.data

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Debug/androidTest extension point for deterministic comment-thread rendering.
 * Production contributes no implementation and always reaches the backend.
 */
interface CommentApiFixture {
    fun configure(rawScenario: String?)

    suspend fun responseOrNull(
        postId: String,
        limit: Int,
        offset: Int,
    ): CommentListResponseDto?
}

@Module
@InstallIn(SingletonComponent::class)
interface CommentApiFixtureMultibindings {
    @Multibinds
    fun fixtures(): Set<CommentApiFixture>
}
