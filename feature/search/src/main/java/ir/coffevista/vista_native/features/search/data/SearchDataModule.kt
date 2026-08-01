package ir.coffevista.vista_native.features.search.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.coffevista.vista_native.core.network.InternalApi
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchRepositoryModule {
    @Provides
    @Singleton
    fun provideSearchRepository(
        api: SearchApi,
        historyDao: ir.coffevista.vista_native.core.database.search.SearchHistoryDao,
    ): SearchRepository = DefaultSearchRepository(api, historyDao)
}

@Module
@InstallIn(SingletonComponent::class)
object SearchApiModule {
    @Provides
    @Singleton
    fun provideSearchApi(
        @InternalApi retrofit: Retrofit,
    ): SearchApi = retrofit.create(SearchApi::class.java)
}
