package com.example.speedometer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep screen on while using speedometer
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark,
                ) {
                    SpeedometerApp()
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Language & Unit Definitions
// -----------------------------------------------------------------------------
private const val PREFS_NAME = "speedometer_prefs"
private const val PREF_KEY_LANGUAGE = "pref_language"
private const val PREF_KEY_UNIT = "pref_unit"
private const val PREF_KEY_LIMIT = "pref_speed_limit"

enum class AppLanguage(val code: String, val displayName: String, val shortName: String) {
    EN("en", "English", "EN"),
    TH("th", "ไทย", "TH"),
}

enum class SpeedUnit(
    val label: String,
    val conversionFactor: Float, // from km/h
    val maxGaugeSpeed: Float,
    val majorTickStep: Float,
) {
    KMH("KM/H", 1.0f, 220f, 20f),
    MPH("MPH", 0.621371f, 140f, 20f),
    KNOTS("KTS", 0.539957f, 120f, 10f),
}

enum class GpsStatus {
    SEARCHING,
    ENABLE_GPS_SETTINGS,
    CONNECTING,
    PERMISSION_DENIED,
    CONNECTED,
    DISABLED;

    fun getMessage(language: AppLanguage): String = when (this) {
        SEARCHING -> if (language == AppLanguage.EN) "Searching for GPS signal..." else "กำลังค้นหาสัญญาณ GPS..."
        ENABLE_GPS_SETTINGS -> if (language == AppLanguage.EN) "Please enable GPS in settings" else "กรุณาเปิด GPS ในการตั้งค่าเครื่อง"
        CONNECTING -> if (language == AppLanguage.EN) "Connecting to GPS satellites..." else "กำลังเชื่อมต่อดาวเทียม GPS..."
        PERMISSION_DENIED -> if (language == AppLanguage.EN) "Location permission denied" else "ไม่ได้รับสิทธิ์ตำแหน่ง"
        CONNECTED -> if (language == AppLanguage.EN) "GPS Connected" else "GPS เชื่อมต่อสมบูรณ์"
        DISABLED -> if (language == AppLanguage.EN) "GPS is disabled" else "GPS ถูกปิดอยู่"
    }
}

private data class SpeedUiState(
    val speedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val totalDistanceMeters: Double = 0.0,
    val altitudeMeters: Double? = null,
    val bearingDegrees: Float? = null,
    val accuracyMeters: Float? = null,
    val isGpsReady: Boolean = false,
    val status: GpsStatus = GpsStatus.SEARCHING,
)

// -----------------------------------------------------------------------------
// Main Composable & Lifecycle Handling
// -----------------------------------------------------------------------------
@Composable
private fun SpeedometerApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var selectedLanguage by remember {
        val savedLangName = prefs.getString(PREF_KEY_LANGUAGE, AppLanguage.EN.name)
        mutableStateOf(
            try {
                AppLanguage.valueOf(savedLangName ?: AppLanguage.EN.name)
            } catch (_: Exception) {
                AppLanguage.EN
            }
        )
    }

    val onLanguageChange: (AppLanguage) -> Unit = { newLang ->
        selectedLanguage = newLang
        prefs.edit().putString(PREF_KEY_LANGUAGE, newLang.name).apply()
    }

    val controller = remember { SpeedController(context.applicationContext) }
    var permitted by remember { mutableStateOf(context.hasLocationPermission()) }

    val permissionRequester = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        permitted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(permitted) {
        if (permitted) controller.start()
    }
    DisposableEffect(Unit) {
        onDispose { controller.stop() }
    }

    if (permitted) {
        ModernDashboardScreen(
            state = controller.state,
            language = selectedLanguage,
            onSelectLanguage = onLanguageChange,
            onResetTrip = { controller.resetTrip() },
        )
    } else {
        ModernPermissionScreen(
            language = selectedLanguage,
            onSelectLanguage = onLanguageChange,
            onGrant = {
                permissionRequester.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            },
        )
    }
}

