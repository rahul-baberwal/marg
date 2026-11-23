package com.rahulbaberwal.marg.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import com.rahulbaberwal.marg.DeveloperSettingsHelper
import com.rahulbaberwal.marg.MapLayer
import com.rahulbaberwal.marg.MargUiState
import com.rahulbaberwal.marg.MargViewModel
import com.rahulbaberwal.marg.Mode
import com.rahulbaberwal.marg.P
import com.rahulbaberwal.marg.R
import com.rahulbaberwal.marg.RouteResult
import com.rahulbaberwal.marg.SimState
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    vm: MargViewModel,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    val minYPx = statusBarTopPx + with(density) { 155.dp.roundToPx() }
    val margin16Px = with(density) { 16.dp.roundToPx() }

    var showSearchOverlay by rememberSaveable { mutableStateOf(false) }
    var searchTarget by rememberSaveable { mutableStateOf("start") }
    var showLayerPicker by rememberSaveable { mutableStateOf(false) }
    var showFeatureGuide by rememberSaveable { mutableStateOf(false) }
    var mapBearing by rememberSaveable { mutableStateOf(0f) }

    val hasSeenGuide by vm.hasSeenFeatureGuide.collectAsState()
    LaunchedEffect(hasSeenGuide) {
        if (hasSeenGuide == false) {
            showFeatureGuide = true
        }
    }

    // Auto-recheck mock location status when user returns to app from Developer Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.recheckMockLocation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }

    // Permission launcher (single unified launcher requested at moment of need)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fineLocationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineLocationGranted) {
            // Check mock location app selection
            val isMockApp = DeveloperSettingsHelper.isMockLocationAppSelected(context)
            if (!isMockApp) {
                val devModeOn = DeveloperSettingsHelper.isDeveloperOptionsEnabled(context)
                if (devModeOn) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Marg is not set as Mock Location app. Opening Developer options...",
                            duration = SnackbarDuration.Short
                        )
                    }
                    DeveloperSettingsHelper.openMockLocationSettings(context)
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Developer options disabled. Tap Build number 7 times in About Phone.",
                            duration = SnackbarDuration.Long
                        )
                    }
                    DeveloperSettingsHelper.openAboutPhoneSettings(context)
                }
                vm.markMockLocationNotSet()
            } else {
                vm.startSim()
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Location permission is required for mock GPS",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    fun attemptStartSimulation() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
            return
        }

        // Check Mock Location selection before starting simulation
        val isMockApp = DeveloperSettingsHelper.isMockLocationAppSelected(context)
        if (!isMockApp) {
            val devModeOn = DeveloperSettingsHelper.isDeveloperOptionsEnabled(context)
            if (devModeOn) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Marg is not set as Mock Location app. Opening Developer options...",
                        duration = SnackbarDuration.Short
                    )
                }
                DeveloperSettingsHelper.openMockLocationSettings(context)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Developer options disabled. Tap Build number 7 times in About Phone.",
                        duration = SnackbarDuration.Long
                    )
                }
                DeveloperSettingsHelper.openAboutPhoneSettings(context)
            }
            vm.markMockLocationNotSet()
            return
        }

        vm.startSim()
    }

    // Error messages surfaced as single clean Snackbar
    LaunchedEffect(uiState.error) {
        val err = uiState.error
        if (!err.isNullOrBlank()) {
            val res = snackbarHostState.showSnackbar(
                message = err,
                actionLabel = if (err.contains("Route failed", ignoreCase = true)) "Retry" else null,
                duration = SnackbarDuration.Short
            )
            if (res == SnackbarResult.ActionPerformed) {
                vm.fetchRoute()
            }
            vm.dismissError()
        }
    }

    // Animate Route Line updates
    LaunchedEffect(uiState.route) {
        val map = mapRef.value ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        updateRouteOnMap(map, style, uiState.route)
    }

    // Update Start, End, and Waypoint Google Maps pin markers
    LaunchedEffect(uiState.startPoint, uiState.endPoint, uiState.waypoints, uiState.mode) {
        val map = mapRef.value ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        updatePointsOnMap(style, uiState)
    }

    // Smooth moving marker interpolation between 1-second ticks
    LaunchedEffect(uiState.position) {
        val map = mapRef.value ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        updateUserPositionOnMap(style, uiState.position)
    }

    // Switch Map Layers when layer state updates
    LaunchedEffect(uiState.selectedLayer) {
        val map = mapRef.value ?: return@LaunchedEffect
        val builder = getMapStyleBuilder(context, uiState.selectedLayer)
        map.setStyle(builder) { newStyle ->
            initMapLayersAndSources(context, newStyle, uiState.selectedLayer)
            restoreMapState(map, newStyle, uiState)
        }
    }

    // Lifecycle Observer for MapView
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            val mv = mapViewRef.value ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> mv.onStart()
                Lifecycle.Event.ON_RESUME -> mv.onResume()
                Lifecycle.Event.ON_PAUSE -> mv.onPause()
                Lifecycle.Event.ON_STOP -> mv.onStop()
                Lifecycle.Event.ON_DESTROY -> mv.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // 5-detent smooth bottom sheet scaffold (StandardBottomSheetState with peek, half, expanded)
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
        confirmValueChange = { true }
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 210.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetShadowElevation = 12.dp,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 220.dp)
            )
        },
        sheetContent = {
            BottomControlSheet(
                uiState = uiState,
                onModeSelected = { vm.setMode(it) },
                onSpeedChanged = { vm.setSpeed(it) },
                onIndoorRadiusChanged = { vm.setIndoorRadius(it) },
                onJitterToggled = { vm.toggleJitter(it) },
                onPlayPause = {
                    when (uiState.simState) {
                        SimState.RUNNING -> vm.pauseSim()
                        SimState.PAUSED -> vm.resumeSim()
                        else -> attemptStartSimulation()
                    }
                },
                onStop = {
                    if (uiState.simState == SimState.PAUSED) {
                        vm.resetSim()
                    } else {
                        vm.stopSim()
                    }
                },
                onOpenMockLocationSettings = {
                    val opened = DeveloperSettingsHelper.openMockLocationSettings(context)
                    if (!opened) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Unable to open Developer options",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                    vm.recheckMockLocation()
                },
                onDismissMockLocationCard = { vm.dismissMockLocationCard() },
                onClearAllStops = { vm.clearWaypoints() }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-screen MapLibre Map with Google Maps pin markers and Hold & Drop pin dragging
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapLibre.getInstance(ctx)
                    val density = ctx.resources.displayMetrics.density

                    var downX = 0f
                    var downY = 0f
                    var activePinId: String? = null
                    var isDraggingPin = false

                    MapView(ctx).apply {
                        mapViewRef.value = this
                        onCreate(null)
                        onStart()
                        onResume()

                        // Touch Listener: Lock map immediately on pin touch so the map NEVER moves while adjusting pins
                        setOnTouchListener { v, event ->
                            val map = mapRef.value
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    downX = event.x
                                    downY = event.y
                                    activePinId = findPinNearScreenPoint(map, uiState, event.x, event.y, density)
                                    if (activePinId != null) {
                                        isDraggingPin = false
                                        // Disable scroll gestures instantly so MapLibre's gesture detector does not pan the map
                                        map?.uiSettings?.setScrollGesturesEnabled(false)
                                        v.parent?.requestDisallowInterceptTouchEvent(true)
                                        return@setOnTouchListener true
                                    }
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    if (activePinId != null && map != null) {
                                        val movedDist = Math.hypot((event.x - downX).toDouble(), (event.y - downY).toDouble())
                                        if (!isDraggingPin && movedDist > (8f * density)) {
                                            isDraggingPin = true
                                            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        }
                                        if (isDraggingPin) {
                                            // Place pin slightly above touch point so pin tip is clearly visible
                                            val targetY = event.y - (30f * density)
                                            val latLng = map.projection.fromScreenLocation(PointF(event.x, targetY))
                                            map.style?.let { style ->
                                                updateSinglePinTemporary(style, activePinId, latLng, uiState)
                                            }
                                            return@setOnTouchListener true
                                        }
                                    }
                                }
                                MotionEvent.ACTION_UP -> {
                                    if (activePinId != null && map != null) {
                                        val movedDist = Math.hypot((event.x - downX).toDouble(), (event.y - downY).toDouble())
                                        if (isDraggingPin || movedDist > (8f * density)) {
                                            val targetY = event.y - (30f * density)
                                            val latLng = map.projection.fromScreenLocation(PointF(event.x, targetY))
                                            val newPoint = P(latLng.latitude, latLng.longitude)
                                            when {
                                                activePinId == "start" -> vm.setStart(newPoint)
                                                activePinId == "end" -> vm.setEnd(newPoint)
                                                activePinId?.startsWith("waypoint_") == true -> {
                                                    val idx = activePinId!!.removePrefix("waypoint_").toIntOrNull()
                                                    if (idx != null) vm.updateWaypoint(idx, newPoint)
                                                }
                                            }
                                            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        } else {
                                            // Tap without dragging -> show inline action chip
                                            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            vm.showPinChip(activePinId)
                                        }
                                        map.uiSettings.setScrollGesturesEnabled(true)
                                        isDraggingPin = false
                                        activePinId = null
                                        return@setOnTouchListener true
                                    }
                                    map?.uiSettings?.setScrollGesturesEnabled(true)
                                    isDraggingPin = false
                                    activePinId = null
                                }
                                MotionEvent.ACTION_CANCEL -> {
                                    map?.uiSettings?.setScrollGesturesEnabled(true)
                                    if (isDraggingPin && map?.style != null) {
                                        updatePointsOnMap(map.style, uiState)
                                    }
                                    isDraggingPin = false
                                    activePinId = null
                                }
                            }
                            false
                        }

                        getMapAsync { map ->
                            mapRef.value = map

                            // Listen for camera rotation changes to animate compass
                            map.addOnCameraMoveListener {
                                mapBearing = map.cameraPosition.bearing.toFloat()
                            }

                            // Apply Initial Map Layer
                            val builder = getMapStyleBuilder(ctx, uiState.selectedLayer)
                            map.setStyle(builder) { style ->
                                initMapLayersAndSources(ctx, style, uiState.selectedLayer)
                                restoreMapState(map, style, uiState)
                            }

                            // India centroid default view if no route / points set
                            if (uiState.route == null && uiState.startPoint == null) {
                                map.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(22.9734, 78.6569))
                                    .zoom(4.2)
                                    .build()
                            }

                            // Long Press Map to drop/move pin
                            map.addOnMapLongClickListener { latLng ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val p = P(latLng.latitude, latLng.longitude)

                                if (uiState.mode == Mode.FIXED || uiState.startPoint == null) {
                                    vm.setStart(p)
                                    map.easeCamera(
                                        CameraUpdateFactory.newLatLngZoom(latLng, 15.0),
                                        500
                                    )
                                } else if (uiState.endPoint == null) {
                                    vm.setEnd(p)
                                } else {
                                    // Add as intermediate stop (waypoint)
                                    vm.addWaypoint(p)
                                }
                                true
                            }

                            // Tap Pin to show inline chip options
                            map.addOnMapClickListener { latLng ->
                                val screenPoint = map.projection.toScreenLocation(latLng)
                                val features = map.queryRenderedFeatures(
                                    PointF(screenPoint.x, screenPoint.y),
                                    "points-symbol-layer"
                                )
                                if (features.isNotEmpty()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val pinId = features[0].getStringProperty("id")
                                    vm.showPinChip(pinId)
                                    true
                                } else {
                                    vm.showPinChip(null)
                                    false
                                }
                            }
                        }
                    }
                }
            )

            // Floating Search Card at Top (Map-first layout)
            SearchCard(
                uiState = uiState,
                onStartClick = {
                    searchTarget = "start"
                    showSearchOverlay = true
                },
                onEndClick = {
                    searchTarget = "destination"
                    showSearchOverlay = true
                },
                onAddStopClick = { insertIdx ->
                    searchTarget = "add_stop_$insertIdx"
                    showSearchOverlay = true
                },
                onEditStopClick = { idx ->
                    searchTarget = "edit_stop_$idx"
                    showSearchOverlay = true
                },
                onSwapClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.swapPoints()
                },
                onClearStart = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.clearStart()
                },
                onClearEnd = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.clearEnd()
                },
                onRemoveWaypoint = { idx ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.removeWaypoint(idx)
                },
                onMoveWaypoint = { from, to ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.moveWaypoint(from, to)
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // Inline Pin Action Chip (when a pin is tapped on map)
            AnimatedVisibility(
                visible = uiState.activePinChip != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 135.dp),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                val chip = uiState.activePinChip
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when {
                                chip == "start" -> if (uiState.mode == Mode.FIXED) "Fixed Location" else "Start Pin"
                                chip == "end" -> "Destination Pin"
                                chip?.startsWith("waypoint_") == true -> {
                                    val idx = chip.removePrefix("waypoint_").toIntOrNull()?.plus(1) ?: 1
                                    "Stop #$idx"
                                }
                                else -> "Stop"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(
                            modifier = Modifier
                                .height(16.dp)
                                .width(1.dp)
                        )
                        if (chip == "start" && uiState.mode != Mode.FIXED) {
                            TextButton(onClick = {
                                vm.swapPoints()
                                vm.showPinChip(null)
                            }) {
                                Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Swap", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        TextButton(onClick = {
                            when {
                                chip == "start" -> vm.clearStart()
                                chip == "end" -> vm.clearEnd()
                                chip?.startsWith("waypoint_") == true -> {
                                    val idx = chip.removePrefix("waypoint_").toIntOrNull()
                                    if (idx != null) vm.removeWaypoint(idx)
                                }
                            }
                            vm.showPinChip(null)
                        }) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (chip?.startsWith("waypoint_") == true) "Remove Stop" else "Remove",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Floating Map Buttons (Layers, Recenter, Zoom to fit, Info)
            // Dynamically positioned right above the bottom sheet dock, gliding smoothly when sheet slides up
            var sideButtonsHeightPx by remember { mutableStateOf(0) }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .onGloballyPositioned { coordinates ->
                        sideButtonsHeightPx = coordinates.size.height
                    }
                    .offset {
                        val sheetOffset = runCatching { sheetState.requireOffset() }.getOrNull()
                        val targetY = if (sheetOffset != null && sideButtonsHeightPx > 0) {
                            (sheetOffset - sideButtonsHeightPx - margin16Px).roundToInt()
                        } else {
                            minYPx
                        }
                        val finalY = maxOf(minYPx, targetY)
                        IntOffset(x = 0, y = finalY)
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                // 1. Compass Button (Always available, rotates needle with map, tap to reset North)
                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        mapRef.value?.easeCamera(CameraUpdateFactory.bearingTo(0.0), 400)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(4.dp, CircleShape)
                        .semantics { contentDescription = "Reset map orientation to North" },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_compass),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                rotationZ = -mapBearing
                            }
                    )
                }

                // 2. Layer Selector Button (Always available)
                FilledTonalIconButton(
                    onClick = { showLayerPicker = true },
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(4.dp, CircleShape)
                        .semantics { contentDescription = "Choose map style layer" },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 2. Recenter Button: Shown only when user has a location or simulation is active
                val hasLocation = uiState.position != null || uiState.startPoint != null
                AnimatedVisibility(
                    visible = hasLocation,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            val pos = uiState.position ?: uiState.startPoint
                            if (pos != null) {
                                mapRef.value?.easeCamera(
                                    CameraUpdateFactory.newLatLngZoom(LatLng(pos.lat, pos.lon), 15.0),
                                    500
                                )
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, CircleShape)
                            .semantics { contentDescription = "Center camera on current location" },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 3. Zoom to Fit Route Button: Shown only when a route is loaded in non-fixed mode
                val canFitRoute = uiState.route != null && uiState.mode != Mode.FIXED
                AnimatedVisibility(
                    visible = canFitRoute,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            val r = uiState.route ?: return@FilledTonalIconButton
                            val boundsBuilder = LatLngBounds.Builder()
                            r.points.forEach { boundsBuilder.include(LatLng(it.lat, it.lon)) }
                            mapRef.value?.easeCamera(
                                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 160, 260, 160, 340),
                                600
                            )
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, CircleShape)
                            .semantics { contentDescription = "Zoom to fit entire route" },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitScreen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // 4. Quick Guide & Hints Button (Always available)
                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showFeatureGuide = true
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(4.dp, CircleShape)
                        .semantics { contentDescription = "Quick guide and feature hints" },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 5. Settings & About Button (Always available)
                FilledTonalIconButton(
                    onClick = onOpenAbout,
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(4.dp, CircleShape)
                        .semantics { contentDescription = "Settings and about info" },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Route Calculation Progress Indicator
            AnimatedVisibility(
                visible = uiState.isRouting,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 110.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }

    // Full-screen Search Overlay
    if (showSearchOverlay) {
        val placeholder = when {
            searchTarget == "start" -> if (uiState.mode == Mode.FIXED) "Search fixed location or paste lat, lon" else "Search start location or paste lat, lon"
            searchTarget == "destination" -> "Search destination or paste lat, lon"
            searchTarget.startsWith("add_stop") -> "Search intermediate stop or paste lat, lon"
            searchTarget.startsWith("edit_stop_") -> {
                val idx = searchTarget.removePrefix("edit_stop_").toIntOrNull()?.plus(1) ?: 1
                "Search stop #$idx or paste lat, lon"
            }
            else -> "Search location or paste lat, lon"
        }

        SearchOverlay(
            isStartField = (searchTarget == "start"),
            placeholderText = placeholder,
            uiState = uiState,
            onSearch = { vm.search(it) },
            onClear = { vm.clearSearch() },
            onResultSelected = { res ->
                val p = P(res.lat, res.lon)
                when {
                    searchTarget == "start" -> vm.setStart(p, res.name)
                    searchTarget == "destination" -> vm.setEnd(p, res.name)
                    searchTarget.startsWith("add_stop") -> {
                        val insertIdx = searchTarget.removePrefix("add_stop_").toIntOrNull() ?: -1
                        vm.addWaypoint(p, insertIdx, res.name)
                    }
                    searchTarget.startsWith("edit_stop_") -> {
                        val idx = searchTarget.removePrefix("edit_stop_").toIntOrNull()
                        if (idx != null) {
                            vm.updateWaypoint(idx, p, res.name)
                        }
                    }
                }
                mapRef.value?.easeCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(p.lat, p.lon), 14.5),
                    600
                )
                showSearchOverlay = false
                vm.clearSearch()
            },
            onDismiss = {
                showSearchOverlay = false
                vm.clearSearch()
            }
        )
    }

    // Layer Picker Bottom Sheet
    if (showLayerPicker) {
        LayerPickerSheet(
            currentLayer = uiState.selectedLayer,
            onLayerSelected = { selected ->
                vm.setLayer(selected)
                showLayerPicker = false
            },
            onDismiss = { showLayerPicker = false }
        )
    }

    // Quick Feature Guide & Hints Bottom Sheet
    if (showFeatureGuide) {
        FeatureGuideSheet(
            onDismiss = { dontShowAgain ->
                showFeatureGuide = false
                if (dontShowAgain) {
                    vm.setFeatureGuideDontShowAgain()
                }
            }
        )
    }
}

