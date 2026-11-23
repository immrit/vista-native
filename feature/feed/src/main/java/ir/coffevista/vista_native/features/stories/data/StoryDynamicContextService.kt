package ir.coffevista.vista_native.features.stories.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.common.DeviceLocationSnapshotCache
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.network.BootstrapApi
import ir.coffevista.vista_native.features.feed.data.LocationMatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class StoryPlaceSuggestion(
    val name: String,
    val subtitle: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val category: String = "place", // "gps", "neighbourhood", "poi", "city", "custom"
    val distanceMeters: Double? = null,
    val address: String = "",
) {
    fun formattedSubtitle(): String {
        val dist = LocationMatcher.formatDistanceFa(distanceMeters)
        val detail = address.ifBlank { subtitle }.trim()
        return when {
            !dist.isNullOrBlank() && detail.isNotBlank() -> "$dist • $detail"
            !dist.isNullOrBlank() -> dist
            else -> detail
        }
    }
}

data class StoryLiveWeather(
    val city: String,
    val subtitle: String = "",
    val latitude: Double,
    val longitude: Double,
    val tempC: Int,
    val feelsLikeC: Int,
    val weatherCode: Int,
    val isDay: Boolean,
    val conditionLabel: String,
    val conditionEmoji: String,
) {
    val tempF: Int get() = ((tempC * 9.0 / 5.0) + 32.0).roundToInt()

    fun formattedTemperature(celsius: Boolean = true): String {
        val v = if (celsius) tempC else tempF
        val unit = if (celsius) "°C" else "°F"
        val sign = if (v < 0) "−" else ""
        return "$sign${kotlin.math.abs(v).toString().toPersianDigits()}$unit"
    }
}