// -----------------------------------------------------------------------------
// Modern Dashboard Screen
// -----------------------------------------------------------------------------
@Composable
private fun ModernDashboardScreen(
    state: SpeedUiState,
    language: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onResetTrip: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var selectedUnit by remember {
        val savedUnitName = prefs.getString(PREF_KEY_UNIT, SpeedUnit.KMH.name)
        mutableStateOf(
            try {
                SpeedUnit.valueOf(savedUnitName ?: SpeedUnit.KMH.name)
            } catch (_: Exception) {
                SpeedUnit.KMH
            }
        )
    }

    var isHudMode by remember { mutableStateOf(false) }

    var speedLimitKmh by remember {
        val savedLimit = prefs.getFloat(PREF_KEY_LIMIT, 120f)
        mutableFloatStateOf(savedLimit)
    }

    val onUnitSelect: (SpeedUnit) -> Unit = { unit ->
        selectedUnit = unit
        prefs.edit().putString(PREF_KEY_UNIT, unit.name).apply()
    }

    val onToggleSpeedLimit = {
        val nextLimit = when (speedLimitKmh) {
            0f -> 80f
            80f -> 100f
            100f -> 120f
            else -> 0f
        }
        speedLimitKmh = nextLimit
        prefs.edit().putFloat(PREF_KEY_LIMIT, nextLimit).apply()
    }

    // Current unit converted values
    val currentSpeedConverted = state.speedKmh * selectedUnit.conversionFactor
    val maxSpeedConverted = state.maxSpeedKmh * selectedUnit.conversionFactor
    val speedLimitConverted = speedLimitKmh * selectedUnit.conversionFactor
    val isOverSpeed = speedLimitKmh > 0 && state.speedKmh > speedLimitKmh

    val distanceDisplay = when (selectedUnit) {
        SpeedUnit.KMH -> "%.2f km".format(state.totalDistanceMeters / 1000.0)
        SpeedUnit.MPH -> "%.2f mi".format(state.totalDistanceMeters * 0.000621371)
        SpeedUnit.KNOTS -> "%.2f nm".format(state.totalDistanceMeters * 0.000539957)
    }

    val altitudeDisplay = state.altitudeMeters?.let {
        if (selectedUnit == SpeedUnit.MPH) "%.0f ft".format(it * 3.28084)
        else "%.0f m".format(it)
    } ?: "--"

    val headingDisplay = state.bearingDegrees?.let { degrees ->
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
        val index = ((degrees + 22.5f) / 45f).toInt() % 8
        "${directions[index]} %.0f°".format(degrees)
    } ?: "--"

    val accuracySearchingText = if (language == AppLanguage.EN) "Searching..." else "ค้นหา..."
    val accuracyDisplay = state.accuracyMeters?.let { "±%.1fm".format(it) } ?: accuracySearchingText

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isHudMode) listOf(Color.Black, Color.Black)
                    else listOf(BgGradientTop, BgDark, BgGradientBottom),
                ),
            )
            .graphicsLayer {
                // HUD Mode: horizontal mirror flip for windshield reflection
                if (isHudMode) {
                    scaleX = -1f
                }
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // --- Top Bar: App Title, Language Toggle, Limit & HUD ---
        TopBarControls(
            language = language,
            onToggleLanguage = onSelectLanguage,
            isHudMode = isHudMode,
            onToggleHud = { isHudMode = !isHudMode },
            speedLimit = speedLimitKmh,
            onToggleSpeedLimit = onToggleSpeedLimit,
        )

        Spacer(Modifier.height(12.dp))

        // --- Unit Selector Pills ---
        UnitSelector(
            currentUnit = selectedUnit,
            onSelectUnit = onUnitSelect,
        )

        Spacer(Modifier.height(16.dp))

        // --- Over-Speed Alert Banner ---
        AnimatedVisibility(
            visible = isOverSpeed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            OverSpeedWarningBanner(
                limit = speedLimitConverted.toInt(),
                unit = selectedUnit.label,
                language = language,
            )
            Spacer(Modifier.height(12.dp))
        }

        // --- Main Gauge Dial ---
        ModernSpeedGauge(
            speed = currentSpeedConverted,
            maxSpeed = selectedUnit.maxGaugeSpeed,
            majorStep = selectedUnit.majorTickStep,
            unitLabel = selectedUnit.label,
            isOverSpeed = isOverSpeed,
            isGpsReady = state.isGpsReady,
        )

        Spacer(Modifier.height(16.dp))

        // --- GPS Status & Precision Badge ---
        GpsStatusBar(
            status = state.status.getMessage(language),
            accuracy = accuracyDisplay,
            accuracyLabel = if (language == AppLanguage.EN) "Accuracy" else "ความแม่นยำ",
            isGpsReady = state.isGpsReady,
        )

        Spacer(Modifier.height(20.dp))

        // --- Telemetry Cards Grid ---
        TelemetryGrid(
            maxSpeed = "%.1f %s".format(maxSpeedConverted, selectedUnit.label),
            distance = distanceDisplay,
            altitude = altitudeDisplay,
            heading = headingDisplay,
            language = language,
        )

        Spacer(Modifier.height(16.dp))

        // --- Trip Reset Button ---
        TripResetBar(
            language = language,
            onResetTrip = onResetTrip,
        )

        Spacer(Modifier.height(24.dp))
    }
}

