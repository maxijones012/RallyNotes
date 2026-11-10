package ar.com.rallygpsai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.media.AudioAttributes
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import kotlin.concurrent.thread
import kotlin.math.*

/** MVP 0.1: navegación GPS foreground, ruta vía OSRM de DEMO, GPX, voz y simulación.
 * Pacenotes aproximadas: no son instrucciones de conducción deportiva en vía pública.
 */
class MainActivity : Activity(), LocationListener {
    private lateinit var mapView: MapView
    private var map: MapLibreMap? = null
    private var plan: RoutePlan? = null
    private var automaticNotePlan: RoutePlan? = null
    private val noteEdits = mutableListOf<NoteCorrection>()
    private val noteEditsStore by lazy { NoteEditStore(this) }
    private var recceNoteIndex: Int? = null
    private var recceMapSelection = false
    private var gps: GeoPoint? = null
    private var lastLocation: Location? = null
    private var target: GeoPoint? = null
    private var chosenOrigin: GeoPoint? = null
    private val intermediatePoints = mutableListOf<GeoPoint>()
    private var mapSelectMode = 0 // 0 destino, 1 origen, 2 parada, 3 reductor manual
    private var destinationTapArmed = false // activado sólo desde PUNTOS → señalar destino
    private var chosenDestinationText = "PUNTO MARCADO"
    private var chosenDestinationApproximate = false
    private var navigator: NavigationEngine? = null
    private var controlsVisible = false
    private lateinit var locationManager: LocationManager
    private var navigating = false
    private var demo = false
    private var demoPaused = false
    private var demoTickGeneration = 0L
    private var routeDestination: GeoPoint? = null
    private var pendingStops = listOf<GeoPoint>()
    private val reroutePolicy = ReroutePolicy()
    private var navigationSession = 0L
    private var voiceEngineSettingsOpened = false
    private var map3d = true
    private var mapVisualStyle = RallyMapStyle.DARK
    private val drivingCamera = DrivingCamera()
    // Avoid uploading image textures to MapLibre on every GPS fix (often 1 Hz).
    private var vehicleImageStyle: Style? = null
    private val vehicleImageKeys = mutableSetOf<String>()
    private var followRouteCamera = true
    private var lastRenderedProgress = -1000.0
    private var voiceOn = true
    private var appLanguage = "es-AR"
    private var warnFactor = 1
    private var positionIndex = 0
    private var lastSpoken = -1
    private var currentNote = 0
    private var lastNoteDistanceM = 0.0
    private var lastRemainingMetres = 0.0
    private val contextualCopilot = ContextCopilot()
    private val manualBumpStore by lazy { ManualTrafficCalmingStore(this) }
    private var roadBumps = emptyList<RouteBump>()
    private val announcedBumps = mutableSetOf<String>()
    private var bumpLookupSession = 0L
    private lateinit var bumpBanner: TextView
    private var lastPriorityNoteAtMs = -1L
    private var contextualCommentIndex = 0
    private var firstGpsCamera = true
    private var demoPosition = 0.0
    private var demoSpeed = 55.0 / 3.6
    private var countdownSequence = 0L
    private var countdownDialog: android.app.AlertDialog? = null
    private var realTripRecorder: RealTripRecorder? = null
    private val tripHistory by lazy { TripHistoryStore(this) }
    private var speakReady = false
    private var tts: TextToSpeech? = null
    private val rallyAudio by lazy { RallyAudioPlayer(this) }
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var searchInput: EditText
    private lateinit var banner: TextView
    private lateinit var noteTitle: TextView
    private lateinit var noteDetail: TextView
    private lateinit var noteBadge: TextView
    private lateinit var noteAccent: View
    private lateinit var noteCard: LinearLayout
    private lateinit var nextTurnDistance: LinearLayout
    private lateinit var distanceNumber: TextView
    private lateinit var distanceUnit: TextView
    private lateinit var primaryNoteTile: RallyNoteTile
    private lateinit var linkedNoteTile: RallyNoteTile
    private lateinit var linkedNoteLabel: TextView
    private lateinit var afterNextHint: TextView
    private lateinit var linkedNoteColumn: LinearLayout
    private lateinit var info: TextView
    private lateinit var tripDistance: TextView
    private lateinit var searchRow: LinearLayout
    private lateinit var searchButton: TextView
    private lateinit var pointsButton: TextView
    private lateinit var settingsButton: TextView
    private lateinit var historyButton: TextView
    private lateinit var recceButton: TextView
    private lateinit var nextCurveCaption: TextView
    private lateinit var destinationIndicator: TextView
    private lateinit var toolsRow: LinearLayout
    private lateinit var action: TextView
    private lateinit var gpsButton: TextView
    private lateinit var modeButton: TextView
    private lateinit var voiceButton: TextView
    private lateinit var floatingControls: LinearLayout
    private lateinit var bottomPanel: LinearLayout
    private lateinit var nextButton: TextView
    private val bg = "#0F1726"
    private val panel = "#172336"
    private val sky = "#22D3EE"
    private val white = "#F8FAFC"
    private val muted = "#9AA9BD"
    private val prefs by lazy { getSharedPreferences("rally_v1", MODE_PRIVATE) }
    private fun tr(key: String) = RallyLanguage.text(appLanguage, key)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0B1019")
        window.navigationBarColor = Color.parseColor("#0B1019")
        MapLibre.getInstance(this)
        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        appLanguage = RallyLanguage.normalized(prefs.getString(RallyLanguage.KEY, RallyLanguage.defaultFor(Locale.getDefault())))
        voiceOn = prefs.getBoolean("voice", true)
        warnFactor = prefs.getInt("anticipation", 1)
        mapVisualStyle = RallyMapStyle.fromStored(prefs.getString("map_visual_style", "dark"))
        buildUI()
        mapView.getMapAsync { loaded ->
            map = loaded
            loaded.uiSettings.isAttributionEnabled = true
            loaded.uiSettings.isLogoEnabled = true
            loaded.uiSettings.isCompassEnabled = true
            installMapStyle(loaded)
            loaded.addOnMapLongClickListener { latlng ->
                if (navigating || demo || demoPaused) {
                    // No destination or road edits while driving.
                    Toast.makeText(this, "Finalizá el recorrido antes de editar el mapa", Toast.LENGTH_SHORT).show()
                    true
                } else {
                val point = GeoPoint(latlng.latitude, latlng.longitude)
                when (mapSelectMode) {
                    1 -> { chosenOrigin = point; status("Origen elegido manualmente", false) }
                    3 -> {
                        if (!navigating && !demo) {
                            manualBumpStore.add(point)
                            Toast.makeText(this, "Reductor guardado en este teléfono", Toast.LENGTH_SHORT).show()
                            refreshRoadBumps()
                        }
                    }
                    2 -> {
                        if (intermediatePoints.size < 4) {
                            intermediatePoints.add(point)
                            status("Parada ${intermediatePoints.size} agregada", false)
                        } else status("Máximo de cuatro paradas", true)
                    }
                    else -> {
                        chooseDestination(point, "PUNTO DEL MAPA")
                    }
                }
                mapSelectMode = 0
                destinationTapArmed = false
                drawWaypoints()
                true
                }
            }
            loaded.addOnMapClickListener { position ->
                if (recceMapSelection && !navigating && !demo && !demoPaused) {
                    recceMapSelection = false
                    val route = plan
                    val selected = if (route != null) route.notes.indices.minByOrNull { index ->
                        val n = route.notes[index]
                        val k=route.metresAtPoint.binarySearch(n.metresFromStart).let {
                            if(it<0) -it-1 else it
                        }.coerceIn(0,route.points.lastIndex)
                        DrivingCamera.distance(route.points[k], GeoPoint(position.latitude,position.longitude))
                    } else null
                    if(selected!=null && route!=null) {
                        val note=route.notes[selected]
                        val k=route.metresAtPoint.binarySearch(note.metresFromStart).let {
                            if(it<0) -it-1 else it
                        }.coerceIn(0,route.points.lastIndex)
                        if(DrivingCamera.distance(route.points[k],GeoPoint(position.latitude,position.longitude))<=90.0)
                            showRecce(selected)
                        else Toast.makeText(this, "No hay una curva a menos de 90 metros", Toast.LENGTH_SHORT).show()
                    }
                    true
                } else if (destinationTapArmed && !navigating && !demo && !demoPaused) {
                    chooseDestination(GeoPoint(position.latitude, position.longitude), "PUNTO DEL MAPA")
                    Toast.makeText(this, "✓ Destino elegido", Toast.LENGTH_SHORT).show()
                    true
                } else {
                    if (navigating || demo || demoPaused) showDrivingControls(!controlsVisible)
                    false
                }
            }
            loaded.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(LatLng(-40.0, -64.0)).zoom(3.7).build()
            ))
        }
        initializeVoiceEngine()
        val saved = prefs.getString("last_route", null)
        if (!saved.isNullOrBlank()) execute("Recuperando ruta", { PaceNoteEngine.createPlan(RouteStore.deserialize(saved)) }) { prepared ->
            setPreparedPlan(prepared as RoutePlan, false)
            // Recover destination only for actual road routes, never arbitrary imported GPX tracks.
            val endpoint = prefs.getString("last_road_destination", null)
            routeDestination = endpoint?.let { runCatching { RouteStore.deserialize(it).firstOrNull() }.getOrNull() }
            pendingStops = prefs.getString("last_road_stops", null)?.let {
                runCatching { RouteStore.deserialize(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            updateDestinationIndicator("DESTINO GUARDADO")
            drawWaypoints()
        }
        if (prefs.getBoolean(RallyLanguage.SETUP_KEY, false)) requestGps()
        else showWelcomeSetup(firstRun = true)
    }

    /** Full-screen first-use setup; can be reopened from Settings without deleting routes. */
    private fun showWelcomeSetup(firstRun: Boolean) {
        val dialog = android.app.Dialog(this)
        dialog.setCancelable(!firstRun)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.parseColor("#090F1B")))
        val root = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#090F1B"))
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(26), dp(24), dp(26), dp(28))
        }
        val iconView = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Rally GPS AI"
        }
        body.addView(iconView, LinearLayout.LayoutParams(dp(105), dp(105)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(22)
        })
        val title = label(tr("welcome"), 26f, white, true).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        val subtitle = label(tr("select"), 15f, muted).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        body.addView(title, LinearLayout.LayoutParams(-1, -2))
        body.addView(subtitle, LinearLayout.LayoutParams(-1, -2).apply { topMargin=dp(9); bottomMargin=dp(22) })
        val heading = label(tr("language"), 12f, sky, true)
        body.addView(heading)
        val options = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        var selectedLanguage = appLanguage
        RallyLanguage.names.forEachIndexed { index, name ->
            val choice = RadioButton(this).apply {
                id = View.generateViewId()
                text = name
                textSize = 16f
                setTextColor(Color.WHITE)
                buttonTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(sky))
                setPadding(dp(8), dp(8), dp(8), dp(8))
                isChecked = RallyLanguage.codes[index] == selectedLanguage
            }
            options.addView(choice, RadioGroup.LayoutParams(-1, dp(51)))
        }
        body.addView(options, LinearLayout.LayoutParams(-1, -2))
        val voiceChoice = CheckBox(this).apply {
            text = "🔊  Copiloto / Co-driver"
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = voiceOn
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(sky))
        }
        body.addView(voiceChoice, LinearLayout.LayoutParams(-1, -2).apply { topMargin=dp(18) })
        val autoChoice = CheckBox(this).apply {
            text = "🚙  Vehículo GPS / GPS vehicle"
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = RallyVehicles.selectedId(prefs) != RallyVehicles.DOT
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(sky))
        }
        body.addView(autoChoice, LinearLayout.LayoutParams(-1, -2).apply { topMargin=dp(7) })
        val galleryButton = button(tr("vehicle"), panel, sky).apply {
            setOnClickListener {
                autoChoice.isChecked = true
                selectVehicleMarker()
            }
        }
        body.addView(galleryButton, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin=dp(9) })
        val caution = label(RallyLanguage.text(selectedLanguage, "voice_hint"), 12f, muted)
        body.addView(caution, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(16); bottomMargin=dp(20) })
        val begin = button(RallyLanguage.text(selectedLanguage, "start"), "#0E8BA1", "#FFFFFF")
        begin.textSize = 17f
        options.setOnCheckedChangeListener { _, checkedId ->
            val index = (0 until options.childCount).indexOfFirst { options.getChildAt(it).id == checkedId }
            if (index >= 0) {
                selectedLanguage = RallyLanguage.codes[index]
                title.text = RallyLanguage.text(selectedLanguage, "welcome")
                subtitle.text = RallyLanguage.text(selectedLanguage, "select")
                heading.text = RallyLanguage.text(selectedLanguage, "language")
                caution.text = RallyLanguage.text(selectedLanguage, "voice_hint")
                begin.text = RallyLanguage.text(selectedLanguage, "start")
                galleryButton.text = RallyLanguage.text(selectedLanguage, "vehicle")
            }
        }
        begin.setOnClickListener {
            val oldLanguage = appLanguage
            appLanguage = RallyLanguage.normalized(selectedLanguage)
            voiceOn = voiceChoice.isChecked
            prefs.edit()
                .putString(RallyLanguage.KEY, appLanguage)
                .putBoolean(RallyLanguage.SETUP_KEY, true)
                .putBoolean("voice", voiceOn)
                .putBoolean("subaru_marker", autoChoice.isChecked)
                .putString(RallyVehicles.KEY, if (autoChoice.isChecked) {
                    val previously = RallyVehicles.selectedId(prefs)
                    if (previously == RallyVehicles.DOT) RallyVehicles.DEFAULT else previously
                } else RallyVehicles.DOT)
                .apply()
            if (!RallyLanguage.isSpanish(appLanguage)) {
                prefs.edit().putString("rally_voice_mode", RallyVoicePack.SYSTEM).apply()
            }
            if (oldLanguage != appLanguage) initializeVoiceEngine()
            updateVehicleLayers()
            refreshLanguageUi()
            dialog.dismiss()
            if (firstRun) requestGps()
        }
        body.addView(begin, LinearLayout.LayoutParams(-1, dp(56)))
        root.addView(body, android.view.ViewGroup.LayoutParams(-1, -2))
        dialog.setContentView(root)
        dialog.show()
        dialog.window?.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.MATCH_PARENT)
    }

    /** The existing map/route session stays in memory when changing the language. */
    private fun refreshLanguageUi() {
        if (!::searchInput.isInitialized) return
        searchInput.hint = tr("search_hint")
        searchButton.text = tr("search")
        voiceButton.text = if (voiceOn) tr("voice_on") else tr("voice_off")
        pointsButton.text = tr("points")
        settingsButton.contentDescription = tr("settings")
        settingsButton.tooltipText = tr("settings")
        historyButton.contentDescription = tr("history")
        recceButton.contentDescription = tr("recce")
        recceButton.text = tr("recce_short")
        nextCurveCaption.text = tr("next")
        action.text = when {
            navigating || demo -> tr("finish")
            plan != null && target == null -> tr("go")
            else -> tr("route")
        }
        if (demo) nextButton.text = tr("pause")
        if (demoPaused) nextButton.text = tr("resume")
        tripDistance.text = RallyDistanceFormatter.remaining(lastRemainingMetres) + " " + tr("remaining")
        if (plan == null) resetNoteCard(sky, "RALLY GPS AI", tr("dest"), tr("dest_sub"))
        else {
            val shownNote = plan?.notes?.getOrNull(currentNote)
            if (shownNote != null && (navigating || demo)) updateNoteCard(shownNote, plan?.notes?.getOrNull(currentNote+1), lastNoteDistanceM)
        }
        status(tr("gps"), false)
    }
    private fun dp(value: Int) = (resources.displayMetrics.density * value + 0.5f).toInt()
    private fun systemBarInset(name: String): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }
    private fun shape(color: String, radius: Int = 15, stroke: String? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(Color.parseColor(color)); cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), Color.parseColor(stroke))
        }
    private fun label(text: String, size: Float = 15f, color: String = white, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text; textSize = size; setTextColor(Color.parseColor(color))
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            gravity = Gravity.CENTER_VERTICAL
        }
    private fun button(text: String, background: String = panel, foreground: String = white): TextView =
        label(text, 13f, foreground, true).apply {
            gravity = Gravity.CENTER
            this.background = shape(background, 12, "#2D425E")
            minHeight = dp(48)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            isClickable = true
        }
    private fun LinearLayout.add(v: View, w: Int = -1, h: Int = -2, weight: Float = 0f) {
        val params = LinearLayout.LayoutParams(w, h, weight)
        if (orientation == LinearLayout.HORIZONTAL && childCount > 0) {
            params.marginStart = dp((tag as? Int) ?: 6)
        }
        addView(v, params)
    }
    private fun row(gap: Int = 8, contents: LinearLayout.() -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            tag = gap
            contents()
        }

    private fun setNoteVisual(color: String, badge: String) {
        noteAccent.setBackgroundColor(Color.parseColor(color))
        noteCard.background = shape("#1D2B42", 10, "#2A4762")
        noteBadge.text = badge
        noteBadge.background = shape("#0E1727", 5, color)
        noteBadge.setTextColor(Color.parseColor(color))
        noteTitle.setTextColor(Color.parseColor(color))
    }

    private fun updateNoteCard(note: PaceNote, next: PaceNote?, distanceMetres: Double) {
        lastNoteDistanceM = distanceMetres
        val display = RallyNotePresentation.render(note, next, distanceMetres)
        setNoteVisual(PaceNotes.color(display.grade), tr("note"))
        nextTurnDistance.visibility = View.VISIBLE
        distanceNumber.text = if (display.distanceUnit.isEmpty()) tr("now") else display.distanceValue
        distanceNumber.textSize = if (prefs.getBoolean("compact_notes", false)) {
            if (display.distanceUnit.isEmpty()) 26f else 30f
        } else if (display.distanceUnit.isEmpty()) 29f else 39f
        distanceUnit.text = display.distanceUnit
        primaryNoteTile.visibility = View.VISIBLE
        primaryNoteTile.show(display.grade, display.side)
        noteTitle.text = if (display.side == PaceNote.Side.LEFT) tr("left") else tr("right")
        noteDetail.text = if (RallyLanguage.isSpanish(appLanguage)) {
            display.featuresLabel + if (display.lowConfidence) " · " + tr("approx") else ""
        } else {
            val details = mutableListOf<String>()
            if (note.cumulativeTurnDegrees >= 145 && note.grade <= 2) details += tr("hairpin")
            if (note.manuallyLong ?: (note.endMetres-note.metresFromStart >= 95.0 || (note.cumulativeTurnDegrees >= 95 && note.grade >= 3))) details += tr("long")
            note.tightensTo?.let { if (it < note.grade) details += tr("close") + " " + it }
            note.opensTo?.let { if (it > note.grade) details += tr("open") + " " + it }
            if (display.lowConfidence) details += tr("approx")
            details.joinToString(" · ").ifBlank { tr("note") }
        }
        // Show the next note all the time, not only when curves are tightly linked.
        // Index is advanced by NavigationEngine as the car crosses each bend.
        val window = plan?.let { route ->
            RollingNotes.at(route, currentNote, (note.metresFromStart - distanceMetres).coerceAtLeast(0.0))
        }
        val upcoming = window?.next ?: next
        linkedNoteColumn.visibility = if (upcoming == null) View.GONE else View.VISIBLE
        if (upcoming != null) {
            linkedNoteTile.show(upcoming.grade, upcoming.side)
            linkedNoteLabel.text = if (window?.linked == true) tr("linked") else tr("queue_next")
        }
        val third = window?.afterNext
        afterNextHint.visibility = if (third == null) View.GONE else View.VISIBLE
        if (third != null) {
            val direction = if (third.side == PaceNote.Side.LEFT) tr("left") else tr("right")
            afterNextHint.text = tr("queue_after") + ": " + direction + " " + third.grade
        }
    }

    private fun resetNoteCard(tone: String, badge: String, headline: String, subline: String) {
        setNoteVisual(tone, badge)
        nextTurnDistance.visibility = View.GONE
        primaryNoteTile.visibility = View.GONE
        linkedNoteColumn.visibility = View.GONE
        afterNextHint.visibility = View.GONE
        noteTitle.text = headline
        noteDetail.text = subline
    }

    private fun buildUI() {
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#070C14"))
        root.addView(mapView, FrameLayout.LayoutParams(-1, -1))
        val topInset = systemBarInset("status_bar_height")
        val bottomInset = systemBarInset("navigation_bar_height")

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10) + topInset, dp(14), dp(14))
            background = shape(bg, 16, "#294561")
            elevation = dp(10).toFloat()
        }
        searchRow = row(8) {
            searchInput = EditText(this@MainActivity).apply {
                hint = tr("search_hint"); setHintTextColor(Color.parseColor("#758A9F"))
                setTextColor(Color.WHITE); textSize = 14f; isSingleLine = true
                setPadding(dp(12), 0, dp(8), 0); background = shape("#233148", 10)
                imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                setOnEditorActionListener { _, _, _ -> lookupPlace(); true }
            }
            add(searchInput, 0, dp(50), 1f)
            searchButton = button(tr("search"), "#116B78", "#FFFFFF").apply { setOnClickListener { lookupPlace() } }
            add(searchButton, dp(104), dp(50))
        }
        top.addView(searchRow, LinearLayout.LayoutParams(-1, -2))
        // The destination stays visible even if fresh GPS fixes overwrite the status line.
        destinationIndicator = label("✓ DESTINO ELEGIDO", 12f, "#ADFFE0", true).apply {
            visibility = View.GONE
            isClickable = true
            background = shape("#103C35", 8, "#2C977E")
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener {
                val fixed = target ?: routeDestination
                if (fixed != null) {
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(fixed.latitude, fixed.longitude), 16.0), 450)
                }
            }
        }
        // Destination is represented by a map pin; no duplicate green banner.
        banner = label(tr("gps"), 11f, muted)
        banner.visibility = View.GONE
        top.addView(banner, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(8) })

        noteAccent = View(this).apply { setBackgroundColor(Color.parseColor(sky)) }
        noteBadge = label("PACENOTE", 11f, sky, true).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        noteTitle = label(tr("dest"), 21f, white, true).apply {
            setSingleLine(false)
        }
        noteDetail = label(tr("dest_sub"), 11.5f, muted).apply {
            maxLines = 2
        }
        primaryNoteTile = RallyNoteTile(this).apply { visibility = View.GONE }
        linkedNoteTile = RallyNoteTile(this)
        linkedNoteLabel = label("ENLAZA", 9f, muted, true).apply { gravity = Gravity.CENTER }
        linkedNoteColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            addView(linkedNoteLabel, LinearLayout.LayoutParams(dp(66), dp(20)))
            addView(linkedNoteTile, LinearLayout.LayoutParams(dp(66), dp(66)))
        }
        val noteWords = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            // The heading is redundant (NOTA DE RALLY): leave only the turn direction.
            addView(noteTitle, LinearLayout.LayoutParams(-1, -2))
            addView(noteDetail, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        }
        // Large next-bend distance follows familiar navigation hierarchy: number first,
        // unit secondary, pacenote directly underneath (instead of a thin text badge).
        distanceNumber = label("0", 39f, white, true).apply {
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = false
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }
        distanceUnit = label("m", 18f, muted, true).apply {
            gravity = Gravity.BOTTOM
            setPadding(0, 0, 0, dp(6))
        }
        nextTurnDistance = row(7) {
            add(distanceNumber, -2, dp(51))
            add(distanceUnit, -2, dp(51))
            nextCurveCaption = label(tr("next"), 10f, muted, true).apply {
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            }
            add(nextCurveCaption, 0, dp(48), 1f)
        }.apply {
            visibility = View.GONE
            setPadding(dp(3), dp(1), dp(3), dp(8))
            contentDescription = "Distancia hasta la próxima curva"
        }
        val paceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(primaryNoteTile, LinearLayout.LayoutParams(dp(78), dp(78)).apply { marginEnd = dp(10) })
            addView(noteWords, LinearLayout.LayoutParams(0, -2, 1f))
            addView(linkedNoteColumn, LinearLayout.LayoutParams(dp(66), -2).apply { marginStart = dp(6) })
        }
        afterNextHint = label("", 11f, muted, true).apply {
            visibility = View.GONE
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val noteContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(7), dp(10), dp(10))
            addView(nextTurnDistance, LinearLayout.LayoutParams(-1, -2))
            addView(paceRow, LinearLayout.LayoutParams(-1, -2))
            addView(afterNextHint, LinearLayout.LayoutParams(-1,-2).apply { topMargin = dp(5) })
        }
        noteCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = shape("#1D2B42", 9, "#2A4762")
            addView(noteAccent, LinearLayout.LayoutParams(dp(5), -1))
            addView(noteContent, LinearLayout.LayoutParams(0, -2, 1f))
        }
        top.addView(noteCard, LinearLayout.LayoutParams(-1,-2).apply { topMargin = dp(10) })
        bumpBanner = label("", 12f, "#241900", true).apply {
            visibility = View.GONE
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12),dp(8),dp(12),dp(8))
            background = shape("#F9C95E", 10, "#FFDE83")
        }
        top.addView(bumpBanner, LinearLayout.LayoutParams(-1,-2).apply { topMargin = dp(7) })
        resetNoteCard(sky, "RALLY GPS AI", tr("dest"), tr("dest_sub"))

        root.addView(top, FrameLayout.LayoutParams(-1,-2,Gravity.TOP).apply {
            setMargins(dp(9),dp(8),dp(9),0)
        })

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(bg, 18, "#34506C")
            setPadding(dp(12), dp(10), dp(12), dp(12))
            elevation=dp(10).toFloat()
        }
        info = label("SIN RUTA · GPS", 15f, muted, true)
        tripDistance = label("— km restantes", 18f, white, true).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }
        bottom.addView(row(12) {
            add(info, 0, dp(36), 1f)
            add(tripDistance, -2, dp(36))
        })

        val actionRow = row(8) {
            action = button(tr("route"), "#0E8BA1", "#FFFFFF").also { it.setOnClickListener { mainAction() } }
            add(action, 0, dp(52), 1.5f)
            nextButton = button("DEMO", "#463377", "#FFFFFF").also { it.setOnClickListener { startDemo() } }
            add(nextButton, 0, dp(52), 0.8f)
            gpsButton = button("◎ GPS", panel, sky).also { it.setOnClickListener { centerGps() } }
            add(gpsButton, 0, dp(52), 0.72f)
        }
        bottom.addView(actionRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) })

        toolsRow = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val mainTools = row(8) {
            pointsButton = button(tr("points")).apply { textSize=13f; contentDescription="Marcar origen, destino o reductor"; setOnClickListener { showPointOptions() } }
            add(pointsButton, 0, dp(50),1f)
            recceButton = button(tr("recce_short")).apply {
                textSize=13f; contentDescription=tr("recce")
                setOnClickListener { showRecce() }
            }
            add(recceButton, 0, dp(50),1f)
            historyButton = button("HIST.").apply {
                textSize=13f; contentDescription=tr("history")
                setOnClickListener { showTripHistory() }
            }
            add(historyButton, 0, dp(50),1f)
        }
        val secondaryTools = row(8) {
            add(button(tr("gpx_import")).apply {
                textSize=12f; contentDescription="Importar ruta GPX"
                setOnClickListener { importGpx() }
            }, 0, dp(50), 1f)
            add(button(tr("gpx_export")).apply {
                textSize=12f; contentDescription="Guardar ruta GPX"
                setOnClickListener { exportGpx() }
            }, 0, dp(50), 1f)
            settingsButton = button("⚙  AJUSTES").apply {
                textSize=13f; contentDescription=tr("settings")
                tooltipText=tr("settings")
                setOnClickListener { showSettings() }
            }
            add(settingsButton,0,dp(50),1f)
        }
        toolsRow.addView(mainTools)
        toolsRow.addView(secondaryTools, LinearLayout.LayoutParams(-1,-2).apply {
            topMargin=dp(7)
        })
        bottom.addView(toolsRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        bottomPanel = bottom
        root.addView(bottom, FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM).apply {
            setMargins(dp(9),0,dp(9),dp(10) + bottomInset)
        })

        // Floating map controls: bottom-right, directly above the bottom navigation bar.
        // The note panel no longer wastes vertical room on title, 3D or voice buttons.
        floatingControls = row(7) {
            modeButton = button(if (map3d) "3D" else "2D", panel, sky).apply {
                contentDescription = "Alternar vista 2D y 3D"
                setOnClickListener { toggle3D() }
            }
            add(modeButton, dp(63), dp(48))
            voiceButton = button(if (voiceOn) tr("voice_on") else tr("voice_off"), panel, sky).apply {
                contentDescription = "Activar o silenciar el copiloto"
                setOnClickListener {
                    voiceOn = !voiceOn
                    prefs.edit().putBoolean("voice", voiceOn).apply()
                    voiceButton.text = if (voiceOn) tr("voice_on") else tr("voice_off")
                    if (!voiceOn) { tts?.stop(); rallyAudio.stop() }
                }
            }
            add(voiceButton, dp(79), dp(48))
        }.apply {
            elevation = dp(14).toFloat()
        }
        root.addView(floatingControls, FrameLayout.LayoutParams(-2, -2, Gravity.RIGHT or Gravity.BOTTOM).apply {
            rightMargin = dp(17)
            bottomMargin = dp(220) + bottomInset
        })
        bottom.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> positionFloatingControls() }
        root.post { positionFloatingControls() }
        setContentView(root)
        applyNoteViewMode()
    }

    /** Keep the large rally board by default; compact mode frees extra map space. */
    private fun applyNoteViewMode() {
        if (!::primaryNoteTile.isInitialized || !::linkedNoteTile.isInitialized) return
        val compact = prefs.getBoolean("compact_notes", false) ||
            resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val p = primaryNoteTile.layoutParams as? LinearLayout.LayoutParams
        p?.width = dp(if (compact) 56 else 78)
        p?.height = dp(if (compact) 56 else 78)
        if (p != null) primaryNoteTile.layoutParams = p
        val n = linkedNoteTile.layoutParams as? LinearLayout.LayoutParams
        n?.width = dp(if (compact) 49 else 66)
        n?.height = dp(if (compact) 49 else 66)
        if (n != null) linkedNoteTile.layoutParams = n
        val col = linkedNoteColumn.layoutParams as? LinearLayout.LayoutParams
        col?.width = dp(if (compact) 54 else 66)
        if (col != null) linkedNoteColumn.layoutParams = col
        noteTitle.textSize = if (compact) 17f else 21f
        noteDetail.textSize = if (compact) 10f else 11.5f
        afterNextHint.textSize = if (compact) 10f else 11f
        distanceNumber.textSize = if (compact) 30f else 39f
        noteCard.requestLayout()
    }

    private fun selectNoteViewMode() {
        val names = arrayOf(tr("note_large"), tr("note_compact"))
        android.app.AlertDialog.Builder(this).setTitle(tr("note_view"))
            .setSingleChoiceItems(names, if (prefs.getBoolean("compact_notes",false)) 1 else 0) { dialog,index ->
                prefs.edit().putBoolean("compact_notes",index==1).apply()
                applyNoteViewMode()
                plan?.notes?.getOrNull(currentNote)?.takeIf { navigating || demo }?.let { note ->
                    updateNoteCard(note,plan?.notes?.getOrNull(currentNote+1), lastNoteDistanceM)
                }
                dialog.dismiss()
            }.setNegativeButton(tr("back"), null).show()
    }

    private fun positionFloatingControls() {
        if (!::floatingControls.isInitialized || !::bottomPanel.isInitialized) return
        val params = floatingControls.layoutParams as? FrameLayout.LayoutParams ?: return
        val nextBottomMargin = bottomPanel.height + dp(23) + systemBarInset("navigation_bar_height")
        if (params.bottomMargin != nextBottomMargin) {
            params.bottomMargin = nextBottomMargin
            floatingControls.layoutParams = params
        }
    }

    private fun updateDestinationIndicator(summary: String? = null, approximate: Boolean? = null) {
        if (summary != null) chosenDestinationText = summary.take(45)
        if (approximate != null) chosenDestinationApproximate = approximate
        // Show the actual pin on the map instead of a persistent duplicate label.
        if (::destinationIndicator.isInitialized) destinationIndicator.visibility = View.GONE
    }

    private fun chooseDestination(point: GeoPoint, name: String, approximate: Boolean = false) {
        target = point
        hideSearchKeyboard()
        destinationTapArmed = false
        updateDestinationIndicator(name, approximate)
        drawWaypoints()
        Toast.makeText(this, if (approximate) "Punto aproximado marcado. Revisalo en el mapa"
            else "Destino señalado en el mapa", Toast.LENGTH_SHORT).show()
    }

    private fun status(message: String, error: Boolean) {
        banner.text = message
        banner.setTextColor(Color.parseColor(if (error) "#FF907B" else muted))
        // Do not reserve a permanent status line when a route is already displayed.
        // Important errors stay visible; normal status stays out of the map.
        banner.visibility = if (error) View.VISIBLE else View.GONE
    }
    private fun execute(label: String, request: () -> Any?, success: (Any?) -> Unit) {
        status("$label…", false)
        thread {
            try {
                val result = request()
                runOnUiThread { success(result) }
            } catch (e: Exception) {
                runOnUiThread { status("Error: ${e.message ?: "Conexión fallida"}", true) }
            }
        }
    }
    private fun lookupPlace() {
        val name = searchInput.text.toString().trim()
        if (name.length < 3) { status("Escribí una dirección o lugar para buscar", true); return }
        // No establecer automáticamente la primera coincidencia global.
        // En calles sin numeración OSM, pedir al usuario confirmar el punto aproximado.
        val reference = gps ?: chosenOrigin
        execute("Buscando lugares cerca de tu ubicación", { RouteServices.searchResults(name, reference) }) { result ->
            @Suppress("UNCHECKED_CAST")
            val matches = result as List<RouteServices.PlaceCandidate>
            if (matches.isEmpty()) {
                status("Sin resultados. Probá: calle y altura, ciudad, provincia", true)
                return@execute
            }
            val entries = matches.map { match ->
                val distance = match.distanceKm?.let { "  ·  ${"%.1f".format(Locale("es", "AR"),it)} km" } ?: ""
                val accuracy = if (match.approximate) "⚠ Ubicación aproximada · " else ""
                "${accuracy}${match.address.take(150)}$distance"
            }.toTypedArray()
            android.app.AlertDialog.Builder(this)
                .setTitle("ELEGÍ EL DESTINO CORRECTO")
                .setItems(entries) { _, index ->
                    val choice = matches[index]
                    searchInput.setText(name)
                    chooseDestination(choice.point, choice.address, choice.approximate)
                    map?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                        .target(LatLng(choice.point.latitude,choice.point.longitude))
                        .zoom(16.0).bearing(0.0).tilt(0.0).build()), 550)
                    status(if (choice.approximate)
                        "Punto aproximado: confirmá la altura en el mapa antes de trazar"
                    else "Destino elegido · tocá TRAZAR RUTA", choice.approximate)
                }
                .setNegativeButton("CANCELAR", null)
                .show()
        }
    }
    private fun mainAction() {
        if (navigating || demo || demoPaused) {
            finishRealTrip()
            navigationSession++
            reroutePolicy.reset()
            navigating = false; demo = false; demoPaused = false; ++demoTickGeneration; updateScreenAwake(); tts?.stop(); rallyAudio.stop(); lastSpoken = -1
            contextualCopilot.reset(); lastPriorityNoteAtMs = -1L
            showDrivingControls(true)
            bumpBanner.visibility = View.GONE
            action.text = tr("go"); nextButton.text = "DEMO"
            resetNoteCard("#F0475A", "STOP", "NAVEGACIÓN DETENIDA", "Podés retomar el recorrido o trazar uno nuevo")
            status(if (plan != null) "Recorrido finalizado. Revisá el historial para ver las estadísticas." else "Navegación detenida", false)
            return
        }
        if (plan != null && target == null) { startGpsNavigation(); return }
        if (target == null) { status("Ingresá un destino o mantené pulsado el mapa", true); return }
        val origin = chosenOrigin ?: gps ?: run { status("Necesito GPS o un origen manual", true); requestGps(); return }
        val destination = target ?: return
        val stops = intermediatePoints.toList()
        execute("Calculando ruta (OSRM demo)", {
            val response=RouteServices.routeVerified(origin, stops, destination)
            Pair(PaceNoteEngine.createPlan(response.points),response.inspection)
        }) { result ->
            val verified=result as Pair<*,*>
            val prepared=verified.first as RoutePlan
            val assessment=verified.second as RoadInspection
            val accept: () -> Unit = {
                setPreparedPlan(prepared,true)
                routeDestination=destination
                pendingStops=stops
                prefs.edit()
                    .putString("last_road_destination", RouteStore.serialize(listOf(destination)))
                    .putString("last_road_stops", RouteStore.serialize(stops)).apply()
                target=null
                updateDestinationIndicator("DESTINO DE LA RUTA")
                drawWaypoints()
                status(if(assessment.reviewRequired) tr("route_warning") else
                    "Ruta calculada y lista para revisar en el mapa",false)
            }
            if(assessment.reviewRequired) {
                android.app.AlertDialog.Builder(this).setTitle(tr("check_route"))
                    .setMessage(tr("route_warning")+"\n\n"+
                        assessment.reasons.joinToString("\n")+
                        "\n\nOSRM usa OpenStreetMap; comprobá las calles antes de iniciar.")
                    .setPositiveButton("ACEPTAR RUTA") { _,_ -> accept() }
                    .setNegativeButton("CANCELAR",null).show()
            } else accept()
        }
    }
    /** Apply only corrections stored for exactly this route's geometry. */
    private fun correctedRoute(source: RoutePlan): RoutePlan {
        automaticNotePlan = source
        noteEdits.clear()
        noteEdits.addAll(noteEditsStore.load(source))
        recceNoteIndex = null
        recceMapSelection = false
        return EditablePacenotes.apply(source, noteEdits)
    }

    private fun refreshManualNotes() {
        val base = automaticNotePlan ?: return
        val updated = EditablePacenotes.apply(base, noteEdits)
        plan = updated
        navigator = NavigationEngine(updated) { warnFactor }
        currentNote = 0
        lastSpoken = -1
        drawPlan()
        val count = noteEdits.size
        status("Notas actualizadas · $count correcciones guardadas", false)
    }

    private fun setPreparedPlan(sourcePlan: RoutePlan, save: Boolean) {
        val parsed = correctedRoute(sourcePlan)
        navigationSession++
        reroutePolicy.reset()
        // A GPX is not necessarily a drivable road; OSRM destinations are assigned
        // only after the server verifies the route.
        routeDestination = null
        pendingStops = emptyList()
        updateDestinationIndicator()
        if (save) prefs.edit().remove("last_road_destination").remove("last_road_stops").apply()
        plan = parsed; navigator = NavigationEngine(parsed) { warnFactor }; drivingCamera.reset()
        contextualCopilot.reset(); lastPriorityNoteAtMs = -1L
        positionIndex = 0; lastSpoken = -1; currentNote = 0
        navigating = false; demo = false; demoPaused = false; ++demoTickGeneration; updateScreenAwake(); demoPosition = 0.0
        action.text = tr("go"); nextButton.text = "DEMO"
        showDrivingControls(false)
        val km = "%.1f".format(Locale.US, parsed.totalMetres / 1000.0)
        resetNoteCard("#22D3EE", tr("ready"), parsed.notes.size.toString() + " " + tr("detected"), km + " km · " + tr("dest_sub"))
        info.text = parsed.notes.size.toString() + " " + tr("detected") + " · 1–6"
        lastRemainingMetres = parsed.totalMetres
        tripDistance.text = "$km km " + tr("remaining")
        drawPlan()
        lastRenderedProgress = -1000.0
        updateRouteProgress(0.0)
        refreshRoadBumps()
        if (save && parsed.points.size <= 30000) prefs.edit().putString("last_route", RouteStore.serialize(parsed.points)).apply()
        val center = parsed.points[parsed.points.size/2]
        map?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
            .target(LatLng(center.latitude, center.longitude)).zoom(12.7).tilt(0.0).build()), 600)
    }

    /** Display and speak a cancellable 3, 2, 1, GO before changing navigation state. */
    private fun startCountdown(onGo: () -> Unit) {
        if (countdownDialog != null || navigating || demo || isFinishing || isDestroyed) return
        val token = ++countdownSequence
        val number = label("3", 78f, white, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }
        val note = label("PREPARATE · 3, 2, 1, GO", 14f, muted, true).apply {
            gravity = Gravity.CENTER
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(18))
            background = shape(bg, 18, "#34506C")
            addView(note, LinearLayout.LayoutParams(-1, dp(37)))
            addView(number, LinearLayout.LayoutParams(-1, dp(130)))
        }
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(content)
            .setNegativeButton("CANCELAR") { _, _ -> countdownSequence++ }
            .create()
        countdownDialog = dialog
        dialog.setCancelable(false)
        dialog.setOnDismissListener {
            if (countdownDialog === dialog) countdownDialog = null
            countdownSequence++
        }
        dialog.show()
        fun advance(step: Int) {
            if (token != countdownSequence || !dialog.isShowing || isFinishing || isDestroyed) return
            number.text = RallyStartSequence.screen(step)
            number.setTextColor(Color.parseColor(if (step == 3) "#43D9A6" else white))
            if (voiceOn && speakReady) say(RallyStartSequence.voice(step, appLanguage))
            if (step == 3) {
                ui.postDelayed({
                    if (token != countdownSequence || !dialog.isShowing || isFinishing || isDestroyed) return@postDelayed
                    dialog.dismiss()
                    onGo()
                }, 400L)
            } else ui.postDelayed({ advance(step + 1) }, 1000L)
        }
        advance(0)
    }

    private fun updateScreenAwake() {
        if (navigating || demo) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun hideSearchKeyboard() {
        if (!::searchInput.isInitialized) return
        val manager = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        manager?.hideSoftInputFromWindow(searchInput.windowToken, 0)
        searchInput.clearFocus()
    }

    private fun startGpsNavigation() {
        if (plan == null) { status("Primero elegí un recorrido", true); return }
        if (gps == null) { requestGps(); status("Esperando GPS", true); return }
        startCountdown { beginGpsNavigation() }
    }

    private fun beginGpsNavigation() {
        if (gps == null) { requestGps(); status("Esperando GPS", true); return }
        navigationSession++
        reroutePolicy.reset()
        navigating = true; demo = false; positionIndex = 0; lastSpoken = -1; currentNote = 0
        updateScreenAwake()
        hideSearchKeyboard()
        realTripRecorder = RealTripRecorder(System.currentTimeMillis(), SystemClock.elapsedRealtime(),
            plan?.points.orEmpty())
        navigator?.reset(); drivingCamera.reset(); followRouteCamera = true; showDrivingControls(false)
        contextualCopilot.reset(); lastPriorityNoteAtMs = -1L
        action.text = tr("finish"); nextButton.text = "DEMO"
        status("GPS activo · notas estimadas del trazado · respetá la señalización", false)
        lastLocation?.let {
            updateProgress(GeoPoint(it.latitude,it.longitude), it.speed)
            drawPosition(GeoPoint(it.latitude,it.longitude))
        }
    }
    private fun startDemo() {
        if (demo) {
            demo = false
            updateScreenAwake()
            ++demoTickGeneration
            demoPaused = true
            nextButton.text = tr("resume")
            action.text = tr("finish")
            // Keep the note cursor, route progress and spoken state intact.
            tts?.stop()
            rallyAudio.stop()
            showDrivingControls(false)
            return
        }
        if (demoPaused) {
            beginDemo(resuming = true)
            return
        }
        // Never draw a fabricated road through real city blocks. A DEMO may simulate
        // movement on an already calculated road route or a user-imported GPX only.
        if (!DemoRoutePolicy.canStart(plan)) {
            android.app.AlertDialog.Builder(this)
                .setTitle("DEMO · PRIMERO ELEGÍ UNA RUTA")
                .setMessage("Para simular necesitás un recorrido real calculado por el navegador o un GPX que hayas importado. Elegí un destino y tocá TRAZAR RUTA. DEMO no inventa calles ni cruza manzanas.")
                .setPositiveButton("ENTENDIDO", null)
                .setNeutralButton("IMPORTAR GPX") { _, _ -> importGpx() }
                .show()
            return
        }
        if (navigating) {
            status("Finalizá primero la navegación GPS para usar DEMO", true)
            return
        }
        startCountdown { beginDemo() }
    }

    private fun beginDemo(resuming: Boolean = false) {
        if (!DemoRoutePolicy.canStart(plan) || isFinishing || isDestroyed) return
        if (!resuming) {
            navigationSession++
            reroutePolicy.reset()
            demoPosition = 0.0; positionIndex = 0; lastSpoken = -1
            navigator?.reset(); drivingCamera.reset()
            contextualCopilot.reset(); lastPriorityNoteAtMs = -1L
        }
        demo = true; demoPaused = false; navigating = false
        updateScreenAwake()
        hideSearchKeyboard()
        followRouteCamera = true
        showDrivingControls(false)
        action.text = tr("finish"); nextButton.text=tr("pause")
        val tickVersion = ++demoTickGeneration
        val tick = object : Runnable {
            override fun run() {
                if (!demo || tickVersion != demoTickGeneration) return
                val p = plan ?: return
                demoPosition = (demoPosition + demoSpeed * 0.5).coerceAtMost(p.totalMetres)
                val point = DrivingCamera.pointAt(p, demoPosition)
                positionIndex = p.metresAtPoint.binarySearch(demoPosition).let { found ->
                    (if(found < 0) -found-2 else found).coerceIn(0, p.points.lastIndex-1)
                }
                updateProgress(point, demoSpeed.toFloat())
                drawPosition(point)
                if (demoPosition >= p.totalMetres - 0.5) {
                    demo=false; demoPaused=false; ++demoTickGeneration
                    updateScreenAwake()
                    action.text=tr("go"); nextButton.text="DEMO"
                    showDrivingControls(true)
                    return
                }
                ui.postDelayed(this, 500)
            }
        }
        ui.post(tick)
    }

    private fun updateProgress(point: GeoPoint, speed: Float) {
        val route = plan ?: return
        if (!navigating && !demo) return
        val result = navigator?.update(point, speed, if(demo) 3f else lastLocation?.accuracy ?: 5f) ?: return
        val now = SystemClock.elapsedRealtime()
        val accuracy = if (demo) 3f else lastLocation?.accuracy ?: 10f
        if (result.offRoute) {
            if (navigating && !demo && routeDestination != null &&
                reroutePolicy.observe(true, result.fix.errorM, accuracy, now)) {
                recalculateFrom(point, result.fix.metres)
            }
            status(if (reroutePolicy.isBusy()) "Recalculando desde tu posición…" else
                if (routeDestination == null) "Fuera del trazado GPX: volvé a la ruta; recálculo no disponible" else
                    "Fuera de ruta: comprobando GPS para recalcular", true)
            return
        }
        reroutePolicy.observe(false, result.fix.errorM, accuracy, now)
        positionIndex = result.fix.segment.coerceIn(0, route.points.lastIndex)
        val remaining = (route.totalMetres-result.fix.metres).coerceAtLeast(0.0)
        lastRemainingMetres = remaining
        currentNote = result.noteIndex.coerceAtLeast(0)
        updateRouteProgress(result.fix.metres)
        info.text = (if (demo) "DEMO · " else "") + "${(speed*3.6).toInt().coerceAtLeast(0)} km/h"
        tripDistance.text = RallyDistanceFormatter.remaining(remaining) + " " + tr("remaining")
        val next = route.notes.getOrNull(result.noteIndex)
        if (next == null) {
            resetNoteCard("#43D9A6", "FIN", "TRAMO LIMPIO", "SIN NOTAS INMEDIATAS  ·  ATENCIÓN AL CAMINO")
        } else {
            val follow = route.notes.getOrNull(result.noteIndex+1)
            updateNoteCard(next, follow, result.distanceToNoteM)
        }
        updateRoadBumpWarning(result.fix.metres, result.distanceToNoteM, speed.toDouble(),
            result.announcement != null, now)
        if (result.announcement != null) lastPriorityNoteAtMs = now
        result.announcement?.let { message ->
            if (result.noteIndex >= 0 && !result.arrived) {
                lastSpoken = result.noteIndex
                val spokenNote = route.notes.getOrNull(result.noteIndex)
                if (spokenNote != null) {
                    speakRallyNote(spokenNote, route.notes.getOrNull(result.noteIndex+1),
                        result.distanceToNoteM)
                } else say(message)
            } else say(message)
        }
        // A contextual observation never competes with an imminent turn call.
        // No camera, model downloads or network calls: pure precomputed route geometry.
        if (!result.arrived && result.announcement == null && voiceOn && speakReady &&
            (demo || accuracy <= 25f) &&
            (lastPriorityNoteAtMs < 0 || now - lastPriorityNoteAtMs >= 18_000L) &&
            tts?.isSpeaking != true && !rallyAudio.isBusy() && !reroutePolicy.isBusy()) {
            val mode = ContextMode.fromId(prefs.getString("context_commentary_mode", ContextMode.OCCASIONAL.id))
            val cue = contextualCopilot.evaluate(
                route, result.fix.metres, speed.toDouble(), now, mode,
                result.distanceToNoteM, priorityBusy = false
            )
            if (cue != null) {
                contextualCommentIndex++
                say(RallyLanguage.contextComment(appLanguage, cue, contextualCommentIndex))
            }
        }
        if (result.arrived) {
            finishRealTrip()
            navigationSession++
            contextualCopilot.reset()
            reroutePolicy.reset()
            navigating = false; demo = false; demoPaused = false; updateScreenAwake(); action.text = tr("go")
            bumpBanner.visibility = View.GONE
            showDrivingControls(true)
        }
    }
    private fun recalculateFrom(current: GeoPoint, progressMetres: Double) {
        val destination = routeDestination ?: run { reroutePolicy.completeAttempt(); return }
        val oldPlan = plan ?: run { reroutePolicy.completeAttempt(); return }
        val session = navigationSession
        // Preserve stops that are ahead on the ORIGINAL route, not ones already passed.
        val stopsAhead = pendingStops.filter { stop ->
            // The navigator intentionally searches only ~650 m ahead. Stops could be
            // kilometres away, so inspect the original polyline rather than its local window.
            val closestIndex = oldPlan.points.indices.minByOrNull { i ->
                DrivingCamera.distance(oldPlan.points[i], stop)
            } ?: -1
            closestIndex >= 0 && oldPlan.metresAtPoint[closestIndex] > progressMetres + 30.0
        }
        status("Recalculando recorrido desde tu GPS…", false)
        thread {
            try {
                val checked=RouteServices.routeVerified(current, stopsAhead, destination)
                // Never replace an active road path with a route that needs manual verification.
                if(checked.inspection.reviewRequired)
                    error("Nueva ruta sospechosa: " + checked.inspection.reasons.joinToString("; "))
                val updated = PaceNoteEngine.createPlan(checked.points)
                runOnUiThread {
                    if (session == navigationSession && navigating && !demo && routeDestination == destination && plan === oldPlan) {
                        val edited = correctedRoute(updated)
                        plan = edited
                        navigator = NavigationEngine(edited) { warnFactor }
                        contextualCopilot.reset(); lastPriorityNoteAtMs = -1L
                        drivingCamera.reset()
                        lastSpoken = -1
                        pendingStops = stopsAhead
                        prefs.edit()
                            .putString("last_route", RouteStore.serialize(updated.points))
                            .putString("last_road_stops", RouteStore.serialize(stopsAhead)).apply()
                        drawPlan()
                        lastRenderedProgress = -1000.0
                        updateRouteProgress(0.0)
                        refreshRoadBumps()
                        updateProgress(current, lastLocation?.speed ?: 0f)
                        status("Nueva ruta calculada · notas actualizadas", false)
                        say("Ruta recalculada. Nuevas notas disponibles.")
                    }
                    reroutePolicy.completeAttempt()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (session == navigationSession && navigating) {
                        status("No se pudo validar el recálculo. Se conserva la ruta anterior.", true)
                    }
                    reroutePolicy.completeAttempt()
                }
            }
        }
    }

    private fun initializeVoiceEngine() {
        speakReady = false
        tts?.stop()
        tts?.shutdown()
        tts = TextToSpeech(this) { result ->
            runOnUiThread {
                if (result != TextToSpeech.SUCCESS) {
                    speakReady = false
                    return@runOnUiThread
                }
                speakReady = true
                val engine = tts ?: return@runOnUiThread
                val language = engine.setLanguage(RallyLanguage.locale(appLanguage))
                if (language < TextToSpeech.LANG_AVAILABLE && RallyLanguage.isSpanish(appLanguage)) engine.language = Locale("es", "ES")
                engine.setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                applyVoicePreferences()
            }
        }
    }

    private fun availableLocalizedVoices(): List<Voice> = tts?.voices.orEmpty()
        .filter { it.locale.language == RallyLanguage.locale(appLanguage).language && !it.isNetworkConnectionRequired }
        .sortedWith(compareBy<Voice> { if (it.locale.country == RallyLanguage.locale(appLanguage).country) 0 else 1 }
            .thenBy { it.name })

    private fun applyVoicePreferences() {
        val engine = tts ?: return
        val savedVoice = prefs.getString("voice_name", null)
        availableLocalizedVoices().firstOrNull { it.name == savedVoice }?.let { engine.setVoice(it) }
        engine.setSpeechRate(prefs.getFloat("voice_speed", 1.15f))
        engine.setPitch(prefs.getFloat("voice_pitch", 0.96f))
    }

    private fun chooseVoice() {
        if (!speakReady) {
            status("Motor de voz iniciándose. Revisá los ajustes TTS del teléfono.", true)
            return
        }
        val options = availableLocalizedVoices()
        if (options.isEmpty()) {
            android.app.AlertDialog.Builder(this).setTitle("VOCES EN ESPAÑOL")
                .setMessage("No hay voces españolas sin conexión instaladas en el motor actual. Instalá voces desde Android, RHVoice o SherpaTTS y elegí ese motor en el sistema.")
                .setPositiveButton("AJUSTES DEL TELÉFONO") { _, _ -> openTtsSettings() }
                .setNegativeButton("CERRAR",null).show()
            return
        }
        val labels = options.map { "${it.locale.getDisplayName(Locale("es", "AR"))} · ${it.name}" }.toTypedArray()
        android.app.AlertDialog.Builder(this).setTitle("VOCES DESCARGADAS")
            .setSingleChoiceItems(labels, options.indexOfFirst { it.name == tts?.voice?.name }) { dialog, index ->
                val selected = options[index]
                if (tts?.setVoice(selected) == TextToSpeech.SUCCESS) {
                    prefs.edit().putString("voice_name", selected.name).apply()
                    say("En cien metros, derecha cinco larga, enlaza izquierda tres.")
                }
                dialog.dismiss()
            }.setNeutralButton("MÁS VOCES") { _, _ -> openTtsSettings() }
            .setNegativeButton("CERRAR", null).show()
    }

    private fun openTtsSettings() {
        try {
            voiceEngineSettingsOpened = true
            startActivity(Intent("com.android.settings.TTS_SETTINGS"))
        } catch (_: Exception) {
            voiceEngineSettingsOpened = false
            status("Abrí Ajustes > Accesibilidad > Salida de texto a voz para descargar voces", true)
        }
    }

    private fun adjustVoice() {
        val choices = arrayOf("Normal", "Copiloto rápido", "Copiloto intenso", "Copiloto pausado")
        val presets = listOf(1.0f to 1.0f, 1.19f to 0.97f, 1.31f to 0.9f, 0.86f to 1.03f)
        android.app.AlertDialog.Builder(this).setTitle("ESTILO DE COPILOTO")
            .setItems(choices) { _, index ->
                val (speed, pitch) = presets[index]
                prefs.edit().putFloat("voice_speed",speed).putFloat("voice_pitch",pitch).apply()
                applyVoicePreferences()
                say("En doscientos metros, derecha cuatro, se cierra a dos.")
            }.setNegativeButton("CERRAR",null).show()
    }

    private fun speakRallyNote(note: PaceNote, next: PaceNote?, distance: Double) {
        if (!voiceOn) return
        val mode = prefs.getString("rally_voice_mode", RallyVoicePack.SYSTEM) ?: RallyVoicePack.SYSTEM
        val phrase = RallyLanguage.call(appLanguage, note, next)
        val prefix = RallyLanguage.distanceCall(appLanguage, distance) + ", "
        if (mode != RallyVoicePack.SYSTEM && RallyLanguage.isSpanish(appLanguage)) {
            tts?.stop()
            if (rallyAudio.play(mode, RallyVoicePack.tokens(note, next, distance))) return
            status("Pack de voz no disponible; utilizando voz Android", true)
        }
        say(prefix + phrase)
    }

    private fun selectRallyVoiceMode() {
        if (!RallyLanguage.isSpanish(appLanguage)) {
            status("Recorded CarlFM clips are Spanish-only; using Android TTS", false)
            prefs.edit().putString("rally_voice_mode", RallyVoicePack.SYSTEM).apply()
            return
        }
        val choices = arrayOf("Voz Android (TTS)", "CarlFM · claro · offline", "CarlFM · radio de rally · offline")
        val modes = RallyVoicePack.modes
        val saved = prefs.getString("rally_voice_mode", RallyVoicePack.SYSTEM) ?: RallyVoicePack.SYSTEM
        android.app.AlertDialog.Builder(this).setTitle("VOCES DE COPILOTO")
            .setSingleChoiceItems(choices, modes.indexOf(saved).coerceAtLeast(0)) { dialog, position ->
                val mode = modes[position]
                if (mode != RallyVoicePack.SYSTEM && !rallyAudio.available(mode)) {
                    status("Pack CarlFM no disponible en esta APK. Usá TTS Android.", true)
                } else {
                    tts?.stop(); rallyAudio.stop()
                    prefs.edit().putString("rally_voice_mode", mode).apply()
                    status("Copiloto: ${choices[position]}", false)
                    previewRallyVoice()
                }
                dialog.dismiss()
            }.setNeutralButton("LICENCIAS") { _, _ ->
                android.app.AlertDialog.Builder(this).setTitle("LICENCIAS DEL PACK")
                    .setMessage("CarlFM (es_ES, x_low): catálogo MIT de Piper Voices. Datos declarados como dominio público; modelo entrenado desde cero. Audio generado exclusivamente para Rally GPS AI. No incluye voces de DiRT Rally ni Sideways. Fuente: huggingface.co/rhasspy/piper-voices")
                    .setPositiveButton("CERRAR", null).show()
            }.setNegativeButton("CERRAR", null).show()
    }

    private fun previewRallyVoice() {
        val sample = PaceNote(100.0, PaceNote.Side.RIGHT, 5, 85, 100.0, 200.0)
        val next = PaceNote(245.0, PaceNote.Side.LEFT, 3, 55, 60.0, 300.0)
        speakRallyNote(sample, next, 100.0)
    }

    private fun say(text: String) {
        // Prevent any outdated phrase continuing after a reroute or a new announcement.
        rallyAudio.stop()
        if (voiceOn && speakReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rally_${System.nanoTime()}")
    }

    /** The completed section is only a visual layer; notes/routing remain unchanged. */
    private fun updateRouteProgress(progressM: Double) {
        val route = plan ?: return
        val style = map?.style ?: return
        if (!progressM.isFinite() || kotlin.math.abs(progressM-lastRenderedProgress) < 8.0) return
        val split = RouteProgressGeometry.at(route, progressM)
        val points = split.travelled
        if(points.size < 2) return
        val line = Feature.fromGeometry(LineString.fromLngLats(points.map {
            Point.fromLngLat(it.longitude,it.latitude)
        }))
        val collection = FeatureCollection.fromFeatures(arrayOf(line))
        val source = style.getSourceAs<GeoJsonSource>("rally_completed_route")
        if(source == null) {
            style.addSource(GeoJsonSource("rally_completed_route",collection))
            val layer=LineLayer("rally_completed_line","rally_completed_route").withProperties(
                lineColor("#4B6177"),lineWidth(7f),lineOpacity(0.90f))
            if(style.getLayer("rally_curves_layer") != null)
                style.addLayerBelow(layer,"rally_curves_layer")
            else style.addLayer(layer)
        } else source.setGeoJson(collection)
        lastRenderedProgress = progressM
    }

    private fun drawPlan() {
        val s = map?.style ?: return
        val p = plan ?: return
        val line = Feature.fromGeometry(LineString.fromLngLats(p.points.map { Point.fromLngLat(it.longitude,it.latitude) }))
        val source = s.getSourceAs<GeoJsonSource>("rally_route")
        if (source == null) {
            s.addSource(GeoJsonSource("rally_route", FeatureCollection.fromFeatures(arrayOf(line))))
            s.addLayer(LineLayer("rally_line","rally_route").withProperties(
                lineColor("#8058D8"), lineWidth(7f), lineOpacity(0.94f)))
        } else source.setGeoJson(FeatureCollection.fromFeatures(arrayOf(line)))
        val turns = p.notes.mapNotNull { n ->
            val a = p.metresAtPoint.indexOfFirst { it >= (n.metresFromStart-20).coerceAtLeast(0.0) }.coerceAtLeast(0)
            val b = p.metresAtPoint.indexOfFirst { it >= n.metresFromStart+45 }.let { if (it<0) p.points.lastIndex else it }
            if (b-a < 2) null else Feature.fromGeometry(LineString.fromLngLats(p.points.subList(a,b+1).map { Point.fromLngLat(it.longitude,it.latitude) })).apply {
                addStringProperty("tone",PaceNotes.color(n.grade))
            }
        }
        val turnsCollection = FeatureCollection.fromFeatures(turns)
        val turnsSource = s.getSourceAs<GeoJsonSource>("rally_curves")
        if (turnsSource == null) {
            s.addSource(GeoJsonSource("rally_curves",turnsCollection))
            s.addLayer(LineLayer("rally_curves_layer","rally_curves").withProperties(
                lineColor(Expression.get("tone")), lineWidth(9f), lineOpacity(0.95f)))
        } else turnsSource.setGeoJson(turnsCollection)
        val dots = p.notes.map { n ->
            val index = p.metresAtPoint.indexOfFirst { it >= n.metresFromStart }.coerceIn(0,p.points.lastIndex)
            val at = p.points[index]
            Feature.fromGeometry(Point.fromLngLat(at.longitude,at.latitude)).apply {
                addStringProperty("tone", PaceNotes.color(n.grade))
                addStringProperty("label", "${if(n.side==PaceNote.Side.LEFT) "I" else "D"}${n.grade}")
            }
        }
        val dotsCollection = FeatureCollection.fromFeatures(dots)
        val dotsSource = s.getSourceAs<GeoJsonSource>("rally_dots")
        if (dotsSource == null) {
            s.addSource(GeoJsonSource("rally_dots",dotsCollection))
            s.addLayer(CircleLayer("rally_dots_layer","rally_dots").withProperties(
                circleColor(Expression.get("tone")),circleRadius(5f),circleStrokeColor("#FFFFFF"),circleStrokeWidth(1.5f)))
        } else dotsSource.setGeoJson(dotsCollection)
        if (s.getLayer("rally_note_labels") == null) {
            s.addLayer(SymbolLayer("rally_note_labels", "rally_dots").withProperties(
                textField(Expression.get("label")), textSize(12f), textColor("#FFFFFF"),
                textHaloColor("#111827"), textHaloWidth(2f), textAllowOverlap(true),
                textOffset(arrayOf(0f,-1.55f))))
        }
        drawWaypoints()
        drawRecceFocus()
        // Route and bend layers may have been added after the GPS marker.
        // Recreate the marker layers last, so the vehicle is always above coloured roads.
        if (s.getSource("rally_me") != null) {
            for (id in listOf("rally_car_layer", "rally_me_layer", "rally_car_halo")) {
                if (s.getLayer(id) != null) s.removeLayer(id)
            }
            s.addLayer(CircleLayer("rally_car_halo", "rally_me").withProperties(
                circleColor("#0B2134"), circleRadius(24f), circleOpacity(0.72f),
                circleStrokeColor("#79E6FF"), circleStrokeWidth(2.4f)))
            s.addLayer(CircleLayer("rally_me_layer", "rally_me").withProperties(
                circleColor(sky), circleRadius(11f),
                circleStrokeColor("#FFFFFF"), circleStrokeWidth(3f)))
            updateVehicleLayers()
        }
        drawRoadBumps()
        raiseWaypointLayers()
        if (navigating || demo) updateRouteProgress(if(demo) demoPosition else navigator?.locate(gps ?: p.points.first())?.metres ?: 0.0)
    }
    /**
     * OSM is an optional enhancement; missing data or network errors never claim the
     * road is obstacle-free. One lookup per newly prepared/recalculated route only.
     */
    private fun refreshRoadBumps() {
        val route = plan
        val session = ++bumpLookupSession
        roadBumps = emptyList()
        announcedBumps.clear()
        if (::bumpBanner.isInitialized) bumpBanner.visibility = View.GONE
        if (route == null) { drawRoadBumps(); return }
        roadBumps = TrafficCalming.onRoute(route, manualBumpStore.list())
        drawRoadBumps()
        thread {
            val osm = runCatching { OSMTrafficCalmingService.fetchFor(route) }
                .getOrElse { emptyList() }
            val complete = TrafficCalming.onRoute(route, osm + manualBumpStore.list())
            runOnUiThread {
                if (session == bumpLookupSession && plan === route && !isFinishing && !isDestroyed) {
                    roadBumps = complete
                    drawRoadBumps()
                }
            }
        }
    }

    private fun drawRoadBumps() {
        val style = map?.style ?: return
        val visible = if (plan == null)
            manualBumpStore.list()
        else roadBumps.map { it.raw }
        val features = visible.map { bump ->
            Feature.fromGeometry(Point.fromLngLat(bump.point.longitude,bump.point.latitude))
                .apply {
                    addStringProperty("label", if (bump.source == BumpSource.MANUAL) "M" else "R")
                    addStringProperty("kind", bump.kind)
                }
        }
        val collection = FeatureCollection.fromFeatures(features)
        val source = style.getSourceAs<GeoJsonSource>("rally_traffic_calming")
        if (source == null) {
            style.addSource(GeoJsonSource("rally_traffic_calming",collection))
            style.addLayer(CircleLayer("rally_traffic_calming_dots","rally_traffic_calming").withProperties(
                circleColor("#FFC44D"), circleRadius(13f),
                circleStrokeColor("#171921"), circleStrokeWidth(3f)))
            style.addLayer(SymbolLayer("rally_traffic_calming_labels","rally_traffic_calming")
                .withProperties(textField(Expression.get("label")), textSize(13f),
                    textColor("#191B26"), textAllowOverlap(true)))
        } else source.setGeoJson(collection)
    }

    private fun updateRoadBumpWarning(
        progressMetres: Double, distanceToNote: Double, speedMps: Double,
        priorityNote: Boolean, nowMs: Long
    ) {
        if (!::bumpBanner.isInitialized) return
        val upcoming = TrafficCalming.upcoming(roadBumps, progressMetres)
        if (upcoming == null) { bumpBanner.visibility = View.GONE; return }
        val distance = upcoming.routeMetres-progressMetres
        if (distance < -8.0 || distance > 200.0) {
            bumpBanner.visibility = View.GONE
            return
        }
        val distanceText = if (distance <= 16.0) "AHORA" else
            "${((distance/10.0).roundToInt()*10).coerceAtLeast(10)} m"
        val sourceLabel = if(upcoming.raw.source==BumpSource.MANUAL) "AGREGADO POR VOS"
            else "REGISTRADO EN OSM"
        bumpBanner.text = "⚠ REDUCTOR · $distanceText  ·  $sourceLabel"
        bumpBanner.visibility = View.VISIBLE
        val elapsed = if (lastPriorityNoteAtMs < 0) 30_000L else nowMs-lastPriorityNoteAtMs
        val busy = tts?.isSpeaking == true || rallyAudio.isBusy() || reroutePolicy.isBusy()
        val canSpeak = voiceOn && speakReady &&
            RallySafetySpeech.mayAlert(distance,speedMps,distanceToNote,priorityNote,busy,elapsed)
        if (canSpeak && announcedBumps.add(upcoming.raw.id)) {
            say(when {
                appLanguage.startsWith("en") -> "Caution, mapped traffic calming ahead. Confirm visually."
                appLanguage.startsWith("pt") -> "Atenção, redutor sinalizado no mapa. Confira visualmente."
                else -> "Atención, ${TrafficCalming.voiceName(upcoming.raw.kind)} señalado en el mapa. Verificá visualmente."
            })
        }
    }

    /** High contrast map pins, visible above route strokes and labels. */
    private fun drawWaypoints() {
        val style = map?.style ?: return
        val markers = mutableListOf<Feature>()
        fun marker(point: GeoPoint, label: String, kind: String) {
            if (!point.latitude.isFinite() || !point.longitude.isFinite()) return
            markers.add(Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)).apply {
                addStringProperty("label", label)
                addStringProperty("kind", kind)
            })
        }
        val origin = chosenOrigin ?: plan?.points?.firstOrNull()
        origin?.let { marker(it, "ORIGEN", "origin") }
        (target ?: routeDestination ?: plan?.points?.lastOrNull())?.let {
            marker(it, "DESTINO", "destination")
        }
        intermediatePoints.forEachIndexed { index, p -> marker(p, "PARADA ${index+1}", "stop") }
        val features = FeatureCollection.fromFeatures(markers)
        val source = style.getSourceAs<GeoJsonSource>("rally_waypoints")
        if (source == null) {
            style.addSource(GeoJsonSource("rally_waypoints", features))
            for (kind in WaypointArtwork.Kind.values()) {
                style.addImage("rally_pin_" + kind.id, WaypointArtwork.bitmap(kind))
                val symbol = SymbolLayer("rally_pin_layer_" + kind.id, "rally_waypoints")
                    .withProperties(
                        iconImage("rally_pin_" + kind.id), iconSize(0.74f),
                        iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
                        iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                        iconAllowOverlap(true), iconIgnorePlacement(true))
                symbol.setFilter(Expression.eq(Expression.get("kind"), Expression.literal(kind.id)))
                style.addLayer(symbol)
            }
            style.addLayer(SymbolLayer("rally_waypoints_labels", "rally_waypoints").withProperties(
                textField(Expression.get("label")), textSize(12f), textColor("#FFFFFF"),
                textHaloColor("#101827"), textHaloWidth(2.5f), textAllowOverlap(true),
                textOffset(arrayOf(0f, 0.8f))))
        } else source.setGeoJson(features)
        raiseWaypointLayers()
    }

    private fun raiseWaypointLayers() {
        val style = map?.style ?: return
        for (id in listOf("rally_pin_layer_origin", "rally_pin_layer_stop",
            "rally_pin_layer_destination", "rally_waypoints_labels")) {
            val layer = style.getLayer(id) ?: continue
            // MapLibre allows re-adding a Layer only when removed by object, not by ID.
            style.removeLayer(layer)
            style.addLayer(layer)
        }
    }

    /**
     * Real-position map marker: the Subaru's hood always points in the actual driving
     * direction (route tangent in DEMO, reliable GPS heading otherwise).
     */
    private fun carBearing(point: GeoPoint): Float {
        val route = plan
        if ((navigating || demo || demoPaused) && route != null && route.points.size >= 2) {
            val metres = if (demo || demoPaused) demoPosition else navigator?.locate(point)?.metres ?: 0.0
            val a = DrivingCamera.pointAt(route, (metres - 4.0).coerceAtLeast(0.0))
            val b = DrivingCamera.pointAt(route, (metres + 20.0).coerceAtMost(route.totalMetres))
            if (DrivingCamera.distance(a,b) >= 1.0) return DrivingCamera.bearing(a,b).toFloat()
        }
        return if (lastLocation?.hasBearing() == true) lastLocation!!.bearing else 0f
    }

    private fun drawPosition(p: GeoPoint) {
        val style = map?.style ?: return
        val point = Feature.fromGeometry(Point.fromLngLat(p.longitude, p.latitude))
        val source = style.getSourceAs<GeoJsonSource>("rally_me")
        if (source == null) {
            style.addSource(GeoJsonSource("rally_me", point))
            style.addLayer(CircleLayer("rally_car_halo", "rally_me").withProperties(
                circleColor("#0B2134"), circleRadius(24f), circleOpacity(0.72f),
                circleStrokeColor("#79E6FF"), circleStrokeWidth(2.4f)))
            // Classic GPS dot remains available if selected or if bitmap decoding fails.
            style.addLayer(CircleLayer("rally_me_layer", "rally_me").withProperties(
                circleColor(sky), circleRadius(11f),
                circleStrokeColor("#FFFFFF"), circleStrokeWidth(3f)))
        } else {
            source.setGeoJson(point)
        }
        updateVehicleLayers()
        style.getLayerAs<SymbolLayer>("rally_car_layer")?.setProperties(iconRotate(carBearing(p)))
        if ((navigating || demo) && followRouteCamera) {
            val route = plan ?: return
            val progressM = if (demo) demoPosition else navigator?.locate(p)?.metres ?: 0.0
            val speed = if (demo) demoSpeed else lastLocation?.speed?.toDouble() ?: 0.0
            val pose = drivingCamera.pose(route, progressM, speed, map3d)
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .target(LatLng(pose.focus.latitude, pose.focus.longitude))
                .zoom(pose.zoom).tilt(pose.tilt).bearing(pose.bearing).build()), if (demo) 430 else 650)
        }
    }

    /**
     * Switch map sprite in place. All bitmaps face geographic north before rotation,
     * including the selected vehicle's thumbnail in the chooser.
     */
    private fun updateVehicleLayers() {
        val style = map?.style ?: return
        if (style.getSource("rally_me") == null) return
        val selected = RallyVehicles.selectedId(prefs)
        var activeCar = selected != RallyVehicles.DOT
        if (activeCar) {
            val bitmap = runCatching { RallyVehicles.marker(selected) }.getOrNull()
            if (bitmap != null) {
                val imageKey = "rally_vehicle_" + selected
                if (vehicleImageStyle !== style) {
                    vehicleImageStyle = style
                    vehicleImageKeys.clear()
                }
                if (vehicleImageKeys.add(imageKey)) {
                    style.addImage(imageKey, bitmap)
                }
                val setting = prefs.getString("vehicle_marker_size", "large")
                val size = when (setting) {
                    "normal" -> if (selected == RallyVehicles.DEFAULT) 1.9f else 1.55f
                    "xl" -> if (selected == RallyVehicles.DEFAULT) 3.7f else 3.0f
                    else -> if (selected == RallyVehicles.DEFAULT) 2.85f else 2.35f
                }
                val bearing = (if (demo || demoPaused) plan?.points?.getOrNull(positionIndex) else gps)
                    ?.let { carBearing(it) } ?: 0f
                val layer = style.getLayerAs<SymbolLayer>("rally_car_layer")
                if (layer == null) {
                    style.addLayer(SymbolLayer("rally_car_layer", "rally_me").withProperties(
                        iconImage(imageKey),
                        iconSize(size),
                        iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                        iconAllowOverlap(true), iconIgnorePlacement(true),
                        iconRotate(bearing)))
                } else {
                    layer.setProperties(iconImage(imageKey), iconSize(size),
                        iconRotate(bearing), visibility(Property.VISIBLE))
                }
            } else activeCar = false
        }
        val radius = when (prefs.getString("vehicle_marker_size", "large")) {
            "normal" -> 19f
            "xl" -> 39f
            else -> 29f
        }
        style.getLayerAs<CircleLayer>("rally_car_halo")?.setProperties(
            circleRadius(radius),
            visibility(if (activeCar) Property.VISIBLE else Property.NONE))
        style.getLayer("rally_car_layer")?.setProperties(
            visibility(if (activeCar) Property.VISIBLE else Property.NONE))
        style.getLayer("rally_me_layer")?.setProperties(
            visibility(if (activeCar) Property.NONE else Property.VISIBLE))
    }

    private fun installMapStyle(current: MapLibreMap? = map) {
        val activeMap = current ?: return
        vehicleImageStyle = null
        vehicleImageKeys.clear()
        lastRenderedProgress = -1000.0
        activeMap.setStyle(Style.Builder().fromUri(mapVisualStyle.url)) {
            drawPlan()
            drawWaypoints()
            drawRoadBumps()
            drawLocation()
            raiseWaypointLayers()
            val route = plan
            if (route != null && (navigating || demo)) {
                updateRouteProgress(if (demo) demoPosition else
                    navigator?.locate(gps ?: route.points.first())?.metres ?: 0.0)
            }
        }
    }

    private fun chooseMapStyle() {
        if (navigating || demo) {
            status("Cambiá el diseño del mapa con la navegación detenida", true)
            return
        }
        val available = arrayOf(tr("map_dark"), tr("map_day"))
        android.app.AlertDialog.Builder(this).setTitle(tr("map_style"))
            .setSingleChoiceItems(available, if(mapVisualStyle == RallyMapStyle.DARK) 0 else 1) { dialog, index ->
                mapVisualStyle = if(index==0) RallyMapStyle.DARK else RallyMapStyle.DAY
                prefs.edit().putString("map_visual_style",mapVisualStyle.id).apply()
                installMapStyle()
                dialog.dismiss()
            }
            .setNegativeButton(tr("back"),null).show()
    }

    private fun selectVehicleMarkerSize() {
        val sizes = listOf("normal", "large", "xl")
        val labels = arrayOf(tr("size_normal"), tr("size_large"), tr("size_xl"))
        val current = prefs.getString("vehicle_marker_size", "large")
        android.app.AlertDialog.Builder(this).setTitle(tr("vehicle_size"))
            .setSingleChoiceItems(labels, sizes.indexOf(current).coerceAtLeast(1)) { dialog, which ->
                prefs.edit().putString("vehicle_marker_size", sizes[which]).apply()
                updateVehicleLayers()
                dialog.dismiss()
            }
            .setNegativeButton(tr("back"), null).show()
    }

    /** Real, scrollable, offline image gallery; selection persists across app restarts. */
    private fun selectVehicleMarker() {
        val selectedBefore = RallyVehicles.selectedId(prefs)
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            setBackgroundColor(Color.parseColor("#0D1524"))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(12))
        }
        val intro = label("Elegí el vehículo que vas a ver en el mapa.", 13f, muted)
        content.addView(intro, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(10) })
        lateinit var dialog: android.app.AlertDialog
        var lastGroup = ""
        for (chunk in RallyVehicles.models.groupBy { it.group }) {
            val groupName = chunk.key
            if (groupName != lastGroup) {
                content.addView(label(groupName.uppercase(Locale.ROOT), 12f, sky, true),
                    LinearLayout.LayoutParams(-1,dp(32)))
                lastGroup = groupName
            }
            for (rowModels in chunk.value.chunked(2)) {
                val vehicleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                for (model in rowModels) {
                    val active = model.id == selectedBefore
                    val tile = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER_HORIZONTAL
                        setPadding(dp(5), dp(7), dp(5), dp(7))
                        background = shape(if (active) "#16364C" else "#172336", 10,
                            if (active) "#22D3EE" else "#283F59")
                        isClickable = true
                        isFocusable = true
                        contentDescription = model.name + if (active) ", seleccionado" else ""
                    }
                    val image = ImageView(this).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageBitmap(RallyVehicles.preview(model.id))
                        contentDescription = model.name + ", vista superior"
                    }
                    tile.addView(image, LinearLayout.LayoutParams(dp(88), dp(106)))
                    val text = label(model.name, 11f, white, true).apply {
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    tile.addView(text, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin=dp(3) })
                    val chosen = label(if(active) "✓ SELECCIONADO" else "TOCAR PARA ELEGIR",
                        9f, if(active) "#69E7C4" else muted, true).apply {
                        gravity = Gravity.CENTER
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                    }
                    tile.addView(chosen, LinearLayout.LayoutParams(-1,dp(23)))
                    tile.setOnClickListener {
                        prefs.edit()
                            .putString(RallyVehicles.KEY, model.id)
                            .putBoolean("subaru_marker", model.id != RallyVehicles.DOT)
                            .apply()
                        updateVehicleLayers()
                        status("Vehículo GPS: " + model.name, false)
                        dialog.dismiss()
                    }
                    vehicleRow.addView(tile, LinearLayout.LayoutParams(0,dp(198),1f).apply {
                        marginStart=dp(3); marginEnd=dp(3); bottomMargin=dp(7)
                    })
                }
                repeat(2 - rowModels.size) {
                    vehicleRow.addView(View(this), LinearLayout.LayoutParams(0,dp(198),1f))
                }
                content.addView(vehicleRow,LinearLayout.LayoutParams(-1,-2))
            }
        }
        scroll.addView(content)
        dialog = android.app.AlertDialog.Builder(this).setTitle("VEHÍCULOS · RALLY GPS")
            .setView(scroll)
            .setNegativeButton("CERRAR",null)
            .create()
        dialog.show()
        dialog.window?.setLayout(
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.81f).toInt())
    }

    private fun drawLocation() {
        val current = if (demo || demoPaused) plan?.let {
            DrivingCamera.pointAt(it,demoPosition)
        } else gps
        current?.let { drawPosition(it) }
    }
    private fun toggle3D() {
        map3d = !map3d
        modeButton.text = if (map3d) "3D" else "2D"
        drivingCamera.reset()
        val live = if (demo || demoPaused) plan?.points?.getOrNull(positionIndex) else gps
        if ((navigating || demo) && live != null) drawPosition(live)
        else (gps ?: plan?.points?.firstOrNull())?.let { point ->
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .target(LatLng(point.latitude,point.longitude)).zoom(15.5)
                .tilt(if(map3d) 50.0 else 0.0).bearing(0.0).build()), 450)
        }
    }
    private fun centerGps() {
        if (navigating || demo || demoPaused) {
            followRouteCamera = true
            val current = if (demo || demoPaused) plan?.points?.getOrNull(positionIndex) else gps
            current?.let { drawPosition(it) }
            return
        }
        val p = gps ?: run { requestGps(); status("Esperando ubicación GPS",true);return }
        map?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
            .target(LatLng(p.latitude,p.longitude)).zoom(15.5).tilt(0.0).bearing(0.0).build()),450)
    }
    private fun requestGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),900)
            return
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,2f,this)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,2000,8f,this)
            val last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if(last != null) onLocationChanged(last)
        } catch (e: SecurityException) { status("Ubicación no autorizada",true) }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==900) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                requestGps()
            } else {
                status("Para notas de rally y velocidad necesitás ubicación precisa", true)
            }
        }
        else if (requestCode==900) status("Podés usar GPX y DEMO sin permiso GPS",true)
    }
    override fun onLocationChanged(location: Location) {
        val ageMs = (android.os.SystemClock.elapsedRealtimeNanos() -
            location.elapsedRealtimeNanos) / 1_000_000L
        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        if (!GpsFixPolicy.usable(location.latitude,location.longitude,accuracy,ageMs)) {
            if (navigating) status("GPS poco preciso: esperando señal confiable", true)
            return
        }
        val speed = GpsFixPolicy.safeSpeed(location.speed,location.hasSpeed())
        gps = GeoPoint(location.latitude,location.longitude); lastLocation=location
        if (navigating) {
            realTripRecorder?.accept(gps!!,speed,accuracy ?: 100f,
                SystemClock.elapsedRealtime())
            updateProgress(gps!!, speed)
        }
        if (!demo && !demoPaused) drawPosition(gps!!)
        if (firstGpsCamera) { firstGpsCamera=false; if (!navigating) centerGps() }
        if (!navigating && !demo && !demoPaused) status("GPS disponible · mantené pulsado el mapa para elegir destino",false)
    }

    private fun importGpx() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="*/*"; addCategory(Intent.CATEGORY_OPENABLE) }
        startActivityForResult(intent,101)
    }
    private fun exportGpx() {
        if (plan == null) { status("Primero cargá una ruta",true);return }
        val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type="application/gpx+xml"; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_TITLE,"RallyGPSAI-recorrido.gpx")
        }
        startActivityForResult(intent,102)
    }
    @Deprecated("Android Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK || data?.data==null) return
        when(requestCode) {
            101 -> {
                val uri = data.data!!
                execute("Analizando GPX", {
                    val pts = contentResolver.openInputStream(uri)?.use { GpxIO.read(it) } ?: error("Archivo vacío")
                    PaceNoteEngine.createPlan(pts)
                }) { result ->
                    setPreparedPlan(result as RoutePlan, true)
                    status("GPX cargado: ${plan?.notes?.size} curvas aproximadas",false)
                }
            }
            102 -> {
                val uri = data.data!!
                val pts = plan?.points.orEmpty()
                execute("Guardando GPX", {
                    contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(GpxIO.write(pts)) }
                        ?: error("No fue posible abrir el archivo")
                    true
                }) { status("GPX guardado en tu dispositivo",false) }
            }
        }
    }
    /** Finish an actual GPS trip. DEMO never creates a recorded speed or statistics. */
    private fun finishRealTrip() {
        val recorder = realTripRecorder ?: return
        realTripRecorder = null
        val record = recorder.finish(SystemClock.elapsedRealtime()) ?: return
        thread {
            runCatching { tripHistory.save(record) }.onFailure { failure ->
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) status(
                        "No se pudo guardar el historial: " + (failure.message ?: "error de almacenamiento"), true)
                }
            }
        }
    }

    private fun showTripHistory() {
        if (navigating || demo) {
            status("Finalizá el recorrido para consultar el historial", true)
            return
        }
        thread {
            val result = runCatching { tripHistory.list() }.getOrElse { emptyList() }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (result.isEmpty()) {
                    android.app.AlertDialog.Builder(this)
                        .setTitle(tr("history"))
                        .setMessage("Todavía no hay recorridos reales guardados. Las DEMO no generan estadísticas.")
                        .setPositiveButton("CERRAR", null).show()
                } else {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es","AR"))
                    val rows = result.mapIndexed { index,trip ->
                        "Ruta " + (result.size-index) + " · " + dateFormat.format(Date(trip.startedAtWallMs)) +
                        "\n" + String.format(Locale("es","AR"), "%.1f km  ·  %.0f km/h máx.",
                            trip.distanceM/1000.0, trip.maximumKmh)
                    }.toTypedArray()
                    android.app.AlertDialog.Builder(this).setTitle(tr("history"))
                        .setItems(rows) { _, index -> showTripDetails(result[index], result.size-index) }
                        .setNegativeButton("CERRAR", null).show()
                }
            }
        }
    }

    private fun showTripDetails(record: TripRecord, number: Int) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            background = shape(bg, 14)
        }
        val preview = TripRoutePreview(this, record.actualTrack.ifEmpty { record.route })
        root.addView(preview, LinearLayout.LayoutParams(-1, dp(160)))
        val totalSeconds = record.durationMs / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        val duration = String.format(Locale.ROOT, "%02d:%02d:%02d",h,m,s)
        val stats = String.format(Locale("es","AR"),
            "TIEMPO\n%s\n\nDISTANCIA RECORRIDA\n%.2f km\n\nVELOCIDAD PROMEDIO\n%.1f km/h\n\nVELOCIDAD MÁXIMA\n%.1f km/h",
            duration, record.distanceM/1000.0, record.averageKmh, record.maximumKmh)
        root.addView(label(stats, 15f, white, true), LinearLayout.LayoutParams(-1,-2).apply {
            topMargin=dp(16)
        })
        root.addView(label("Estadísticas estimadas a partir del GPS real. No incluyen DEMO.",
            11f, muted),LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(12) })
        val scroll = ScrollView(this)
        scroll.addView(root)
        android.app.AlertDialog.Builder(this).setTitle("RUTA $number")
            .setView(scroll)
            .setPositiveButton("REPETIR RUTA") { _, _ -> openStoredTrip(record, false) }
            .setNeutralButton("VER MAPA") { _, _ -> openStoredTrip(record, true) }
            .setNegativeButton("ELIMINAR") { _, _ ->
                android.app.AlertDialog.Builder(this)
                    .setTitle("¿Eliminar recorrido?")
                    .setMessage("Se borrará el registro guardado en este teléfono.")
                    .setPositiveButton("ELIMINAR") { _, _ ->
                        thread { tripHistory.delete(record.id) }
                    }
                    .setNegativeButton("CANCELAR", null).show()
            }.show()
    }

    private fun openStoredTrip(record: TripRecord, viewActualTrack: Boolean) {
        if (navigating || demo) return
        val points = if (viewActualTrack && record.actualTrack.size >= 2)
            record.actualTrack else record.route
        execute(if(viewActualTrack) "Mostrando trazado registrado" else "Preparando recorrido guardado", {
            PaceNoteEngine.createPlan(points)
        }) { result ->
            setPreparedPlan(result as RoutePlan, true)
            status(if (viewActualTrack) "Trazado registrado cargado en el mapa"
                else "Ruta guardada lista: podés iniciar GPS o DEMO",false)
        }
    }


    /** Reconnaissance is available only while parked/not navigating. */
    private fun showRecce(index: Int? = null) {
        if (navigating || demo) {
            status("Finalizá la navegación para revisar y editar notas", true)
            return
        }
        val route = plan
        if (route == null || route.notes.isEmpty()) {
            android.app.AlertDialog.Builder(this).setTitle(tr("recce"))
                .setMessage("Primero cargá una ruta con curvas detectadas (OSRM o GPX).")
                .setPositiveButton("ENTENDIDO",null).show()
            return
        }
        if (index != null) {
            showRecceNote(index.coerceIn(0,route.notes.lastIndex))
            return
        }
        val labels = route.notes.mapIndexed { i,n ->
            val symbol=if (n.side == PaceNote.Side.LEFT) "↰" else "↱"
            "${i+1}. ${RallyDistanceFormatter.remaining(n.metresFromStart)} · $symbol ${n.grade}" +
                if (noteEdits.any { it.baseIndex == i }) "  ✎" else ""
        }.toTypedArray()
        android.app.AlertDialog.Builder(this).setTitle(tr("recce") + " · ${route.notes.size} notas")
            .setItems(labels) { _, i -> showRecceNote(i) }
            .setNeutralButton("ELEGIR EN MAPA") { _, _ ->
                destinationTapArmed = false
                mapSelectMode = 0
                recceMapSelection = true
                status("Tocá una curva resaltada en el mapa para revisarla", false)
                Toast.makeText(this, "Tocá una curva del recorrido", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton(tr("back"),null).show()
    }

    private fun focusRecceNote(index: Int) {
        val route = plan ?: return
        val note = route.notes.getOrNull(index) ?: return
        recceNoteIndex = index
        drawRecceFocus()
        val i = route.metresAtPoint.binarySearch(note.metresFromStart).let {
            if (it < 0) -it-1 else it
        }.coerceIn(0,route.points.lastIndex)
        val point = route.points[i]
        map?.animateCamera(CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder()
                .target(LatLng(point.latitude,point.longitude)).zoom(16.5).tilt(0.0).build()), 400)
    }

    private fun drawRecceFocus() {
        val style = map?.style ?: return
        val route = plan
        val note = recceNoteIndex?.let { route?.notes?.getOrNull(it) }
        if (route == null || note == null) {
            style.getLayer("rally_recce_layer")?.setProperties(visibility(Property.NONE))
            return
        }
        style.getLayer("rally_recce_layer")?.setProperties(visibility(Property.VISIBLE))
        val i = route.metresAtPoint.binarySearch(note.metresFromStart).let {
            if(it<0) -it-1 else it
        }.coerceIn(0,route.points.lastIndex)
        val p = route.points[i]
        val source = style.getSourceAs<GeoJsonSource>("rally_recce")
        val point = Feature.fromGeometry(Point.fromLngLat(p.longitude,p.latitude))
        if(source == null) {
            style.addSource(GeoJsonSource("rally_recce",point))
            style.addLayer(CircleLayer("rally_recce_layer","rally_recce").withProperties(
                circleColor("#FFD348"),circleRadius(13f),
                circleStrokeColor("#111827"),circleStrokeWidth(3f)))
        } else source.setGeoJson(point)
    }

    private fun showRecceNote(index: Int) {
        val route = plan ?: return
        val n = route.notes.getOrNull(index) ?: return
        focusRecceNote(index)
        val follows=route.notes.getOrNull(index+1)
        val heading = "${index+1}/${route.notes.size} · " +
            (if(n.side==PaceNote.Side.LEFT) tr("left") else tr("right"))+" ${n.grade}"
        val details = RallyNotePresentation.render(n,follows, n.metresFromStart)
        val text = "Desde la salida: ${RallyDistanceFormatter.remaining(n.metresFromStart)}\n" +
            "Nota: ${RallyLanguage.call(appLanguage,n,follows)}\n" +
            "Características: ${details.featuresLabel}\n" +
            if (noteEdits.any { it.baseIndex == index }) "✎ CORREGIDA MANUALMENTE" else "NOTA ESTIMADA DEL MAPA"
        val actions = arrayOf(tr("previous"), tr("following"), tr("edit_note"), tr("listen"),
            "VER MAPA", tr("restore"), tr("recce"))
        android.app.AlertDialog.Builder(this).setTitle(heading)
            .setMessage(text)
            .setItems(actions) { _, action ->
                when(action) {
                    0 -> showRecce((index-1).coerceAtLeast(0))
                    1 -> showRecce((index+1).coerceAtMost(route.notes.lastIndex))
                    2 -> showEditNote(index)
                    3 -> {
                        speakRallyNote(n,follows,60.0)
                        showRecce(index)
                    }
                    4 -> status("Curva ${index+1} resaltada en amarillo", false)
                    5 -> resetEditedNote(index)
                    6 -> showRecce()
                }
            }
            .setNegativeButton(tr("back")) { _,_ -> showRecce() }
            .show()
    }

    private fun resetEditedNote(index: Int) {
        val base = automaticNotePlan ?: return
        val changed = noteEdits.filterNot { it.baseIndex==index }
        if(!noteEditsStore.save(base,changed)) {
            Toast.makeText(this,"No se pudo guardar el cambio",Toast.LENGTH_LONG).show()
            return
        }
        noteEdits.clear(); noteEdits.addAll(changed)
        refreshManualNotes()
        showRecce(index)
    }

    /** Edits only affect pacenotes, never map geometry, routing or vehicle position. */
    private fun showEditNote(index: Int) {
        if(navigating || demo) return
        val base = automaticNotePlan ?: return
        val original = base.notes.getOrNull(index) ?: return
        val n = plan?.notes?.getOrNull(index) ?: original
        val layout = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(20),dp(10),dp(20),dp(12))
        }
        val direction = Spinner(this)
        direction.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf(tr("left"),tr("right")))
        direction.setSelection(if(n.side==PaceNote.Side.LEFT) 0 else 1)
        layout.addView(label("SENTIDO",12f,muted,true))
        layout.addView(direction)
        val gradeText = label(tr("grade")+": ${n.grade}",17f,white,true)
        val grade = SeekBar(this).apply {
            max=5; progress=n.grade-1
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(v:SeekBar?,progress:Int,fromUser:Boolean) {
                    gradeText.text=tr("grade")+": ${progress+1}"
                }
                override fun onStartTrackingTouch(v:SeekBar?){}
                override fun onStopTrackingTouch(v:SeekBar?){}
            })
        }
        layout.addView(gradeText)
        layout.addView(grade)
        val positionLabel=label("",14f,white)
        val offset = SeekBar(this).apply {
            max=160
            progress=(80+(n.metresFromStart-original.metresFromStart).toInt()).coerceIn(0,160)
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(v:SeekBar?,progress:Int,fromUser:Boolean) {
                    val delta = progress-80
                    positionLabel.text=tr("position")+": " +
                        RallyDistanceFormatter.remaining(original.metresFromStart+delta) +
                        "  (${if(delta>=0) "+" else ""}$delta m)"
                }
                override fun onStartTrackingTouch(v:SeekBar?){}
                override fun onStopTrackingTouch(v:SeekBar?){}
            })
        }
        positionLabel.text=tr("position")+": "+
            RallyDistanceFormatter.remaining(n.metresFromStart)
        layout.addView(positionLabel)
        layout.addView(offset)
        layout.addView(label(tr("modifiers"),12f,muted,true))
        val options = mutableListOf("SIN MODIFICADOR")
        (1..6).forEach { options += tr("close")+" $it" }
        (1..6).forEach { options += tr("open")+" $it" }
        val modifier = Spinner(this).apply {
            adapter=ArrayAdapter(this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,options)
            setSelection(when {
                n.tightensTo != null -> n.tightensTo
                n.opensTo != null -> n.opensTo + 6
                else -> 0
            })
        }
        layout.addView(modifier)
        layout.addView(label("DURACIÓN DE LA CURVA",12f,muted,true))
        val longOption = Spinner(this).apply {
            adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,
                arrayOf("Automática","LARGA","NO LARGA"))
            setSelection(when(n.manuallyLong){true->1;false->2;null->0})
        }
        layout.addView(longOption)
        val feedback=label("",12f,"#FF907B")
        layout.addView(feedback)
        val scroll=ScrollView(this)
        scroll.addView(layout)
        val dialog=android.app.AlertDialog.Builder(this).setTitle(tr("edit_note"))
            .setView(scroll)
            .setPositiveButton(tr("save"),null)
            .setNeutralButton(tr("restore"),null)
            .setNegativeButton(tr("back")) { _,_ -> showRecce(index) }
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val g=grade.progress+1
                val mod=modifier.selectedItemPosition
                val updated=NoteCorrection(index,original.metresFromStart,
                    original.metresFromStart+(offset.progress-80),
                    if(direction.selectedItemPosition==0) PaceNote.Side.LEFT else PaceNote.Side.RIGHT,
                    g,if(mod in 1..6) mod else null,if(mod in 7..12) mod-6 else null,
                    when(longOption.selectedItemPosition){1->true;2->false;else->null})
                val issue=EditablePacenotes.validate(base,updated)
                if(issue!=null) { feedback.text=issue; return@setOnClickListener }
                val proposed=noteEdits.filterNot { it.baseIndex==index }+updated
                val prepared=EditablePacenotes.apply(base,proposed)
                if(prepared.notes.zipWithNext().any { (a,b) -> b.metresFromStart-a.metresFromStart<5.0 }) {
                    feedback.text="La nota se cruza con otra modificación"
                    return@setOnClickListener
                }
                if(!noteEditsStore.save(base,proposed)) {
                    feedback.text="No se pudo guardar en el teléfono"
                    return@setOnClickListener
                }
                noteEdits.clear();noteEdits.addAll(proposed)
                refreshManualNotes()
                dialog.dismiss()
                showRecce(index)
            }
            dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                dialog.dismiss()
                resetEditedNote(index)
            }
        }
        dialog.show()
    }

    private fun showPointOptions() {
        val choices = arrayOf(tr("destination_point"), tr("origin_point"), tr("via_point"),
            tr("gps_origin"), tr("clear_stops"), "Agregar reductor en el mapa",
            "Borrar reductores manuales")
        android.app.AlertDialog.Builder(this).setTitle(tr("points_title"))
            .setItems(choices) { _, index ->
                when (index) {
                    0, 1, 2 -> {
                        mapSelectMode = when(index) { 1 -> 1; 2 -> 2; else -> 0 }
                        destinationTapArmed = index == 0
                        status(if (index == 0) "Tocá el punto en el mapa para marcar destino" else
                            "Mantené pulsado el punto elegido en el mapa", false)
                    }
                    3 -> { chosenOrigin = null; drawWaypoints(); status("Origen: GPS actual", false) }
                    4 -> { intermediatePoints.clear(); drawWaypoints(); status("Paradas eliminadas", false) }
                    5 -> {
                        if (navigating || demo) {
                            status("Detené la navegación antes de marcar un reductor", true)
                        } else {
                            mapSelectMode = 3
                            Toast.makeText(this, "Mantené pulsado el lugar del reductor",Toast.LENGTH_LONG).show()
                        }
                    }
                    6 -> android.app.AlertDialog.Builder(this)
                        .setTitle("Borrar reductores agregados")
                        .setMessage("Se eliminarán solo los reductores marcados por vos. Los de OpenStreetMap no se modifican.")
                        .setPositiveButton("BORRAR") { _, _ ->
                            manualBumpStore.clear(); refreshRoadBumps()
                        }.setNegativeButton("CANCELAR",null).show()
                }
            }.setNegativeButton("CERRAR",null).show()
    }
    private fun showDrivingControls(show: Boolean) {
        controlsVisible = show
        val state = NavigationUiPolicy.state(navigating, demo || demoPaused, show, plan != null)
        searchRow.visibility = if (state.showSearch) View.VISIBLE else View.GONE
        // Edit, import and export are never offered while the phone is navigating.
        toolsRow.visibility = if (!navigating && !demo && !demoPaused) View.VISIBLE else View.GONE
        updateDestinationIndicator()
    }

    private fun selectContextualMode() {
        val modes = listOf(ContextMode.OFF, ContextMode.OCCASIONAL, ContextMode.TALKATIVE)
        val labels = arrayOf(tr("context_off"), tr("context_occasional"), tr("context_talkative"))
        val saved = ContextMode.fromId(prefs.getString("context_commentary_mode", ContextMode.OCCASIONAL.id))
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("context_menu"))
            .setSingleChoiceItems(labels, modes.indexOf(saved)) { dialog, which ->
                val selected = modes[which]
                prefs.edit().putString("context_commentary_mode", selected.id).apply()
                contextualCopilot.reset()
                status(tr("context_menu") + ": " + labels[which], false)
                dialog.dismiss()
            }
            .setNeutralButton(tr("test_voice")) { _, _ ->
                if (voiceOn && speakReady && !navigating && !demo) {
                    val preview = ContextCue(ContextKind.WINDING, "preview")
                    say(RallyLanguage.contextComment(appLanguage, preview))
                } else if (navigating || demo) {
                    status("Probá la voz con el recorrido detenido", false)
                } else {
                    status("Activá la voz del copiloto para escuchar la prueba", false)
                }
            }
            .setNegativeButton(tr("settings"), null).show()
    }

    /** Four understandable sections instead of a long, undifferentiated list. */
    private fun showSettings() {
        val sections = arrayOf(
            tr("settings_nav") + "\n" + tr("settings_nav_desc"),
            tr("settings_voice") + "\n" + tr("settings_voice_desc"),
            tr("settings_map") + "\n" + tr("settings_map_desc"),
            tr("settings_app") + "\n" + tr("settings_app_desc")
        )
        android.app.AlertDialog.Builder(this).setTitle(tr("settings"))
            .setItems(sections) { _, section -> showSettingsSection(section) }
            .setNegativeButton(tr("back"), null).show()
    }

    private fun showSettingsSection(section: Int) {
        val indices: List<Int> = when (section) {
            0 -> listOf(0,1,2,3,4,15)
            1 -> listOf(5,6,13,7,8,9)
            2 -> listOf(11,14,16)
            else -> listOf(12,10)
        }
        val labelFor: (Int) -> String = { index ->
            when (index) {
                0 -> tr("warn_short")
                1 -> tr("warn_normal")
                2 -> tr("warn_early")
                3 -> tr("repeat")
                4 -> tr("all_notes")
                5 -> tr("copilot")
                6 -> tr("test_voice")
                7 -> tr("android_voice")
                8 -> tr("pitch")
                9 -> tr("engine")
                10 -> tr("licenses")
                11 -> tr("vehicle")
                12 -> tr("change")
                13 -> tr("context_menu")
                14 -> tr("vehicle_size")
                15 -> tr("note_view")
                16 -> tr("map_style")
                else -> ""
            }
        }
        val names = indices.map { i ->
            val selected = if (i == warnFactor) "✓  " else ""
            selected + labelFor(i)
        }.toTypedArray()
        val titles = arrayOf(tr("settings_nav"),tr("settings_voice"),tr("settings_map"),tr("settings_app"))
        android.app.AlertDialog.Builder(this).setTitle(titles.getOrElse(section) { tr("settings") })
            .setItems(names) { _, which -> handleSettingsOption(indices[which]) }
            .setNegativeButton(tr("back")) { _, _ -> showSettings() }.show()
    }

    private fun handleSettingsOption(index: Int) {
        when (index) {
            0,1,2 -> {
                warnFactor = index
                prefs.edit().putInt("anticipation", index).apply()
                status("Anticipación: " + arrayOf(tr("warn_short"),tr("warn_normal"),tr("warn_early"))[index],false)
            }
            3 -> plan?.notes?.getOrNull(lastSpoken)?.let {
                speakRallyNote(it,plan?.notes?.getOrNull(lastSpoken+1),50.0)
            }
            4 -> {
                val text = plan?.notes?.mapIndexed { j,n ->
                    "${j+1}. ${n.metresFromStart.toInt()} m: ${PaceNotes.phrase(n)}"
                }?.joinToString("\n") ?: "Todavía no hay un recorrido"
                android.app.AlertDialog.Builder(this).setTitle(tr("all_notes"))
                    .setMessage(text.take(14000)).setPositiveButton(tr("back"),null).show()
            }
            5 -> selectRallyVoiceMode()
            6 -> previewRallyVoice()
            7 -> chooseVoice()
            8 -> adjustVoice()
            9 -> openTtsSettings()
            10 -> android.app.AlertDialog.Builder(this).setTitle(tr("licenses"))
                .setMessage("CarlFM · rhasspy/piper-voices · MIT. Los audios OGG se generaron con datos declarados de dominio público. Ver docs/VOICE_LICENSES.md en GitHub.")
                .setPositiveButton(tr("back"),null).show()
            11 -> selectVehicleMarker()
            12 -> showWelcomeSetup(firstRun = false)
            13 -> selectContextualMode()
            14 -> selectVehicleMarkerSize()
            15 -> selectNoteViewMode()
            16 -> chooseMapStyle()
        }
    }

    override fun onStart() { super.onStart();mapView.onStart() }
    override fun onResume() {
        super.onResume(); mapView.onResume()
        if (voiceEngineSettingsOpened) {
            voiceEngineSettingsOpened = false
            initializeVoiceEngine()
        }
    }
    override fun onPause() { mapView.onPause();super.onPause() }
    override fun onStop() { mapView.onStop();super.onStop() }
    override fun onLowMemory() { super.onLowMemory();mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
    override fun onDestroy() {
        countdownSequence++
        countdownDialog?.dismiss()
        countdownDialog = null
        finishRealTrip()
        demo=false; demoPaused=false; ++demoTickGeneration; navigating=false; updateScreenAwake(); ui.removeCallbacksAndMessages(null)
        locationManager.removeUpdates(this)
        rallyAudio.stop()
        tts?.stop();tts?.shutdown()
        mapView.onDestroy();super.onDestroy()
    }
}
