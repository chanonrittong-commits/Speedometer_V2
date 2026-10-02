package com.titanforge.velox

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.WindowManager
import android.graphics.Color as AndroidColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        
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
private const val PREF_KEY_THEME = "pref_theme"

enum class GaugeTheme(
    val id: String,
    val displayNameEn: String,
    val displayNameTh: String,
    val icon: String,
    val primaryNeon: Color,
    val secondaryNeon: Color,
    val accentNeon: Color,
    val bgGradientTop: Color,
    val bgDark: Color,
    val bgGradientBottom: Color,
    val cardBg: Color,
    val borderDark: Color,
    val borderLight: Color,
    val trackBg: Color,
) {
    MODERN_CYAN(
        id = "cyan",
        displayNameEn = "Modern Cyan",
        displayNameTh = "โมเดิร์น ไซอัน",
        icon = "💠",
        primaryNeon = Color(0xFF00F2FE),
        secondaryNeon = Color(0xFF4FACFE),
        accentNeon = Color(0xFFFF9100),
        bgGradientTop = Color(0xFF0C101A),
        bgDark = Color(0xFF07090E),
        bgGradientBottom = Color(0xFF05070A),
        cardBg = Color(0xFF101522),
        borderDark = Color(0xFF1E2638),
        borderLight = Color(0xFF2E3B54),
        trackBg = Color(0xFF161D2B),
    ),
    CYBERPUNK_NEON(
        id = "cyberpunk",
        displayNameEn = "Cyberpunk Neon",
        displayNameTh = "ไซเบอร์พังค์ นีออน",
        icon = "⚡",
        primaryNeon = Color(0xFFFF007F), // Radiant Neon Magenta/Hot Pink
        secondaryNeon = Color(0xFF9D00FF), // Electric Neon Purple
        accentNeon = Color(0xFFFFE600), // High-voltage Cyber Yellow
        bgGradientTop = Color(0xFF180A2E), // Deep Cyber Violet
        bgDark = Color(0xFF0C0318),
        bgGradientBottom = Color(0xFF07010F),
        cardBg = Color(0xFF1B0C33),
        borderDark = Color(0xFF3B1966),
        borderLight = Color(0xFF5E279E),
        trackBg = Color(0xFF281147),
    ),
}

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

    var selectedTheme by remember {
        val savedThemeName = prefs.getString(PREF_KEY_THEME, GaugeTheme.MODERN_CYAN.name)
        mutableStateOf(
            try {
                GaugeTheme.valueOf(savedThemeName ?: GaugeTheme.MODERN_CYAN.name)
            } catch (_: Exception) {
                GaugeTheme.MODERN_CYAN
            }
        )
    }

    val onLanguageChange: (AppLanguage) -> Unit = { newLang ->
        selectedLanguage = newLang
        prefs.edit().putString(PREF_KEY_LANGUAGE, newLang.name).apply()
    }

    val onThemeChange: (GaugeTheme) -> Unit = { newTheme ->
        selectedTheme = newTheme
        prefs.edit().putString(PREF_KEY_THEME, newTheme.name).apply()
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
            theme = selectedTheme,
            onSelectTheme = onThemeChange,
            onResetTrip = { controller.resetTrip() },
        )
    } else {
        ModernPermissionScreen(
            language = selectedLanguage,
            onSelectLanguage = onLanguageChange,
            theme = selectedTheme,
            onSelectTheme = onThemeChange,
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
    theme: GaugeTheme,
    onSelectTheme: (GaugeTheme) -> Unit,
    onResetTrip: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var showSettings by remember { mutableStateOf(false) }

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

    val onSpeedLimitSelect: (Float) -> Unit = { limit ->
        speedLimitKmh = limit
        prefs.edit().putFloat(PREF_KEY_LIMIT, limit).apply()
    }

    val onToggleSpeedLimit = {
        val nextLimit = when (speedLimitKmh) {
            0f -> 80f
            80f -> 100f
            100f -> 120f
            else -> 0f
        }
        onSpeedLimitSelect(nextLimit)
    }

    if (showSettings) {
        SettingsDialog(
            language = language,
            onSelectLanguage = onSelectLanguage,
            currentUnit = selectedUnit,
            onSelectUnit = onUnitSelect,
            speedLimit = speedLimitKmh,
            onSelectSpeedLimit = onSpeedLimitSelect,
            theme = theme,
            onSelectTheme = onSelectTheme,
            onDismiss = { showSettings = false },
        )
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

    val configuration = LocalConfiguration.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            // Landscape Widescreen Cockpit Split View
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (isHudMode) {
                            scaleX = -1f
                        }
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = if (isHudMode) listOf(Color.Black, Color.Black)
                            else listOf(theme.bgGradientTop, theme.bgDark, theme.bgGradientBottom),
                        ),
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left Column: Main Speedometer Dial
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    ModernSpeedGauge(
                        speed = currentSpeedConverted,
                        maxSpeed = selectedUnit.maxGaugeSpeed,
                        majorStep = selectedUnit.majorTickStep,
                        unitLabel = selectedUnit.label,
                        isOverSpeed = isOverSpeed,
                        isGpsReady = state.isGpsReady,
                        theme = theme,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Right Column: Controls, Warnings & Telemetry Grid
                Column(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Top Bar
                    TopBarControls(
                        language = language,
                        isHudMode = isHudMode,
                        onToggleHud = { isHudMode = !isHudMode },
                        speedLimit = speedLimitKmh,
                        onToggleSpeedLimit = onToggleSpeedLimit,
                        onOpenSettings = { showSettings = true },
                        theme = theme,
                    )

                    Spacer(Modifier.height(8.dp))

                    // Unit Selector Pills
                    UnitSelector(
                        currentUnit = selectedUnit,
                        onSelectUnit = onUnitSelect,
                        theme = theme,
                    )

                    Spacer(Modifier.height(8.dp))

                    // Over-Speed Alert Banner
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
                        Spacer(Modifier.height(8.dp))
                    }

                    // GPS Status & Precision Badge
                    GpsStatusBar(
                        status = state.status.getMessage(language),
                        accuracy = accuracyDisplay,
                        accuracyLabel = if (language == AppLanguage.EN) "Accuracy" else "ความแม่นยำ",
                        isGpsReady = state.isGpsReady,
                        theme = theme,
                    )

                    Spacer(Modifier.height(8.dp))

                    // Telemetry Cards Grid
                    TelemetryGrid(
                        maxSpeed = "%.1f %s".format(maxSpeedConverted, selectedUnit.label),
                        distance = distanceDisplay,
                        altitude = altitudeDisplay,
                        heading = headingDisplay,
                        language = language,
                        theme = theme,
                    )

                    Spacer(Modifier.height(10.dp))

                    // Trip Reset Button
                    TripResetBar(
                        language = language,
                        onResetTrip = onResetTrip,
                        theme = theme,
                    )
                }
            }
        } else {
            // Portrait Stacked View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isHudMode) listOf(Color.Black, Color.Black)
                            else listOf(theme.bgGradientTop, theme.bgDark, theme.bgGradientBottom),
                        ),
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .graphicsLayer {
                        if (isHudMode) {
                            scaleX = -1f
                        }
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top Bar
                TopBarControls(
                    language = language,
                    isHudMode = isHudMode,
                    onToggleHud = { isHudMode = !isHudMode },
                    speedLimit = speedLimitKmh,
                    onToggleSpeedLimit = onToggleSpeedLimit,
                    onOpenSettings = { showSettings = true },
                    theme = theme,
                )

                Spacer(Modifier.height(12.dp))

                // Unit Selector Pills
                UnitSelector(
                    currentUnit = selectedUnit,
                    onSelectUnit = onUnitSelect,
                    theme = theme,
                )

                Spacer(Modifier.height(16.dp))

                // Over-Speed Alert Banner
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

                // Main Gauge Dial
                ModernSpeedGauge(
                    speed = currentSpeedConverted,
                    maxSpeed = selectedUnit.maxGaugeSpeed,
                    majorStep = selectedUnit.majorTickStep,
                    unitLabel = selectedUnit.label,
                    isOverSpeed = isOverSpeed,
                    isGpsReady = state.isGpsReady,
                    theme = theme,
                )

                Spacer(Modifier.height(16.dp))

                // GPS Status & Precision Badge
                GpsStatusBar(
                    status = state.status.getMessage(language),
                    accuracy = accuracyDisplay,
                    accuracyLabel = if (language == AppLanguage.EN) "Accuracy" else "ความแม่นยำ",
                    isGpsReady = state.isGpsReady,
                    theme = theme,
                )

                Spacer(Modifier.height(20.dp))

                // Telemetry Cards Grid
                TelemetryGrid(
                    maxSpeed = "%.1f %s".format(maxSpeedConverted, selectedUnit.label),
                    distance = distanceDisplay,
                    altitude = altitudeDisplay,
                    heading = headingDisplay,
                    language = language,
                    theme = theme,
                )

                Spacer(Modifier.height(16.dp))

                // Trip Reset Button
                TripResetBar(
                    language = language,
                    onResetTrip = onResetTrip,
                    theme = theme,
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Top Bar Controls
// -----------------------------------------------------------------------------
@Composable
private fun TopBarControls(
    language: AppLanguage,
    isHudMode: Boolean,
    onToggleHud: () -> Unit,
    speedLimit: Float,
    onToggleSpeedLimit: () -> Unit,
    onOpenSettings: () -> Unit,
    theme: GaugeTheme,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = "VELOX",
                color = theme.primaryNeon,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.5.sp,
                maxLines = 1,
            )
            Text(
                text = if (language == AppLanguage.EN) "GPS TELEMETRY PRO" else "ระบบวัดความเร็ว GPS PRO",
                color = TextDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                maxLines = 1,
            )
        }

        Spacer(Modifier.width(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Speed Limit Switcher Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (speedLimit > 0) LimitActiveBg else theme.cardBg)
                    .border(1.dp, if (speedLimit > 0) WarningRed.copy(alpha = 0.6f) else theme.borderDark, RoundedCornerShape(20.dp))
                    .clickable { onToggleSpeedLimit() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (speedLimit > 0) "LIMIT ${speedLimit.toInt()}" else "LIMIT OFF",
                    color = if (speedLimit > 0) WarningRed else TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }

            // HUD Mode Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isHudMode) theme.primaryNeon.copy(alpha = 0.2f) else theme.cardBg)
                    .border(1.dp, if (isHudMode) theme.primaryNeon else theme.borderDark, RoundedCornerShape(20.dp))
                    .clickable { onToggleHud() }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isHudMode) "HUD ON" else "HUD",
                    color = if (isHudMode) theme.primaryNeon else TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }

            // Settings ⚙️ Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(theme.cardBg)
                    .border(1.dp, theme.borderDark, CircleShape)
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "⚙️",
                    fontSize = 15.sp,
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Settings Dialog Modal
// -----------------------------------------------------------------------------
@Composable
private fun SettingsDialog(
    language: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    currentUnit: SpeedUnit,
    onSelectUnit: (SpeedUnit) -> Unit,
    speedLimit: Float,
    onSelectSpeedLimit: (Float) -> Unit,
    theme: GaugeTheme,
    onSelectTheme: (GaugeTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.65f else 0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, theme.borderDark, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            shape = RoundedCornerShape(24.dp),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val maxCardHeight = maxHeight * (if (isLandscape) 0.88f else 0.82f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxCardHeight)
                        .padding(if (isLandscape) 18.dp else 22.dp),
                ) {
                    // Header: Title & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "⚙️",
                                fontSize = 20.sp,
                            )
                            Text(
                                text = if (language == AppLanguage.EN) "SETTINGS" else "การตั้งค่า",
                                color = theme.primaryNeon,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(theme.borderDark.copy(alpha = 0.5f))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "✕",
                                color = TextDim,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(Modifier.height(if (isLandscape) 12.dp else 18.dp))

                    // Scrollable Settings Options
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        // Section 1: Language Selection (EN / TH)
                        Text(
                            text = if (language == AppLanguage.EN) "LANGUAGE" else "ภาษา (LANGUAGE)",
                            color = TextDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                val isSelected = lang == language
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) theme.primaryNeon.copy(alpha = 0.18f) else Color(0xFF0C101A).copy(alpha = 0.5f))
                                        .border(
                                            1.dp,
                                            if (isSelected) theme.primaryNeon else theme.borderDark,
                                            RoundedCornerShape(12.dp),
                                        )
                                        .clickable { onSelectLanguage(lang) }
                                        .padding(vertical = if (isLandscape) 8.dp else 11.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.EN) "🇬🇧 English" else "🇹🇭 ภาษาไทย",
                                        color = if (isSelected) TextWhite else TextDim,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(if (isLandscape) 12.dp else 16.dp))

                        // Section 2: Theme Selection (Modern Cyan / Cyberpunk Neon)
                        Text(
                            text = if (language == AppLanguage.EN) "GAUGE THEME" else "ธีมหน้าปัด (GAUGE THEME)",
                            color = TextDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GaugeTheme.entries.forEach { th ->
                                val isSelected = th == theme
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) th.primaryNeon.copy(alpha = 0.2f) else Color(0xFF0C101A).copy(alpha = 0.5f))
                                        .border(
                                            1.dp,
                                            if (isSelected) th.primaryNeon else theme.borderDark,
                                            RoundedCornerShape(12.dp),
                                        )
                                        .clickable { onSelectTheme(th) }
                                        .padding(vertical = if (isLandscape) 8.dp else 11.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(text = th.icon, fontSize = 13.sp)
                                        Text(
                                            text = if (language == AppLanguage.EN) th.displayNameEn else th.displayNameTh,
                                            color = if (isSelected) TextWhite else TextDim,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(if (isLandscape) 12.dp else 16.dp))

                        // Section 3: Speed Unit Selection
                        Text(
                            text = if (language == AppLanguage.EN) "SPEED UNIT" else "หน่วยความเร็ว (SPEED UNIT)",
                            color = TextDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SpeedUnit.entries.forEach { unit ->
                                val isSelected = unit == currentUnit
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) theme.primaryNeon.copy(alpha = 0.18f) else Color(0xFF0C101A).copy(alpha = 0.5f))
                                        .border(
                                            1.dp,
                                            if (isSelected) theme.primaryNeon else theme.borderDark,
                                            RoundedCornerShape(12.dp),
                                        )
                                        .clickable { onSelectUnit(unit) }
                                        .padding(vertical = if (isLandscape) 8.dp else 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = unit.label,
                                        color = if (isSelected) theme.primaryNeon else TextDim,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(if (isLandscape) 12.dp else 16.dp))

                        // Section 4: Speed Limit Warning
                        Text(
                            text = if (language == AppLanguage.EN) "SPEED LIMIT ALERT" else "เตือนความเร็วเกินกำหนด",
                            color = TextDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(0f, 80f, 100f, 120f).forEach { limit ->
                                val isSelected = speedLimit == limit
                                val label = if (limit == 0f) {
                                    if (language == AppLanguage.EN) "OFF" else "ปิด"
                                } else {
                                    "${limit.toInt()}"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) (if (limit > 0) WarningRed.copy(alpha = 0.2f) else theme.primaryNeon.copy(alpha = 0.18f))
                                            else Color(0xFF0C101A).copy(alpha = 0.5f),
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) (if (limit > 0) WarningRed else theme.primaryNeon) else theme.borderDark,
                                            RoundedCornerShape(10.dp),
                                        )
                                        .clickable { onSelectSpeedLimit(limit) }
                                        .padding(vertical = if (isLandscape) 7.dp else 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) (if (limit > 0) WarningRed else theme.primaryNeon) else TextDim,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(if (isLandscape) 6.dp else 14.dp))
                    }

                    Spacer(Modifier.height(if (isLandscape) 8.dp else 14.dp))

                    // Fixed Done Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLandscape) 42.dp else 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primaryNeon,
                            contentColor = theme.bgDark,
                        ),
                    ) {
                        Text(
                            text = if (language == AppLanguage.EN) "Done" else "เสร็จสิ้น",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
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
    theme: GaugeTheme,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(theme.cardBg)
            .border(1.dp, theme.borderDark, RoundedCornerShape(30.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SpeedUnit.entries.forEach { unit ->
            val isSelected = unit == currentUnit
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (isSelected) theme.primaryNeon else Color.Transparent)
                    .clickable { onSelectUnit(unit) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = unit.label,
                    color = if (isSelected) theme.bgDark else TextDim,
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
// Custom High-Tech Speed Gauge (Zero-Overlap Cockpit Core & Orbiting Saber)
// -----------------------------------------------------------------------------
@Composable
private fun ModernSpeedGauge(
    speed: Float,
    maxSpeed: Float,
    majorStep: Float,
    unitLabel: String,
    isOverSpeed: Boolean,
    isGpsReady: Boolean,
    theme: GaugeTheme,
    modifier: Modifier = Modifier
        .fillMaxWidth(0.96f)
        .aspectRatio(1f),
) {
    val clampedSpeed = speed.coerceIn(0f, maxSpeed)
    val animatedSpeed by animateFloatAsState(
        targetValue = clampedSpeed,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "animatedSpeed",
    )

    val gaugeColor by animateColorAsState(
        targetValue = if (isOverSpeed) WarningRed else theme.primaryNeon,
        label = "gaugeColor",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val outerRadius = min(size.width, size.height) * 0.44f
            val coreRadius = outerRadius * 0.52f
            val startAngle = 135f
            val sweepTotal = 270f

            // 1. Outer Ambient Track Ring
            drawArc(
                color = theme.trackBg,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round),
            )

            // 2. Active Dynamic Gradient Progress Arc
            val currentSweep = (animatedSpeed / maxSpeed) * sweepTotal
            if (currentSweep > 0.5f) {
                val activeBrush = Brush.sweepGradient(
                    colors = listOf(
                        theme.primaryNeon,
                        theme.secondaryNeon,
                        theme.accentNeon,
                        WarningRed,
                    ),
                    center = center,
                )
                drawArc(
                    brush = activeBrush,
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            // 3. Minor & Major Ticks + Scale Text Numerals
            val totalTicks = (maxSpeed / (majorStep / 2)).toInt()
            val textPaint = Paint().apply {
                color = TextMuted.toArgb()
                textSize = 9.5.sp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val outerTickR = outerRadius - 10.dp.toPx()
            for (i in 0..totalTicks) {
                val tickSpeed = i * (majorStep / 2)
                val isMajor = i % 2 == 0
                val tickAngle = startAngle + (tickSpeed / maxSpeed) * sweepTotal
                val radians = Math.toRadians(tickAngle.toDouble())

                val innerTickR = outerTickR - (if (isMajor) 11.dp.toPx() else 5.5.dp.toPx())

                val tickColor = when {
                    tickSpeed >= maxSpeed * 0.85f -> WarningRed.copy(alpha = 0.85f)
                    tickSpeed <= animatedSpeed -> theme.primaryNeon
                    else -> theme.borderLight
                }

                // Draw Tick Line
                drawLine(
                    color = tickColor,
                    start = Offset(
                        center.x + cos(radians).toFloat() * innerTickR,
                        center.y + sin(radians).toFloat() * innerTickR,
                    ),
                    end = Offset(
                        center.x + cos(radians).toFloat() * outerTickR,
                        center.y + sin(radians).toFloat() * outerTickR,
                    ),
                    strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                // Draw Scale Number (on Major Ticks)
                if (isMajor) {
                    val numR = innerTickR - 11.dp.toPx()
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

            // 4. Central Core Pod Background & Bezel
            // Outer glow ring
            drawCircle(
                color = gaugeColor.copy(alpha = 0.12f),
                radius = coreRadius + 4.dp.toPx(),
                center = center,
            )
            // Core capsule solid background
            drawCircle(
                color = theme.cardBg,
                radius = coreRadius,
                center = center,
            )
            // Core capsule bezel border
            drawCircle(
                color = if (isOverSpeed) WarningRed.copy(alpha = 0.8f) else theme.borderDark,
                radius = coreRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
            // Inner decorative accent ring
            drawCircle(
                color = theme.borderDark.copy(alpha = 0.5f),
                radius = coreRadius - 5.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )

            // 5. Orbiting Saber Needle (Zero Overlap with Core Pod)
            val needleAngle = startAngle + (animatedSpeed / maxSpeed) * sweepTotal
            val needleRad = Math.toRadians(needleAngle.toDouble())

            val needleInnerR = coreRadius + 3.dp.toPx()
            val needleOuterR = outerRadius - 3.dp.toPx()

            val tipX = center.x + cos(needleRad).toFloat() * needleOuterR
            val tipY = center.y + sin(needleRad).toFloat() * needleOuterR

            val baseCenter = Offset(
                center.x + cos(needleRad).toFloat() * needleInnerR,
                center.y + sin(needleRad).toFloat() * needleInnerR,
            )

            val leftRad = needleRad - Math.PI / 2
            val rightRad = needleRad + Math.PI / 2
            val baseHalfWidth = 4.5.dp.toPx()

            val baseLeft = Offset(
                baseCenter.x + cos(leftRad).toFloat() * baseHalfWidth,
                baseCenter.y + sin(leftRad).toFloat() * baseHalfWidth,
            )
            val baseRight = Offset(
                baseCenter.x + cos(rightRad).toFloat() * baseHalfWidth,
                baseCenter.y + sin(rightRad).toFloat() * baseHalfWidth,
            )

            val saberPath = Path().apply {
                moveTo(tipX, tipY)
                lineTo(baseRight.x, baseRight.y)
                lineTo(baseLeft.x, baseLeft.y)
                close()
            }

            // Draw Saber Needle Body with Radiant Glow Gradient
            drawPath(
                path = saberPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        gaugeColor,
                        gaugeColor.copy(alpha = 0.75f),
                    ),
                    start = Offset(tipX, tipY),
                    end = baseCenter,
                ),
            )

            // Draw Orbiting Base Marker at Core Rim
            drawCircle(
                color = gaugeColor,
                radius = 4.dp.toPx(),
                center = baseCenter,
            )
            drawCircle(
                color = TextWhite,
                radius = 1.8.dp.toPx(),
                center = baseCenter,
            )
        }

        // Center Digital Display (Securely inside Core Pod)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 2.dp),
        ) {
            Text(
                text = "%.0f".format(animatedSpeed),
                color = if (isOverSpeed) WarningRed else TextWhite,
                fontSize = 68.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-2).sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isOverSpeed) WarningRed.copy(alpha = 0.2f) else theme.bgDark)
                    .border(1.dp, if (isOverSpeed) WarningRed else theme.borderDark, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            ) {
                Text(
                    text = unitLabel,
                    color = if (isOverSpeed) WarningRed else theme.primaryNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }

        // Digital Clock (hr:min:sec)
        DigitalClock(
            theme = theme,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
        )
    }
}

// -----------------------------------------------------------------------------
// Digital Clock Component (HH:mm:ss)
// -----------------------------------------------------------------------------
@Composable
private fun DigitalClock(
    theme: GaugeTheme,
    modifier: Modifier = Modifier,
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    var timeText by remember { mutableStateOf(timeFormat.format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            timeText = timeFormat.format(Date())
            val delayMs = 1000L - (System.currentTimeMillis() % 1000L)
            delay(delayMs.coerceAtLeast(100L))
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.cardBg.copy(alpha = 0.85f))
            .border(1.dp, theme.borderDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = timeText,
            color = TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp,
        )
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
    theme: GaugeTheme,
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
            .background(theme.cardBg)
            .border(1.dp, theme.borderDark, RoundedCornerShape(14.dp))
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
    theme: GaugeTheme,
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
                accentColor = theme.accentNeon,
                theme = theme,
            )
            TelemetryCard(
                modifier = Modifier.weight(1f),
                title = distanceTitle,
                value = distance,
                iconText = "📍",
                accentColor = theme.primaryNeon,
                theme = theme,
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
                accentColor = theme.secondaryNeon,
                theme = theme,
            )
            TelemetryCard(
                modifier = Modifier.weight(1f),
                title = headingTitle,
                value = heading,
                iconText = "🧭",
                accentColor = SuccessGreen,
                theme = theme,
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
    theme: GaugeTheme,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = theme.cardBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.borderDark),
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
    theme: GaugeTheme,
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
            .background(theme.cardBg)
            .border(1.dp, theme.borderDark, RoundedCornerShape(12.dp))
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
    theme: GaugeTheme,
    onSelectTheme: (GaugeTheme) -> Unit,
    onGrant: () -> Unit,
) {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsDialog(
            language = language,
            onSelectLanguage = onSelectLanguage,
            currentUnit = SpeedUnit.KMH,
            onSelectUnit = {},
            speedLimit = 0f,
            onSelectSpeedLimit = {},
            theme = theme,
            onSelectTheme = onSelectTheme,
            onDismiss = { showSettings = false },
        )
    }

    val configuration = LocalConfiguration.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(theme.bgGradientTop, theme.bgDark, theme.bgGradientBottom),
                        ),
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left Column: Hero Icon & Branding
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(theme.primaryNeon.copy(alpha = 0.12f))
                            .border(2.dp, theme.primaryNeon.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🛰️", fontSize = 32.sp)
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "VELOX PRO",
                        color = theme.primaryNeon,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = if (language == AppLanguage.EN)
                            "Precise GPS location permission is required to calculate real-time speed."
                        else
                            "ต้องการสิทธิ์เข้าถึงตำแหน่งที่แม่นยำ (Precise GPS) เพื่อคำนวณความเร็วแบบเรียลไทม์",
                        color = TextDim,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }

                // Right Column: Feature List & Action Button
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(theme.cardBg)
                                .border(1.dp, theme.borderDark, CircleShape)
                                .clickable { showSettings = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "⚙️",
                                fontSize = 15.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.cardBg)
                            .border(1.dp, theme.borderDark, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PermissionFeatureItem(
                            icon = "⚡",
                            title = if (language == AppLanguage.EN) "Real-time GPS Speed" else "วัดความเร็ว GPS เรียลไทม์",
                            desc = if (language == AppLanguage.EN) "Direct satellite speed tracking" else "วัดความเร็วจากดาวเทียมตรง ไม่ใช้อินเทอร์เน็ต",
                        )
                        PermissionFeatureItem(
                            icon = "🛡️",
                            title = if (language == AppLanguage.EN) "100% Privacy" else "ความเป็นส่วนตัว 100%",
                            desc = if (language == AppLanguage.EN) "No location data stored or transmitted" else "ไม่มีการเก็บหรือส่งข้อมูลพิกัดออกจากเครื่อง",
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = onGrant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primaryNeon,
                            contentColor = theme.bgDark,
                        ),
                    ) {
                        Text(
                            text = if (language == AppLanguage.EN) "Grant Permission & Start GPS" else "อนุญาตและเริ่มใช้งาน GPS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(theme.bgGradientTop, theme.bgDark, theme.bgGradientBottom),
                        ),
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(theme.cardBg)
                            .border(1.dp, theme.borderDark, CircleShape)
                            .clickable { showSettings = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "⚙️",
                            fontSize = 16.sp,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(theme.primaryNeon.copy(alpha = 0.12f))
                        .border(2.dp, theme.primaryNeon.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🛰️", fontSize = 38.sp)
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "VELOX PRO",
                    color = theme.primaryNeon,
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
                        .background(theme.cardBg)
                        .border(1.dp, theme.borderDark, RoundedCornerShape(16.dp))
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
                        containerColor = theme.primaryNeon,
                        contentColor = theme.bgDark,
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