// -----------------------------------------------------------------------------
// Top Bar Controls
// -----------------------------------------------------------------------------
@Composable
private fun TopBarControls(
    language: AppLanguage,
    onToggleLanguage: (AppLanguage) -> Unit,
    isHudMode: Boolean,
    onToggleHud: () -> Unit,
    speedLimit: Float,
    onToggleSpeedLimit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "SPEEDOMETER",
                color = NeonCyan,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.5.sp,
            )
            Text(
                text = if (language == AppLanguage.EN) "GPS TELEMETRY PRO" else "ระบบวัดความเร็ว GPS PRO",
                color = TextDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Language selector toggle (TH | EN)
            LanguageSelectorToggle(
                currentLanguage = language,
                onSelectLanguage = onToggleLanguage,
            )

            // Speed Limit Switcher Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (speedLimit > 0) LimitActiveBg else CardBg)
                    .border(1.dp, if (speedLimit > 0) WarningRed.copy(alpha = 0.6f) else BorderDark, RoundedCornerShape(20.dp))
                    .clickable { onToggleSpeedLimit() }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (speedLimit > 0) "LIMIT ${speedLimit.toInt()}" else "LIMIT OFF",
                    color = if (speedLimit > 0) WarningRed else TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // HUD Mode Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isHudMode) NeonCyan.copy(alpha = 0.2f) else CardBg)
                    .border(1.dp, if (isHudMode) NeonCyan else BorderDark, RoundedCornerShape(20.dp))
                    .clickable { onToggleHud() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isHudMode) "HUD ON" else "HUD",
                    color = if (isHudMode) NeonCyan else TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Language Selector Toggle
// -----------------------------------------------------------------------------
@Composable
private fun LanguageSelectorToggle(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppLanguage.entries.forEach { lang ->
            val isSelected = lang == currentLanguage
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) NeonCyan else Color.Transparent)
                    .clickable { onSelectLanguage(lang) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = lang.shortName,
                    color = if (isSelected) BgDark else TextDim,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Unit Selector
// -----------------------------------------------------------------------------
@Composable
private fun UnitSelector(
    currentUnit: SpeedUnit,
    onSelectUnit: (SpeedUnit) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(CardBg)
            .border(1.dp, BorderDark, RoundedCornerShape(30.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SpeedUnit.entries.forEach { unit ->
            val isSelected = unit == currentUnit
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (isSelected) NeonCyan else Color.Transparent)
                    .clickable { onSelectUnit(unit) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = unit.label,
                    color = if (isSelected) BgDark else TextDim,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Over-speed Warning Banner
// -----------------------------------------------------------------------------
@Composable
private fun OverSpeedWarningBanner(
    limit: Int,
    unit: String,
    language: AppLanguage,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    val warningMessage = if (language == AppLanguage.EN) {
        "⚠️ Speed limit exceeded (Limit: $limit $unit)"
    } else {
        "⚠️ ความเร็วเกินกำหนด (จำกัดที่ $limit $unit)"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarningRed.copy(alpha = 0.2f * alpha))
            .border(1.5.dp, WarningRed.copy(alpha = alpha), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = warningMessage,
            color = WarningRed,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// -----------------------------------------------------------------------------
// Custom High-Tech Speed Gauge (Canvas Drawing)
// -----------------------------------------------------------------------------
@Composable
private fun ModernSpeedGauge(
    speed: Float,
    maxSpeed: Float,
    majorStep: Float,
    unitLabel: String,
    isOverSpeed: Boolean,
    isGpsReady: Boolean,
) {
    val clampedSpeed = speed.coerceIn(0f, maxSpeed)
    val animatedSpeed by animateFloatAsState(
        targetValue = clampedSpeed,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "animatedSpeed",
    )

    val gaugeColor by animateColorAsState(
        targetValue = if (isOverSpeed) WarningRed else NeonCyan,
        label = "gaugeColor",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val radius = min(size.width, size.height) * 0.42f
            val startAngle = 135f
            val sweepTotal = 270f

            // 1. Outer ambient track shadow / glow ring
            drawArc(
                color = TrackBg,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
            )

            // 2. Active colored gradient progress arc
            val currentSweep = (animatedSpeed / maxSpeed) * sweepTotal
            if (currentSweep > 0.5f) {
                val activeBrush = Brush.sweepGradient(
                    colors = listOf(
                        NeonCyan,
                        ElectricBlue,
                        WarningOrange,
                        WarningRed,
                    ),
                    center = center,
                )
                drawArc(
                    brush = activeBrush,
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            // 3. Draw Minor & Major Ticks + Scale Text Numerals
            val totalTicks = (maxSpeed / (majorStep / 2)).toInt()
            val textPaint = Paint().apply {
                color = TextMuted.toArgb()
                textSize = 10.sp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            for (i in 0..totalTicks) {
                val tickSpeed = i * (majorStep / 2)
                val isMajor = i % 2 == 0
                val tickAngle = startAngle + (tickSpeed / maxSpeed) * sweepTotal
                val radians = Math.toRadians(tickAngle.toDouble())

                val outerR = radius - 16.dp.toPx()
                val innerR = outerR - (if (isMajor) 14.dp.toPx() else 7.dp.toPx())

                val tickColor = when {
                    tickSpeed >= maxSpeed * 0.85f -> WarningRed.copy(alpha = 0.85f)
                    tickSpeed <= animatedSpeed -> NeonCyan
                    else -> BorderLight
                }

                // Draw Tick Line
                drawLine(
                    color = tickColor,
                    start = Offset(
                        center.x + cos(radians).toFloat() * innerR,
                        center.y + sin(radians).toFloat() * innerR,
                    ),
                    end = Offset(
                        center.x + cos(radians).toFloat() * outerR,
                        center.y + sin(radians).toFloat() * outerR,
                    ),
                    strokeWidth = if (isMajor) 3.5.dp.toPx() else 1.8.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                // Draw Scale Number (on Major Ticks)
                if (isMajor) {
                    val numR = innerR - 14.dp.toPx()
                    val textX = center.x + cos(radians).toFloat() * numR
                    val textY = center.y + sin(radians).toFloat() * numR + (textPaint.textSize / 3)

                    textPaint.color = if (tickSpeed <= animatedSpeed) TextWhite.toArgb() else TextMuted.toArgb()
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            tickSpeed.toInt().toString(),
                            textX,
                            textY,
                            textPaint,
                        )
                    }
                }
            }

            // 4. Draw Illuminated Tapered Needle
            val needleAngle = startAngle + (animatedSpeed / maxSpeed) * sweepTotal
            val needleRad = Math.toRadians(needleAngle.toDouble())
            val needleLength = radius * 0.76f

            val tipX = center.x + cos(needleRad).toFloat() * needleLength
            val tipY = center.y + sin(needleRad).toFloat() * needleLength

            val leftRad = needleRad - Math.PI / 2
            val rightRad = needleRad + Math.PI / 2
            val baseRadius = 8.dp.toPx()

            val baseLeft = Offset(
                center.x + cos(leftRad).toFloat() * baseRadius,
                center.y + sin(leftRad).toFloat() * baseRadius,
            )
            val baseRight = Offset(
                center.x + cos(rightRad).toFloat() * baseRadius,
                center.y + sin(rightRad).toFloat() * baseRadius,
            )

            val needlePath = Path().apply {
                moveTo(tipX, tipY)
                lineTo(baseRight.x, baseRight.y)
                lineTo(baseLeft.x, baseLeft.y)
                close()
            }

            // Needle Body & Outline
            drawPath(
                path = needlePath,
                brush = Brush.linearGradient(
                    colors = listOf(gaugeColor, gaugeColor.copy(alpha = 0.5f)),
                    start = Offset(tipX, tipY),
                    end = center,
                ),
            )

            // 5. Center Metallic Pivot Hub
            drawCircle(color = BgDark, radius = 18.dp.toPx(), center = center)
            drawCircle(color = BorderDark, radius = 16.dp.toPx(), center = center)
            drawCircle(color = gaugeColor, radius = 7.dp.toPx(), center = center)
            drawCircle(color = TextWhite, radius = 3.dp.toPx(), center = center)
        }

        // Center Digital Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 70.dp),
        ) {
            Text(
                text = "%.0f".format(animatedSpeed),
                color = if (isOverSpeed) WarningRed else TextWhite,
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-2).sp,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isOverSpeed) WarningRed.copy(alpha = 0.2f) else CardBg)
                    .border(1.dp, if (isOverSpeed) WarningRed else BorderDark, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            ) {
                Text(
                    text = unitLabel,
                    color = if (isOverSpeed) WarningRed else NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// GPS Status Bar
// -----------------------------------------------------------------------------
@Composable
private fun GpsStatusBar(
    status: String,
    accuracy: String,
    accuracyLabel: String,
    isGpsReady: Boolean,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gpsPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotAlpha",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGpsReady) SuccessGreen.copy(alpha = dotAlpha)
                        else WarningOrange.copy(alpha = dotAlpha),
                    ),
            )
            Text(
                text = status,
                color = if (isGpsReady) TextWhite else WarningOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = "$accuracyLabel $accuracy",
            color = TextDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// -----------------------------------------------------------------------------
// Telemetry Grid
// -----------------------------------------------------------------------------
@Composable
private fun TelemetryGrid(
    maxSpeed: String,
    distance: String,
    altitude: String,
    heading: String,
    language: AppLanguage,
) {
    val maxSpeedTitle = if (language == AppLanguage.EN) "MAX SPEED" else "ความเร็วสูงสุด"
    val distanceTitle = if (language == AppLanguage.EN) "TRIP DISTANCE" else "ระยะทางสะสม"
    val altitudeTitle = if (language == AppLanguage.EN) "ALTITUDE" else "ระดับความสูง"
    val headingTitle = if (language == AppLanguage.EN) "HEADING" else "ทิศทาง"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TelemetryCard(
                modifier = Modifier.weight(1f),
                title = maxSpeedTitle,
                value = maxSpeed,
                iconText = "⚡",
                accentColor = WarningOrange,
            )
            TelemetryCard(
                modifier = Modifier.weight(1f),
                title = distanceTitle,
                value = distance,
                iconText = "📍",
                accentColor = NeonCyan,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TelemetryCard(
                modifier = Modifier.weight(1f),
                title = altitudeTitle,
                value = altitude,
                iconText = "⛰️",
                accentColor = ElectricBlue,
            )
            TelemetryCard(
                modifier = Modifier.weight(1f),
                title = headingTitle,
                value = heading,
                iconText = "🧭",
                accentColor = SuccessGreen,
            )
        }
    }
}

@Composable
private fun TelemetryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    iconText: String,
    accentColor: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color = TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text(text = iconText, fontSize = 14.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Trip Reset Bar
// -----------------------------------------------------------------------------
@Composable
private fun TripResetBar(
    language: AppLanguage,
    onResetTrip: () -> Unit,
) {
    val resetText = if (language == AppLanguage.EN) {
        "🔄 Reset Trip & Max Speed"
    } else {
        "🔄 รีเซ็ตระยะทางและความเร็วสูงสุด"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .clickable { onResetTrip() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = resetText,
            color = TextDim,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// -----------------------------------------------------------------------------
// Modern Permission Screen
// -----------------------------------------------------------------------------
@Composable
private fun ModernPermissionScreen(
    language: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onGrant: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgGradientTop, BgDark, BgGradientBottom),
                ),
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            LanguageSelectorToggle(
                currentLanguage = language,
                onSelectLanguage = onSelectLanguage,
            )
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = 0.12f))
                .border(2.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("🛰️", fontSize = 38.sp)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "SPEEDOMETER PRO",
            color = NeonCyan,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = if (language == AppLanguage.EN)
                "Precise GPS location permission is required\nto calculate real-time speed, distance, and heading."
            else
                "ต้องการสิทธิ์เข้าถึงตำแหน่งที่แม่นยำ (Precise GPS)\nเพื่อคำนวณความเร็ว ระยะทาง และทิศทางการเคลื่อนที่แบบเรียลไทม์",
            color = TextDim,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(28.dp))

        // Features list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PermissionFeatureItem(
                icon = "⚡",
                title = if (language == AppLanguage.EN) "Real-time GPS Speed" else "วัดความเร็ว GPS เรียลไทม์",
                desc = if (language == AppLanguage.EN) "Direct satellite speed tracking, no internet required" else "วัดความเร็วจากดาวเทียมตรง ไม่ใช้อินเทอร์เน็ต",
            )
            PermissionFeatureItem(
                icon = "🛡️",
                title = if (language == AppLanguage.EN) "100% Privacy" else "ความเป็นส่วนตัว 100%",
                desc = if (language == AppLanguage.EN) "No location data is stored or transmitted" else "ไม่มีการเก็บหรือส่งข้อมูลพิกัดออกจากเครื่อง",
            )
            PermissionFeatureItem(
                icon = "🚘",
                title = if (language == AppLanguage.EN) "HUD Mode & Speed Alerts" else "โหมด HUD & เตือนความเร็ว",
                desc = if (language == AppLanguage.EN) "Windshield reflection mode for night driving with speed alerts" else "สะท้อนกระจกหน้ารถตอนกลางคืน พร้อมเตือนความเร็ว",
            )
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onGrant,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = BgDark,
            ),
        ) {
            Text(
                text = if (language == AppLanguage.EN) "Grant Permission & Start GPS" else "อนุญาตและเริ่มใช้งาน GPS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PermissionFeatureItem(icon: String, title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(icon, fontSize = 20.sp)
        Column {
            Text(title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = TextDim, fontSize = 11.sp)
        }
    }
}

// -----------------------------------------------------------------------------
// Speed Controller Logic & Sensor Fusion
// -----------------------------------------------------------------------------
private class SpeedController(private val appContext: Context) : LocationListener {
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    var state by mutableStateOf(SpeedUiState())
        private set

    private var smoothedSpeed: Float? = null
    private var lastLocation: Location? = null
    private var listening = false

    fun start() {
        if (listening || !contextHasFineLocation()) return
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            state = state.copy(status = GpsStatus.ENABLE_GPS_SETTINGS, isGpsReady = false)
            return
        }
        state = state.copy(status = GpsStatus.CONNECTING, isGpsReady = false)
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                400L,
                0f,
                this,
                Looper.getMainLooper(),
            )
            listening = true
        } catch (e: SecurityException) {
            state = state.copy(status = GpsStatus.PERMISSION_DENIED, isGpsReady = false)
        }
    }

    fun stop() {
        if (!listening) return
        try {
            locationManager.removeUpdates(this)
        } catch (_: Exception) {}
        listening = false
    }

    fun resetTrip() {
        lastLocation = null
        smoothedSpeed = 0f
        state = state.copy(
            maxSpeedKmh = 0f,
            totalDistanceMeters = 0.0,
        )
    }

    override fun onLocationChanged(location: Location) {
        val rawKmh = if (location.hasSpeed()) (location.speed * 3.6f).coerceAtLeast(0f) else 0f
        val next = if (rawKmh < 1.2f) 0f else rawKmh

        // Exponential smoothing for natural needle flow
        val filtered = smoothedSpeed?.let { prev -> prev * 0.68f + next * 0.32f } ?: next
        smoothedSpeed = filtered

        // Distance accumulation (only if moved with reasonable accuracy)
        var addedDist = 0.0
        val prevLoc = lastLocation
        if (prevLoc != null && location.hasAccuracy() && location.accuracy < 30f) {
            val d = prevLoc.distanceTo(location).toDouble()
            if (d in 0.5..150.0 && filtered > 1.0f) {
                addedDist = d
            }
        }
        lastLocation = location

        val updatedMax = maxOf(state.maxSpeedKmh, filtered)
        val updatedDist = state.totalDistanceMeters + addedDist

        state = state.copy(
            speedKmh = filtered,
            maxSpeedKmh = updatedMax,
            totalDistanceMeters = updatedDist,
            altitudeMeters = if (location.hasAltitude()) location.altitude else state.altitudeMeters,
            bearingDegrees = if (location.hasBearing() && filtered > 2.0f) location.bearing else state.bearingDegrees,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
            isGpsReady = true,
            status = GpsStatus.CONNECTED,
        )
    }

    override fun onProviderEnabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            start()
        }
    }

    override fun onProviderDisabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            state = state.copy(status = GpsStatus.DISABLED, isGpsReady = false, speedKmh = 0f)
        }
    }

    private fun contextHasFineLocation(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

// -----------------------------------------------------------------------------
// Color Palette (Cyberpunk / Modern Cockpit Theme)
// -----------------------------------------------------------------------------
private val BgDark = Color(0xFF07090E)
private val BgGradientTop = Color(0xFF0C101A)
private val BgGradientBottom = Color(0xFF05070A)

private val CardBg = Color(0xFF101522)
private val LimitActiveBg = Color(0xFF261217)
private val BorderDark = Color(0xFF1E2638)
private val BorderLight = Color(0xFF2E3B54)
private val TrackBg = Color(0xFF161D2B)

private val NeonCyan = Color(0xFF00F2FE)
private val ElectricBlue = Color(0xFF4FACFE)
private val WarningOrange = Color(0xFFFF9100)
private val WarningRed = Color(0xFFFF1744)
private val SuccessGreen = Color(0xFF00E676)

private val TextWhite = Color(0xFFF6F8FC)
private val TextDim = Color(0xFF8A99AD)
private val TextMuted = Color(0xFF53627A)
