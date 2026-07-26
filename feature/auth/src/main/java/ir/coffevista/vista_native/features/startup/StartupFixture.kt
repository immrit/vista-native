package ir.coffevista.vista_native.features.startup

import dagger.Module
import dagger.multibindings.Multibinds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Extension point consumed by StartupResolver. The release graph contributes no implementation;
 * deterministic implementations live in debug/test source sets only.
 */
interface StartupFixture {
    fun configure(rawScenario: String?)
    suspend fun destinationOrNull(): StartupDestination?
}

@Module
@InstallIn(SingletonComponent::class)
interface StartupFixtureMultibindings {
    @Multibinds
    fun fixtures(): Set<StartupFixture>
}
