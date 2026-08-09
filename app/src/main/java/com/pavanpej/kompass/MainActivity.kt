package com.pavanpej.kompass

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pavanpej.kompass.location.CoordinateFormat
import com.pavanpej.kompass.location.CoordinateFormatter
import com.pavanpej.kompass.location.LocationFix
import com.pavanpej.kompass.sensor.AngleMath
import com.pavanpej.kompass.sensor.NorthMode
import com.pavanpej.kompass.sensor.SensorData
import com.pavanpej.kompass.ui.AccuracyBanner
import com.pavanpej.kompass.ui.AltitudeReadout
import com.pavanpej.kompass.ui.BearingLockBanner
import com.pavanpej.kompass.ui.CompassDial
import com.pavanpej.kompass.ui.MagneticFieldReadout
import com.pavanpej.kompass.ui.SegmentedToggle
import com.pavanpej.kompass.ui.SettingsScreen
import com.pavanpej.kompass.ui.theme.KompassColors
import com.pavanpej.kompass.ui.theme.KompassTheme

private enum class Screen { Compass, Settings }

class MainActivity : ComponentActivity() {

    private val viewModel: CompassViewModel by viewModels()

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KompassTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val sensorData by viewModel.sensorData.collectAsState()
                    val northMode by viewModel.northMode.collectAsState()
                    val declinationDegrees by viewModel.declinationDegrees.collectAsState()
                    val lockedBearing by viewModel.lockedBearing.collectAsState()
                    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
                    val defaultNorthMode by viewModel.defaultNorthMode.collectAsState()
                    val coordinateFormat by viewModel.coordinateFormat.collectAsState()
                    val locationFix by viewModel.locationFix.collectAsState()
                    val sunAzimuth by viewModel.sunAzimuth.collectAsState()
                    val moonAzimuth by viewModel.moonAzimuth.collectAsState()

                    var screen by remember { mutableStateOf(Screen.Compass) }

                    val context = LocalContext.current
                    val clipboardManager = LocalClipboardManager.current