@Singleton
class StoryDynamicContextService @Inject constructor(
    @ApplicationContext private val context: Context,
    @BootstrapApi baseHttpClient: OkHttpClient,
    private val storyApi: StoryApi,
    private val snapshotCache: DeviceLocationSnapshotCache? = null,
) {
    private val fastHttp: OkHttpClient = baseHttpClient.newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val weatherCache = ConcurrentHashMap<String, Pair<Long, StoryLiveWeather>>()
    private val placeCache = ConcurrentHashMap<String, Pair<Long, List<StoryPlaceSuggestion>>>()

    private val _prewarmedCoordinates = MutableStateFlow<Pair<Double, Double>?>(null)
    val prewarmedCoordinates: StateFlow<Pair<Double, Double>?> = _prewarmedCoordinates.asStateFlow()

    private val _prewarmedPlaces = MutableStateFlow<List<StoryPlaceSuggestion>>(emptyList())
    val prewarmedPlaces: StateFlow<List<StoryPlaceSuggestion>> = _prewarmedPlaces.asStateFlow()

    private val prewarmMutex = Mutex()
    @Volatile
    private var lastPrewarmAtMs: Long = 0L

    fun cachedCoordinates(maxAgeMs: Long = 30 * 60_000L): Pair<Double, Double>? {
        _prewarmedCoordinates.value?.let { return it }
        snapshotCache?.latest(maxAgeMs)?.let { snap ->
            return snap.latitude to snap.longitude
        }
        if (!hasLocationPermission()) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val best = bestLastKnownLocation(lm)
        if (best != null && System.currentTimeMillis() - best.time <= maxAgeMs) {
            val coords = best.latitude to best.longitude
            _prewarmedCoordinates.value = coords
            snapshotCache?.updateCoordinates(best.latitude, best.longitude, best.time)
            return coords
        }
        return snapshotCache?.latestAny()?.let { it.latitude to it.longitude }
    }

    fun cachedSnapshotPlaces(): List<StoryPlaceSuggestion> {
        val direct = _prewarmedPlaces.value
        if (direct.isNotEmpty()) return direct
        val coords = cachedCoordinates() ?: return emptyList()
        val key = placeCacheKey(coords.first, coords.second)
        return placeCache[key]?.second.orEmpty()
    }

    /**
     * Silently acquires GPS/network coordinates and nearby places in the background ONLY when
     * location permission is already granted and location services are enabled.
     * Rate-limited so repeated screen openings do not drain battery or hit rate limits.
     */
    suspend fun prewarmLocationInBackground(
        minRefreshIntervalMs: Long = 8 * 60_000L,
        force: Boolean = false,
    ) {
        if (!hasLocationPermission() || !isLocationServicesEnabled()) return
        prewarmMutex.withLock {
            val now = System.currentTimeMillis()
            val hasWarmData = _prewarmedCoordinates.value != null && _prewarmedPlaces.value.isNotEmpty()
            if (!force && hasWarmData && (now - lastPrewarmAtMs) < minRefreshIntervalMs) {
                return
            }
            lastPrewarmAtMs = now
            val coords = obtainDeviceCoordinates(timeoutMs = 3_000L, forceFresh = false) ?: return
            resolveCurrentPlacesFlow(
                lat = coords.first,
                lon = coords.second,
                forceRefresh = force,
            ).collect { places ->
                if (places.isNotEmpty()) {
                    _prewarmedPlaces.value = places
                }
            }
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationServicesEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            runCatching {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }.getOrDefault(false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun bestLastKnownLocation(lm: LocationManager): Location? {
        val providerNames = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
        return providerNames
            .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
            .filter { loc -> loc.latitude != 0.0 || loc.longitude != 0.0 }
            .maxByOrNull { it.time }
    }

    private fun recordCoordinates(lat: Double, lon: Double, timestampMs: Long = System.currentTimeMillis()): Pair<Double, Double> {
        val pair = lat to lon
        _prewarmedCoordinates.value = pair
        snapshotCache?.updateCoordinates(lat, lon, timestampMs)
        return pair
    }

    @SuppressLint("MissingPermission")
    suspend fun obtainDeviceCoordinates(
        timeoutMs: Long = 3_200L,
        forceFresh: Boolean = false,
    ): Pair<Double, Double>? {
        if (!hasLocationPermission()) return null

        if (!forceFresh) {
            snapshotCache?.latest(15 * 60_000L)?.let { snap ->
                val pair = snap.latitude to snap.longitude
                _prewarmedCoordinates.value = pair
                return pair
            }
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val nowMs = System.currentTimeMillis()
        val bestLastKnown = bestLastKnownLocation(lm)

        // If we have a recent fix (< 20 minutes) and forceFresh isn't requested, return immediately for zero latency
        if (!forceFresh && bestLastKnown != null && (nowMs - bestLastKnown.time) <= 20 * 60_000L) {
            return recordCoordinates(bestLastKnown.latitude, bestLastKnown.longitude, bestLastKnown.time)
        }

        val enabledProviders = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                runCatching { lm.isProviderEnabled(LocationManager.FUSED_PROVIDER) }.getOrDefault(false)
            ) {
                add(LocationManager.FUSED_PROVIDER)
            }
            if (runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
            if (runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
                add(LocationManager.GPS_PROVIDER)
            }
        }

        if (enabledProviders.isEmpty()) {
            return bestLastKnown?.let { recordCoordinates(it.latitude, it.longitude, it.time) }
                ?: snapshotCache?.latestAny()?.let { it.latitude to it.longitude }
        }

        // Race all enabled providers concurrently so the fastest valid fix wins (usually 200-800ms via FUSED/NETWORK)
        val firstFix = CompletableDeferred<Location?>()
        val freshFix = withTimeoutOrNull(timeoutMs) {
            withContext(Dispatchers.Main) {
                val activeCancellations = mutableListOf<() -> Unit>()
                var pendingProviders = enabledProviders.size

                fun completeEmptyIfExhausted() {
                    pendingProviders -= 1
                    if (pendingProviders <= 0 && !firstFix.isCompleted) {
                        firstFix.complete(null)
                    }
                }

                try {
                    for (provider in enabledProviders) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val cancelSignal = CancellationSignal()
                            activeCancellations += { cancelSignal.cancel() }
                            runCatching {
                                lm.getCurrentLocation(
                                    provider,
                                    cancelSignal,
                                    ContextCompat.getMainExecutor(context),
                                ) { loc ->
                                    if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                                        firstFix.complete(loc)
                                    } else {
                                        completeEmptyIfExhausted()
                                    }
                                }
                            }.onFailure {
                                completeEmptyIfExhausted()
                            }
                        } else {
                            val listener = object : LocationListener {
                                override fun onLocationChanged(location: Location) {
                                    runCatching { lm.removeUpdates(this) }
                                    if (location.latitude != 0.0 || location.longitude != 0.0) {
                                        firstFix.complete(location)
                                    } else {
                                        completeEmptyIfExhausted()
                                    }
                                }
                                @Deprecated("Deprecated in Java")
                                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                                override fun onProviderEnabled(provider: String) = Unit
                                override fun onProviderDisabled(provider: String) {
                                    runCatching { lm.removeUpdates(this) }
                                    completeEmptyIfExhausted()
                                }
                            }
                            activeCancellations += { runCatching { lm.removeUpdates(listener) } }
                            runCatching {
                                @Suppress("DEPRECATION")
                                lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                            }.onFailure {
                                completeEmptyIfExhausted()
                            }
                        }
                    }
                    firstFix.await()
                } finally {
                    activeCancellations.forEach { cancel -> runCatching { cancel() } }
                }
            }
        }

        val chosen = freshFix ?: bestLastKnown
        return chosen?.let { recordCoordinates(it.latitude, it.longitude, it.time) }
            ?: snapshotCache?.latestAny()?.let { it.latitude to it.longitude }
    }

    private fun placeCacheKey(lat: Double, lon: Double): String =
        "rev:${"%.3f".format(Locale.US, lat)}:${"%.3f".format(Locale.US, lon)}"

    private fun mergeAndSortPlaces(
        lat: Double,
        lon: Double,
        immediatePlaces: List<StoryPlaceSuggestion>,
        additionalPlaces: List<StoryPlaceSuggestion>,
    ): List<StoryPlaceSuggestion> {
        val immediateKeys = immediatePlaces.map { LocationMatcher.normalize(it.name) }.toSet()
        val allNearby = deduplicatePlaces(immediatePlaces + additionalPlaces).map { item ->
            if (item.distanceMeters == null && item.latitude != null && item.longitude != null &&
                (item.latitude != 0.0 || item.longitude != 0.0)
            ) {
                item.copy(
                    distanceMeters = LocationMatcher.haversineMeters(lat, lon, item.latitude, item.longitude),
                )
            } else {
                item
            }
        }
        val topImmediate = allNearby.filter { LocationMatcher.normalize(it.name) in immediateKeys }
        val nearbyPois = allNearby
            .filterNot { LocationMatcher.normalize(it.name) in immediateKeys }
            .sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
        return topImmediate + nearbyPois
    }

    private fun cacheResolvedPlaces(lat: Double, lon: Double, cacheKey: String, places: List<StoryPlaceSuggestion>) {
        if (places.isEmpty()) return
        placeCache[cacheKey] = System.currentTimeMillis() to places
        _prewarmedPlaces.value = places
        val bestCity = places.firstOrNull { it.category == "city" }?.name
            ?: places.firstOrNull { it.category == "neighbourhood" }?.subtitle
            ?: places.firstOrNull()?.name
        if (!bestCity.isNullOrBlank()) {
            snapshotCache?.updateAddress(lat, lon, bestCity, province = null)
        }
    }

    /**
     * Progressively streams real-world places around `(lat, lon)`:
     * Stage 1 emits fast geocoder + Photon results within ~0.3–1.8s so the UI populates right away,
     * Stage 2 enriches with Nominatim / Overpass / backend (bounded by tight timeouts).
     */
    fun resolveCurrentPlacesFlow(
        lat: Double,
        lon: Double,
        forceRefresh: Boolean = false,
    ): Flow<List<StoryPlaceSuggestion>> = channelFlow {
        withContext(Dispatchers.IO) {
            val cacheKey = placeCacheKey(lat, lon)
            if (!forceRefresh) {
                placeCache[cacheKey]?.let { (ts, items) ->
                    if (System.currentTimeMillis() - ts < 15 * 60_000L && items.isNotEmpty()) {
                        _prewarmedPlaces.value = items
                        send(items)
                        return@withContext
                    }
                }
            }

            val androidGeoDeferred = async {
                withTimeoutOrNull(1_600L) { reverseGeocodeAndroid(lat, lon) }.orEmpty()
            }
            val bigDataDeferred = async {
                withTimeoutOrNull(2_000L) { reverseGeocodeBigDataCloud(lat, lon) }.orEmpty()
            }
            val photonDeferred = async {
                withTimeoutOrNull(2_300L) { fetchNearbyPhoton(lat, lon) }.orEmpty()
            }
            val nominatimDeferred = async {
                withTimeoutOrNull(2_500L) { reverseGeocodeNominatim(lat, lon) }.orEmpty()
            }
            val overpassDeferred = async {
                withTimeoutOrNull(2_800L) { fetchNearbyOverpass(lat, lon) }.orEmpty()
            }
            val backendDeferred = async {
                withTimeoutOrNull(2_200L) {
                    runCatching {
                        storyApi.getStoryPlacesContext(lat = lat, lon = lon).places.map { dto ->
                            val hasValidCoords = dto.latitude != 0.0 || dto.longitude != 0.0
                            val pLat = dto.latitude.takeIf { hasValidCoords }
                            val pLon = dto.longitude.takeIf { hasValidCoords }
                            val dist = if (pLat != null && pLon != null) {
                                LocationMatcher.haversineMeters(lat, lon, pLat, pLon)
                            } else {
                                null
                            }
                            StoryPlaceSuggestion(
                                name = dto.name,
                                subtitle = dto.subtitle.orEmpty(),
                                latitude = pLat,
                                longitude = pLon,
                                category = dto.category ?: "place",
                                distanceMeters = dist,
                                address = dto.subtitle.orEmpty(),
                            )
                        }
                    }.getOrDefault(emptyList())
                }.orEmpty()
            }

            // Stage 1: emit fast Android + BigDataCloud + Photon immediately
            val stage1Immediate = deduplicatePlaces(
                bigDataDeferred.await() + androidGeoDeferred.await(),
            )
            val stage1List = mergeAndSortPlaces(
                lat = lat,
                lon = lon,
                immediatePlaces = stage1Immediate,
                additionalPlaces = photonDeferred.await(),
            )
            if (stage1List.isNotEmpty()) {
                cacheResolvedPlaces(lat, lon, cacheKey, stage1List)
                send(stage1List)
            }

            // Stage 2: enrich with Nominatim, Overpass & backend without blocking Stage 1
            val nomResults = nominatimDeferred.await()
            val overpassResults = overpassDeferred.await()
            val backendResults = backendDeferred.await()

            val fullImmediate = deduplicatePlaces(nomResults + stage1Immediate)
            val stage2List = mergeAndSortPlaces(
                lat = lat,
                lon = lon,
                immediatePlaces = fullImmediate,
                additionalPlaces = photonDeferred.await() + overpassResults + backendResults,
            )

            if (stage2List.isNotEmpty()) {
                if (stage2List != stage1List) {
                    cacheResolvedPlaces(lat, lon, cacheKey, stage2List)
                    send(stage2List)
                }
            } else if (stage1List.isEmpty()) {
                send(
                    listOf(
                        StoryPlaceSuggestion(
                            name = "${"%.4f".format(Locale.US, lat)}, ${"%.4f".format(Locale.US, lon)}",
                            subtitle = "مختصات موقعیت فعلی",
                            latitude = lat,
                            longitude = lon,
                            category = "gps",
                            distanceMeters = 0.0,
                        ),
                    ),
                )
            }
        }
    }

    /**
     * Resolves multi-granularity real-world place suggestions (neighborhood, street, POI, city)
     * around the user's current coordinates with accurate Haversine distances.
     */
    suspend fun resolveCurrentPlaces(
        lat: Double,
        lon: Double,
        forceRefresh: Boolean = false,
    ): List<StoryPlaceSuggestion> =
        resolveCurrentPlacesFlow(lat, lon, forceRefresh).last()

    /**
     * Live dynamic place/city/POI search in Persian & English, optionally biased toward user's coordinates.
     */
    suspend fun searchPlaces(
        query: String,
        biasLat: Double? = null,
        biasLon: Double? = null,
    ): List<StoryPlaceSuggestion> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()
        val hasBias = biasLat != null && biasLon != null && (biasLat != 0.0 || biasLon != 0.0)
        val cacheKey = if (hasBias) {
            "q:${q.lowercase()}:${"%.2f".format(Locale.US, biasLat!!)}:${"%.2f".format(Locale.US, biasLon!!)}"
        } else {
            "q:${q.lowercase()}"
        }
        placeCache[cacheKey]?.let { (ts, items) ->
            if (System.currentTimeMillis() - ts < 10 * 60_000L && items.isNotEmpty()) return@withContext items
        }

        val combined = coroutineScope {
            val photonDeferred = async { searchPhoton(q, limit = 12, biasLat = biasLat, biasLon = biasLon) }
            val nominatimDeferred = async { searchNominatim(q, limit = 8, biasLat = biasLat, biasLon = biasLon) }
            val openMeteoDeferred = async { searchOpenMeteoCities(q, count = 6) }
            val backendDeferred = async {
                runCatching {
                    storyApi.getStoryPlacesContext(query = q).places.map { dto ->
                        val hasValidCoords = dto.latitude != 0.0 || dto.longitude != 0.0
                        StoryPlaceSuggestion(
                            name = dto.name,
                            subtitle = dto.subtitle.orEmpty(),
                            latitude = dto.latitude.takeIf { hasValidCoords },
                            longitude = dto.longitude.takeIf { hasValidCoords },
                            category = dto.category ?: "place",
                            address = dto.subtitle.orEmpty(),
                        )
                    }
                }.getOrDefault(emptyList())
            }
            deduplicatePlaces(
                photonDeferred.await() +
                    nominatimDeferred.await() +
                    openMeteoDeferred.await() +
                    backendDeferred.await(),
            ).map { item ->
                if (hasBias && item.distanceMeters == null && item.latitude != null && item.longitude != null &&
                    (item.latitude != 0.0 || item.longitude != 0.0)
                ) {
                    item.copy(
                        distanceMeters = LocationMatcher.haversineMeters(
                            biasLat!!,
                            biasLon!!,
                            item.latitude,
                            item.longitude,
                        ),
                    )
                } else {
                    item
                }
            }
        }

        if (combined.isNotEmpty()) {
            placeCache[cacheKey] = System.currentTimeMillis() to combined
        }
        combined
    }

    /**
     * Fetches live real-time weather from Open-Meteo (zero API key, free global forecast API)
     * for exact coordinates, automatically resolving the Persian city name if omitted.
     */
    suspend fun fetchWeatherForCoordinates(
        lat: Double,
        lon: Double,
        preferredCityName: String? = null,
        preferredSubtitle: String = "",
    ): StoryLiveWeather? = withContext(Dispatchers.IO) {
        val resolvedCity = preferredCityName?.trim()?.takeIf { it.isNotBlank() }
            ?: resolveCityNameForCoords(lat, lon)
            ?: "موقعیت فعلی"

        val cacheKey = "${"%.2f".format(Locale.US, lat)}:${"%.2f".format(Locale.US, lon)}:$resolvedCity"
        weatherCache[cacheKey]?.let { (ts, cached) ->
            if (System.currentTimeMillis() - ts < 10 * 60_000L) return@withContext cached
        }

        val directWeather = runCatching {
            val url = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,apparent_temperature,weather_code,is_day&timezone=auto",
                lat,
                lon,
            )
            val body = httpGetString(url) ?: return@runCatching null
            val root = json.parseToJsonElement(body).jsonObject
            val current = root["current"]?.jsonObject ?: return@runCatching null
            val tempC = current["temperature_2m"]?.jsonPrimitive?.doubleOrNull?.roundToInt() ?: return@runCatching null
            val feelsC = current["apparent_temperature"]?.jsonPrimitive?.doubleOrNull?.roundToInt() ?: tempC
            val code = current["weather_code"]?.jsonPrimitive?.intOrNull ?: 0
            val isDay = (current["is_day"]?.jsonPrimitive?.intOrNull ?: 1) == 1
            val (condLabel, condEmoji) = mapWmoWeather(code, isDay)
            StoryLiveWeather(
                city = resolvedCity,
                subtitle = preferredSubtitle,
                latitude = lat,
                longitude = lon,
                tempC = tempC,
                feelsLikeC = feelsC,
                weatherCode = code,
                isDay = isDay,
                conditionLabel = condLabel,
                conditionEmoji = condEmoji,
            )
        }.getOrNull()

        val result = directWeather ?: runCatching {
            val dto = storyApi.getStoryWeatherContext(lat = lat, lon = lon, city = resolvedCity)
            if (dto.city.isBlank()) null
            else StoryLiveWeather(
                city = dto.city,
                subtitle = dto.country.orEmpty(),
                latitude = dto.latitude,
                longitude = dto.longitude,
                tempC = dto.tempC,
                feelsLikeC = dto.feelsLikeC,
                weatherCode = dto.weatherCode,
                isDay = dto.isDay,
                conditionLabel = dto.conditionText,
                conditionEmoji = dto.conditionIcon,
            )
        }.getOrNull()

        if (result != null) {
            weatherCache[cacheKey] = System.currentTimeMillis() to result
        }
        result
    }

    suspend fun searchWeatherCities(query: String): List<StoryPlaceSuggestion> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext defaultPopularCities
        val found = searchOpenMeteoCities(q, count = 8)
        if (found.isNotEmpty()) found else searchNominatim(q, limit = 5)
    }

    suspend fun fetchWeatherForCityName(cityName: String): StoryLiveWeather? = withContext(Dispatchers.IO) {
        val q = cityName.trim().ifBlank { "تهران" }
        val preset = defaultPopularCities.firstOrNull { it.name == q }
        if (preset?.latitude != null && preset.longitude != null) {
            return@withContext fetchWeatherForCoordinates(
                lat = preset.latitude,
                lon = preset.longitude,
                preferredCityName = preset.name,
                preferredSubtitle = preset.subtitle,
            )
        }
        val match = searchOpenMeteoCities(q, count = 1).firstOrNull()
            ?: searchNominatim(q, limit = 1).firstOrNull()
        if (match?.latitude != null && match.longitude != null) {
            fetchWeatherForCoordinates(
                lat = match.latitude,
                lon = match.longitude,
                preferredCityName = match.name,
                preferredSubtitle = match.subtitle,
            )
        } else {
            null
        }
    }

    private suspend fun resolveCityNameForCoords(lat: Double, lon: Double): String? {
        val bigData = reverseGeocodeBigDataCloud(lat, lon)
        bigData.firstOrNull { it.category == "city" }?.name?.takeIf { it.isNotBlank() }?.let { return it }
        bigData.firstOrNull()?.name?.takeIf { it.isNotBlank() }?.let { return it }

        val androidGeo = reverseGeocodeAndroid(lat, lon)
        androidGeo.firstOrNull { it.category == "city" }?.name?.takeIf { it.isNotBlank() }?.let { return it }
        androidGeo.firstOrNull()?.name?.takeIf { it.isNotBlank() }?.let { return it }

        val nom = reverseGeocodeNominatim(lat, lon)
        return nom.firstOrNull { it.category == "city" }?.name ?: nom.firstOrNull()?.name
    }

    private fun reverseGeocodeAndroid(lat: Double, lon: Double): List<StoryPlaceSuggestion> {
        if (!Geocoder.isPresent()) return emptyList()
        return runCatching {
            val geocoder = Geocoder(context, Locale.forLanguageTag("fa-IR"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 3).orEmpty()
            val out = ArrayList<StoryPlaceSuggestion>(4)
            for (addr in addresses) {
                val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                val subLocality = addr.subLocality ?: addr.thoroughfare ?: addr.featureName
                val country = addr.countryName.orEmpty()
                if (!subLocality.isNullOrBlank() && subLocality != city && subLocality.any { it.isLetter() }) {
                    val label = if (!city.isNullOrBlank()) "$subLocality، $city" else subLocality
                    out += StoryPlaceSuggestion(
                        name = label,
                        subtitle = city ?: country,
                        latitude = lat,
                        longitude = lon,
                        category = "neighbourhood",
                    )
                }
                if (!city.isNullOrBlank()) {
                    out += StoryPlaceSuggestion(
                        name = city,
                        subtitle = country,
                        latitude = lat,
                        longitude = lon,
                        category = "city",
                    )
                }
            }
            out
        }.getOrDefault(emptyList())
    }

    private fun reverseGeocodeBigDataCloud(lat: Double, lon: Double): List<StoryPlaceSuggestion> = runCatching {
        val url = String.format(
            Locale.US,
            "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=%.5f&longitude=%.5f&localityLanguage=fa",
            lat,
            lon,
        )
        val body = httpGetString(url) ?: return@runCatching emptyList()
        val obj = json.parseToJsonElement(body).jsonObject
        val locality = obj["locality"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val city = obj["city"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            .ifBlank { obj["principalSubdivision"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty() }
        val country = obj["countryName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

        val out = ArrayList<StoryPlaceSuggestion>(3)
        if (locality.isNotBlank() && locality != city) {
            val label = if (city.isNotBlank()) "$locality، $city" else locality
            out += StoryPlaceSuggestion(
                name = label,
                subtitle = city.ifBlank { country },
                latitude = lat,
                longitude = lon,
                category = "neighbourhood",
            )
        }
        if (city.isNotBlank()) {
            out += StoryPlaceSuggestion(
                name = city,
                subtitle = country,
                latitude = lat,
                longitude = lon,
                category = "city",
            )
        }
        out
    }.getOrDefault(emptyList())

    private fun reverseGeocodeNominatim(lat: Double, lon: Double): List<StoryPlaceSuggestion> = runCatching {
        val url = String.format(
            Locale.US,
            "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%.5f&lon=%.5f&accept-language=fa&addressdetails=1",
            lat,
            lon,
        )
        val body = httpGetString(url) ?: return@runCatching emptyList()
        val obj = json.parseToJsonElement(body).jsonObject
        val address = obj["address"]?.jsonObject ?: JsonObject(emptyMap())
        val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val city = firstJsonString(address, "city", "town", "village", "county", "state")
        val district = name.ifBlank {
            firstJsonString(address, "neighbourhood", "suburb", "quarter", "road", "amenity")
        }
        val country = firstJsonString(address, "country")

        val out = ArrayList<StoryPlaceSuggestion>(3)
        if (district.isNotBlank() && district != city) {
            val label = if (city.isNotBlank()) "$district، $city" else district
            out += StoryPlaceSuggestion(
                name = label,
                subtitle = city.ifBlank { country },
                latitude = lat,
                longitude = lon,
                category = "neighbourhood",
            )
        }
        if (city.isNotBlank()) {
            out += StoryPlaceSuggestion(
                name = city,
                subtitle = country,
                latitude = lat,
                longitude = lon,
                category = "city",
            )
        }
        out
    }.getOrDefault(emptyList())

    private fun fetchNearbyPhoton(lat: Double, lon: Double): List<StoryPlaceSuggestion> = runCatching {
        val url = String.format(
            Locale.US,
            "https://photon.komoot.io/reverse?lat=%.5f&lon=%.5f&limit=15&radius=5",
            lat,
            lon,
        )
        val body = httpGetString(url) ?: return@runCatching emptyList()
        parsePhotonFeatures(body, refLat = lat, refLon = lon)
    }.getOrDefault(emptyList())

    private fun searchPhoton(
        query: String,
        limit: Int,
        biasLat: Double? = null,
        biasLon: Double? = null,
    ): List<StoryPlaceSuggestion> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val biasParams = if (biasLat != null && biasLon != null && (biasLat != 0.0 || biasLon != 0.0)) {
            String.format(Locale.US, "&lat=%.5f&lon=%.5f", biasLat, biasLon)
        } else {
            ""
        }
        val url = "https://photon.komoot.io/api/?q=$encoded&limit=$limit$biasParams"
        val body = httpGetString(url) ?: return@runCatching emptyList()
        parsePhotonFeatures(body, refLat = biasLat, refLon = biasLon)
    }.getOrDefault(emptyList())

    private fun parsePhotonFeatures(
        body: String,
        refLat: Double?,
        refLon: Double?,
    ): List<StoryPlaceSuggestion> {
        val root = json.parseToJsonElement(body).jsonObject
        val features = root["features"] as? JsonArray ?: return emptyList()
        val hasRef = refLat != null && refLon != null && (refLat != 0.0 || refLon != 0.0)
        return features.mapNotNull { el ->
            val feat = el as? JsonObject ?: return@mapNotNull null
            val props = feat["properties"] as? JsonObject ?: return@mapNotNull null
            val name = firstJsonString(props, "name", "street", "district", "locality")
            if (name.isBlank()) return@mapNotNull null

            val coords = (feat["geometry"] as? JsonObject)?.get("coordinates") as? JsonArray
            val pLon = coords?.getOrNull(0)?.jsonPrimitive?.doubleOrNull
            val pLat = coords?.getOrNull(1)?.jsonPrimitive?.doubleOrNull

            val osmKey = firstJsonString(props, "osm_key")
            val osmValue = firstJsonString(props, "osm_value")
            val category = when {
                osmKey == "place" && osmValue in setOf("city", "town", "village") -> "city"
                osmKey == "place" && osmValue in setOf("suburb", "neighbourhood", "quarter", "district", "locality") -> "neighbourhood"
                else -> "poi"
            }

            val street = firstJsonString(props, "street")
            val district = firstJsonString(props, "district", "locality")
            val city = firstJsonString(props, "city", "county", "state")
            val country = firstJsonString(props, "country")
            val addressParts = listOf(street, district, city, country)
                .filter { it.isNotBlank() && !it.equals(name, ignoreCase = true) }
                .distinct()
            val detail = addressParts.take(2).joinToString("، ")
            val dist = if (hasRef && pLat != null && pLon != null) {
                LocationMatcher.haversineMeters(refLat!!, refLon!!, pLat, pLon)
            } else {
                null
            }

            StoryPlaceSuggestion(
                name = name,
                subtitle = detail,
                latitude = pLat,
                longitude = pLon,
                category = category,
                distanceMeters = dist,
                address = detail,
            )
        }
    }

    private fun fetchNearbyOverpass(lat: Double, lon: Double): List<StoryPlaceSuggestion> = runCatching {
        val query = String.format(
            Locale.US,
            "[out:json][timeout:6];(" +
                "node[\"place\"~\"suburb|neighbourhood|quarter|square|town|city\"][\"name\"](around:4500,%.5f,%.5f);" +
                "nwr[\"tourism\"~\"attraction|museum|viewpoint|hotel\"][\"name\"](around:3500,%.5f,%.5f);" +
                "nwr[\"leisure\"~\"park|garden|stadium\"][\"name\"](around:3500,%.5f,%.5f);" +
                "nwr[\"shop\"~\"mall|department_store\"][\"name\"](around:3500,%.5f,%.5f);" +
                "nwr[\"amenity\"~\"marketplace|university|cinema|arts_centre|cafe|restaurant|hospital|library\"][\"name\"](around:2500,%.5f,%.5f);" +
                ");out center 25;",
            lat, lon, lat, lon, lat, lon, lat, lon, lat, lon,
        )
        val encoded = URLEncoder.encode(query, "UTF-8")
        val body = httpGetString("https://overpass-api.de/api/interpreter?data=$encoded")
            ?: httpGetString("https://overpass.kumi.systems/api/interpreter?data=$encoded")
            ?: return@runCatching emptyList()
        val root = json.parseToJsonElement(body).jsonObject
        val elements = root["elements"] as? JsonArray ?: return@runCatching emptyList()
        elements.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val tags = obj["tags"] as? JsonObject ?: return@mapNotNull null
            val name = firstJsonString(tags, "name:fa", "name")
            if (name.isBlank()) return@mapNotNull null

            val center = obj["center"] as? JsonObject
            val pLat = obj["lat"]?.jsonPrimitive?.doubleOrNull
                ?: center?.get("lat")?.jsonPrimitive?.doubleOrNull
                ?: return@mapNotNull null
            val pLon = obj["lon"]?.jsonPrimitive?.doubleOrNull
                ?: center?.get("lon")?.jsonPrimitive?.doubleOrNull
                ?: return@mapNotNull null

            val placeTag = firstJsonString(tags, "place")
            val category = when (placeTag) {
                "city", "town" -> "city"
                "suburb", "neighbourhood", "quarter", "square" -> "neighbourhood"
                else -> "poi"
            }

            val street = firstJsonString(tags, "addr:street")
            val district = firstJsonString(tags, "addr:district", "addr:suburb")
            val city = firstJsonString(tags, "addr:city")
            val detail = listOf(street, district, city)
                .filter { it.isNotBlank() && !it.equals(name, ignoreCase = true) }
                .distinct()
                .joinToString("، ")

            StoryPlaceSuggestion(
                name = name,
                subtitle = detail,
                latitude = pLat,
                longitude = pLon,
                category = category,
                distanceMeters = LocationMatcher.haversineMeters(lat, lon, pLat, pLon),
                address = detail,
            )
        }
    }.getOrDefault(emptyList())

    private fun searchNominatim(
        query: String,
        limit: Int,
        biasLat: Double? = null,
        biasLon: Double? = null,
    ): List<StoryPlaceSuggestion> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val viewbox = if (biasLat != null && biasLon != null && (biasLat != 0.0 || biasLon != 0.0)) {
            val delta = 0.65
            String.format(
                Locale.US,
                "&viewbox=%.4f,%.4f,%.4f,%.4f",
                biasLon - delta,
                biasLat + delta,
                biasLon + delta,
                biasLat - delta,
            )
        } else {
            ""
        }
        val url = "https://nominatim.openstreetmap.org/search?format=jsonv2&q=$encoded&accept-language=fa&limit=$limit&addressdetails=1$viewbox"
        val body = httpGetString(url) ?: return@runCatching emptyList()
        val arr = json.parseToJsonElement(body) as? JsonArray ?: return@runCatching emptyList()
        val hasBias = biasLat != null && biasLon != null && (biasLat != 0.0 || biasLon != 0.0)
        arr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val lat = obj["lat"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            val lon = obj["lon"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            val address = obj["address"]?.jsonObject ?: JsonObject(emptyMap())
            val rawName = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val displayName = obj["display_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val primary = rawName.ifBlank { displayName.substringBefore(",").substringBefore("،").trim() }
            if (primary.isBlank()) return@mapNotNull null
            val city = firstJsonString(address, "city", "town", "county", "state")
            val country = firstJsonString(address, "country")
            val sub = when {
                city.isNotBlank() && city != primary && country.isNotBlank() -> "$city، $country"
                city.isNotBlank() && city != primary -> city
                else -> country
            }
            val dist = if (hasBias && lat != null && lon != null) {
                LocationMatcher.haversineMeters(biasLat!!, biasLon!!, lat, lon)
            } else {
                null
            }
            StoryPlaceSuggestion(
                name = primary,
                subtitle = sub,
                latitude = lat,
                longitude = lon,
                category = obj["category"]?.jsonPrimitive?.contentOrNull ?: "poi",
                distanceMeters = dist,
                address = sub,
            )
        }
    }.getOrDefault(emptyList())

    private fun searchOpenMeteoCities(query: String, count: Int): List<StoryPlaceSuggestion> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=$count&language=fa&format=json"
        val body = httpGetString(url) ?: return@runCatching emptyList()
        val root = json.parseToJsonElement(body).jsonObject
        val results = root["results"]?.jsonArray ?: return@runCatching emptyList()
        results.mapNotNull { el ->
            val obj = el.jsonObject
            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val lat = obj["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val lon = obj["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val admin1 = obj["admin1"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val country = obj["country"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val sub = buildList {
                if (admin1.isNotBlank() && admin1 != name) add(admin1)
                if (country.isNotBlank()) add(country)
            }.joinToString("، ")
            StoryPlaceSuggestion(
                name = name,
                subtitle = sub,
                latitude = lat,
                longitude = lon,
                category = "city",
                address = sub,
            )
        }
    }.getOrDefault(emptyList())

    private fun httpGetString(url: String): String? {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "VistaAndroid/2.6.3 (+https://cafevista.ir)")
            .header("Accept", "application/json")
            .get()
            .build()
        return fastHttp.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) null else resp.body?.string()
        }
    }

    private fun firstJsonString(obj: JsonObject, vararg keys: String): String {
        for (k in keys) {
            val v = obj[k]?.jsonPrimitive?.contentOrNull?.trim()
            if (!v.isNullOrBlank()) return v
        }
        return ""
    }

    private fun deduplicatePlaces(items: List<StoryPlaceSuggestion>): List<StoryPlaceSuggestion> {
        val byKey = LinkedHashMap<String, StoryPlaceSuggestion>(items.size)
        for (item in items) {
            val clean = item.name.trim()
            if (clean.isBlank()) continue
            val normKey = LocationMatcher.normalize(clean).ifBlank { clean.lowercase(Locale.ROOT) }
            val normalizedItem = item.copy(name = clean)
            val existing = byKey[normKey]
            if (existing == null) {
                byKey[normKey] = normalizedItem
            } else {
                byKey[normKey] = existing.copy(
                    latitude = existing.latitude ?: normalizedItem.latitude,
                    longitude = existing.longitude ?: normalizedItem.longitude,
                    distanceMeters = existing.distanceMeters ?: normalizedItem.distanceMeters,
                    subtitle = existing.subtitle.ifBlank { normalizedItem.subtitle },
                    address = existing.address.ifBlank { normalizedItem.address },
                )
            }
        }
        return byKey.values.toList()
    }

    companion object {
        val defaultPopularCities: List<StoryPlaceSuggestion> = listOf(
            StoryPlaceSuggestion("تهران", "ایران", 35.6892, 51.3890, "city"),
            StoryPlaceSuggestion("شیراز", "فارس، ایران", 29.5918, 52.5837, "city"),
            StoryPlaceSuggestion("اصفهان", "اصفهان، ایران", 32.6546, 51.6680, "city"),
            StoryPlaceSuggestion("مشهد", "خراسان رضوی، ایران", 36.2972, 59.6067, "city"),
            StoryPlaceSuggestion("تبریز", "آذربایجان شرقی، ایران", 38.0962, 46.2738, "city"),
            StoryPlaceSuggestion("رشت", "گیلان، ایران", 37.2808, 49.5832, "city"),
            StoryPlaceSuggestion("کیش", "هرمزگان، ایران", 26.5578, 53.9806, "city"),
            StoryPlaceSuggestion("اهواز", "خوزستان، ایران", 31.3183, 48.6706, "city"),
        )

        /**
         * Maps WMO 4677 Weather Interpretation Codes (used by Open-Meteo) to Persian label & sky emoji.
         */
        fun mapWmoWeather(code: Int, isDay: Boolean): Pair<String, String> = when {
            code == 0 -> if (isDay) "آفتابی" to "☀️" else "صاف شبانه" to "🌙"
            code == 1 || code == 2 -> if (isDay) "نیمه‌ابری" to "⛅" else "کمی ابری" to "☁️"
            code == 3 -> "ابری" to "☁️"
            code == 45 || code == 48 -> "مه‌آلود" to "🌫️"
            code in 51..67 -> "بارانی" to "🌧️"
            code in 71..77 -> "برفی" to "❄️"
            code in 80..82 -> "رگبار باران" to "🌦️"
            code == 85 || code == 86 -> "بارش برف" to "❄️"
            code >= 95 -> "رعدوبرق" to "⛈️"
            else -> if (isDay) "معتدل" to "☀️" else "شب آرام" to "🌙"
        }
    }
}
