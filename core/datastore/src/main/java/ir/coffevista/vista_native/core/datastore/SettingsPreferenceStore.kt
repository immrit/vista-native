package ir.coffevista.vista_native.core.datastore

import androidx.datastore.core.DataStore
import ir.coffevista.vista_native.core.datastore.proto.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SettingsPreferences(
    val locale: String = "fa",
    val themeMode: String = "light",
    val reduceMotion: Boolean = false,
    val chatEntryMode: String = "adaptive",
    val emojiStyle: String = "custom",
    val autoplayMedia: Boolean = true,
    val mediaQuality: String = "high",
    val videoDataSaver: Boolean = false,
)

interface SettingsPreferenceStore {
    val preferences: Flow<SettingsPreferences>
    suspend fun update(transform: (SettingsPreferences) -> SettingsPreferences)
}

class ProtoSettingsPreferenceStore(
    private val dataStore: DataStore<AppPreferences>,
) : SettingsPreferenceStore {
    override val preferences: Flow<SettingsPreferences> = dataStore.data.map { it.toSettingsPreferences() }

    override suspend fun update(transform: (SettingsPreferences) -> SettingsPreferences) {
        dataStore.updateData { current ->
            val next = transform(current.toSettingsPreferences())
            current.toBuilder()
                .setSchemaVersion(CURRENT_SCHEMA_VERSION)
                .setAppLocale(next.locale)
                .setThemeMode(next.themeMode)
                .setReduceMotion(next.reduceMotion)
                .setChatEntryMode(next.chatEntryMode)
                .setEmojiStyle(next.emojiStyle)
                .setAutoplayMedia(next.autoplayMedia)
                .setMediaQuality(next.mediaQuality)
                .setVideoDataSaver(next.videoDataSaver)
                .build()
        }
    }
}

internal fun AppPreferences.toSettingsPreferences() = SettingsPreferences(
    locale = appLocale.ifBlank { "fa" },
    themeMode = themeMode.ifBlank { "light" },
    reduceMotion = reduceMotion,
    chatEntryMode = chatEntryMode.takeIf { it in setOf("adaptive", "minimal", "off") } ?: "adaptive",
    emojiStyle = emojiStyle.takeIf { it in setOf("custom", "system") } ?: "custom",
    autoplayMedia = if (schemaVersion < 2) true else autoplayMedia,
    mediaQuality = mediaQuality.ifBlank { "high" },
    videoDataSaver = videoDataSaver,
)