private fun findPinNearScreenPoint(
    map: MapLibreMap?,
    uiState: MargUiState,
    touchX: Float,
    touchY: Float,
    density: Float
): String? {
    if (map == null) return null
    val touchRadius = 45f * density
    var bestPinId: String? = null
    var minDistance = touchRadius.toDouble()

    // 1. Start Pin
    uiState.startPoint?.let { p ->
        val screen = map.projection.toScreenLocation(LatLng(p.lat, p.lon))
        val pinHeadY = screen.y - (24f * density)
        val dist = Math.hypot((touchX - screen.x).toDouble(), (touchY - pinHeadY).toDouble())
        if (dist < minDistance) {
            minDistance = dist
            bestPinId = "start"
        }
    }

    // 2. Waypoints
    if (uiState.mode != Mode.FIXED) {
        uiState.waypoints.forEachIndexed { idx, wp ->
            val screen = map.projection.toScreenLocation(LatLng(wp.lat, wp.lon))
            val pinHeadY = screen.y - (24f * density)
            val dist = Math.hypot((touchX - screen.x).toDouble(), (touchY - pinHeadY).toDouble())
            if (dist < minDistance) {
                minDistance = dist
                bestPinId = "waypoint_$idx"
            }
        }
    }

    // 3. Destination Pin
    if (uiState.mode != Mode.FIXED) {
        uiState.endPoint?.let { p ->
            val screen = map.projection.toScreenLocation(LatLng(p.lat, p.lon))
            val pinHeadY = screen.y - (24f * density)
            val dist = Math.hypot((touchX - screen.x).toDouble(), (touchY - pinHeadY).toDouble())
            if (dist < minDistance) {
                minDistance = dist
                bestPinId = "end"
            }
        }
    }

    return bestPinId
}

