package ir.coffevista.vista_native.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface VerifiedTlsPolicyDao {
    @Query("SELECT * FROM verified_tls_policy WHERE singleton_id = 1")
    suspend fun read(): VerifiedTlsPolicyEntity?

    @Upsert
    suspend fun upsert(entity: VerifiedTlsPolicyEntity)

    @Query("DELETE FROM verified_tls_policy")
    suspend fun clear()

    @Transaction
    suspend fun replaceAtomically(
        entity: VerifiedTlsPolicyEntity,
        failAfterWriteForTest: Boolean = false,
    ) {
        clear()
        upsert(entity)
        check(!failAfterWriteForTest) { "Injected transaction failure" }
    }
}
