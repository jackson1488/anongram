package com.anongram.app.features.chats.mediaeditor.ui.paint

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.mediaeditor.model.EditorPalette
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Анимационная капсула 2×7 из 14 цветов: слева кнопка `<`, по центру 2 ряда по 7 кружков, справа `+`.
 */
@Composable
fun ColorPaletteBar(
    selectedColorArgb: Long,
    onSelectColor: (Long) -> Unit,
    onBackToBrushes: (() -> Unit)? = null,
    onOpenFullPicker: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    var showInternalFullPicker by remember { mutableStateOf(false) }

    val row1 = EditorPalette.quickColors.take(7)
    val row2 = EditorPalette.quickColors.drop(7).take(7)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (onBackToBrushes != null) {
            IconButton(
                onClick = onBackToBrushes,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.islandBg)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(row1, row2).forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowColors.forEach { argb ->
                        val selected = (argb and 0x00FFFFFFL) == (selectedColorArgb and 0x00FFFFFFL)
                        val dotColor = EditorPalette.colorFromArgb(argb)
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable { onSelectColor(argb) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, if (argb == 0xFF000000L) Color.White else dotColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                        .then(
                                            if (argb == 0xFF000000L) Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                            else Modifier
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = {
                if (onOpenFullPicker != null) onOpenFullPicker()
                else showInternalFullPicker = true
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showInternalFullPicker) {
        FullColorPickerBottomSheet(
            initialArgb = selectedColorArgb,
            onLiveColorChange = { onSelectColor(it) },
            onConfirm = {
                onSelectColor(it)
                showInternalFullPicker = false
            },
            onDismiss = { showInternalFullPicker = false }
        )
    }
}

/**
 * Полная шторка выбора цвета для рисования и текста с 3 вкладками и свайпом (ПАЛИТРА | СПЕКТР | ШКАЛА),
 * живым применением цвета в реальном времени, полосой прозрачности, RGB-слайдерами и HEX-кодом.
 */
@Composable
fun FullColorPickerBottomSheet(
    initialArgb: Long,
    onLiveColorChange: (Long) -> Unit = {},
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val scope = rememberCoroutineScope()
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    val hsvInit = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialArgb.toInt(), it) }
    }
    var hue by remember { mutableFloatStateOf(hsvInit[0]) }
    var sat by remember { mutableFloatStateOf(hsvInit[1]) }
    var value by remember { mutableFloatStateOf(hsvInit[2]) }
    var alphaFrac by remember {
        mutableFloatStateOf((((initialArgb ushr 24) and 0xFF).toFloat() / 255f).coerceIn(0.1f, 1f))
    }

    val currentRgbInt = android.graphics.Color.HSVToColor(
        (alphaFrac * 255f).roundToInt().coerceIn(1, 255),
        floatArrayOf(hue, sat, value)
    )
    val currentLong = currentRgbInt.toLong() and 0xFFFFFFFFL
    val redInt = (currentRgbInt ushr 16) and 0xFF
    val greenInt = (currentRgbInt ushr 8) and 0xFF
    val blueInt = currentRgbInt and 0xFF
    val hexString = String.format("%02X%02X%02X", redInt, greenInt, blueInt)

    // Real-time live color propagation to the active drawing brush / text layer!
    LaunchedEffect(currentLong) {
        onLiveColorChange(currentLong)
    }

    fun updateFromRgb(r: Int, g: Int, b: Int) {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255), hsv)
        hue = hsv[0]
        sat = hsv[1]
        value = hsv[2]
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val tabTitles = if (isRu) listOf("ПАЛИТРА", "СПЕКТР", "ШКАЛА") else listOf("PALETTE", "SPECTRUM", "SLIDERS")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                detectTapGestures { onConfirm(currentLong) }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(scheme.surface)
                .border(0.8.dp, scheme.islandBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .pointerInput(Unit) { detectTapGestures { /* consume */ } }
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top handle bar (also swipeable left/right to switch tabs)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .pointerInput(Unit) {
                        var accumDx = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { accumDx = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                accumDx += dragAmount
                            },
                            onDragEnd = {
                                if (accumDx < -50f && pagerState.currentPage < 2) {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                } else if (accumDx > 50f && pagerState.currentPage > 0) {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(scheme.textMuted.copy(alpha = 0.4f))
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3 Swipeable Tabs Content (ПАЛИТРА | СПЕКТР | ШКАЛА)
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
            ) { page ->
                when (page) {
                    0 -> PaletteSwatchesGrid(
                        selectedRgbInt = currentRgbInt,
                        onSelectColorInt = { c ->
                            updateFromRgb(
                                (c ushr 16) and 0xFF,
                                (c ushr 8) and 0xFF,
                                c and 0xFF
                            )
                        }
                    )
                    1 -> Spectrum2DPicker(
                        hue = hue,
                        sat = sat,
                        value = value,
                        selectedColor = Color(currentRgbInt),
                        onHsvChange = { h, s, v ->
                            hue = h; sat = s; value = v
                        }
                    )
                    2 -> RgbSlidersTab(
                        isRu = isRu,
                        red = redInt,
                        green = greenInt,
                        blue = blueInt,
                        hexString = hexString,
                        onRgbChange = { r, g, b -> updateFromRgb(r, g, b) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Opacity checkerboard slider + current color preview circle on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OpacityCheckerboardBar(
                    baseColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))),
                    alphaFrac = alphaFrac,
                    onAlphaChange = { alphaFrac = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(currentRgbInt))
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clean Bottom Navigation Capsule: [✕] | ( ПАЛИТРА | СПЕКТР | ШКАЛА ) | [✓]
            // Also supports horizontal swipe across the bottom bar to switch tabs!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .pointerInput(Unit) {
                        var accumDx = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { accumDx = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                accumDx += dragAmount
                            },
                            onDragEnd = {
                                if (accumDx < -45f && pagerState.currentPage < 2) {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                } else if (accumDx > 45f && pagerState.currentPage > 0) {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                }
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(scheme.islandBg)
                        .border(1.dp, scheme.islandBorder, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clean segmented capsule for the 3 tabs
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.islandBg)
                        .border(1.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabTitles.forEachIndexed { idx, title ->
                        val selected = pagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) scheme.accent else Color.Transparent)
                                .clickable {
                                    scope.launch { pagerState.animateScrollToPage(idx) }
                                }
                                .padding(horizontal = 13.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (selected) scheme.onAccent else scheme.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(scheme.accent)
                        .clickable { onConfirm(currentLong) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = scheme.onAccent,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatchesGrid(
    selectedRgbInt: Int,
    onSelectColorInt: (Int) -> Unit
) {
    val cols = 12
    val rows = 10
    val hues = remember {
        floatArrayOf(205f, 220f, 265f, 290f, 335f, 10f, 25f, 38f, 48f, 58f, 75f, 110f)
    }

    val gridColors = remember {
        Array(rows) { r ->
            IntArray(cols) { c ->
                if (r == 0) {
                    val v = ((cols - 1 - c).toFloat() / (cols - 1) * 255f).roundToInt()
                    android.graphics.Color.rgb(v, v, v)
                } else {
                    val h = hues[c]
                    val t = r.toFloat() / (rows - 1)
                    val s = if (t < 0.6f) 0.95f else (1f - (t - 0.6f) * 1.35f).coerceIn(0.25f, 1f)
                    val v = if (t < 0.55f) (0.32f + t * 1.2f).coerceIn(0.3f, 1f) else 1f
                    android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
                }
            }
        }
    }

    // Note: PaletteSwatchesGrid uses tap to pick a cell so horizontal swipes across the grid smoothly switch between ПАЛИТРА | СПЕКТР | ШКАЛА!
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val c = ((offset.x / size.width) * cols).toInt().coerceIn(0, cols - 1)
                    val r = ((offset.y / size.height) * rows).toInt().coerceIn(0, rows - 1)
                    onSelectColorInt(gridColors[r][c])
                }
            }
    ) {
        val cellW = size.width / cols
        val cellH = size.height / rows
        val selRgbNoAlpha = selectedRgbInt and 0x00FFFFFF
        var selectedCell: Pair<Int, Int>? = null

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cellColorInt = gridColors[r][c]
                drawRect(
                    color = Color(cellColorInt),
                    topLeft = Offset(c * cellW, r * cellH),
                    size = Size(cellW + 0.5f, cellH + 0.5f)
                )
                if ((cellColorInt and 0x00FFFFFF) == selRgbNoAlpha) {
                    selectedCell = r to c
                }
            }
        }

        selectedCell?.let { (sr, sc) ->
            drawRect(
                color = Color.White,
                topLeft = Offset(sc * cellW + 1.5f, sr * cellH + 1.5f),
                size = Size((cellW - 3f).coerceAtLeast(2f), (cellH - 3f).coerceAtLeast(2f)),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}

@Composable
private fun Spectrum2DPicker(
    hue: Float,
    sat: Float,
    value: Float,
    selectedColor: Color,
    onHsvChange: (Float, Float, Float) -> Unit
) {
    val rainbowColors = remember {
        listOf(
            Color(0xFFFF0000),
            Color(0xFFFF8800),
            Color(0xFFFFFF00),
            Color(0xFF00FF00),
            Color(0xFF00FFFF),
            Color(0xFF0000FF),
            Color(0xFFFF00FF),
            Color(0xFFFF0000)
        )
    }

    val cursorXFrac = if (value >= 0.98f) (sat * 0.5f) else (0.5f + (1f - value) * 0.5f)
    val cursorYFrac = (hue / 360f).coerceIn(0f, 1f)

    fun handleOffset(pos: Offset, w: Float, h: Float) {
        val xf = (pos.x / w.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val yf = (pos.y / h.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val newHue = yf * 360f
        val newSat: Float
        val newVal: Float
        if (xf <= 0.5f) {
            newSat = (xf / 0.5f).coerceIn(0f, 1f)
            newVal = 1f
        } else {
            newSat = 1f
            newVal = (1f - (xf - 0.5f) / 0.5f).coerceIn(0f, 1f)
        }
        onHsvChange(newHue, newSat, newVal)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectTapGestures { pos -> handleOffset(pos, size.width.toFloat(), size.height.toFloat()) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos -> handleOffset(pos, size.width.toFloat(), size.height.toFloat()) },
                    onDrag = { change, _ ->
                        change.consume()
                        handleOffset(change.position, size.width.toFloat(), size.height.toFloat())
                    }
                )
            }
    ) {
        drawRect(brush = Brush.verticalGradient(rainbowColors))
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.White,
                0.5f to Color.Transparent,
                1f to Color.Black
            )
        )

        val cx = (cursorXFrac * size.width).coerceIn(14.dp.toPx(), size.width - 14.dp.toPx())
        val cy = (cursorYFrac * size.height).coerceIn(14.dp.toPx(), size.height - 14.dp.toPx())
        // Filled color indicator circle with crisp white ring
        drawCircle(
            color = selectedColor,
            radius = 13.dp.toPx(),
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White,
            radius = 13.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
private fun RgbSlidersTab(
    isRu: Boolean,
    red: Int,
    green: Int,
    blue: Int,
    hexString: String,
    onRgbChange: (Int, Int, Int) -> Unit
) {
    val scheme = LocalAnonGramScheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RgbChannelSliderRow(
            label = if (isRu) "КРАСНЫЙ" else "RED",
            value = red,
            trackColor = Color(0xFFFF3B30),
            onValueChange = { onRgbChange(it, green, blue) }
        )
        RgbChannelSliderRow(
            label = if (isRu) "ЗЕЛЁНЫЙ" else "GREEN",
            value = green,
            trackColor = Color(0xFF34C759),
            onValueChange = { onRgbChange(red, it, blue) }
        )
        RgbChannelSliderRow(
            label = if (isRu) "СИНИЙ" else "BLUE",
            value = blue,
            trackColor = Color(0xFF0A84FF),
            onValueChange = { onRgbChange(red, green, it) }
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRu) "КОД ЦВЕТА #" else "HEX COLOR #",
                color = scheme.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(10.dp))
            var hexDraft by remember(hexString) { mutableStateOf(hexString) }
            Box(
                modifier = Modifier
                    .width(86.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.card)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = hexDraft,
                    onValueChange = { raw ->
                        val clean = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
                        hexDraft = clean
                        if (clean.length == 6) {
                            runCatching {
                                val parsed = clean.toInt(16)
                                onRgbChange((parsed ushr 16) and 0xFF, (parsed ushr 8) and 0xFF, parsed and 0xFF)
                            }
                        }
                    },
                    textStyle = TextStyle(
                        color = scheme.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(scheme.accent)
                )
            }
        }
    }
}

@Composable
private fun RgbChannelSliderRow(
    label: String,
    value: Int,
    trackColor: Color,
    onValueChange: (Int) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = scheme.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val frac = (value / 255f).coerceIn(0f, 1f)
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            val nv = ((pos.x / size.width.toFloat().coerceAtLeast(1f)) * 255f).roundToInt().coerceIn(0, 255)
                            onValueChange(nv)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val nv = ((change.position.x / size.width.toFloat().coerceAtLeast(1f)) * 255f).roundToInt().coerceIn(0, 255)
                            onValueChange(nv)
                        }
                    }
            ) {
                val trackH = 12.dp.toPx()
                val topY = (size.height - trackH) / 2f
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color.Black, trackColor)),
                    topLeft = Offset(0f, topY),
                    size = Size(size.width, trackH),
                    cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                )
                val cx = (frac * size.width).coerceIn(10.dp.toPx(), size.width - 10.dp.toPx())
                drawCircle(
                    color = trackColor,
                    radius = 10.dp.toPx(),
                    center = Offset(cx, size.height / 2f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 10.dp.toPx(),
                    center = Offset(cx, size.height / 2f),
                    style = Stroke(width = 2.2.dp.toPx())
                )
            }

            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.card)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    color = scheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OpacityCheckerboardBar(
    baseColor: Color,
    alphaFrac: Float,
    onAlphaChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    onAlphaChange((pos.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0.1f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onAlphaChange((change.position.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0.1f, 1f))
                }
            }
    ) {
        val sq = size.height / 2f
        val cols = (size.width / sq).toInt() + 1
        for (r in 0..1) {
            for (c in 0..cols) {
                val dark = (r + c) % 2 == 0
                drawRect(
                    color = if (dark) Color(0xFF3A3A3C) else Color(0xFF636366),
                    topLeft = Offset(c * sq, r * sq),
                    size = Size(sq, sq)
                )
            }
        }
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, baseColor)
            )
        )
        val cx = (alphaFrac.coerceIn(0f, 1f) * size.width).coerceIn(10.dp.toPx(), size.width - 10.dp.toPx())
        drawCircle(
            color = baseColor,
            radius = 9.dp.toPx(),
            center = Offset(cx, size.height / 2f)
        )
        drawCircle(
            color = Color.White,
            radius = 9.dp.toPx(),
            center = Offset(cx, size.height / 2f),
            style = Stroke(width = 2.2.dp.toPx())
        )
    }
}
