package com.rahulbaberwal.marg

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val Context.dataStore by preferencesDataStore(name = "marg_prefs")

enum class MapLayer(val displayName: String, val attribution: String) {
    STANDARD("Standard", "© OpenStreetMap contributors"),
    DARK("Dark", "OpenFreeMap · © OpenStreetMap contributors"),
    TERRAIN("Terrain", "Map data: © OpenStreetMap, SRTM | Style: OpenTopoMap"),
    SATELLITE("Satellite", "Tiles © Esri — Source: Esri, Maxar, Earthstar")
}

enum class AppThemeMode(val title: String) {
    SYSTEM("System (Auto)"),
    LIGHT("Light"),
    DARK("Dark")
}

data class MargUiState(
    val simState: SimState = SimState.IDLE,
    val position: P? = null,
    val progress: Float = 0f,
    val route: RouteResult? = null,
    val mode: Mode = Mode.DRIVE,
    val speedKmh: Float = 40f,
    val indoorRadiusM: Float = 10f,
    val jitter: Boolean = true,
    val startPoint: P? = null,
    val endPoint: P? = null,
    val waypoints: List<P> = emptyList(),
    val waypointLabels: List<String> = emptyList(),
    val startLabel: String = "",
    val endLabel: String = "",
    val selectedLayer: MapLayer = MapLayer.STANDARD,
    val searchResults: List<SearchResult> = emptyList(),
    val recentSearches: List<SearchResult> = emptyList(),
    val error: String? = null,
    val isRouting: Boolean = false,
    val isSearching: Boolean = false,
    val isMockLocationNotSet: Boolean = false,
    val activePinChip: String? = null, // "start", "end", or waypoint index
)

class MargViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx get() = getApplication<Application>()

    private val _mode = MutableStateFlow(Mode.DRIVE)
    private val _speedKmh = MutableStateFlow(40f)
    private val _indoorRadiusM = MutableStateFlow(10f)
    private val _jitter = MutableStateFlow(true)
    private val _startPoint = MutableStateFlow<P?>(null)
    private val _endPoint = MutableStateFlow<P?>(null)
    private val _waypoints = MutableStateFlow<List<P>>(emptyList())
    private val _waypointLabels = MutableStateFlow<List<String>>(emptyList())
    private val _startLabel = MutableStateFlow("")
    private val _endLabel = MutableStateFlow("")
    private val _selectedLayer = MutableStateFlow(MapLayer.STANDARD)
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    private val _recentSearches = MutableStateFlow<List<SearchResult>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)
    private val _isRouting = MutableStateFlow(false)
    private val _isSearching = MutableStateFlow(false)
    private val _isMockLocationNotSet = MutableStateFlow(false)
    private val _activePinChip = MutableStateFlow<String?>(null)
    private val _route = MutableStateFlow<RouteResult?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _hasSeenFeatureGuide = MutableStateFlow<Boolean?>(null)
    val hasSeenFeatureGuide: StateFlow<Boolean?> = _hasSeenFeatureGuide.asStateFlow()
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val KEY_LAYER = intPreferencesKey("map_layer")
    private val KEY_SPEED = floatPreferencesKey("speed_kmh")
    private val KEY_INDOOR_RADIUS = floatPreferencesKey("indoor_radius")
    private val KEY_JITTER = intPreferencesKey("jitter")
    private val KEY_MODE = stringPreferencesKey("mode")
    private val KEY_HAS_SEEN_GUIDE = booleanPreferencesKey("has_seen_feature_guide")
    private val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")

    // Combined UI State
    val uiState: StateFlow<MargUiState> = combine(
        MockLocationService.state,
        MockLocationService.position,
        MockLocationService.progress,
        _route,
        _mode
    ) { state, pos, prog, route, mode ->
        MargUiState(
            simState = state,
            position = pos,
            progress = prog,
            route = route,
            mode = mode
        )
    }.combine(_speedKmh) { s, sp -> s.copy(speedKmh = sp) }
     .combine(_indoorRadiusM) { s, ir -> s.copy(indoorRadiusM = ir) }
     .combine(_jitter) { s, j -> s.copy(jitter = j) }
     .combine(_startPoint) { s, sp -> s.copy(startPoint = sp) }
     .combine(_endPoint) { s, ep -> s.copy(endPoint = ep) }
     .combine(_waypoints) { s, wp -> s.copy(waypoints = wp) }
     .combine(_waypointLabels) { s, wl -> s.copy(waypointLabels = wl) }
     .combine(_startLabel) { s, sl -> s.copy(startLabel = sl) }
     .combine(_endLabel) { s, el -> s.copy(endLabel = el) }
     .combine(_selectedLayer) { s, l -> s.copy(selectedLayer = l) }
     .combine(_searchResults) { s, sr -> s.copy(searchResults = sr) }
     .combine(_recentSearches) { s, rc -> s.copy(recentSearches = rc) }
     .combine(_error) { s, err -> s.copy(error = err) }
     .combine(_isRouting) { s, r -> s.copy(isRouting = r) }
     .combine(_isSearching) { s, srch -> s.copy(isSearching = srch) }
     .combine(_isMockLocationNotSet) { s, mls -> s.copy(isMockLocationNotSet = mls) }
     .combine(_activePinChip) { s, chip -> s.copy(activePinChip = chip) }
     .stateIn(
         viewModelScope,
         SharingStarted.WhileSubscribed(5000),
         MargUiState()
     )

    init {
        // Collect service error flow
        viewModelScope.launch {
            MockLocationService.error.collect { errCode ->
                if (errCode == "mock_not_selected") {
                    _isMockLocationNotSet.value = true
                } else {
                    _error.value = errCode
                }
            }
        }

        // Restore persisted preferences
        viewModelScope.launch {
            ctx.dataStore.data.collect { prefs ->
                _hasSeenFeatureGuide.value = prefs[KEY_HAS_SEEN_GUIDE] ?: false
                prefs[KEY_THEME_MODE]?.let { tm ->
                    AppThemeMode.entries.find { it.name == tm }?.let { _themeMode.value = it }
                }
                prefs[KEY_LAYER]?.let { l ->
                    _selectedLayer.value = MapLayer.entries.getOrNull(l) ?: MapLayer.STANDARD
                }
                prefs[KEY_SPEED]?.let { _speedKmh.value = it }
                prefs[KEY_INDOOR_RADIUS]?.let { _indoorRadiusM.value = it }
                prefs[KEY_JITTER]?.let { _jitter.value = it != 0 }
                prefs[KEY_MODE]?.let { m ->
                    Mode.entries.find { it.name == m }?.let {
                        _mode.value = it
                        MockLocationService.isFixedMode = (it == Mode.FIXED)
                    }
                }
            }
        }

        // Debounced search (350ms)
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            _searchQuery
                .debounce(350L)
                .collect { q ->
                    val query = q.trim()
                    if (query.length < 2) {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                        return@collect
                    }
                    _isSearching.value = true
                    val results = withContext(Dispatchers.IO) {
                        val biasLat = _startPoint.value?.lat ?: 20.5937
                        val biasLon = _startPoint.value?.lon ?: 78.9629
                        runCatching {
                            GeocodingRepository.search(query, userLat = biasLat, userLon = biasLon)
                        }.getOrElse { emptyList() }
                    }
                    _searchResults.value = results
                    _isSearching.value = false
                }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun setStart(p: P, label: String? = null) {
        _startPoint.value = p
        val initialLabel = label ?: "%.5f, %.5f".format(p.lat, p.lon)
        _startLabel.value = initialLabel
        if (_mode.value == Mode.FIXED) {
            MockLocationService.fixedLocation = p
        }
        addRecent(SearchResult(initialLabel, "%.5f, %.5f".format(p.lat, p.lon), p.lat, p.lon))

        if (label == null) {
            resolveReverseGeocode(p) { name ->
                if (_startPoint.value == p) _startLabel.value = name
            }
        }

        if (_mode.value != Mode.FIXED && _endPoint.value != null) {
            fetchRoute()
        }
    }

    fun setEnd(p: P, label: String? = null) {
        _endPoint.value = p
        val initialLabel = label ?: "%.5f, %.5f".format(p.lat, p.lon)
        _endLabel.value = initialLabel
        addRecent(SearchResult(initialLabel, "%.5f, %.5f".format(p.lat, p.lon), p.lat, p.lon))

        if (label == null) {
            resolveReverseGeocode(p) { name ->
                if (_endPoint.value == p) _endLabel.value = name
            }
        }

        if (_startPoint.value != null) {
            fetchRoute()
        }
    }

    fun addWaypoint(p: P, index: Int = -1, initialLabel: String = "") {
        val currentPoints = _waypoints.value.toMutableList()
        val currentLabels = _waypointLabels.value.toMutableList()
        while (currentLabels.size < currentPoints.size) currentLabels.add("")
        val insertIdx = if (index in 0..currentPoints.size) index else currentPoints.size
        currentPoints.add(insertIdx, p)
        currentLabels.add(insertIdx, initialLabel)
        _waypoints.value = currentPoints
        _waypointLabels.value = currentLabels

        if (initialLabel.isEmpty()) {
            resolveReverseGeocode(p) { name ->
                val updated = _waypointLabels.value.toMutableList()
                if (insertIdx in updated.indices && _waypoints.value.getOrNull(insertIdx) == p) {
                    updated[insertIdx] = name
                    _waypointLabels.value = updated
                }
            }
        }
        if (_startPoint.value != null && _endPoint.value != null) {
            fetchRoute()
        }
    }

    fun removeWaypoint(index: Int) {
        val currentPoints = _waypoints.value.toMutableList()
        val currentLabels = _waypointLabels.value.toMutableList()
        if (index in currentPoints.indices) {
            currentPoints.removeAt(index)
            if (index in currentLabels.indices) {
                currentLabels.removeAt(index)
            }
            _waypoints.value = currentPoints
            _waypointLabels.value = currentLabels
            if (_startPoint.value != null && _endPoint.value != null) {
                fetchRoute()
            }
        }
    }

    fun updateWaypoint(index: Int, p: P, label: String = "") {
        val currentPoints = _waypoints.value.toMutableList()
        val currentLabels = _waypointLabels.value.toMutableList()
        while (currentLabels.size < currentPoints.size) currentLabels.add("")
        if (index in currentPoints.indices) {
            currentPoints[index] = p
            currentLabels[index] = label
            _waypoints.value = currentPoints
            _waypointLabels.value = currentLabels
            if (label.isEmpty()) {
                resolveReverseGeocode(p) { name ->
                    val updated = _waypointLabels.value.toMutableList()
                    if (index in updated.indices && _waypoints.value.getOrNull(index) == p) {
                        updated[index] = name
                        _waypointLabels.value = updated
                    }
                }
            }
            if (_startPoint.value != null && _endPoint.value != null) {
                fetchRoute()
            }
        }
    }

    fun moveWaypoint(fromIndex: Int, toIndex: Int) {
        val currentPoints = _waypoints.value.toMutableList()
        val currentLabels = _waypointLabels.value.toMutableList()
        while (currentLabels.size < currentPoints.size) currentLabels.add("")
        if (fromIndex in currentPoints.indices && toIndex in currentPoints.indices && fromIndex != toIndex) {
            val p = currentPoints.removeAt(fromIndex)
            currentPoints.add(toIndex, p)
            val l = currentLabels.removeAt(fromIndex)
            currentLabels.add(toIndex, l)
            _waypoints.value = currentPoints
            _waypointLabels.value = currentLabels
            if (_startPoint.value != null && _endPoint.value != null) {
                fetchRoute()
            }
        }
    }

    fun clearWaypoints() {
        _waypoints.value = emptyList()
        _waypointLabels.value = emptyList()
        if (_startPoint.value != null && _endPoint.value != null) {
            fetchRoute()
        }
    }

    fun clearStart() {
        _startPoint.value = null
        _startLabel.value = ""
        _route.value = null
        MockLocationService.fixedLocation = null
    }

    fun clearEnd() {
        _endPoint.value = null
        _endLabel.value = ""
        _route.value = null
    }

    fun swapPoints() {
        val s = _startPoint.value
        val e = _endPoint.value
        val sl = _startLabel.value
        val el = _endLabel.value
        _startPoint.value = e
        _startLabel.value = el
        _endPoint.value = s
        _endLabel.value = sl
        val currentPoints = _waypoints.value
        val currentLabels = _waypointLabels.value
        if (currentPoints.isNotEmpty()) {
            _waypoints.value = currentPoints.reversed()
            _waypointLabels.value = currentLabels.reversed()
        }
        if (s != null && e != null) {
            fetchRoute()
        } else {
            _route.value = null
        }
    }

    fun setMode(m: Mode) {
        _mode.value = m
        MockLocationService.isFixedMode = (m == Mode.FIXED)
        when (m) {
            Mode.FIXED -> {
                _endPoint.value = null
                _endLabel.value = ""
                _waypoints.value = emptyList()
                _waypointLabels.value = emptyList()
                _route.value = null
                _startPoint.value?.let { MockLocationService.fixedLocation = it }
            }
            else -> {
                _speedKmh.value = m.defaultKmh.toFloat()
                MockLocationService.speedKmh = m.defaultKmh.toDouble()
                if (_startPoint.value != null && _endPoint.value != null) {
                    fetchRoute()
                }
            }
        }
        viewModelScope.launch {
            ctx.dataStore.edit { it[KEY_MODE] = m.name }
        }
    }

    fun setSpeed(speed: Float) {
        _speedKmh.value = speed
        MockLocationService.speedKmh = speed.toDouble()
        viewModelScope.launch { ctx.dataStore.edit { it[KEY_SPEED] = speed } }
    }

    fun setIndoorRadius(radius: Float) {
        _indoorRadiusM.value = radius
        MockLocationService.indoorRadiusM = radius.toDouble()
        viewModelScope.launch { ctx.dataStore.edit { it[KEY_INDOOR_RADIUS] = radius } }
    }

    fun toggleJitter(enabled: Boolean) {
        _jitter.value = enabled
        MockLocationService.jitter = enabled
        viewModelScope.launch { ctx.dataStore.edit { it[KEY_JITTER] = if (enabled) 1 else 0 } }
    }

    fun setLayer(layer: MapLayer) {
        _selectedLayer.value = layer
        viewModelScope.launch { ctx.dataStore.edit { it[KEY_LAYER] = layer.ordinal } }
    }

    fun showPinChip(chipId: String?) {
        _activePinChip.value = chipId
    }

    fun fetchRoute() {
        val s = _startPoint.value ?: return
        val e = _endPoint.value ?: return
        val m = _mode.value
        if (m == Mode.FIXED) return

        _isRouting.value = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { RouteRepository.fetch(m, s, e, _waypoints.value) }
            }
            result.onSuccess { r ->
                _route.value = r
                _error.value = null
            }.onFailure { err ->
                _route.value = null
                _error.value = "Routing error: ${err.message ?: "Unable to find path"}"
            }
            _isRouting.value = false
        }
    }

    fun startSim() {
        val mode = _mode.value
        MockLocationService.startLabel = _startLabel.value.ifEmpty { if (mode == Mode.FIXED) "Fixed Location" else "Start location" }
        MockLocationService.endLabel = _endLabel.value.ifEmpty { "Destination" }
        MockLocationService.modeName = mode.name.lowercase().replaceFirstChar { it.uppercase() }

        if (mode == Mode.FIXED) {
            val s = _startPoint.value ?: run {
                _error.value = "Select a fixed location first"
                return
            }
            MockLocationService.isFixedMode = true
            MockLocationService.fixedLocation = s
            MockLocationService.route = listOf(s)
            MockLocationService.indoorRadiusM = _indoorRadiusM.value.toDouble()
            MockLocationService.jitter = _jitter.value
        } else {
            val r = _route.value ?: run {
                _error.value = "Set start and destination first to generate a route"
                return
            }
            MockLocationService.isFixedMode = false
            MockLocationService.route = r.points
            MockLocationService.speedKmh = _speedKmh.value.toDouble()
            MockLocationService.jitter = _jitter.value
        }
        ContextCompat.startForegroundService(
            ctx,
            Intent(ctx, MockLocationService::class.java).setAction(MockLocationService.ACTION_START)
        )
    }

    fun pauseSim() = sendServiceAction(MockLocationService.ACTION_PAUSE)
    fun resumeSim() = sendServiceAction(MockLocationService.ACTION_RESUME)
    fun stopSim() = sendServiceAction(MockLocationService.ACTION_STOP)
    fun resetSim() = sendServiceAction(MockLocationService.ACTION_RESET)

    fun dismissError() { _error.value = null }
    fun dismissMockLocationCard() { _isMockLocationNotSet.value = false }
    fun markMockLocationNotSet() { _isMockLocationNotSet.value = true }

    fun setFeatureGuideDontShowAgain() {
        viewModelScope.launch {
            ctx.dataStore.edit { it[KEY_HAS_SEEN_GUIDE] = true }
            _hasSeenFeatureGuide.value = true
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            ctx.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
        }
    }

    fun recheckMockLocation() {
        val isSelected = DeveloperSettingsHelper.isMockLocationAppSelected(ctx)
        _isMockLocationNotSet.value = !isSelected
    }

    private fun sendServiceAction(action: String) {
        ctx.startService(Intent(ctx, MockLocationService::class.java).setAction(action))
    }

    private fun addRecent(res: SearchResult) {
        val list = _recentSearches.value.toMutableList()
        list.removeAll { it.lat == res.lat && it.lon == res.lon }
        list.add(0, res)
        _recentSearches.value = list.take(10)
    }

    private fun resolveReverseGeocode(p: P, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) {
                runCatching { GeocodingRepository.reverse(p.lat, p.lon) }.getOrNull()
            }
            if (name != null) onResult(name)
        }
    }
}
