package com.anongram.app.features.chats.attach.ui.tabs.location

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anongram.app.core.theme.LocalAnonGramScheme
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Динамическая интерактивная карта OSMDroid.
 * Перехватывает touch-события на себя, чтобы шторка вложений двигалась ТОЛЬКО за верхнюю ручку.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun OsmMapView(
    centerLat: Double,
    centerLon: Double,
    recenterNonce: Int,
    userLat: Double?,
    userLon: Double?,
    layerMode: MapLayerMode,
    targetPinLat: Double? = null,
    targetPinLon: Double? = null,
    initialZoom: Double = 16.0,
    onCenterChanged: (Double, Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCenterChanged by rememberUpdatedState(onCenterChanged)

    OsmMapHelper.ensureInitialized(context)

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 3.0
            maxZoomLevel = 20.0
            controller.setZoom(initialZoom)
            controller.setCenter(GeoPoint(centerLat, centerLon))

            // Запрещаем родительской шторке/списку перехватывать свайпы по карте
            setOnTouchListener { v, _ ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }

    // Привязка жизненного цикла
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Слушатель перемещения/зума карты
    DisposableEffect(mapView) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                val c = mapView.mapCenter
                currentOnCenterChanged(c.latitude, c.longitude)
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                val c = mapView.mapCenter
                currentOnCenterChanged(c.latitude, c.longitude)
                return false
            }
        }
        mapView.addMapListener(listener)
        onDispose {
            mapView.removeMapListener(listener)
        }
    }

    // Переключение слоя карты и темы
    LaunchedEffect(layerMode, scheme.isDark) {
        mapView.setTileSource(OsmMapHelper.resolveTileSource(layerMode, scheme.isDark))
        mapView.invalidate()
    }

    // Перелёт камеры при явном запросе (кнопка "Моё местоположение" или поиск адреса)
    LaunchedEffect(recenterNonce) {
        if (recenterNonce > 0) {
            mapView.controller.animateTo(GeoPoint(centerLat, centerLon))
        }
    }

    // Маркеры: синяя точка пользователя + опциональный красный пин цели (для просмотра из чата)
    val accentArgb = scheme.accent.toArgb()
    LaunchedEffect(userLat, userLon, targetPinLat, targetPinLon, accentArgb) {
        mapView.overlays.clear()

        if (userLat != null && userLon != null) {
            val userMarker = Marker(mapView).apply {
                position = GeoPoint(userLat, userLon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = OsmMapHelper.createUserDotDrawable(context, accentArgb)
                infoWindow = null
            }
            mapView.overlays.add(userMarker)
        }

        if (targetPinLat != null && targetPinLon != null) {
            val pinMarker = Marker(mapView).apply {
                position = GeoPoint(targetPinLat, targetPinLon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = OsmMapHelper.createRedPinDrawable(context)
                infoWindow = null
            }
            mapView.overlays.add(pinMarker)
        }

        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}
