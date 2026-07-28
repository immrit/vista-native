package ir.coffevista.vista_native.core.database.feed

import androidx.room.TypeConverter

class FeedConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(separator = "|||")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split("|||")
    }
}
