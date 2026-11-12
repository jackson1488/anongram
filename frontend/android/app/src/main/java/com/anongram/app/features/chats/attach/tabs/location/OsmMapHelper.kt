package com.anongram.app.features.chats.attach.tabs.location

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

enum class MapLayerMode {
    AUTO,
    STANDARD,
    DARK,
    SATELLITE,
    HYBRID
}

object OsmMapHelper {

    private val MAPBOX_ACCESS_TOKEN: String
        get() = com.anongram.app.BuildConfig.MAPBOX_ACCESS_TOKEN

    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        val appCtx = context.applicationContext
        val cfg = Configuration.getInstance()
        cfg.userAgentValue = appCtx.packageName
        val baseDir = File(appCtx.cacheDir, "osmdroid")
        val tileDir = File(baseDir, "tiles")
        baseDir.mkdirs()
        tileDir.mkdirs()
        cfg.osmdroidBasePath = baseDir
        cfg.osmdroidTileCache = tileDir
        initialized = true
        prewarmTilesCache(context)
    }

    fun prewarmTilesCache(context: Context, lat: Double = 42.8746, lon: Double = 74.5698) {
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            for (z in 13..15) {
                try {
                    loadStaticPreviewTile(appCtx, lat, lon, isDark = false, zoom = z)
                    loadStaticPreviewTile(appCtx, lat, lon, isDark = true, zoom = z)
                } catch (_: Throwable) {}
            }
        }
    }

    private fun mapboxTileSource(
        name: String,
        styleId: String,
        ext: String = ".png",
        maxZoom: Int = 20
    ): OnlineTileSourceBase = object : OnlineTileSourceBase(
        name,
        0,
        maxZoom,
        256,
        ext,
        arrayOf("https://api.mapbox.com/styles/v1/mapbox/$styleId/tiles/256/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "https://api.mapbox.com/styles/v1/mapbox/$styleId/tiles/256/$z/$x/$y@2x?access_token=$MAPBOX_ACCESS_TOKEN"
        }
    }

    val MAPBOX_STREETS_SOURCE: OnlineTileSourceBase = mapboxTileSource(
        name = "MapboxStreets",
        styleId = "streets-v12"
    )

    val MAPBOX_DARK_SOURCE: OnlineTileSourceBase = mapboxTileSource(
        name = "MapboxDark",
        styleId = "dark-v11"
    )

    val MAPBOX_SATELLITE_SOURCE: OnlineTileSourceBase = mapboxTileSource(
        name = "MapboxSatellite",
        styleId = "satellite-v9",
        ext = ".jpg"
    )

    val MAPBOX_HYBRID_SOURCE: OnlineTileSourceBase = mapboxTileSource(
        name = "MapboxHybrid",
        styleId = "satellite-streets-v12",
        ext = ".jpg"
    )

    private val OSM_FALLBACK_SOURCE: OnlineTileSourceBase = TileSourceFactory.MAPNIK

    private val ESRI_SATELLITE_FALLBACK_SOURCE: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "EsriWorldImagery",
        0,
        18,
        256,
        ".jpg",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            return "$baseUrl$z/$y/$x"
        }
    }

    fun resolveTileSource(mode: MapLayerMode, isDarkTheme: Boolean): OnlineTileSourceBase {
        if (MAPBOX_ACCESS_TOKEN.isBlank()) {
            return when (mode) {
                MapLayerMode.SATELLITE, MapLayerMode.HYBRID -> ESRI_SATELLITE_FALLBACK_SOURCE
                else -> OSM_FALLBACK_SOURCE
            }
        }
        return when (mode) {
            MapLayerMode.AUTO -> if (isDarkTheme) MAPBOX_DARK_SOURCE else MAPBOX_STREETS_SOURCE
            MapLayerMode.STANDARD -> MAPBOX_STREETS_SOURCE
            MapLayerMode.DARK -> MAPBOX_DARK_SOURCE
            MapLayerMode.SATELLITE -> MAPBOX_SATELLITE_SOURCE
            MapLayerMode.HYBRID -> MAPBOX_HYBRID_SOURCE
        }
    }

    fun createUserDotDrawable(context: Context, accentArgb: Int): BitmapDrawable {
        val density = context.resources.displayMetrics.density
        val sizePx = (36 * density).toInt().coerceAtLeast(36)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            alpha = 52
        }
        c.drawCircle(cx, cy, sizePx * 0.46f, haloPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
        }
        c.drawCircle(cx, cy, sizePx * 0.23f, borderPaint)

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2563EB.toInt()
        }
        c.drawCircle(cx, cy, sizePx * 0.17f, corePaint)

        return BitmapDrawable(context.resources, bmp)
    }

    fun createRedPinDrawable(context: Context): BitmapDrawable {
        val density = context.resources.displayMetrics.density
        val w = (34 * density).toInt().coerceAtLeast(34)
        val h = (44 * density).toInt().coerceAtLeast(44)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = w / 2f
        val r = w * 0.36f
        val cy = r + 2 * density

        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFEA4335.toInt()
        }
        val path = android.graphics.Path().apply {
            moveTo(cx, h - 2 * density)
            lineTo(cx - r * 0.85f, cy + r * 0.45f)
            lineTo(cx + r * 0.85f, cy + r * 0.45f)
            close()
        }
        c.drawPath(path, pinPaint)
        c.drawCircle(cx, cy, r, pinPaint)

        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8C1D18.toInt()
        }
        c.drawCircle(cx, cy, r * 0.40f, innerPaint)

        return BitmapDrawable(context.resources, bmp)
    }

    private fun fetchMapboxGeocodeJson(urlStr: String): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4500
                readTimeout = 4500
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body)
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): String {
        val fallback = String.format(Locale.US, "%.5f, %.5f", lat, lon)
        return withContext(Dispatchers.IO) {
            val lang = Uri.encode(Locale.getDefault().language.ifBlank { "en" })
            val mapboxUrl = "https://api.mapbox.com/geocoding/v5/mapbox.places/$lon,$lat.json?access_token=$MAPBOX_ACCESS_TOKEN&limit=1&language=$lang"
            val feature = fetchMapboxGeocodeJson(mapboxUrl)
                ?.optJSONArray("features")
                ?.optJSONObject(0)
            val mapboxName = feature?.optString("place_name")?.takeIf { it.isNotBlank() }
                ?: feature?.optString("text")?.takeIf { it.isNotBlank() }
            if (mapboxName != null) return@withContext mapboxName

            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { results ->
                            if (cont.isActive) cont.resume(results)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, 1)
                }
                val addr = list?.firstOrNull() ?: return@withContext fallback
                val parts = listOfNotNull(
                    addr.thoroughfare?.takeIf { it.isNotBlank() }?.let { street ->
                        val num = addr.subThoroughfare?.takeIf { it.isNotBlank() }
                        if (num != null) "$street, $num" else street
                    },
                    addr.locality?.takeIf { it.isNotBlank() },
                    addr.subAdminArea?.takeIf { it.isNotBlank() && it != addr.locality },
                    addr.adminArea?.takeIf { it.isNotBlank() },
                    addr.countryName?.takeIf { it.isNotBlank() }
                ).distinct()
                if (parts.isNotEmpty()) parts.joinToString(", ")
                else addr.getAddressLine(0)?.takeIf { it.isNotBlank() } ?: fallback
            } catch (_: Throwable) {
                fallback
            }
        }
    }

    suspend fun searchLocation(context: Context, query: String): Pair<Double, Double>? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            val lang = Uri.encode(Locale.getDefault().language.ifBlank { "en" })
            val encodedQuery = Uri.encode(trimmed)
            if (MAPBOX_ACCESS_TOKEN.isNotBlank()) {
                val mapboxUrl = "https://api.mapbox.com/geocoding/v5/mapbox.places/$encodedQuery.json?access_token=$MAPBOX_ACCESS_TOKEN&limit=1&language=$lang"
                val center = fetchMapboxGeocodeJson(mapboxUrl)
                    ?.optJSONArray("features")
                    ?.optJSONObject(0)
                    ?.optJSONArray("center")
                if (center != null && center.length() >= 2) {
                    val lon = center.optDouble(0, Double.NaN)
                    val lat = center.optDouble(1, Double.NaN)
                    if (!lat.isNaN() && !lon.isNaN()) {
                        return@withContext lat to lon
                    }
                }
            } else {
                try {
                    val nomUrl = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=$encodedQuery"
                    val conn = (URL(nomUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4500
                        readTimeout = 4500
                        setRequestProperty("User-Agent", context.packageName)
                        setRequestProperty("Accept-Language", lang)
                    }
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val arr = org.json.JSONArray(body)
                        val first = arr.optJSONObject(0)
                        val lat = first?.optString("lat")?.toDoubleOrNull()
                        val lon = first?.optString("lon")?.toDoubleOrNull()
                        if (lat != null && lon != null) {
                            return@withContext lat to lon
                        }
                    }
                } catch (_: Throwable) {}
            }

            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocationName(trimmed, 1) { results ->
                            if (cont.isActive) cont.resume(results)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(trimmed, 1)
                }
                val first = list?.firstOrNull() ?: return@withContext null
                first.latitude to first.longitude
            } catch (_: Throwable) {
                null
            }
        }
    }

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun openInExternalMapChooser(
        context: Context,
        lat: Double,
        lon: Double,
        label: String,
        chooserTitle: String
    ): Boolean {
        val encoded = Uri.encode(label.ifBlank { "$lat,$lon" })
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($encoded)")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun openNavigationChooser(
        context: Context,
        lat: Double,
        lon: Double,
        label: String,
        chooserTitle: String
    ): Boolean {
        val encoded = Uri.encode(label.ifBlank { "$lat,$lon" })
        val navIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$lat,$lon")
        )
        val geoFallbackIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:0,0?q=$lat,$lon($encoded)")
        )
        val webFallbackIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://maps.google.com/maps?daddr=$lat,$lon")
        )

        return try {
            val chooser = Intent.createChooser(geoFallbackIntent, chooserTitle).apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(navIntent, webFallbackIntent))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Throwable) {
            try {
                context.startActivity(webFallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    private val tileMemoryCache = object : android.util.LruCache<String, Bitmap>(48) {}

    /**
     * Загружает 256x256 тайл карты для компактного превью в пузыре сообщения.
     */
    suspend fun loadStaticPreviewTile(
        context: Context,
        lat: Double,
        lon: Double,
        isDark: Boolean,
        zoom: Int = 15
    ): Pair<Bitmap, Pair<Float, Float>>? {
        if (lat == 0.0 && lon == 0.0) return null
        val n = 1 shl zoom
        val xFloat = (lon + 180.0) / 360.0 * n
        val latRad = lat * PI / 180.0
        val yFloat = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
        val tileX = floor(xFloat).toInt().coerceIn(0, n - 1)
        val tileY = floor(yFloat).toInt().coerceIn(0, n - 1)
        val fracX = (xFloat - tileX).toFloat().coerceIn(0.15f, 0.85f)
        val fracY = (yFloat - tileY).toFloat().coerceIn(0.15f, 0.85f)

        val key = "${if (isDark) "d" else "l"}_${zoom}_${tileX}_$tileY"
        tileMemoryCache.get(key)?.let { return it to (fracX to fracY) }

        val bmp = withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val urlStr = if (MAPBOX_ACCESS_TOKEN.isNotBlank()) {
                    val style = if (isDark) "dark-v11" else "streets-v12"
                    "https://api.mapbox.com/styles/v1/mapbox/$style/tiles/256/$zoom/$tileX/$tileY@2x?access_token=$MAPBOX_ACCESS_TOKEN"
                } else {
                    "https://tile.openstreetmap.org/$zoom/$tileX/$tileY.png"
                }
                conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4500
                    readTimeout = 4500
                    setRequestProperty("User-Agent", context.packageName)
                }
                if (conn.responseCode == 200) {
                    conn.inputStream.use { BitmapFactory.decodeStream(it) }
                } else null
            } catch (_: Throwable) {
                null
            } finally {
                conn?.disconnect()
            }
        } ?: return null

        tileMemoryCache.put(key, bmp)
        return bmp to (fracX to fracY)
    }
}
