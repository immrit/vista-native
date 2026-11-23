package ir.coffevista.vista_native.features.feed.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists known postId -> location mappings locally so a newly published or viewed post
 * retains its selected location even across feed/profile refreshes or when an endpoint omits `location`.
 */
@Singleton
class PostLocationRegistry private constructor(
    private val prefs: SharedPreferences?,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    /** Pure JVM constructor for unit tests without Android [Context]. */
    constructor() : this(prefs = null)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val entries: LinkedHashMap<String, String> = linkedMapOf()

    init {
        loadFromPrefs()
    }

    @Synchronized
    fun record(postId: String?, location: String?) {
        val cleanId = postId?.trim()?.takeIf(String::isNotBlank) ?: return
        val cleanLocation = location?.trim()?.takeIf(String::isNotBlank) ?: return
        if (entries[cleanId] == cleanLocation) return
        entries.remove(cleanId)
        entries[cleanId] = cleanLocation
        while (entries.size > MAX_ENTRIES) {
            val oldestKey = entries.entries.firstOrNull()?.key ?: break
            entries.remove(oldestKey)
        }
        persistToPrefs()
    }

    @Synchronized
    fun get(postId: String?): String? {
        val cleanId = postId?.trim()?.takeIf(String::isNotBlank) ?: return null
        return entries[cleanId]
    }

    @Synchronized
    fun resolve(postId: String, remoteLocation: String?): String? {
        val cleanRemote = remoteLocation?.trim()?.takeIf(String::isNotBlank)
        if (cleanRemote != null) {
            record(postId, cleanRemote)
            return cleanRemote
        }
        return get(postId)
    }

    @Synchronized
    fun snapshot(): Map<String, String> = HashMap(entries)

    private fun loadFromPrefs() {
        val raw = prefs?.getString(KEY_MAP, null)?.takeIf(String::isNotBlank) ?: return
        runCatching {
            val decoded = json.decodeFromString<Map<String, String>>(raw)
            decoded.forEach { (id, loc) ->
                val cleanId = id.trim()
                val cleanLoc = loc.trim()
                if (cleanId.isNotEmpty() && cleanLoc.isNotEmpty()) {
                    entries[cleanId] = cleanLoc
                }
            }
        }
    }

    private fun persistToPrefs() {
        val target = prefs ?: return
        runCatching {
            val serialized = json.encodeToString(entries as Map<String, String>)
            target.edit().putString(KEY_MAP, serialized).apply()
        }
    }

    private companion object {
        const val PREFS_NAME = "vista_post_locations"
        const val KEY_MAP = "post_locations_json"
        const val MAX_ENTRIES = 250
    }
}
