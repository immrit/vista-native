package ir.coffevista.vista_native.core.datastore

import android.content.Context
import androidx.datastore.core.DataMigration
import ir.coffevista.vista_native.core.datastore.proto.AppPreferences

/** Imports only explicit Flutter media choices, without deleting rollback data. */
internal class FlutterMediaSettingsMigration(
    context: Context,
    private val nativePreferencesExisted: Boolean,
) : DataMigration<AppPreferences> {
    private val appContext = context.applicationContext

    override suspend fun shouldMigrate(currentData: AppPreferences): Boolean =
        shouldImportFlutterMediaSettings(nativePreferencesExisted, currentData)

    override suspend fun migrate(currentData: AppPreferences): AppPreferences =
        migrateFlutterMediaSettings(
            currentData,
            appContext.getSharedPreferences(FLUTTER_PREFERENCES, Context.MODE_PRIVATE).all,
        )

    override suspend fun cleanUp() = Unit
}

internal fun shouldImportFlutterMediaSettings(
    nativePreferencesExisted: Boolean,
    current: AppPreferences,
): Boolean = !nativePreferencesExisted && !current.flutterMediaSettingsMigrated

internal fun migrateFlutterMediaSettings(
    current: AppPreferences,
    flutterValues: Map<String, *>,
): AppPreferences {
    val builder = current.toBuilder().setFlutterMediaSettingsMigrated(true)
    if (!current.onboardingCompleted) {
        val completed = flutterValues["flutter.onboarding_completed"] as? Boolean ?: false
        val version = (flutterValues["flutter.onboarding_version"] as? String).orEmpty()
        if (isCurrentOnboardingCompletion(completed, version)) {
            builder.setOnboardingCompleted(true)
            builder.setOnboardingVersion(version)
        }
    }
    (flutterValues["flutter.data_upload_quality"] as? String)
        ?.takeIf { it in setOf("high", "standard", "data_saver") }
        ?.let(builder::setMediaQuality)
    (flutterValues["flutter.video_auto_play"] as? Boolean)
        ?.let(builder::setAutoplayMedia)
    (flutterValues["flutter.video_data_saver"] as? Boolean)
        ?.let(builder::setVideoDataSaver)
    return builder.build()
}

private const val FLUTTER_PREFERENCES = "FlutterSharedPreferences"
