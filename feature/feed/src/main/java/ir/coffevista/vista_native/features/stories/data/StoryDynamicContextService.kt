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
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.network.BootstrapApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
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
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class StoryPlaceSuggestion(
    val name: String,
    val subtitle: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val category: String = "place", // "gps", "neighbourhood", "poi", "city"
)

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
) {
    private val fastHttp: OkHttpClient = baseHttpClient.newBuilder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val weatherCache = ConcurrentHashMap<String, Pair<Long, StoryLiveWeather>>()
    private val placeCache = ConcurrentHashMap<String, Pair<Long, List<StoryPlaceSuggestion>>>()

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
    suspend fun obtainDeviceCoordinates(timeoutMs: Long = 5_500L): Pair<Double, Double>? {
        if (!hasLocationPermission()) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )

        val nowMs = System.currentTimeMillis()
        val bestLastKnown: Location? = providers
            .mapNotNull { provider ->
                runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
            }
            .filter { loc -> loc.latitude != 0.0 || loc.longitude != 0.0 }
            .maxByOrNull { it.time }

        // If we have a recent fix (< 15 minutes), return immediately for zero latency
        if (bestLastKnown != null && (nowMs - bestLastKnown.time) <= 15 * 60_000L) {
            return bestLastKnown.latitude to bestLastKnown.longitude
        }

        val activeProvider = when {
            runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) ->
                LocationManager.NETWORK_PROVIDER
            runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) ->
                LocationManager.GPS_PROVIDER
            else -> null
        }

        if (activeProvider == null) {
            return bestLastKnown?.let { it.latitude to it.longitude }
        }

        val freshFix = withTimeoutOrNull(timeoutMs) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Location?> { cont ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val cancelSignal = CancellationSignal()
                        cont.invokeOnCancellation { cancelSignal.cancel() }
                        runCatching {
                            lm.getCurrentLocation(
                                activeProvider,
                                cancelSignal,
                                ContextCompat.getMainExecutor(context),
                            ) { loc ->
                                if (cont.isActive) cont.resume(loc)
                            }
                        }.onFailure {
                            if (cont.isActive) cont.resume(null)
                        }
                    } else {
                        val listener = object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                runCatching { lm.removeUpdates(this) }
                                if (cont.isActive) cont.resume(location)
                            }
                            @Deprecated("Deprecated in Java")
                            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                            override fun onProviderEnabled(provider: String) = Unit
                            override fun onProviderDisabled(provider: String) {
                                runCatching { lm.removeUpdates(this) }
                                if (cont.isActive) cont.resume(null)
                            }
                        }
                        cont.invokeOnCancellation {
                            runCatching { lm.removeUpdates(listener) }
                        }
                        runCatching {
                            @Suppress("DEPRECATION")
                            lm.requestSingleUpdate(activeProvider, listener, Looper.getMainLooper())
                        }.onFailure {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
            }
        }

        val chosen = freshFix ?: bestLastKnown
        return chosen?.let { it.latitude to it.longitude }
    }

    /**
     * Resolves multi-granularity real-world place suggestions (neighborhood, street, POI, city)
     * around the user's current coordinates (Telegram LocationMarker / Instagram Places UX).
     */
    suspend fun resolveCurrentPlaces(lat: Double, lon: Double): List<StoryPlaceSuggestion> =
        withContext(Dispatchers.IO) {
            val cacheKey = "rev:${"%.3f".format(Locale.US, lat)}:${"%.3f".format(Locale.US, lon)}"
            placeCache[cacheKey]?.let { (ts, items) ->
                if (System.currentTimeMillis() - ts < 10 * 60_000L && items.isNotEmpty()) return@withContext items
            }

            val merged = coroutineScope {
                val androidGeoDeferred = async { reverseGeocodeAndroid(lat, lon) }
                val bigDataDeferred = async { reverseGeocodeBigDataCloud(lat, lon) }
                val nominatimDeferred = async { reverseGeocodeNominatim(lat, lon) }
                val backendDeferred = async {
                    runCatching {
                        storyApi.getStoryPlacesContext(lat = lat, lon = lon).places.map {
                            StoryPlaceSuggestion(
                                name = it.name,
                                subtitle = it.subtitle.orEmpty(),
                                latitude = it.latitude,
                                longitude = it.longitude,
                                category = it.category ?: "place",
                            )
                        }
                    }.getOrDefault(emptyList())
                }

                deduplicatePlaces(
                    nominatimDeferred.await() +
                        bigDataDeferred.await() +
                        androidGeoDeferred.await() +
                        backendDeferred.await(),
                )
            }

            if (merged.isNotEmpty()) {
                placeCache[cacheKey] = System.currentTimeMillis() to merged
                merged
            } else {
                listOf(
                    StoryPlaceSuggestion(
                        name = "${"%.4f".format(Locale.US, lat)}, ${"%.4f".format(Locale.US, lon)}",
                        subtitle = "مختصات موقعیت فعلی",
                        latitude = lat,
                        longitude = lon,
                        category = "gps",
                    ),
                )
            }
        }

    /**
     * Live dynamic place/city/POI search in Persian & English.
     */
    suspend fun searchPlaces(query: String): List<StoryPlaceSuggestion> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()
        val cacheKey = "q:${q.lowercase()}"
        placeCache[cacheKey]?.let { (ts, items) ->
            if (System.currentTimeMillis() - ts < 10 * 60_000L && items.isNotEmpty()) return@withContext items
        }

        val combined = coroutineScope {
            val nominatimDeferred = async { searchNominatim(q, limit = 8) }
            val openMeteoDeferred = async { searchOpenMeteoCities(q, count = 6) }
            val backendDeferred = async {
                runCatching {
                    storyApi.getStoryPlacesContext(query = q).places.map {
                        StoryPlaceSuggestion(
                            name = it.name,
                            subtitle = it.subtitle.orEmpty(),
                            latitude = it.latitude.takeIf { v -> v != 0.0 },
                            longitude = it.longitude.takeIf { v -> v != 0.0 },
                            category = it.category ?: "place",
                        )
                    }
                }.getOrDefault(emptyList())
            }
            deduplicatePlaces(
                nominatimDeferred.await() + openMeteoDeferred.await() + backendDeferred.await(),
            )
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

    private fun searchNominatim(query: String, limit: Int): List<StoryPlaceSuggestion> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?format=jsonv2&q=$encoded&accept-language=fa&limit=$limit&addressdetails=1"
        val body = httpGetString(url) ?: return@runCatching emptyList()
        val arr = json.parseToJsonElement(body) as? JsonArray ?: return@runCatching emptyList()
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
            StoryPlaceSuggestion(
                name = primary,
                subtitle = sub,
                latitude = lat,
                longitude = lon,
                category = obj["category"]?.jsonPrimitive?.contentOrNull ?: "poi",
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
        val seen = LinkedHashSet<String>()
        val out = ArrayList<StoryPlaceSuggestion>(items.size)
        for (item in items) {
            val clean = item.name.trim()
            if (clean.isBlank()) continue
            val normKey = clean.lowercase()
            if (seen.add(normKey)) {
                out += item.copy(name = clean)
            }
        }
        return out
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
