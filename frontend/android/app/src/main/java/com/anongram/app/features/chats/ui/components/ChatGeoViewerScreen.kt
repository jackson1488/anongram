package com.anongram.app.features.chats.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.permissions.PermissionUtils
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.auth.data.crypto.KeystoreVault
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.ui.attach.i18n.AttachI18n
import com.anongram.app.features.chats.ui.attach.ui.tabs.location.MapLayerMode
import com.anongram.app.features.chats.ui.attach.ui.tabs.location.OsmMapHelper
import com.anongram.app.features.chats.ui.attach.ui.tabs.location.OsmMapView
import com.anongram.app.features.chats.ui.input.banner.ChatActionBannerBus
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Полноэкранный просмотр отправленной или полученной геопозиции в стиле Telegram:
 * - Живая карта OSMDroid с красным пином точки и синей точкой пользователя.
 * - Сверху: Назад (←), пилюля "Открыть карту" (системный выбор приложения), переключатель слоёв.
 * - Справа: кнопка "Моё местоположение".
 * - Снизу: карточка с системным/профильным аватаром отправителя, именем, расстоянием и кнопкой "Маршрут".
 */
@SuppressLint("MissingPermission")
@Composable
internal fun ChatGeoViewerScreen(
    message: ChatMessage,
    geo: MessageAttachment.Geo,
    peerName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val scheme = LocalAnonGramScheme.current
    val appLanguage by LanguageSettingsStore.language.collectAsState()
    val strings = remember(appLanguage) { AttachI18n.forLanguage(appLanguage) }
    val context = LocalContext.current

    val targetLat = if (geo.lat != 0.0 || geo.lon != 0.0) geo.lat else 42.8746
    val targetLon = if (geo.lat != 0.0 || geo.lon != 0.0) geo.lon else 74.5698

    var cameraLat by remember(targetLat) { mutableDoubleStateOfFix(targetLat) }
    var cameraLon by remember(targetLon) { mutableDoubleStateOfFix(targetLon) }
    var recenterNonce by remember { mutableIntStateOf(0) }
    var layerMode by remember { mutableStateOf(MapLayerMode.AUTO) }
    var isLayerMenuOpen by remember { mutableStateOf(false) }

    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLon by remember { mutableStateOf<Double?>(null) }

    // Профиль текущего пользователя для исходящих сообщений (системный/локальный аватар + имя)
    val identity = remember(context) { runCatching { KeystoreVault(context).loadIdentity() }.getOrNull() }
    val isOutgoing = message.direction == MessageDirection.OUTGOING
    val displayName = if (isOutgoing) {
        identity?.nickname?.takeIf { it.isNotBlank() } ?: strings.you
    } else {
        peerName.ifBlank { "Anon" }
    }

    val avatarBitmap: ImageBitmap? = remember(isOutgoing, identity?.avatarUri) {
        val uriStr = if (isOutgoing) identity?.avatarUri else null
        if (uriStr.isNullOrBlank()) null
        else runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriStr))?.use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }

    DisposableEffect(Unit) {
        val hasPerm = PermissionUtils.hasLocationPermission(context)
        val lm = if (hasPerm) context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager else null
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                userLat = loc.latitude
                userLon = loc.longitude
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        try {
            val best = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (best != null) {
                userLat = best.latitude
                userLon = best.longitude
            }
            lm?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 10f, listener)
            lm?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, listener)
        } catch (_: Exception) {}

        onDispose {
            try {
                lm?.removeUpdates(listener)
            } catch (_: Exception) {}
        }
    }

    val distanceLabel = remember(userLat, userLon, targetLat, targetLon, strings) {
        val uLat = userLat
        val uLon = userLon
        if (uLat != null && uLon != null) {
            val meters = OsmMapHelper.distanceMeters(uLat, uLon, targetLat, targetLon)
            if (meters < 1000f) {
                strings.distanceMetersAway(meters.roundToInt())
            } else {
                strings.distanceKmAway(String.format(Locale.US, "%.1f", meters / 1000f))
            }
        } else {
            geo.address
        }
    }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        // Динамическая карта OSMDroid
        OsmMapView(
            centerLat = cameraLat,
            centerLon = cameraLon,
            recenterNonce = recenterNonce,
            userLat = userLat,
            userLon = userLon,
            layerMode = layerMode,
            targetPinLat = targetLat,
            targetPinLon = targetLon,
            initialZoom = 16.5,
            modifier = Modifier.fillMaxSize()
        )

        // Верхняя плавающая панель: Назад, "Открыть карту", Слои
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = statusTop + 10.dp, start = 12.dp, end = 12.dp)
        ) {
            // Кнопка "Назад"
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(scheme.islandBg.copy(alpha = 0.94f))
                    .border(1.dp, scheme.islandBorder, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = strings.back,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }

            // Центральная пилюля "Открыть карту" -> системный диалог выбора приложения
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .shadow(8.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(scheme.islandBg.copy(alpha = 0.94f))
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                    .clickable {
                        val ok = OsmMapHelper.openInExternalMapChooser(
                            context = context,
                            lat = targetLat,
                            lon = targetLon,
                            label = geo.address,
                            chooserTitle = strings.chooseMapApp
                        )
                        if (!ok) {
                            ChatActionBannerBus.show(strings.noMapAppFound, isError = true)
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.openMap,
                    color = scheme.accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Кнопка слоёв справа
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, CircleShape)
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
                        MapLayerMode.SATELLITE to strings.mapLayerSatellite
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
                                isLayerMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        // Нижний блок: кнопка "Моё местоположение" + карточка отправителя + кнопка "Маршрут"
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Кнопка GPS справа над нижней карточкой
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 14.dp, bottom = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(scheme.islandBg.copy(alpha = 0.94f))
                        .border(1.dp, scheme.islandBorder, CircleShape)
                        .clickable {
                            val uLat = userLat
                            val uLon = userLon
                            if (uLat != null && uLon != null) {
                                cameraLat = uLat
                                cameraLon = uLon
                                recenterNonce++
                            } else {
                                cameraLat = targetLat
                                cameraLon = targetLon
                                recenterNonce++
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = strings.myLocation,
                        tint = scheme.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Нижняя карточка отправителя и маршрута
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(scheme.card)
                    .border(1.dp, scheme.cardBorder, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .padding(bottom = navBottom)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Аватарка отправителя (системная/профильная для себя или инициалы собеседника)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(scheme.accentSoft)
                            .border(1.dp, scheme.cardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap,
                                contentDescription = displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = displayName.trim().take(2).uppercase(),
                                color = scheme.accent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            color = scheme.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = distanceLabel,
                            color = scheme.textSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Широкая кнопка "Маршрут" -> системный выбор навигатора/приложения карт
                Button(
                    onClick = {
                        val ok = OsmMapHelper.openNavigationChooser(
                            context = context,
                            lat = targetLat,
                            lon = targetLon,
                            label = geo.address,
                            chooserTitle = strings.chooseRouteApp
                        )
                        if (!ok) {
                            ChatActionBannerBus.show(strings.noMapAppFound, isError = true)
                        }
                    },
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.accent,
                        contentColor = scheme.onAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.route,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun mutableDoubleStateOfFix(initial: Double) =
    remember(initial) { androidx.compose.runtime.mutableDoubleStateOf(initial) }
