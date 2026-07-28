package ir.coffevista.vista_native.features.feed.data

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Debug/androidTest extension point. Release contributes no implementation.
 */
interface FeedApiFixture {
    fun configure(rawScenario: String?)
    suspend fun responseOrNull(limit: Int, offset: Int): FeedResponseDto?
}

@Module
@InstallIn(SingletonComponent::class)
interface FeedApiFixtureMultibindings {
    @Multibinds
    fun fixtures(): Set<FeedApiFixture>
}