private fun updateSinglePinTemporary(
    style: Style?,
    dragPinId: String?,
    newLatLng: LatLng,
    uiState: MargUiState
) {
    if (style == null || dragPinId == null) return
    val pointsSrc = style.getSourceAs<GeoJsonSource>("points-source") ?: return
    val features = mutableListOf<Feature>()

    // Start Pin
    val startCoord = if (dragPinId == "start") newLatLng else uiState.startPoint?.let { LatLng(it.lat, it.lon) }
    startCoord?.let {
        val f = Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude))
        f.addStringProperty("icon", if (uiState.mode == Mode.FIXED) "marker-fixed" else "marker-start")
        f.addStringProperty("id", "start")
        features.add(f)
    }

    // Waypoints
    if (uiState.mode != Mode.FIXED) {
        uiState.waypoints.forEachIndexed { idx, wp ->
            val coord = if (dragPinId == "waypoint_$idx") newLatLng else LatLng(wp.lat, wp.lon)
            val f = Feature.fromGeometry(Point.fromLngLat(coord.longitude, coord.latitude))
            f.addStringProperty("icon", "marker-waypoint-${(idx + 1).coerceAtMost(10)}")
            f.addStringProperty("id", "waypoint_$idx")
            features.add(f)
        }
    }

    // End Pin
    if (uiState.mode != Mode.FIXED) {
        val endCoord = if (dragPinId == "end") newLatLng else uiState.endPoint?.let { LatLng(it.lat, it.lon) }
        endCoord?.let {
            val f = Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude))
            f.addStringProperty("icon", "marker-end")
            f.addStringProperty("id", "end")
            features.add(f)
        }
    }

    pointsSrc.setGeoJson(FeatureCollection.fromFeatures(features))
}

