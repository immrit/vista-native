package ir.coffevista.vista_native.features.feed.data

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.coffevista.vista_native.core.network.InternalApi
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FeedDataModule {
    @Binds
    @Singleton
    abstract fun bindFeedRepository(
        impl: OfflineFirstFeedRepository
    ): FeedRepository
}

@Module
@InstallIn(SingletonComponent::class)
object FeedApiModule {
    @Provides
    @Singleton
    fun provideFeedApi(
        @InternalApi retrofit: Retrofit,
        fixtures: Set<@JvmSuppressWildcards FeedApiFixture>,
    ): FeedApi {
        val remote = retrofit.create(FeedApi::class.java)
        return object : FeedApi {
            override suspend fun getFeed(limit: Int, offset: Int): FeedResponseDto {
                fixtures.forEach { fixture ->
                    fixture.responseOrNull(limit, offset)?.let { return it }
                }
                return remote.getFeed(limit, offset)
            }
        }
    }
}
