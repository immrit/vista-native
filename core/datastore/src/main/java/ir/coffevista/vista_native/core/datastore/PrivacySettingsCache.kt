package ir.coffevista.vista_native.core.datastore

import androidx.datastore.core.DataStore
import ir.coffevista.vista_native.core.datastore.proto.AppPreferences
import kotlinx.coroutines.flow.first

/** Non-secret, account-scoped privacy preference snapshot for offline display only. */
data class PrivacySettingsSnapshot(
    val isPrivate: Boolean,
    val lastSeenVisibility: String,
    val messagePrivacy: String,
    val groupAddPrivacy: String,
    val readReceipts: Boolean,
    val allowProfileZoom: Boolean,
)

interface PrivacySettingsCache {
    suspend fun read(ownerId: String): PrivacySettingsSnapshot?
    suspend fun write(ownerId: String, snapshot: PrivacySettingsSnapshot)
}

class ProtoPrivacySettingsCache(
    private val dataStore: DataStore<AppPreferences>,
) : PrivacySettingsCache {
    override suspend fun read(ownerId: String): PrivacySettingsSnapshot? {
        if (ownerId.isBlank()) return null
        val stored = dataStore.data.first()
        return stored.privacySnapshotFor(ownerId)
    }

    override suspend fun write(ownerId: String, snapshot: PrivacySettingsSnapshot) {
        if (ownerId.isBlank()) return
        dataStore.updateData { current ->
            current.toBuilder()
                .setSchemaVersion(CURRENT_SCHEMA_VERSION)
                .setPrivacyCacheOwnerId(ownerId)
                .setPrivacyCacheIsPrivate(snapshot.isPrivate)
                .setPrivacyCacheLastSeenVisibility(snapshot.lastSeenVisibility)
                .setPrivacyCacheMessagePrivacy(snapshot.messagePrivacy)
                .setPrivacyCacheGroupAddPrivacy(snapshot.groupAddPrivacy)
                .setPrivacyCacheReadReceipts(snapshot.readReceipts)
                .setPrivacyCacheAllowProfileZoom(snapshot.allowProfileZoom)
                .build()
        }
    }
}

internal fun AppPreferences.privacySnapshotFor(ownerId: String): PrivacySettingsSnapshot? {
    if (ownerId.isBlank() || privacyCacheOwnerId != ownerId) return null
    return PrivacySettingsSnapshot(
        isPrivate = privacyCacheIsPrivate,
        lastSeenVisibility = privacyCacheLastSeenVisibility.ifBlank { "everyone" },
        messagePrivacy = privacyCacheMessagePrivacy.ifBlank { "everyone" },
        groupAddPrivacy = privacyCacheGroupAddPrivacy.ifBlank { "everyone" },
        readReceipts = privacyCacheReadReceipts,
        allowProfileZoom = privacyCacheAllowProfileZoom,
    )
}
