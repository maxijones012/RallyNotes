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
import kotlin.concurrent.thread
import kotlin.math.*

/** MVP 0.1: navegación GPS foreground, ruta vía OSRM de DEMO, GPX, voz y simulación.
 * Pacenotes aproximadas: no son instrucciones de conducción deportiva en vía pública.
 */
class MainActivity : Activity(), LocationListener {
    private lateinit var mapView: MapView
    private var map: MapLibreMap? = null
    private var plan: RoutePlan? = null
    private var gps: GeoPoint? = null
    private var lastLocation: Location? = null
    private var target: GeoPoint? = null
    private var chosenOrigin: GeoPoint? = null
    private val intermediatePoints = mutableListOf<GeoPoint>()
    private var mapSelectMode = 0 // 0 destino, 1 origen, 2 parada
    private var destinationTapArmed = false // activado sólo desde PUNTOS → señalar destino
    private var chosenDestinationText = "PUNTO MARCADO"
    private var chosenDestinationApproximate = false
    private var navigator: NavigationEngine? = null
    private var controlsVisible = false
    private lateinit var locationManager: LocationManager
    private var navigating = false
    private var demo = false
    private var routeDestination: GeoPoint? = null
    private var pendingStops = listOf<GeoPoint>()
    private val reroutePolicy = ReroutePolicy()
    private var navigationSession = 0L
    private var voiceEngineSettingsOpened = false
    private var map3d = true
    private val drivingCamera = DrivingCamera()
    // Avoid uploading image textures to MapLibre on every GPS fix (often 1 Hz).
    private var vehicleImageStyle: Style? = null
    private val vehicleImageKeys = mutableSetOf<String>()
    private var followRouteCamera = true
    private var voiceOn = true
    private var appLanguage = "es-AR"
    private var warnFactor = 1
    private var positionIndex = 0
    private var lastSpoken = -1
    private var currentNote = 0
    private var lastNoteDistanceM = 0.0
    private var lastRemainingMetres = 0.0
    private val contextualCopilot = ContextCopilot()
    private var lastPriorityNoteAtMs = -1L
    private var contextualCommentIndex = 0
    private var firstGpsCamera = true
    private var demoPosition = 0.0
    private var demoSpeed = 55.0 / 3.6
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
    private lateinit var linkedNoteColumn: LinearLayout
    private lateinit var info: TextView
    private lateinit var tripDistance: TextView
    private lateinit var searchRow: LinearLayout
    private lateinit var searchButton: TextView
    private lateinit var pointsButton: TextView
    private lateinit var settingsButton: TextView
    private lateinit var nextCurveCaption: TextView
    private lateinit var destinationIndicator: TextView
    private lateinit var toolsRow: LinearLayout
    private lateinit var action: TextView
    private lateinit var gpsButton: TextView
    private lateinit var modeButton: TextView
    private lateinit var voiceButton: TextView
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
        buildUI()
        mapView.getMapAsync { loaded ->
            map = loaded
            loaded.uiSettings.isAttributionEnabled = true
            loaded.uiSettings.isLogoEnabled = true
            loaded.uiSettings.isCompassEnabled = true
            loaded.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/dark")) {
                drawPlan()
                drawWaypoints()
                drawLocation()
            }
            loaded.addOnMapLongClickListener { latlng ->
                val point = GeoPoint(latlng.latitude, latlng.longitude)
                when (mapSelectMode) {
                    1 -> { chosenOrigin = point; status("Origen elegido manualmente", false) }
                    2 -> {
                        if (intermediatePoints.size < 4) {
                            intermediatePoints.add(point)
                            status("Parada ${intermediatePoints.size} agregada", false)
                        } else status("Máximo de cuatro paradas", true)
                    }
                    else -> {
                        chooseDestination(point, "PUNTO DEL MAPA")
                        Toast.makeText(this, "✓ Destino elegido", Toast.LENGTH_SHORT).show()
                    }
                }
                mapSelectMode = 0
                destinationTapArmed = false
                drawWaypoints()
                true
            }
            loaded.addOnMapClickListener { position ->
                if (destinationTapArmed && !navigating && !demo) {
                    chooseDestination(GeoPoint(position.latitude, position.longitude), "PUNTO DEL MAPA")
                    Toast.makeText(this, "✓ Destino elegido", Toast.LENGTH_SHORT).show()
                    true
                } else {
                    if (navigating || demo) showDrivingControls(!controlsVisible)
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
        root.addView(body, ScrollView.LayoutParams(-1, -1))
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
        settingsButton.text = tr("settings")
        nextCurveCaption.text = tr("next")
        action.text = when {
            navigating || demo -> tr("finish")
            plan != null && target == null -> tr("go")
            else -> tr("route")
        }
        if (demo) nextButton.text = tr("pause")
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
        distanceNumber.textSize = if (display.distanceUnit.isEmpty()) 29f else 39f
        distanceUnit.text = display.distanceUnit
        primaryNoteTile.visibility = View.VISIBLE
        primaryNoteTile.show(display.grade, display.side)
        noteTitle.text = if (display.side == PaceNote.Side.LEFT) tr("left") else tr("right")
        noteDetail.text = if (RallyLanguage.isSpanish(appLanguage)) {
            display.featuresLabel + if (display.lowConfidence) " · " + tr("approx") else ""
        } else {
            val details = mutableListOf<String>()
            if (note.cumulativeTurnDegrees >= 145 && note.grade <= 2) details += tr("hairpin")
            if (note.endMetres-note.metresFromStart >= 95.0 || (note.cumulativeTurnDegrees >= 95 && note.grade >= 3)) details += tr("long")
            note.tightensTo?.let { if (it < note.grade) details += tr("close") + " " + it }
            note.opensTo?.let { if (it > note.grade) details += tr("open") + " " + it }
            if (display.lowConfidence) details += tr("approx")
            details.joinToString(" · ").ifBlank { tr("note") }
        }
        val linked = display.chained
        linkedNoteColumn.visibility = if (linked == null) View.GONE else View.VISIBLE
        if (linked != null) {
            linkedNoteTile.show(linked.grade, linked.side)
            linkedNoteLabel.text = tr("linked")
        }
    }

    private fun resetNoteCard(tone: String, badge: String, headline: String, subline: String) {
        setNoteVisual(tone, badge)
        nextTurnDistance.visibility = View.GONE
        primaryNoteTile.visibility = View.GONE
        linkedNoteColumn.visibility = View.GONE
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
        top.add(row(8) {
            add(label("◢", 23f, sky, true), dp(27))
            add(label("RALLY GPS AI", 16f, white, true), 0, -2, 1f)
            modeButton = button("3D", panel, sky).also { it.setOnClickListener { toggle3D() } }
            add(modeButton, dp(56), dp(48))
            voiceButton = button(if (voiceOn) tr("voice_on") else tr("voice_off"), panel, sky).also { it.setOnClickListener {
                voiceOn = !voiceOn; prefs.edit().putBoolean("voice", voiceOn).apply()
                it as TextView; it.text = if (voiceOn) tr("voice_on") else tr("voice_off")
                if (!voiceOn) { tts?.stop(); rallyAudio.stop() }
            } }
            add(voiceButton, dp(78), dp(48))
        })

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
        top.addView(searchRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
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
        top.addView(destinationIndicator, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
        banner = label(tr("gps"), 11f, muted)
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
            addView(noteBadge, LinearLayout.LayoutParams(-1, -2))
            addView(noteTitle, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
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
        val noteContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(7), dp(10), dp(10))
            addView(nextTurnDistance, LinearLayout.LayoutParams(-1, -2))
            addView(paceRow, LinearLayout.LayoutParams(-1, -2))
        }
        noteCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = shape("#1D2B42", 9, "#2A4762")
            addView(noteAccent, LinearLayout.LayoutParams(dp(5), -1))
            addView(noteContent, LinearLayout.LayoutParams(0, -2, 1f))
        }
        top.addView(noteCard, LinearLayout.LayoutParams(-1,-2).apply { topMargin = dp(10) })
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

        toolsRow = row(6) {
            pointsButton = button(tr("points")).apply { setOnClickListener { showPointOptions() } }
            add(pointsButton, 0,dp(46),1f)
            add(button("GPX ↓").apply { setOnClickListener { importGpx() } }, 0,dp(46),1f)
            add(button("GPX ↑").apply { setOnClickListener { exportGpx() } }, 0,dp(46),1f)
            settingsButton = button(tr("settings")).apply { setOnClickListener { showSettings() } }
            add(settingsButton,0,dp(46),1f)
        }
        bottom.addView(toolsRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        root.addView(bottom, FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM).apply {
            setMargins(dp(9),0,dp(9),dp(10) + bottomInset)
        })
        setContentView(root)
    }

    private fun updateDestinationIndicator(summary: String? = null, approximate: Boolean? = null) {
        if (summary != null) chosenDestinationText = summary.take(45).ifBlank { "PUNTO MARCADO" }
        if (approximate != null) chosenDestinationApproximate = approximate
        val point = target ?: routeDestination
        if (point == null) {
            destinationIndicator.visibility = View.GONE
            return
        }
        destinationIndicator.visibility = if ((navigating || demo) && !controlsVisible) View.GONE else View.VISIBLE
        val suffix = if (chosenDestinationApproximate) " · APROXIMADO" else ""
        destinationIndicator.text = "✓ DESTINO: $chosenDestinationText$suffix  ·  TOCÁ PARA VER"
        destinationIndicator.setTextColor(Color.parseColor(if (chosenDestinationApproximate) "#FFE49D" else "#ADFFE0"))
    }

    private fun chooseDestination(point: GeoPoint, name: String, approximate: Boolean = false) {
        target = point
        destinationTapArmed = false
        updateDestinationIndicator(name, approximate)
        drawWaypoints()
        status(if (approximate) "Dirección aproximada: verificá el pin en el mapa"
            else "Destino confirmado · tocá TRAZAR RUTA", approximate)
    }

    private fun status(message: String, error: Boolean) {
        banner.text = message
        banner.setTextColor(Color.parseColor(if (error) "#FF907B" else muted))
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
        if (navigating || demo) {
            navigationSession++
            reroutePolicy.reset()
            navigating = false; demo = false; tts?.stop(); rallyAudio.stop(); lastSpoken = -1
            contextualCopilot.reset(); lastPriorityNoteAtMs = -1L
            showDrivingControls(true)
            action.text = tr("go"); nextButton.text = "DEMO"
            resetNoteCard("#F0475A", "STOP", "NAVEGACIÓN DETENIDA", "Podés retomar el recorrido o trazar uno nuevo")
            status("Recorrido guardado. Podés retomarlo.", false)
            return
        }
        if (plan != null && target == null) { startGpsNavigation(); return }
        if (target == null) { status("Ingresá un destino o mantené pulsado el mapa", true); return }
        val origin = chosenOrigin ?: gps ?: run { status("Necesito GPS o un origen manual", true); requestGps(); return }
        val destination = target ?: return
        val stops = intermediatePoints.toList()
        execute("Calculando ruta (servidor demo)", {
            PaceNoteEngine.createPlan(RouteServices.route(origin, stops, destination))
        }) { result ->
            setPreparedPlan(result as RoutePlan, true)
            routeDestination = destination
            pendingStops = stops
            prefs.edit()
                .putString("last_road_destination", RouteStore.serialize(listOf(destination)))
                .putString("last_road_stops", RouteStore.serialize(stops)).apply()
            target = null
            updateDestinationIndicator("DESTINO DE LA RUTA")
            drawWaypoints()
            status("Ruta calculada. Si te desviás, intentará recalcular con internet.", false)
        }
    }
    private fun setPlan(points: List<GeoPoint>, save: Boolean) =
        setPreparedPlan(PaceNoteEngine.createPlan(points), save)

    private fun setPreparedPlan(parsed: RoutePlan, save: Boolean) {
        navigationSession++
        reroutePolicy.reset()
        routeDestination = null
        pendingStops = emptyList()
        updateDestinationIndicator()
        if (save) prefs.edit().remove("last_road_destination").remove("last_road_stops").apply()
        plan = parsed; navigator = NavigationEngine(parsed) { warnFactor }; drivingCamera.reset()
        contextualCopilot.reset(); lastPriorityNoteAtMs = -1L
        positionIndex = 0; lastSpoken = -1; currentNote = 0
        navigating = false; demo = false; demoPosition = 0.0
        action.text = tr("go"); nextButton.text = "DEMO"
        showDrivingControls(false)
        val km = "%.1f".format(Locale.US, parsed.totalMetres / 1000.0)
        resetNoteCard("#22D3EE", tr("ready"), parsed.notes.size.toString() + " " + tr("detected"), km + " km · " + tr("dest_sub"))
        info.text = parsed.notes.size.toString() + " " + tr("detected") + " · 1–6"
        lastRemainingMetres = parsed.totalMetres
        tripDistance.text = "$km km " + tr("remaining")
        drawPlan()
        if (save && parsed.points.size <= 30000) prefs.edit().putString("last_route", RouteStore.serialize(parsed.points)).apply()
        val center = parsed.points[parsed.points.size/2]
        map?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
            .target(LatLng(center.latitude, center.longitude)).zoom(12.7).tilt(0.0).build()), 600)
    }

    private fun startGpsNavigation() {
        if (gps == null) { requestGps(); status("Esperando GPS", true); return }
        navigationSession++
        reroutePolicy.reset()
        navigating = true; demo = false; positionIndex = 0; lastSpoken = -1; currentNote = 0
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
        if (demo) { demo=false; contextualCopilot.reset(); nextButton.text="DEMO"; action.text=tr("go"); showDrivingControls(true); return }
        if (plan == null) {
            val center = gps ?: GeoPoint(-43.249, -65.309)
            setPlan(PaceNotes.synthetic(center), false)
            status("RUTA SIMULADA: no corresponde a calles reales", false)
        }
        navigationSession++
        reroutePolicy.reset()
        demo = true; navigating = false; demoPosition = 0.0; positionIndex=0; lastSpoken=-1
        navigator?.reset(); drivingCamera.reset(); followRouteCamera = true; showDrivingControls(false)
        contextualCopilot.reset(); lastPriorityNoteAtMs = -1L
        action.text = tr("finish"); nextButton.text=tr("pause")
        val tick = object : Runnable {
            override fun run() {
                if (!demo) return
                val p = plan ?: return
                demoPosition += demoSpeed * 0.5
                val index = p.metresAtPoint.indexOfFirst { it >= demoPosition }.let { if (it<0) p.points.lastIndex else it }
                positionIndex = index
                val point = p.points[index]
                updateProgress(point, demoSpeed.toFloat())
                drawPosition(point)
                if (index >= p.points.lastIndex) { demo=false; action.text=tr("go"); nextButton.text="DEMO"; showDrivingControls(true); return }
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
        info.text = "${(speed*3.6).toInt().coerceAtLeast(0)} km/h"
        tripDistance.text = RallyDistanceFormatter.remaining(remaining) + " " + tr("remaining")
        val next = route.notes.getOrNull(result.noteIndex)
        if (next == null) {
            resetNoteCard("#43D9A6", "FIN", "TRAMO LIMPIO", "SIN NOTAS INMEDIATAS  ·  ATENCIÓN AL CAMINO")
        } else {
            val follow = route.notes.getOrNull(result.noteIndex+1)
            updateNoteCard(next, follow, result.distanceToNoteM)
        }
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
            navigationSession++
            contextualCopilot.reset()
            reroutePolicy.reset()
            navigating = false; demo = false; action.text = tr("go")
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
                val updated = PaceNoteEngine.createPlan(RouteServices.route(current, stopsAhead, destination))
                runOnUiThread {
                    if (session == navigationSession && navigating && !demo && routeDestination == destination && plan === oldPlan) {
                        plan = updated
                        navigator = NavigationEngine(updated) { warnFactor }
                        contextualCopilot.reset(); lastPriorityNoteAtMs = -1L
                        drivingCamera.reset()
                        lastSpoken = -1
                        pendingStops = stopsAhead
                        prefs.edit()
                            .putString("last_route", RouteStore.serialize(updated.points))
                            .putString("last_road_stops", RouteStore.serialize(stopsAhead)).apply()
                        drawPlan()
                        updateProgress(current, lastLocation?.speed ?: 0f)
                        status("Nueva ruta calculada · notas actualizadas", false)
                        say("Ruta recalculada. Nuevas notas disponibles.")
                    }
                    reroutePolicy.completeAttempt()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (session == navigationSession && navigating) {
                        status("No pude recalcular sin conexión. Se conserva el recorrido anterior.", true)
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
    }
    private fun drawWaypoints() {
        val style = map?.style ?: return
        val markers = mutableListOf<Feature>()
        fun pointMarker(point: GeoPoint, name: String, tone: String) {
            markers.add(Feature.fromGeometry(Point.fromLngLat(point.longitude,point.latitude)).apply {
                addStringProperty("label", name)
                addStringProperty("tone", tone)
            })
        }
        chosenOrigin?.let { pointMarker(it,"ORIGEN","#46C8EA") }
        // A calculated road route retains the destination pin after target is cleared.
        (target ?: routeDestination)?.let { pointMarker(it,"DESTINO","#43D9A6") }
        intermediatePoints.forEachIndexed { index,p ->
            pointMarker(p,"VÍA ${index+1}","#F3D14A")
        }
        val collection = FeatureCollection.fromFeatures(markers)
        val source = style.getSourceAs<GeoJsonSource>("rally_waypoints")
        if (source == null) {
            style.addSource(GeoJsonSource("rally_waypoints",collection))
            style.addLayer(CircleLayer("rally_waypoints_circles","rally_waypoints").withProperties(
                circleColor(Expression.get("tone")), circleRadius(8f),
                circleStrokeColor("#FFFFFF"), circleStrokeWidth(2f)))
            style.addLayer(SymbolLayer("rally_waypoints_labels","rally_waypoints").withProperties(
                textField(Expression.get("label")), textSize(12f), textColor("#FFFFFF"),
                textHaloColor("#111827"), textHaloWidth(2f),
                textOffset(arrayOf(0f,-1.8f))))
        } else source.setGeoJson(collection)
    }

    /**
     * Real-position map marker: the Subaru's hood always points in the actual driving
     * direction (route tangent in DEMO, reliable GPS heading otherwise).
     */
    private fun carBearing(point: GeoPoint): Float {
        val route = plan
        if ((navigating || demo) && route != null && route.points.size >= 2) {
            val metres = if (demo) demoPosition else navigator?.locate(point)?.metres ?: 0.0
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
            // Keep dot source in all cases: it is a valid alternative and a robust fallback.
            style.addLayer(CircleLayer("rally_me_layer", "rally_me").withProperties(
                circleColor(sky), circleRadius(9f),
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
                val size = if (selected == RallyVehicles.DEFAULT) 0.86f else 0.63f
                val bearing = (if (demo) plan?.points?.getOrNull(positionIndex) else gps)
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
        style.getLayer("rally_car_layer")?.setProperties(
            visibility(if (activeCar) Property.VISIBLE else Property.NONE))
        style.getLayer("rally_me_layer")?.setProperties(
            visibility(if (activeCar) Property.NONE else Property.VISIBLE))
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
        val intro = label("Seleccioná tu vehículo para el mapa GPS y la DEMO.", 13f, muted)
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
            for (rowModels in chunk.value.chunked(3)) {
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
                    tile.addView(image, LinearLayout.LayoutParams(dp(60), dp(76)))
                    val text = label(model.name, 11f, white, true).apply {
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    tile.addView(text, LinearLayout.LayoutParams(-1, dp(39)).apply { topMargin=dp(3) })
                    tile.setOnClickListener {
                        prefs.edit()
                            .putString(RallyVehicles.KEY, model.id)
                            .putBoolean("subaru_marker", model.id != RallyVehicles.DOT)
                            .apply()
                        updateVehicleLayers()
                        status("Vehículo GPS: " + model.name, false)
                        dialog.dismiss()
                    }
                    vehicleRow.addView(tile, LinearLayout.LayoutParams(0,dp(136),1f).apply {
                        marginStart=dp(3); marginEnd=dp(3); bottomMargin=dp(7)
                    })
                }
                repeat(3 - rowModels.size) {
                    vehicleRow.addView(View(this), LinearLayout.LayoutParams(0,dp(136),1f))
                }
                content.addView(vehicleRow,LinearLayout.LayoutParams(-1,-2))
            }
        }
        scroll.addView(content)
        dialog = android.app.AlertDialog.Builder(this).setTitle("ELEGIR VEHÍCULO GPS")
            .setView(scroll)
            .setNegativeButton("CERRAR",null)
            .create()
        dialog.show()
        dialog.window?.setLayout(
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.81f).toInt())
    }

    private fun drawLocation() { gps?.let { drawPosition(it) } }
    private fun toggle3D() {
        map3d = !map3d
        modeButton.text = if (map3d) "3D" else "2D"
        drivingCamera.reset()
        val live = if (demo) plan?.points?.getOrNull(positionIndex) else gps
        if ((navigating || demo) && live != null) drawPosition(live)
        else (gps ?: plan?.points?.firstOrNull())?.let { point ->
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .target(LatLng(point.latitude,point.longitude)).zoom(15.5)
                .tilt(if(map3d) 50.0 else 0.0).bearing(0.0).build()), 450)
        }
    }
    private fun centerGps() {
        if (navigating || demo) {
            followRouteCamera = true
            val current = if (demo) plan?.points?.getOrNull(positionIndex) else gps
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
        if (requestCode==900 && grantResults.any{it==PackageManager.PERMISSION_GRANTED}) requestGps()
        else if (requestCode==900) status("Podés usar GPX y DEMO sin permiso GPS",true)
    }
    override fun onLocationChanged(location: Location) {
        if (location.hasAccuracy() && location.accuracy > 65f) {
            if (navigating) status("Señal GPS débil: esperando precisión", true)
            return
        }
        val ageNs = android.os.SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        if (ageNs > 15_000_000_000L || ageNs < -2_000_000_000L) return
        gps = GeoPoint(location.latitude,location.longitude); lastLocation=location
        if (navigating) updateProgress(gps!!, location.speed)
        if (!demo) drawPosition(gps!!)
        if (firstGpsCamera) { firstGpsCamera=false; if (!navigating) centerGps() }
        if (!navigating && !demo) status("GPS disponible · mantené pulsado el mapa para elegir destino",false)
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
    private fun showPointOptions() {
        val choices = arrayOf(tr("destination_point"), tr("origin_point"), tr("via_point"),
            tr("gps_origin"), tr("clear_stops"))
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
                }
            }.setNegativeButton("CERRAR",null).show()
    }
    private fun showDrivingControls(show: Boolean) {
        controlsVisible = show
        searchRow.visibility = if (navigating || demo) { if (show) View.VISIBLE else View.GONE } else View.VISIBLE
        toolsRow.visibility = if (navigating || demo) { if (show) View.VISIBLE else View.GONE } else View.VISIBLE
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

    private fun showSettings() {
        val names=arrayOf(tr("warn_short"), tr("warn_normal"), tr("warn_early"),
            tr("repeat"), tr("all_notes"), tr("copilot"), tr("test_voice"),
            tr("android_voice"), tr("pitch"), tr("engine"), tr("licenses"),
            tr("vehicle"), tr("change"), tr("context_menu"))
        android.app.AlertDialog.Builder(this).setTitle("COPILOTO · " + tr("settings"))
            .setItems(names) { _, i ->
                if(i in 0..2) { warnFactor=i; prefs.edit().putInt("anticipation",i).apply();status("Anticipación: ${names[i]}",false) }
                if(i==3) plan?.notes?.getOrNull(lastSpoken)?.let {
                    speakRallyNote(it, plan?.notes?.getOrNull(lastSpoken+1), 50.0)
                }
                if (i==5) selectRallyVoiceMode()
                if (i==6) previewRallyVoice()
                if (i==7) chooseVoice()
                if (i==8) adjustVoice()
                if (i==9) openTtsSettings()
                if (i==11) selectVehicleMarker()
                if (i==12) showWelcomeSetup(firstRun = false)
                if (i==13) selectContextualMode()
                if (i==10) android.app.AlertDialog.Builder(this).setTitle("LICENCIAS")
                    .setMessage("CarlFM (es-ES) · rhasspy/piper-voices · MIT. Dataset: dominio público. El modelo y el sintetizador no están incluidos, solo audios OGG. Más datos en github.com/maxijones012/RallyNotes/blob/main/docs/VOICE_LICENSES.md")
                    .setPositiveButton("CERRAR",null).show()
                if(i==4) {
                    val text=plan?.notes?.mapIndexed { j,n -> "${j+1}. ${n.metresFromStart.toInt()} m: ${PaceNotes.phrase(n)}" }?.joinToString("\n")
                        ?: "Todavía no hay un recorrido"
                    android.app.AlertDialog.Builder(this).setTitle("LISTA DE NOTAS · ESTIMADAS")
                        .setMessage(text.take(14000)).setPositiveButton("CERRAR",null).show()
                }
            }.setNegativeButton("CERRAR",null).show()
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
        demo=false; navigating=false; ui.removeCallbacksAndMessages(null)
        locationManager.removeUpdates(this)
        rallyAudio.stop()
        tts?.stop();tts?.shutdown()
        mapView.onDestroy();super.onDestroy()
    }
}
