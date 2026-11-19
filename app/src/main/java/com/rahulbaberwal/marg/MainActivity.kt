package com.rahulbaberwal.marg

import android.Manifest
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rahulbaberwal.marg.databinding.ActivityMainBinding
import com.rahulbaberwal.marg.databinding.DialogLocationPickerBinding
import com.rahulbaberwal.marg.databinding.ItemSearchResultBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

enum class PointTarget { START, END }

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var map: MapLibreMap? = null
    private var start: P? = null
    private var end: P? = null
    private var route: RouteResult? = null
    private var mode = Mode.DRIVE
    private var pendingMapTarget: PointTarget? = null

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
            if (r[Manifest.permission.ACCESS_FINE_LOCATION] == true) startSimulation()
            else toast("Location permission is required to run the simulation")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.mapView.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(b.cardLocations) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val lp = view.layoutParams as ViewGroup.MarginLayoutParams
            lp.topMargin = statusBarHeight + (12 * resources.displayMetrics.density).toInt()
            view.layoutParams = lp
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(b.cardControls) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp = view.layoutParams as ViewGroup.MarginLayoutParams
            lp.bottomMargin = navBarHeight + (12 * resources.displayMetrics.density).toInt()
            view.layoutParams = lp
            insets
        }

        b.mapView.getMapAsync { m ->
            map = m
            m.setStyle(Style.Builder().fromJson(STYLE_JSON)) { style ->
                style.addSource(GeoJsonSource("route"))
                style.addLayer(
                    LineLayer("route-line", "route").withProperties(
                        lineColor("#0F766E"), lineWidth(5f)
                    )
                )
                style.addSource(GeoJsonSource("points"))
                style.addLayer(
                    CircleLayer("points-layer", "points").withProperties(
                        circleRadius(9f), circleColor(Expression.get("color")),
                        circleStrokeColor("#FFFFFF"), circleStrokeWidth(3f)
                    )
                )
                style.addSource(GeoJsonSource("me"))
                style.addLayer(
                    CircleLayer("me-layer", "me").withProperties(
                        circleRadius(8f), circleColor("#2563EB"),
                        circleStrokeColor("#FFFFFF"), circleStrokeWidth(3f)
                    )
                )
            }
            m.cameraPosition = CameraPosition.Builder().target(LatLng(22.97, 78.65)).zoom(4.0).build()
            m.addOnMapLongClickListener { onLongPress(it); true }
        }

        // Location selection click listeners
        b.rowStart.setOnClickListener { showLocationPickerDialog(PointTarget.START) }
        b.btnSearchStart.setOnClickListener { showLocationPickerDialog(PointTarget.START) }
        b.btnClearStart.setOnClickListener { clearLocationPoint(PointTarget.START) }

        b.rowEnd.setOnClickListener { showLocationPickerDialog(PointTarget.END) }
        b.btnSearchEnd.setOnClickListener { showLocationPickerDialog(PointTarget.END) }
        b.btnClearEnd.setOnClickListener { clearLocationPoint(PointTarget.END) }

        b.btnSwapPoints.setOnClickListener { swapPoints() }

        b.toggleMode.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            mode = when (id) {
                R.id.btnDrive -> Mode.DRIVE
                R.id.btnWalk -> Mode.WALK
                R.id.btnBike -> Mode.BIKE
                R.id.btnTrain -> Mode.TRAIN
                R.id.btnFixed -> Mode.FIXED
                else -> Mode.DRIVE
            }
            applyModeUI(mode)
        }
        b.sliderSpeed.addOnChangeListener { _, v, _ ->
            if (mode == Mode.FIXED) {
                MockLocationService.indoorRadiusM = v.toDouble()
                updateSpeedLabel(v)
            } else {
                MockLocationService.speedKmh = v.toDouble()
                updateSpeedLabel(v)
            }
            updateInfo()
        }
        b.switchJitter.setOnCheckedChangeListener { _, on -> MockLocationService.jitter = on }

        // Initialize default Drive mode
        b.toggleMode.check(R.id.btnDrive)
        mode = Mode.DRIVE
        MockLocationService.speedKmh = 40.0
        MockLocationService.jitter = true
        MockLocationService.isFixedMode = false
        applyModeUI(Mode.DRIVE)

        b.btnPlay.setOnClickListener {
            when (MockLocationService.state.value) {
                SimState.RUNNING -> sendAction(MockLocationService.ACTION_PAUSE)
                SimState.PAUSED -> sendAction(MockLocationService.ACTION_RESUME)
                else -> requestPermsAndStart()
            }
        }
        b.btnStop.setOnClickListener {
            if (MockLocationService.state.value == SimState.PAUSED) {
                sendAction(MockLocationService.ACTION_RESET)
            } else {
                sendAction(MockLocationService.ACTION_STOP)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    MockLocationService.state.collect {
                        updateButtons(it)
                        updateInfo()
                    }
                }
                launch { MockLocationService.progress.collect { b.progress.setProgressCompat((it * 1000).toInt(), true) } }
                launch {
                    MockLocationService.position.collect {
                        showMe(it)
                        if (MockLocationService.state.value == SimState.PAUSED) {
                            updateInfo()
                        }
                    }
                }
                launch { MockLocationService.error.collect { onServiceError(it) } }
            }
        }
    }

    // ---- Location Picker Dialog ----
    private fun showLocationPickerDialog(target: PointTarget) {
        if (MockLocationService.state.value == SimState.RUNNING ||
            MockLocationService.state.value == SimState.PAUSED
        ) {
            toast("Stop simulation before changing locations")
            return
        }

        val dialog = BottomSheetDialog(this)
        val db = DialogLocationPickerBinding.inflate(layoutInflater)
        dialog.setContentView(db.root)

        val targetName = if (target == PointTarget.START) {
            if (mode == Mode.FIXED) "Fixed Location" else "Start Location"
        } else {
            "Destination"
        }
        db.tvPickerTitle.text = "Set $targetName"
        db.tvMapPrompt.text = "Tap below or long-press on the map to set $targetName."

        fun showPanel(search: Boolean, coords: Boolean, map: Boolean) {
            db.panelSearch.visibility = if (search) View.VISIBLE else View.GONE
            db.panelCoords.visibility = if (coords) View.VISIBLE else View.GONE
            db.panelMap.visibility = if (map) View.VISIBLE else View.GONE
        }

        db.togglePickerMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnTabSearch -> showPanel(search = true, coords = false, map = false)
                R.id.btnTabCoords -> {
                    showPanel(search = false, coords = true, map = false)
                    val curr = if (target == PointTarget.START) start else end
                    if (curr != null) {
                        db.etLatitude.setText("%.6f".format(curr.lat))
                        db.etLongitude.setText("%.6f".format(curr.lon))
                    }
                }
                R.id.btnTabMap -> showPanel(search = false, coords = false, map = true)
            }
        }

        // 1. Search Panel
        var searchJob: Job? = null
        db.rvSearchResults.layoutManager = LinearLayoutManager(this)

        fun doSearch(query: String) {
            val q = query.trim()
            if (q.length < 2) {
                db.rvSearchResults.visibility = View.GONE
                db.tvSearchEmpty.visibility = View.VISIBLE
                db.tvSearchEmpty.text = "Search by place name, address, or coordinates"
                db.searchProgress.visibility = View.GONE
                return
            }
            db.searchProgress.visibility = View.VISIBLE
            db.tvSearchEmpty.visibility = View.GONE
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                val results = withContext(Dispatchers.IO) {
                    GeocodingRepository.search(q)
                }
                db.searchProgress.visibility = View.GONE
                if (results.isEmpty()) {
                    db.rvSearchResults.visibility = View.GONE
                    db.tvSearchEmpty.visibility = View.VISIBLE
                    db.tvSearchEmpty.text = "No places found for \"$q\""
                } else {
                    db.tvSearchEmpty.visibility = View.GONE
                    db.rvSearchResults.visibility = View.VISIBLE
                    db.rvSearchResults.adapter = SearchResultAdapter(results) { res ->
                        val p = P(res.lat, res.lon)
                        applyLocationPoint(target, p, res.name)
                        dialog.dismiss()
                    }
                }
            }
        }

        db.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch(db.etSearch.text?.toString().orEmpty())
                true
            } else false
        }

        var debounceJob: Job? = null
        db.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                debounceJob?.cancel()
                debounceJob = lifecycleScope.launch {
                    delay(500)
                    doSearch(s?.toString().orEmpty())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 2. Lat/Long Panel
        db.btnMapCenter.setOnClickListener {
            val center = map?.cameraPosition?.target
            if (center != null) {
                db.etLatitude.setText("%.6f".format(center.latitude))
                db.etLongitude.setText("%.6f".format(center.longitude))
            } else {
                toast("Map not ready yet")
            }
        }

        db.btnPasteCoords.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
            if (clip.isNullOrEmpty()) {
                toast("Clipboard is empty")
                return@setOnClickListener
            }
            val match = Regex("""^([+-]?\d+(?:\.\d+)?)[,\s]+([+-]?\d+(?:\.\d+)?)$""").find(clip)
            if (match != null) {
                db.etLatitude.setText(match.groupValues[1])
                db.etLongitude.setText(match.groupValues[2])
                toast("Pasted coordinates")
            } else {
                db.togglePickerMode.check(R.id.btnTabSearch)
                db.etSearch.setText(clip)
                doSearch(clip)
            }
        }

        db.btnApplyCoords.setOnClickListener {
            val lat = db.etLatitude.text?.toString()?.toDoubleOrNull()
            val lon = db.etLongitude.text?.toString()?.toDoubleOrNull()
            if (lat == null || lat !in -90.0..90.0) {
                toast("Enter valid latitude between -90 and 90")
                return@setOnClickListener
            }
            if (lon == null || lon !in -180.0..180.0) {
                toast("Enter valid longitude between -180 and 180")
                return@setOnClickListener
            }
            val p = P(lat, lon)
            applyLocationPoint(target, p, "%.5f, %.5f".format(lat, lon))
            dialog.dismiss()
        }

        // 3. Map Select Panel
        db.btnConfirmMapSelect.setOnClickListener {
            pendingMapTarget = target
            toast("Long-press on the map to set $targetName")
            dialog.dismiss()
        }

        db.btnPickerClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun applyLocationPoint(target: PointTarget, p: P, label: String? = null) {
        val initialText = label ?: "%.5f, %.5f".format(p.lat, p.lon)
        if (target == PointTarget.START) {
            start = p
            b.tvStartLocation.text = initialText
            b.btnClearStart.visibility = View.VISIBLE
            if (mode == Mode.FIXED) {
                MockLocationService.fixedLocation = p
            }
        } else {
            end = p
            b.tvEndLocation.text = initialText
            b.btnClearEnd.visibility = View.VISIBLE
        }

        refreshPoints()

        // Asynchronously fetch reverse geocoded name if not already provided
        if (label == null || label.startsWith("%.5f".format(p.lat).substring(0, 3))) {
            lifecycleScope.launch {
                val name = withContext(Dispatchers.IO) { GeocodingRepository.reverse(p.lat, p.lon) }
                if (name != null) {
                    if (target == PointTarget.START && start == p) {
                        b.tvStartLocation.text = name
                    } else if (target == PointTarget.END && end == p) {
                        b.tvEndLocation.text = name
                    }
                }
            }
        }

        if (mode == Mode.FIXED) {
            map?.easeCamera(CameraUpdateFactory.newLatLngZoom(LatLng(p.lat, p.lon), 16.0), 600)
            b.tvInfo.text = "Fixed location ready. Tap Start to mock indoor presence."
        } else if (start != null && end != null) {
            fetchRoute()
        } else {
            map?.easeCamera(CameraUpdateFactory.newLatLngZoom(LatLng(p.lat, p.lon), 13.0), 600)
            b.tvInfo.text = if (target == PointTarget.START) {
                "Start set. Choose destination via top bar or long-press map."
            } else {
                "Destination set. Choose start via top bar or long-press map."
            }
        }
        updateButtons(MockLocationService.state.value)
    }

    private fun clearLocationPoint(target: PointTarget) {
        if (MockLocationService.state.value == SimState.RUNNING ||
            MockLocationService.state.value == SimState.PAUSED
        ) {
            toast("Stop simulation before clearing locations")
            return
        }
        if (target == PointTarget.START) {
            start = null
            b.tvStartLocation.text = if (mode == Mode.FIXED) "Fixed Location: Search, lat/long or map" else "Start: Search, lat/long or map"
            b.btnClearStart.visibility = View.GONE
            MockLocationService.fixedLocation = null
        } else {
            end = null
            b.tvEndLocation.text = "Destination: Search, lat/long or map"
            b.btnClearEnd.visibility = View.GONE
        }
        clearRouteLine()
        refreshPoints()
        updateButtons(MockLocationService.state.value)
        updateInfo()
    }

    private fun clearRouteLine() {
        route = null
        map?.style?.getSourceAs<GeoJsonSource>("route")?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
    }

    private fun swapPoints() {
        if (MockLocationService.state.value == SimState.RUNNING ||
            MockLocationService.state.value == SimState.PAUSED
        ) {
            toast("Stop simulation before swapping points")
            return
        }
        val tempStart = start
        val tempStartText = b.tvStartLocation.text
        val tempStartClear = b.btnClearStart.visibility

        start = end
        b.tvStartLocation.text = if (end != null) b.tvEndLocation.text else "Start: Search, lat/long or map"
        b.btnClearStart.visibility = b.btnClearEnd.visibility

        end = tempStart
        b.tvEndLocation.text = if (tempStart != null) tempStartText else "Destination: Search, lat/long or map"
        b.btnClearEnd.visibility = tempStartClear

        refreshPoints()
        if (start != null && end != null) {
            fetchRoute()
        } else {
            clearRouteLine()
            updateButtons(MockLocationService.state.value)
        }
    }

    // ---- map interaction ----
    private fun onLongPress(ll: LatLng) {
        if (MockLocationService.state.value != SimState.IDLE &&
            MockLocationService.state.value != SimState.FINISHED
        ) return
        val p = P(ll.latitude, ll.longitude)

        val target = pendingMapTarget
        if (target != null) {
            pendingMapTarget = null
            applyLocationPoint(target, p)
            return
        }

        if (mode == Mode.FIXED) {
            applyLocationPoint(PointTarget.START, p)
            return
        }

        if (start == null || end != null) {
            clearLocationPoint(PointTarget.END)
            applyLocationPoint(PointTarget.START, p)
            b.tvInfo.text = "Start set. Long-press again or search for destination."
        } else {
            applyLocationPoint(PointTarget.END, p)
        }
    }

    private fun refreshPoints() {
        val f = mutableListOf<Feature>()
        start?.let { f += Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat)).apply { addStringProperty("color", "#16A34A") } }
        if (mode != Mode.FIXED) {
            end?.let { f += Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat)).apply { addStringProperty("color", "#DC2626") } }
        }
        map?.style?.getSourceAs<GeoJsonSource>("points")?.setGeoJson(FeatureCollection.fromFeatures(f))
    }

    private fun showMe(p: P?) {
        val src = map?.style?.getSourceAs<GeoJsonSource>("me") ?: return
        if (p == null) src.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        else src.setGeoJson(Point.fromLngLat(p.lon, p.lat))
    }

    private fun applyModeUI(m: Mode) {
        if (m == Mode.FIXED) {
            MockLocationService.isFixedMode = true
            b.rowEnd.visibility = View.GONE
            b.btnSwapPoints.visibility = View.GONE
            b.progress.visibility = View.GONE
            b.tvStartLocation.text = if (start != null) {
                b.tvStartLocation.text
            } else {
                "Fixed Location: Search, lat/long or map"
            }
            b.sliderSpeed.valueFrom = 0f
            b.sliderSpeed.valueTo = 30f
            b.sliderSpeed.stepSize = 1f
            b.sliderSpeed.value = MockLocationService.indoorRadiusM.toFloat().coerceIn(0f, 30f)
            updateSpeedLabel(b.sliderSpeed.value)
            b.switchJitter.text = "Realistic GPS jitter (indoor walking)"

            clearRouteLine()
            refreshPoints()
            if (start != null) {
                MockLocationService.fixedLocation = start
                map?.easeCamera(CameraUpdateFactory.newLatLngZoom(LatLng(start!!.lat, start!!.lon), 16.0), 600)
            }
            updateButtons(MockLocationService.state.value)
            updateInfo()
        } else {
            MockLocationService.isFixedMode = false
            b.rowEnd.visibility = View.VISIBLE
            b.btnSwapPoints.visibility = View.VISIBLE
            b.progress.visibility = View.VISIBLE
            b.tvStartLocation.text = if (start != null) {
                b.tvStartLocation.text
            } else {
                "Start: Search, lat/long or map"
            }
            b.sliderSpeed.valueFrom = 1f
            b.sliderSpeed.valueTo = 200f
            b.sliderSpeed.stepSize = 1f
            b.sliderSpeed.value = m.defaultKmh.toFloat()
            updateSpeedLabel(b.sliderSpeed.value)
            MockLocationService.speedKmh = m.defaultKmh.toDouble()
            b.switchJitter.text = "Realistic GPS jitter"

            refreshPoints()
            if (start != null && end != null) {
                fetchRoute()
            }
            updateButtons(MockLocationService.state.value)
            updateInfo()
        }
    }

    private fun updateSpeedLabel(v: Float) {
        if (mode == Mode.FIXED) {
            val r = v.toInt()
            b.tvSpeed.text = if (r == 0) {
                "Fixed spot only (0m wander, subtle antenna noise)"
            } else {
                "Indoor wander: ~${r}m radius (walking inside home)"
            }
        } else {
            b.tvSpeed.text = "${v.toInt()} km/h"
        }
    }

    // ---- routing ----
    private fun fetchRoute() {
        val s = start ?: return
        val e = end ?: return
        b.tvInfo.text = "Finding ${mode.name.lowercase()} route…"
        lifecycleScope.launch {
            val res = withContext(Dispatchers.IO) { runCatching { RouteRepository.fetch(mode, s, e) } }
            res.onSuccess { r ->
                route = r
                map?.style?.getSourceAs<GeoJsonSource>("route")?.setGeoJson(
                    LineString.fromLngLats(r.points.map { Point.fromLngLat(it.lon, it.lat) })
                )
                val bounds = LatLngBounds.Builder().includes(r.points.map { LatLng(it.lat, it.lon) }).build()
                map?.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 160), 800)
                updateInfo()
            }.onFailure {
                route = null
                b.tvInfo.text = "Route failed: ${it.message}"
            }
            updateButtons(MockLocationService.state.value)
        }
    }

    private fun updateInfo() {
        val s = MockLocationService.state.value
        val pos = MockLocationService.position.value

        if (mode == Mode.FIXED) {
            when (s) {
                SimState.RUNNING -> {
                    val coordStr = if (pos != null) "%.5f, %.5f".format(pos.lat, pos.lon) else ""
                    val radius = b.sliderSpeed.value.toInt()
                    b.tvInfo.text = "Fixed mode active • Walking drift (~${radius}m radius) • $coordStr"
                }
                SimState.PAUSED -> {
                    val coordStr = if (pos != null) "%.5f, %.5f".format(pos.lat, pos.lon) else ""
                    b.tvInfo.text = "Paused at fixed location: $coordStr"
                    if (pos != null) {
                        lifecycleScope.launch {
                            val name = withContext(Dispatchers.IO) { GeocodingRepository.reverse(pos.lat, pos.lon) }
                            if (name != null && MockLocationService.state.value == SimState.PAUSED) {
                                b.tvInfo.text = "Paused at: $name · $coordStr"
                            }
                        }
                    }
                }
                else -> {
                    if (start == null) {
                        b.tvInfo.text = "Select a fixed location by search, lat/long or map."
                    } else {
                        b.tvInfo.text = "Fixed location ready. Tap Start to mock indoor presence."
                    }
                }
            }
            return
        }

        if (s == SimState.PAUSED && pos != null) {
            val pct = (MockLocationService.progress.value * 100).toInt()
            val coordStr = "%.5f, %.5f".format(pos.lat, pos.lon)
            b.tvInfo.text = "Paused at: $coordStr ($pct% completed)"
            lifecycleScope.launch {
                val name = withContext(Dispatchers.IO) { GeocodingRepository.reverse(pos.lat, pos.lon) }
                if (name != null && MockLocationService.state.value == SimState.PAUSED) {
                    b.tvInfo.text = "Paused at: $name · $coordStr ($pct%)"
                }
            }
            return
        }

        val r = route
        if (r == null) {
            if (start == null && end == null) {
                b.tvInfo.text = "Choose points by search, lat/long or long-press on map."
            } else if (start != null && end == null) {
                b.tvInfo.text = "Start set. Choose destination to route, or select Fixed mode."
            } else if (start == null && end != null) {
                b.tvInfo.text = "Destination set. Select start location."
            }
            return
        }

        val km = r.distanceM / 1000
        val speed = b.sliderSpeed.value.coerceAtLeast(1f)
        val mins = (r.distanceM / (speed / 3.6) / 60).toInt()
        b.tvInfo.text = "%.1f km · about %d min at %d km/h".format(km, mins, speed.toInt())
    }

    // ---- simulation control ----
    private fun updateButtons(s: SimState) {
        b.btnPlay.text = when (s) {
            SimState.RUNNING -> "Pause"
            SimState.PAUSED -> "Resume"
            else -> "Start"
        }
        b.btnStop.text = when (s) {
            SimState.PAUSED -> "Reset"
            else -> "Stop"
        }
        val canStart = if (mode == Mode.FIXED) {
            start != null || s == SimState.RUNNING || s == SimState.PAUSED
        } else {
            route != null || s == SimState.RUNNING || s == SimState.PAUSED
        }
        b.btnPlay.isEnabled = canStart
        b.btnStop.isEnabled = s == SimState.RUNNING || s == SimState.PAUSED
    }

    private fun requestPermsAndStart() {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        val fineOk = ContextCompat.checkSelfPermission(this, perms[0]) == PackageManager.PERMISSION_GRANTED
        if (fineOk) startSimulation() else permLauncher.launch(perms.toTypedArray())
    }

    private fun startSimulation() {
        if (mode == Mode.FIXED) {
            val s = start
            if (s == null) {
                toast("Select a fixed location first")
                return
            }
            MockLocationService.isFixedMode = true
            MockLocationService.fixedLocation = s
            MockLocationService.route = listOf(s)
            MockLocationService.indoorRadiusM = b.sliderSpeed.value.toDouble()
            MockLocationService.jitter = b.switchJitter.isChecked
            ContextCompat.startForegroundService(
                this,
                Intent(this, MockLocationService::class.java).setAction(MockLocationService.ACTION_START)
            )
            return
        }

        val r = route ?: return
        MockLocationService.isFixedMode = false
        MockLocationService.route = r.points
        MockLocationService.speedKmh = b.sliderSpeed.value.toDouble()
        MockLocationService.jitter = b.switchJitter.isChecked
        ContextCompat.startForegroundService(
            this,
            Intent(this, MockLocationService::class.java).setAction(MockLocationService.ACTION_START)
        )
    }

    private fun sendAction(action: String) {
        startService(Intent(this, MockLocationService::class.java).setAction(action))
    }

    private fun onServiceError(code: String) {
        if (code == "mock_not_selected") {
            MaterialAlertDialogBuilder(this)
                .setTitle("Set Marg as mock location app")
                .setMessage("Enable Developer options, then open \"Select mock location app\" and choose Marg.")
                .setPositiveButton("Open settings") { _, _ ->
                    try { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
                    catch (_: Exception) { toast("Open Settings > Developer options manually") }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else toast(code)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    // ---- MapView lifecycle ----
    override fun onStart() { super.onStart(); b.mapView.onStart() }
    override fun onResume() { super.onResume(); b.mapView.onResume() }
    override fun onPause() { b.mapView.onPause(); super.onPause() }
    override fun onStop() { b.mapView.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); b.mapView.onLowMemory() }
    override fun onDestroy() { b.mapView.onDestroy(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); b.mapView.onSaveInstanceState(outState) }

    companion object {
        // OSM raster tiles for development. For production, use a self-hosted or commercial vector tile source.
        private const val STYLE_JSON = """{"version":8,"sources":{"osm":{"type":"raster","tiles":["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],"tileSize":256,"attribution":"© OpenStreetMap contributors","maxzoom":19}},"layers":[{"id":"osm","type":"raster","source":"osm"}]}"""
    }
}

class SearchResultAdapter(
    private val items: List<SearchResult>,
    private val onClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.VH>() {
    class VH(val b: ItemSearchResultBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.tvName.text = item.name
        holder.b.tvDetails.text = item.details
        holder.b.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