private fun updateRouteOnMap(map: MapLibreMap?, style: Style?, route: RouteResult?) {
    if (style == null) return
    val routeSrc = style.getSourceAs<GeoJsonSource>("route-source") ?: return
    if (route != null && route.points.size >= 2) {
        val coords = route.points.map { Point.fromLngLat(it.lon, it.lat) }
        routeSrc.setGeoJson(LineString.fromLngLats(coords))

        val boundsBuilder = LatLngBounds.Builder()
        route.points.forEach { boundsBuilder.include(LatLng(it.lat, it.lon)) }
        try {
            map?.easeCamera(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 160, 260, 160, 340),
                800
            )
        } catch (_: Exception) {}
    } else {
        routeSrc.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
    }
}

private fun updatePointsOnMap(style: Style?, uiState: MargUiState) {
    if (style == null) return
    val pointsSrc = style.getSourceAs<GeoJsonSource>("points-source") ?: return
    val features = mutableListOf<Feature>()

    // Start Pin (Green Google Maps Pin with 'A' or 'H' for fixed)
    uiState.startPoint?.let { p ->
        val f = Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat))
        f.addStringProperty("icon", if (uiState.mode == Mode.FIXED) "marker-fixed" else "marker-start")
        f.addStringProperty("id", "start")
        features.add(f)
    }

    // Waypoints (Teal Google Maps Pin with '1', '2', etc.)
    if (uiState.mode != Mode.FIXED) {
        uiState.waypoints.forEachIndexed { idx, p ->
            val f = Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat))
            f.addStringProperty("icon", "marker-waypoint-${(idx + 1).coerceAtMost(10)}")
            f.addStringProperty("id", "waypoint_$idx")
            features.add(f)
        }
    }

    // Destination Pin (Red/Saffron Google Maps Pin with 'B')
    if (uiState.mode != Mode.FIXED) {
        uiState.endPoint?.let { p ->
            val f = Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat))
            f.addStringProperty("icon", "marker-end")
            f.addStringProperty("id", "end")
            features.add(f)
        }
    }

    pointsSrc.setGeoJson(FeatureCollection.fromFeatures(features))
}

