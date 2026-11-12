package com.anongram.app.features.chats.attach.tabs.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.permissions.PermissionUtils
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.attach.api.AttachmentDraft
import com.anongram.app.features.chats.attach.i18n.LocalAttachStrings
import com.anongram.app.features.chats.attach.components.PermissionDeniedView
import com.anongram.app.features.chats.attach.tabs.location.MapLayerMode
import com.anongram.app.features.chats.attach.tabs.location.OsmMapHelper
import com.anongram.app.features.chats.attach.tabs.location.OsmMapView
import kotlinx.coroutines.delay

/**
 * Вкладка выбора геопозиции в стиле Telegram на базе динамического OSMDroid SDK:
 * - Живая карта на весь контейнер с панорамированием, зумом и слоями (Авто / Стандарт / Тёмная / Спутник).
 * - Поиск адреса и кнопка "Моё местоположение".
 * - Карта перехватывает жесты, поэтому саму шторку можно двигать только за верхнюю ручку.
 * - Снизу — карточка "Отправить выбранную геопозицию" над панелью вкладок.
 */
@SuppressLint("MissingPermission")
@Composable
fun LocationTab(
    onSendLocation: (AttachmentDraft) -> Unit,
    isFullscreen: Boolean = false,
    sheetOffsetDp: androidx.compose.ui.unit.Dp = 0.dp,
    searchQuery: String = "",
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalAttachStrings.current
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(PermissionUtils.hasLocationPermission(context))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = PermissionUtils.hasLocationPermission(context)
    }

    var pinLat by remember { mutableDoubleStateOf(OsmMapHelper.rememberedPinLat) }
    var pinLon by remember { mutableDoubleStateOf(OsmMapHelper.rememberedPinLon) }
    var userLat by remember { mutableStateOf(OsmMapHelper.rememberedUserLat) }
    var userLon by remember { mutableStateOf(OsmMapHelper.rememberedUserLon) }
    var recenterNonce by remember { mutableIntStateOf(0) }

    var currentAddress by remember {
        mutableStateOf(OsmMapHelper.rememberedAddress.ifBlank { strings.resolvingAddress })
    }
    var layerMode by remember { mutableStateOf(OsmMapHelper.rememberedLayerMode) }
    var isLayerMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(layerMode) {
        OsmMapHelper.rememberedLayerMode = layerMode
    }

    // Реальное отслеживание GPS / Network через Android LocationManager
    DisposableEffect(hasPermission) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                userLat = loc.latitude
                userLon = loc.longitude
                OsmMapHelper.rememberedUserLat = loc.latitude
                OsmMapHelper.rememberedUserLon = loc.longitude
                if (!OsmMapHelper.hasUserMovedPin && !OsmMapHelper.hasGpsFix) {
                    pinLat = loc.latitude
                    pinLon = loc.longitude
                    OsmMapHelper.rememberedPinLat = loc.latitude
                    OsmMapHelper.rememberedPinLon = loc.longitude
                    recenterNonce++
                }
                OsmMapHelper.hasGpsFix = true
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = lastGps ?: lastNet
            if (best != null) {
                userLat = best.latitude
                userLon = best.longitude
                OsmMapHelper.rememberedUserLat = best.latitude
                OsmMapHelper.rememberedUserLon = best.longitude
                if (!OsmMapHelper.hasUserMovedPin && !OsmMapHelper.hasGpsFix) {
                    pinLat = best.latitude
                    pinLon = best.longitude
                    OsmMapHelper.rememberedPinLat = best.latitude
                    OsmMapHelper.rememberedPinLon = best.longitude
                    recenterNonce++
                }
                OsmMapHelper.hasGpsFix = true
            }

            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                4000L,
                5f,
                listener
            )
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                4000L,
                5f,
                listener
            )
        } catch (_: Exception) {}

        onDispose {
            try {
                locationManager?.removeUpdates(listener)
            } catch (_: Exception) {}
        }
    }

    // Поиск по внешнему запросу из TopBar (с debounce)
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length >= 2) {
            delay(350)
            val coords = OsmMapHelper.searchLocation(context, q)
            if (coords != null) {
                pinLat = coords.first
                pinLon = coords.second
                OsmMapHelper.rememberedPinLat = coords.first
                OsmMapHelper.rememberedPinLon = coords.second
                OsmMapHelper.hasUserMovedPin = true
                recenterNonce++
            }
        }
    }

    // Обратное геокодирование при смещении карты (с debounce)
    LaunchedEffect(pinLat, pinLon) {
        delay(350)
        currentAddress = OsmMapHelper.reverseGeocode(context, pinLat, pinLon)
        OsmMapHelper.rememberedAddress = currentAddress
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tabBarClearance = navBottom + 76.dp

    Box(modifier = modifier.fillMaxSize()) {
        // Живая интерактивная карта OSMDroid на всю область вкладки
        OsmMapView(
            centerLat = pinLat,
            centerLon = pinLon,
            recenterNonce = recenterNonce,
            userLat = userLat,
            userLon = userLon,
            layerMode = layerMode,
            onCenterChanged = { newLat, newLon ->
                pinLat = newLat
                pinLon = newLon
                OsmMapHelper.rememberedPinLat = newLat
                OsmMapHelper.rememberedPinLon = newLon
                OsmMapHelper.hasUserMovedPin = true
            },
            modifier = Modifier.fillMaxSize()
        )

        // Фиксированный пин в центре видимой области карты
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-18).dp - sheetOffsetDp / 2),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset(y = 18.dp)
                    .size(12.dp, 5.dp)
                    .clip(CircleShape)
                    .background(scheme.overlayScrim.copy(alpha = 0.28f))
            )
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = strings.locationLabel,
                tint = scheme.accent,
                modifier = Modifier.size(44.dp)
            )
        }

        // Верхний правый угол: выбор слоёв карты (всегда ниже плавающей шапки/поиска)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = if (isFullscreen) statusBarTop + 72.dp else 12.dp,
                    end = 12.dp
                )
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(scheme.islandBg.copy(alpha = 0.94f))
                    .border(1.dp, scheme.islandBorder, CircleShape)
                    .clickable { isLayerMenuOpen = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Layers,
                    contentDescription = strings.mapLayer,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = isLayerMenuOpen,
                onDismissRequest = { isLayerMenuOpen = false },
                containerColor = scheme.card
            ) {
                val options = listOf(
                    MapLayerMode.AUTO to strings.mapLayerAuto,
                    MapLayerMode.STANDARD to strings.mapLayerStandard,
                    MapLayerMode.DARK to strings.mapLayerDark,
                    MapLayerMode.SATELLITE to strings.mapLayerSatellite,
                    MapLayerMode.HYBRID to strings.mapLayerHybrid
                )
                options.forEach { (mode, title) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = title,
                                color = if (layerMode == mode) scheme.accent else scheme.textPrimary,
                                fontWeight = if (layerMode == mode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            layerMode = mode
                            OsmMapHelper.rememberedLayerMode = mode
                            isLayerMenuOpen = false
                        }
                    )
                }
            }
        }

        // Нижний блок: кнопка "Моё местоположение" справа и карточка "Отправить выбранную геопозицию"
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = tabBarClearance + sheetOffsetDp)
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .size(44.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(scheme.islandBg.copy(alpha = 0.95f))
                    .border(1.dp, scheme.islandBorder, CircleShape)
                    .clickable {
                        if (!hasPermission) {
                            launcher.launch(PermissionUtils.getLocationPermissions())
                            return@clickable
                        }
                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                        try {
                            val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (loc != null) {
                                userLat = loc.latitude
                                userLon = loc.longitude
                                pinLat = loc.latitude
                                pinLon = loc.longitude
                                OsmMapHelper.rememberedUserLat = loc.latitude
                                OsmMapHelper.rememberedUserLon = loc.longitude
                                OsmMapHelper.rememberedPinLat = loc.latitude
                                OsmMapHelper.rememberedPinLon = loc.longitude
                                OsmMapHelper.hasUserMovedPin = true
                                recenterNonce++
                            } else if (userLat != null && userLon != null) {
                                pinLat = userLat!!
                                pinLon = userLon!!
                                OsmMapHelper.rememberedPinLat = userLat!!
                                OsmMapHelper.rememberedPinLon = userLon!!
                                OsmMapHelper.hasUserMovedPin = true
                                recenterNonce++
                            }
                        } catch (_: Exception) {}
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = strings.myLocation,
                    tint = scheme.accent,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Карточка "Отправить выбранную геопозицию" как в Telegram
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(scheme.islandBg.copy(alpha = 0.96f))
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(18.dp))
                    .clickable {
                        onSendLocation(
                            AttachmentDraft.Location(
                                lat = pinLat,
                                lon = pinLon,
                                address = currentAddress
                            )
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(scheme.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = scheme.onAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.sendSelectedLocation,
                        color = scheme.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentAddress,
                        color = scheme.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