                    var pendingLocationAction by remember { mutableStateOf<(() -> Unit)?>(null) }
                    val locationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        if (granted) pendingLocationAction?.invoke()
                        pendingLocationAction = null
                    }

                    fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    // Only invokes onGranted once ACCESS_COARSE_LOCATION is actually confirmed --
                    // shared by the compass screen's north-mode toggle, the settings screen's
                    // default-mode toggle, and the initial lat/long permission request below, so
                    // none of them flip state before permission is really granted.
                    val requestLocationPermission: (onGranted: () -> Unit) -> Unit = { onGranted ->
                        if (hasLocationPermission()) {
                            onGranted()
                        } else {
                            pendingLocationAction = onGranted
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    }

                    // Lat/long is a default-visible feature (not gated behind the true-north
                    // toggle), so it requests permission proactively on first view of the compass
                    // screen -- but still lazily (on screen appearance, not app launch), and still
                    // falls back gracefully (lat/long simply stays hidden) if denied.
                    LaunchedEffect(Unit) {
                        if (!hasLocationPermission()) {
                            requestLocationPermission { viewModel.restartLocationTracking() }
                        }
                    }

                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            if (screen == Screen.Settings) {
                                TopAppBar(
                                    title = { Text("Settings", color = KompassColors.OnSurface) },
                                    navigationIcon = {
                                        IconButton(onClick = { screen = Screen.Compass }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = KompassColors.OnSurface
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = KompassColors.TrueBlack
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            if (screen == Screen.Compass) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SegmentedToggle(
                                        options = listOf(NorthMode.MAGNETIC, NorthMode.TRUE),
                                        selected = northMode,
                                        onSelect = { mode ->
                                            if (mode == NorthMode.MAGNETIC) {
                                                viewModel.setNorthMode(mode)
                                            } else {
                                                requestLocationPermission { viewModel.setNorthMode(mode) }
                                            }
                                        },
                                        label = { if (it == NorthMode.MAGNETIC) "MAGNETIC" else "TRUE" }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Crossfade(targetState = screen, label = "screen") { target ->
                            when (target) {
                                Screen.Compass -> Box(modifier = Modifier.fillMaxSize()) {
                                    KompassScreen(
                                        sensorData = sensorData,
                                        northMode = northMode,
                                        declinationDegrees = declinationDegrees,
                                        lockedBearing = lockedBearing,
                                        onTapLock = viewModel::toggleBearingLock,
                                        locationFix = locationFix,
                                        coordinateFormat = coordinateFormat,
                                        onCoordinatesTap = {
                                            locationFix?.let { fix ->
                                                val text = CoordinateFormatter.format(fix.latitude, fix.longitude, coordinateFormat)
                                                clipboardManager.setText(AnnotatedString(text))
                                            }
                                        },
                                        sunAzimuth = sunAzimuth,
                                        moonAzimuth = moonAzimuth,
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                    IconButton(
                                        onClick = { screen = Screen.Settings },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(innerPadding)
                                            .padding(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = KompassColors.OnSurfaceMuted
                                        )
                                    }
                                    MagneticFieldReadout(
                                        microTesla = sensorData.magneticFieldMicroTesla,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(innerPadding)
                                            .padding(16.dp)
                                    )
                                    // Overlay, deliberately NOT part of KompassScreen's Column --
                                    // see BearingLockBanner's kdoc for why (it used to push the
                                    // whole dial upward when it appeared).
                                    BearingLockBanner(
                                        lockedBearing = lockedBearing,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(innerPadding)
                                            .padding(top = 64.dp)
                                    )
                                }

                                Screen.Settings -> SettingsScreen(
                                    hapticsEnabled = hapticsEnabled,
                                    onHapticsEnabledChange = viewModel::setHapticsEnabled,
                                    defaultNorthMode = defaultNorthMode,
                                    onDefaultNorthModeChange = { mode ->
                                        if (mode == NorthMode.MAGNETIC) {
                                            viewModel.setDefaultNorthMode(mode)
                                        } else {
                                            requestLocationPermission { viewModel.setDefaultNorthMode(mode) }
                                        }
                                    },
                                    coordinateFormat = coordinateFormat,
                                    onCoordinateFormatChange = viewModel::setCoordinateFormat,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KompassScreen(
    sensorData: SensorData,
    northMode: NorthMode,
    declinationDegrees: Float?,
    lockedBearing: Float?,
    onTapLock: (currentHeading: Float) -> Unit,
    locationFix: LocationFix?,
    coordinateFormat: CoordinateFormat,
    onCoordinatesTap: () -> Unit,
    sunAzimuth: Float?,
    moonAzimuth: Float?,
    modifier: Modifier = Modifier
) {
    val displayedHeading = remember(sensorData.azimuth, northMode, declinationDegrees) {
        if (northMode == NorthMode.TRUE && declinationDegrees != null) {
            AngleMath.addDegrees(sensorData.azimuth, declinationDegrees)
        } else {
            sensorData.azimuth
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CompassDial(
            heading = displayedHeading,
            gravityX = sensorData.gravityX,
            gravityY = sensorData.gravityY,
            gravityZ = sensorData.gravityZ,
            lockedBearing = lockedBearing,
            onTapLock = onTapLock,
            latitude = locationFix?.latitude,
            longitude = locationFix?.longitude,
            coordinateFormat = coordinateFormat,
            onCoordinatesTap = onCoordinatesTap,
            sunAzimuth = sunAzimuth,
            moonAzimuth = moonAzimuth
        )

        sensorData.barometricAltitudeMeters?.let { altitude ->
            Spacer(modifier = Modifier.height(12.dp))
            AltitudeReadout(altitudeMeters = altitude)
        }

        Spacer(modifier = Modifier.height(20.dp))
        AccuracyBanner(accuracy = sensorData.accuracy)
    }
}
