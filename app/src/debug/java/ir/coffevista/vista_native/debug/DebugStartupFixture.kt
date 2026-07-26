package ir.coffevista.vista_native.debug

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.features.startup.StartupDestination
import ir.coffevista.vista_native.features.startup.StartupFixture
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

enum class DebugStartupScenario(val wireName: String) {
    NONE("none"),
    FIRST_RUN("first-run"),
    MAINTENANCE_ENABLED("maintenance-enabled"),
    MAINTENANCE_DISABLED("maintenance-disabled"),
    VALID_SESSION("valid-session"),
    ACCESS_EXPIRED_REFRESH_SUCCESS("access-expired-refresh-success"),
    REFRESH_EXPIRED("refresh-expired"),
    REVOKED("revoked"),
    MALFORMED("malformed"),
    OFFLINE_VALID_SESSION("offline-valid-session"),
    OFFLINE_NO_SESSION("offline-no-session"),
    ;

    companion object {
        fun fromWireName(raw: String?): DebugStartupScenario =
            entries.firstOrNull { it.wireName == raw } ?: NONE
    }
}

@Singleton
class DebugStartupFixture @Inject constructor() : StartupFixture {
    private val scenario = AtomicReference(DebugStartupScenario.NONE)

    override fun configure(rawScenario: String?) {
        scenario.set(DebugStartupScenario.fromWireName(rawScenario))
    }

    override suspend fun destinationOrNull(): StartupDestination? = when (scenario.get()) {
        DebugStartupScenario.NONE -> null
        DebugStartupScenario.FIRST_RUN -> StartupDestination.Onboarding
        DebugStartupScenario.MAINTENANCE_ENABLED -> StartupDestination.Maintenance
        DebugStartupScenario.MAINTENANCE_DISABLED -> StartupDestination.Authentication
        DebugStartupScenario.VALID_SESSION,
        DebugStartupScenario.ACCESS_EXPIRED_REFRESH_SUCCESS,
        -> authenticated(offline = false)
        DebugStartupScenario.OFFLINE_VALID_SESSION -> authenticated(offline = true)
        DebugStartupScenario.REFRESH_EXPIRED,
        DebugStartupScenario.REVOKED,
        DebugStartupScenario.MALFORMED,
        DebugStartupScenario.OFFLINE_NO_SESSION,
        -> StartupDestination.Authentication
    }

    private fun authenticated(offline: Boolean) = StartupDestination.Authenticated(
        AuthenticatedContext(
            userId = "fnd-debug-user",
            profileCompleted = true,
            passwordRequired = false,
            offline = offline,
            displayName = "کاربر تست Foundation",
        ),
    )
}

@Module
@InstallIn(SingletonComponent::class)
object DebugStartupFixtureModule {
    @Provides
    @IntoSet
    fun provideDebugStartupFixture(fixture: DebugStartupFixture): StartupFixture = fixture
}