private fun updateUserPositionOnMap(style: Style?, pos: P?) {
    if (style == null) return
    val meSrc = style.getSourceAs<GeoJsonSource>("me-source") ?: return
    if (pos == null) {
        meSrc.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
    } else {
        meSrc.setGeoJson(Point.fromLngLat(pos.lon, pos.lat))
    }
}

private fun restoreMapState(map: MapLibreMap, style: Style, uiState: MargUiState) {
    updateRouteOnMap(map, style, uiState.route)
    updatePointsOnMap(style, uiState)
    updateUserPositionOnMap(style, uiState.position)
    if (uiState.route == null && uiState.startPoint != null) {
        map.easeCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(uiState.startPoint.lat, uiState.startPoint.lon), 15.0),
            400
        )
    }
}

private fun getMapStyleBuilder(context: Context, layer: MapLayer): Style.Builder {
    return when (layer) {
        MapLayer.DARK -> Style.Builder().fromUri("https://tiles.openfreemap.org/styles/dark")
        else -> Style.Builder().fromJson(loadMapStyleJson(context, layer))
    }
}

private fun initMapLayersAndSources(context: Context, style: Style, layer: MapLayer = MapLayer.STANDARD) {
    val routeLineColor = if (layer == MapLayer.DARK) "#38BDF8" else "#0E3B43"

    // Register Google Maps style pin icons into map style
    MapMarkerHelper.registerPinIcons(context, style)

    // 1. Route Source & Line Layer
    if (style.getSource("route-source") == null) {
        style.addSource(GeoJsonSource("route-source"))
    }
    if (style.getLayer("route-line-layer") == null) {
        style.addLayer(
            LineLayer("route-line-layer", "route-source").withProperties(
                lineColor(routeLineColor),
                lineWidth(6f),
                lineCap("round"),
                lineJoin("round")
            )
        )
    }

    // 2. Points (Start, End, Waypoints) Source & Google Maps Pin Symbol Layer
    if (style.getSource("points-source") == null) {
        style.addSource(GeoJsonSource("points-source"))
    }
    if (style.getLayer("points-symbol-layer") == null) {
        style.addLayer(
            SymbolLayer("points-symbol-layer", "points-source").withProperties(
                iconImage(Expression.get("icon")),
                iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
    }

    // 3. User Location ("Me") Source & Pulsing Layer
    if (style.getSource("me-source") == null) {
        style.addSource(GeoJsonSource("me-source"))
    }
    if (style.getLayer("me-layer") == null) {
        style.addLayer(
            CircleLayer("me-layer", "me-source").withProperties(
                circleRadius(10f),
                circleColor("#2563EB"),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(3.5f)
            )
        )
    }
}

private fun loadMapStyleJson(context: Context, layer: MapLayer): String {
    val filename = when (layer) {
        MapLayer.STANDARD -> "map_styles/standard.json"
        MapLayer.DARK -> "map_styles/dark.json"
        MapLayer.TERRAIN -> "map_styles/terrain.json"
        MapLayer.SATELLITE -> "map_styles/satellite.json"
    }
    return try {
        context.assets.open(filename).bufferedReader().readText()
    } catch (_: Exception) {
        """{"version":8,"sources":{"osm":{"type":"raster","tiles":["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],"tileSize":256,"attribution":"© OpenStreetMap contributors","maxzoom":19}},"layers":[{"id":"osm","type":"raster","source":"osm"}]}"""
    }
}
