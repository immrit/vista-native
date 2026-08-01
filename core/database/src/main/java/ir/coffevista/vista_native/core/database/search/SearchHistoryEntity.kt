package ir.coffevista.vista_native.core.database.search

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "search_history",
    primaryKeys = ["account_id", "query"],
)
data class SearchHistoryEntity(
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "query")
    val query: String,
    @ColumnInfo(name = "search_type")
    val searchType: String,
    @ColumnInfo(name = "timestamp_epoch_millis")
    val timestampEpochMillis: Long,
)
