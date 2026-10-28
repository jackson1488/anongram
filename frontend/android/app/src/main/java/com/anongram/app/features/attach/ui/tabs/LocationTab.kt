package com.anongram.app.features.attach.ui.tabs

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.rounded.ShareLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.permissions.PermissionUtils
import com.anongram.app.features.attach.api.AttachmentDraft
import com.anongram.app.features.attach.ui.components.PermissionDeniedView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class NearbyPlace(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val category: String
)

private val PurpleAccent = Color(0xFF8B5CF6)
private val CardBg = Color(0xFF182238)

@SuppressLint("MissingPermission")
@Composable
fun LocationTab(
    onSendLocation: (AttachmentDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(PermissionUtils.hasLocationPermission(context))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = PermissionUtils.hasLocationPermission(context)
    }

    if (!hasPermission) {
        PermissionDeniedView(
            message = "Для определения и отправки геопозиции предоставьте доступ к местоположению устройства.",
            onRequestPermission = {
                launcher.launch(PermissionUtils.getLocationPermissions())
            },
            modifier = modifier
        )
        return
    }

    var currentLat by remember { mutableDoubleStateOf(55.751244) } // Default fallback
    var currentLon by remember { mutableDoubleStateOf(37.618423) }
    var currentAddress by remember { mutableStateOf("Определение адреса...") }
    var showLiveDialog by remember { mutableStateOf(false) }
    var selectedLiveDuration by remember { mutableStateOf(15) } // 15, 60, 480 min
    var places by remember { mutableStateOf<List<NearbyPlace>>(emptyList()) }

    // Real LocationManager tracking (no Google Services!)
    DisposableEffect(hasPermission) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                currentLat = loc.latitude
                currentLon = loc.longitude
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
                currentLat = best.latitude
                currentLon = best.longitude
            }

            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000L,
                10f,
                listener
            )
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                10f,
                listener
            )
        } catch (_: Exception) {}

        onDispose {
            try {
                locationManager?.removeUpdates(listener)
            } catch (_: Exception) {}
        }
    }

    // Geocode address when lat/lon changes (with debounce)
    LaunchedEffect(currentLat, currentLon) {
        delay(400) // debounce
        withContext(Dispatchers.IO) {
            currentAddress = reverseGeocode(context, currentLat, currentLon)
            places = generateNearbyPlaces(currentLat, currentLon, currentAddress)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Dark Map View with Center Pin
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color(0xFF0F172A))
        ) {
            // Dark stylised map canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Road grid
                val gridColor = Color(0xFF1E293B)
                val roadColor = Color(0xFF334155)

                for (y in 0 until h.toInt() step 60) {
                    drawLine(gridColor, Offset(0f, y.toFloat()), Offset(w, y.toFloat()), strokeWidth = 1.dp.toPx())
                }
                for (x in 0 until w.toInt() step 60) {
                    drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), h), strokeWidth = 1.dp.toPx())
                }

                // Main avenue paths
                drawLine(roadColor, Offset(0f, h * 0.45f), Offset(w, h * 0.45f), strokeWidth = 6.dp.toPx())
                drawLine(roadColor, Offset(w * 0.4f, 0f), Offset(w * 0.4f, h), strokeWidth = 6.dp.toPx())
                drawLine(roadColor, Offset(0f, h * 0.8f), Offset(w, h * 0.2f), strokeWidth = 4.dp.toPx())

                // Water or park accent
                val parkColor = Color(0xFF064E3B).copy(alpha = 0.3f)
                drawCircle(parkColor, radius = 90.dp.toPx(), center = Offset(w * 0.8f, h * 0.7f))
            }

            // Map Controls (Layers, My Location)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xDD182238))
                        .clickable {},
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = "Layers",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xDD182238))
                        .clickable {
                            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                            try {
                                val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                if (loc != null) {
                                    currentLat = loc.latitude
                                    currentLon = loc.longitude
                                }
                            } catch (_: Exception) {}
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My location",
                        tint = PurpleAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Center Pin with Purple Glow
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                // Pin pulse ring
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PurpleAccent.copy(alpha = 0.3f))
                )
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Location pin",
                    tint = PurpleAccent,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        // Action Cards and Places List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Card 1: "Отправить выбранную геопозицию"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .clickable {
                            onSendLocation(
                                AttachmentDraft.Location(
                                    lat = currentLat,
                                    lon = currentLon,
                                    address = currentAddress
                                )
                            )
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PurpleAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Отправить выбранную геопозицию",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentAddress,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card 2: "Транслировать геопозицию"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .clickable { showLiveDialog = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ShareLocation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Транслировать геопозицию",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Показывать, где я нахожусь в реальном времени",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Или выберите место",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            items(places, key = { it.id }) { place ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSendLocation(
                                AttachmentDraft.Location(
                                    lat = place.lat,
                                    lon = place.lon,
                                    address = "${place.name}, ${place.address}"
                                )
                            )
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = place.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = place.address,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Live Location duration dialog
    if (showLiveDialog) {
        AlertDialog(
            onDismissRequest = { showLiveDialog = false },
            title = {
                Text(
                    text = "Трансляция геопозиции",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Выберите продолжительность трансляции вашего местоположения:",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val durations = listOf(
                        15 to "На 15 минут",
                        60 to "На 1 час",
                        480 to "На 8 часов"
                    )

                    durations.forEach { (mins, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLiveDuration = mins }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = (selectedLiveDuration == mins),
                                onClick = { selectedLiveDuration = mins },
                                colors = RadioButtonDefaults.colors(selectedColor = PurpleAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLiveDialog = false
                        LocationSharingService.start(context, selectedLiveDuration)
                        onSendLocation(
                            AttachmentDraft.LiveLocation(durationMin = selectedLiveDuration)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    Text("Начать", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLiveDialog = false }) {
                    Text("Отмена", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF182238),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

private fun reverseGeocode(context: Context, lat: Double, lon: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        if (!addresses.isNullOrEmpty()) {
            val addr = addresses[0]
            val street = addr.thoroughfare ?: addr.featureName ?: ""
            val locality = addr.locality ?: addr.adminArea ?: ""
            if (street.isNotBlank() && locality.isNotBlank()) "$street, $locality"
            else addr.getAddressLine(0) ?: String.format(Locale.US, "%.5f, %.5f", lat, lon)
        } else {
            String.format(Locale.US, "%.5f, %.5f", lat, lon)
        }
    } catch (_: Exception) {
        String.format(Locale.US, "%.5f, %.5f", lat, lon)
    }
}

private fun generateNearbyPlaces(lat: Double, lon: Double, address: String): List<NearbyPlace> {
    return listOf(
        NearbyPlace(
            id = "poi_1",
            name = "Текущая позиция",
            address = address,
            lat = lat,
            lon = lon,
            category = "Точка"
        ),
        NearbyPlace(
            id = "poi_2",
            name = "Центральный парк",
            address = "Городской парк отдыха",
            lat = lat + 0.0012,
            lon = lon + 0.0015,
            category = "Парк"
        ),
        NearbyPlace(
            id = "poi_3",
            name = "Кофейня CyberPoint",
            address = "Улица Свободы, 12",
            lat = lat - 0.0008,
            lon = lon + 0.0020,
            category = "Кафе"
        ),
        NearbyPlace(
            id = "poi_4",
            name = "Бизнес-центр Horizon",
            address = "Проспект Мира, 45",
            lat = lat + 0.0025,
            lon = lon - 0.0010,
            category = "Офис"
        )
    )
}
