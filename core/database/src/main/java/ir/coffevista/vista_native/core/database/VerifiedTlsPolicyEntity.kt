package ir.coffevista.vista_native.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verified_tls_policy")
data class VerifiedTlsPolicyEntity(
    @PrimaryKey
    @ColumnInfo(name = "singleton_id")
    val singletonId: Int = SINGLETON_ID,
    @ColumnInfo(name = "contract_version")
    val contractVersion: Int,
    val revision: Long,
    val mode: String,
    @ColumnInfo(name = "current_pin_sha256")
    val currentPinSha256: String?,
    @ColumnInfo(name = "next_pin_sha256")
    val nextPinSha256: String?,
    @ColumnInfo(name = "expires_at_epoch_seconds")
    val expiresAtEpochSeconds: Long?,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
