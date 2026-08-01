package ir.coffevista.vista_native.features.profile.data

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Debug/androidTest extension point for the current-user profile endpoint.
 * Release contributes no implementation and always delegates to Retrofit.
 */
interface OwnProfileApiFixture {
    fun configure(rawScenario: String?)
    suspend fun profileOrNull(): ProfileDto?
}

@Module
@InstallIn(SingletonComponent::class)
interface OwnProfileApiFixtureMultibindings {
    @Multibinds
    fun fixtures(): Set<OwnProfileApiFixture>
}
