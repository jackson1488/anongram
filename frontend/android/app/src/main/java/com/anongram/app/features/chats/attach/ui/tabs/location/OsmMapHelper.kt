package com.anongram.app.features.chats.attach.ui.tabs.location

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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
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
    SATELLITE
}

object OsmMapHelper {

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

    val CARTO_VOYAGER_SOURCE: OnlineTileSourceBase = XYTileSource(
        "CartoVoyager",
        0,
        19,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
        )
    )

    val DARK_MATTER_SOURCE: OnlineTileSourceBase = XYTileSource(
        "CartoDarkMatter",
        0,
        19,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/dark_all/",
            "https://b.basemaps.cartocdn.com/dark_all/",
            "https://c.basemaps.cartocdn.com/dark_all/"
        )
    )

    val ESRI_SATELLITE_SOURCE: OnlineTileSourceBase = object : OnlineTileSourceBase(
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
        return when (mode) {
            MapLayerMode.AUTO -> if (isDarkTheme) DARK_MATTER_SOURCE else CARTO_VOYAGER_SOURCE
            MapLayerMode.STANDARD -> CARTO_VOYAGER_SOURCE
            MapLayerMode.DARK -> DARK_MATTER_SOURCE
            MapLayerMode.SATELLITE -> ESRI_SATELLITE_SOURCE
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

    suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): String {
        val fallback = String.format(Locale.US, "%.5f, %.5f", lat, lon)
        return withContext(Dispatchers.IO) {
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
        val pm = context.packageManager
        val hasNav = pm.queryIntentActivities(navIntent, 0).isNotEmpty()
        val hasGeo = pm.queryIntentActivities(geoFallbackIntent, 0).isNotEmpty()

        return try {
            val baseIntent = if (hasGeo) geoFallbackIntent else navIntent
            if (!hasNav && !hasGeo) return false
            val chooser = Intent.createChooser(baseIntent, chooserTitle).apply {
                if (hasNav && hasGeo) {
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(navIntent))
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Throwable) {
            false
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
            try {
                val urlStr = if (isDark) {
                    "https://a.basemaps.cartocdn.com/dark_all/$zoom/$tileX/$tileY.png"
                } else {
                    "https://a.basemaps.cartocdn.com/rastertiles/voyager/$zoom/$tileX/$tileY.png"
                }
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4500
                    readTimeout = 4500
                    setRequestProperty("User-Agent", context.packageName)
                }
                if (conn.responseCode == 200) {
                    conn.inputStream.use { BitmapFactory.decodeStream(it) }
                } else null
            } catch (_: Throwable) {
                null
            }
        } ?: return null

        tileMemoryCache.put(key, bmp)
        return bmp to (fracX to fracY)
    }
}
