package ir.coffevista.vista_native.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import ir.coffevista.vista_native.core.datastore.proto.AppPreferences
import java.io.InputStream
import java.io.OutputStream

object AppPreferencesSerializer : Serializer<AppPreferences> {
    override val defaultValue: AppPreferences = defaultAppPreferences()

    override suspend fun readFrom(input: InputStream): AppPreferences = try {
        AppPreferences.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
        throw CorruptionException("Cannot read AppPreferences proto", exception)
    }

    override suspend fun writeTo(t: AppPreferences, output: OutputStream) {
        t.writeTo(output)
    }
}

internal fun defaultAppPreferences(): AppPreferences =
    AppPreferences.newBuilder()
        .setSchemaVersion(CURRENT_SCHEMA_VERSION)
        .build()
