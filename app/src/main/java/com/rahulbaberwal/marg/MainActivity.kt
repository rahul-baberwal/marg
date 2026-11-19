package com.rahulbaberwal.marg

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rahulbaberwal.marg.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
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

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var map: MapLibreMap? = null
    private var start: P? = null
    private var end: P? = null
    private var route: RouteResult? = null
    private var mode = Mode.DRIVE

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

        b.toggleMode.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            mode = when (id) {
                R.id.btnWalk -> Mode.WALK
                R.id.btnBike -> Mode.BIKE
                else -> Mode.DRIVE
            }
            b.sliderSpeed.value = mode.defaultKmh.toFloat()
            if (start != null && end != null) fetchRoute()
        }
        b.sliderSpeed.addOnChangeListener { _, v, _ ->
            MockLocationService.speedKmh = v.toDouble()
            b.tvSpeed.text = "${v.toInt()} km/h"
            updateInfo()
        }
        b.switchJitter.setOnCheckedChangeListener { _, on -> MockLocationService.jitter = on }

        b.btnPlay.setOnClickListener {
            when (MockLocationService.state.value) {
                SimState.RUNNING -> sendAction(MockLocationService.ACTION_PAUSE)
                SimState.PAUSED -> sendAction(MockLocationService.ACTION_RESUME)
                else -> requestPermsAndStart()
            }
        }
        b.btnStop.setOnClickListener { sendAction(MockLocationService.ACTION_STOP) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { MockLocationService.state.collect { updateButtons(it) } }
                launch { MockLocationService.progress.collect { b.progress.setProgressCompat((it * 1000).toInt(), true) } }
                launch { MockLocationService.position.collect { showMe(it) } }
                launch { MockLocationService.error.collect { onServiceError(it) } }
            }
        }
    }

    // ---- map interaction ----
    private fun onLongPress(ll: LatLng) {
        if (MockLocationService.state.value != SimState.IDLE &&
            MockLocationService.state.value != SimState.FINISHED
        ) return
        val p = P(ll.latitude, ll.longitude)
        if (start == null || end != null) {
            start = p; end = null; route = null
            map?.style?.getSourceAs<GeoJsonSource>("route")?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            b.tvInfo.text = "Start set. Long-press again to set the destination."
        } else {
            end = p
        }
        refreshPoints()
        updateButtons(MockLocationService.state.value)
        if (start != null && end != null) fetchRoute()
    }

    private fun refreshPoints() {
        val f = mutableListOf<Feature>()
        start?.let { f += Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat)).apply { addStringProperty("color", "#16A34A") } }
        end?.let { f += Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat)).apply { addStringProperty("color", "#DC2626") } }
        map?.style?.getSourceAs<GeoJsonSource>("points")?.setGeoJson(FeatureCollection.fromFeatures(f))
    }

    private fun showMe(p: P?) {
        val src = map?.style?.getSourceAs<GeoJsonSource>("me") ?: return
        if (p == null) src.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        else src.setGeoJson(Point.fromLngLat(p.lon, p.lat))
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
        val r = route ?: return
        val km = r.distanceM / 1000
        val mins = (r.distanceM / (b.sliderSpeed.value / 3.6) / 60).toInt()
        b.tvInfo.text = "%.1f km · about %d min at %d km/h".format(km, mins, b.sliderSpeed.value.toInt())
    }

    // ---- simulation control ----
    private fun updateButtons(s: SimState) {
        b.btnPlay.text = when (s) { SimState.RUNNING -> "Pause"; SimState.PAUSED -> "Resume"; else -> "Start" }
        b.btnPlay.isEnabled = route != null || s == SimState.RUNNING || s == SimState.PAUSED
        b.btnStop.isEnabled = s == SimState.RUNNING || s == SimState.PAUSED
    }

    private fun requestPermsAndStart() {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        val fineOk = ContextCompat.checkSelfPermission(this, perms[0]) == PackageManager.PERMISSION_GRANTED
        if (fineOk) startSimulation() else permLauncher.launch(perms.toTypedArray())
    }

    private fun startSimulation() {
        val r = route ?: return
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
    override fun onSaveInstanceState(out: Bundle) { super.onSaveInstanceState(out); b.mapView.onSaveInstanceState(out) }

    companion object {
        // OSM raster tiles for development. For production, use a self-hosted or commercial vector tile source.
        private const val STYLE_JSON = """{"version":8,"sources":{"osm":{"type":"raster","tiles":["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],"tileSize":256,"attribution":"© OpenStreetMap contributors","maxzoom":19}},"layers":[{"id":"osm","type":"raster","source":"osm"}]}"""
    }
}
