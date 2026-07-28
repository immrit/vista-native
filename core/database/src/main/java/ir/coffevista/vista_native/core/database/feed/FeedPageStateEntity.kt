package ir.coffevista.vista_native.core.database.feed

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_page_state")
data class FeedPageStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "next_offset")
    val nextOffset: Int,
    @ColumnInfo(name = "has_more")
    val hasMore: Boolean,
    @ColumnInfo(name = "next_cursor")
    val nextCursor: String?,
    @ColumnInfo(name = "last_refresh_epoch_millis")
    val lastRefreshEpochMillis: Long,
)
