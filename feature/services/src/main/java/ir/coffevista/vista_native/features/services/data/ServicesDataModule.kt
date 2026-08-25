package ir.coffevista.vista_native.features.services.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.coffevista.vista_native.core.network.InternalApi
import retrofit2.Retrofit
import javax.inject.Singleton

import ir.coffevista.vista_native.features.services.data.nearby.DefaultNearbyRepository
import ir.coffevista.vista_native.features.services.data.nearby.NearbyApi
import ir.coffevista.vista_native.features.services.data.nearby.NearbyRepository

@Module
@InstallIn(SingletonComponent::class)
object ServicesDataModule {

    @Provides
    @Singleton
    fun provideServicesApi(
        @InternalApi retrofit: Retrofit,
    ): ServicesApi = retrofit.create(ServicesApi::class.java)

    @Provides
    @Singleton
    fun provideServicesRepository(
        api: ServicesApi,
    ): ServicesRepository = DefaultServicesRepository(api)

    @Provides
    @Singleton
    fun provideNearbyApi(
        @InternalApi retrofit: Retrofit,
    ): NearbyApi = retrofit.create(NearbyApi::class.java)

    @Provides
    @Singleton
    fun provideNearbyRepository(
        api: NearbyApi,
    ): NearbyRepository = DefaultNearbyRepository(api)
}
