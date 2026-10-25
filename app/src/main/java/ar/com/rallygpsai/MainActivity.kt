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
    private lateinit var locationManager: LocationManager
    private var navigating = false
    private var demo = false
    private var map3d = true
    private var voiceOn = true
    private var warnFactor = 1
    private var positionIndex = 0
    private var lastSpoken = -1
    private var currentNote = 0
    private var firstGpsCamera = true
    private var demoPosition = 0.0
    private var demoSpeed = 55.0 / 3.6
    private var speakReady = false
    private var tts: TextToSpeech? = null
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var searchInput: EditText
    private lateinit var banner: TextView
    private lateinit var noteTitle: TextView
    private lateinit var noteDetail: TextView
    private lateinit var info: TextView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0B1019")
        window.navigationBarColor = Color.parseColor("#0B1019")
        MapLibre.getInstance(this)
        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
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
                drawLocation()
            }
            loaded.addOnMapLongClickListener { latlng ->
                target = GeoPoint(latlng.latitude, latlng.longitude)
                status("Destino señalado. Tocá 'TRAZAR RUTA'", false)
                true
            }
            loaded.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(LatLng(-40.0, -64.0)).zoom(3.7).build()
            ))
        }
        tts = TextToSpeech(this) { code ->
            speakReady = code == TextToSpeech.SUCCESS
            if (speakReady) {
                val result = tts?.setLanguage(Locale("es", "AR")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (result < 0) tts?.language = Locale("es", "ES")
                tts?.setSpeechRate(1.08f)
            }
        }
        val saved = prefs.getString("last_route", null)
        if (!saved.isNullOrBlank()) runCatching { setPlan(RouteStore.deserialize(saved), false) }
        requestGps()
    }

    private fun dp(value: Int) = (resources.displayMetrics.density * value + 0.5f).toInt()
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
            minHeight = dp(45)
            setPadding(dp(9), dp(6), dp(9), dp(6))
            isClickable = true
        }
    private fun LinearLayout.add(v: View, w: Int = -1, h: Int = -2, weight: Float = 0f) {
        addView(v, LinearLayout.LayoutParams(w, h, weight))
    }
    private fun row(gap: Int = 8, contents: LinearLayout.() -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            dividerDrawable = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT).apply { setBounds(0,0,dp(gap),1) }
            contents()
        }

    private fun buildUI() {
        val root = FrameLayout(this)
        root.addView(mapView, FrameLayout.LayoutParams(-1, -1))
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(12), dp(13), dp(13))
            background = shape(bg, 17, "#2D4158")
            elevation = dp(10).toFloat()
        }
        top.add(row(6) {
            add(label("◢", 26f, sky, true), dp(35))
            add(label("RALLY GPS AI", 18f, white, true), 0, -2, 1f)
            modeButton = button("3D", panel, sky).also { it.setOnClickListener { toggle3D() } }
            add(modeButton, dp(51))
            voiceButton = button(if (voiceOn) "VOZ ✓" else "MUDO", panel, sky).also { it.setOnClickListener {
                voiceOn = !voiceOn; prefs.edit().putBoolean("voice", voiceOn).apply()
                it as TextView; it.text = if (voiceOn) "VOZ ✓" else "MUDO"
                if (!voiceOn) tts?.stop()
            } }
            add(voiceButton, dp(75))
        })
        val searchRow = row(6) {
            searchInput = EditText(this@MainActivity).apply {
                hint = "Destino: localidad, calle o dirección"; setHintTextColor(Color.parseColor("#758A9F"))
                setTextColor(Color.WHITE); textSize = 14f; isSingleLine = true
                setPadding(dp(10), 0, dp(5), 0); background = shape("#233148", 10)
                imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                setOnEditorActionListener { _, _, _ -> lookupPlace(); true }
            }
            add(searchInput, 0, dp(46), 1f)
            add(button("BUSCAR", "#116B78", "#FFFFFF").apply { setOnClickListener { lookupPlace() } }, dp(73))
        }
        val sp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) }
        top.addView(searchRow, sp)
        banner = label("GPS buscando señal · mantené pulsado el mapa para elegir destino", 11f, muted)
        top.addView(banner, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(7) })
        noteTitle = label("ELEGÍ UN DESTINO", 21f, white, true)
        noteDetail = label("Se crearán las notas 1–6 automáticamente", 12f, muted)
        val noteBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = shape("#1F2F47", 13, "#2B485C")
            add(noteTitle); add(noteDetail)
        }
        top.addView(noteBox, LinearLayout.LayoutParams(-1,-2).apply { topMargin = dp(10) })
        root.addView(top, FrameLayout.LayoutParams(-1,-2,Gravity.TOP).apply {
            setMargins(dp(9),dp(8),dp(9),0)
        })

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(bg, 19, "#34506C")
            setPadding(dp(12), dp(10), dp(12), dp(12))
            elevation=dp(10).toFloat()
        }
        info = label("SIN RUTA · GPS", 13f, muted, true).apply { gravity=Gravity.CENTER }
        bottom.add(info)
        bottom.add(row(7) {
            action = button("TRAZAR RUTA", "#0E8BA1", "#FFFFFF").also { it.setOnClickListener { mainAction() } }
            add(action, 0, dp(49), 1f)
            nextButton = button("DEMO", "#463377", "#FFFFFF").also { it.setOnClickListener { startDemo() } }
            add(nextButton, dp(77), dp(49))
            gpsButton = button("◎ GPS", panel, sky).also { it.setOnClickListener { centerGps() } }
            add(gpsButton, dp(73), dp(49))
        })
        bottom.add(row(7) {
            add(button("IMPORTAR GPX").apply { setOnClickListener { importGpx() } }, 0,dp(43),1f)
            add(button("EXPORTAR GPX").apply { setOnClickListener { exportGpx() } }, 0,dp(43),1f)
            add(button("AJUSTES").apply { setOnClickListener { showSettings() } },0,dp(43),1f)
        })
        root.addView(bottom, FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM).apply { setMargins(dp(9),0,dp(9),dp(11)) })
        setContentView(root)
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
        if (name.length < 3) { status("Escribí un destino de al menos 3 caracteres", true); return }
        execute("Buscando destino", { RouteServices.search(name) }) { result ->
            target = result as? GeoPoint
            if (target == null) status("No encontré ese destino", true)
            else {
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(target!!.latitude, target!!.longitude), 13.0))
                status("Destino elegido: ${name.take(38)} · tocá TRAZAR RUTA", false)
            }
        }
    }
    private fun mainAction() {
        if (navigating || demo) {
            navigating = false; demo = false; tts?.stop(); lastSpoken = -1
            action.text = "INICIAR GPS"; nextButton.text = "DEMO"
            noteTitle.text = "NAVEGACIÓN DETENIDA"
            status("Recorrido guardado. Podés retomarlo.", false)
            return
        }
        if (plan != null && target == null) { startGpsNavigation(); return }
        if (target == null) { status("Ingresá un destino o mantené pulsado el mapa", true); return }
        val origin = gps ?: run { status("Necesito la ubicación GPS para trazar la ruta", true); requestGps(); return }
        execute("Calculando la ruta (servidor demo)", { RouteServices.route(origin, target!!) }) { result ->
            setPlan(result as List<GeoPoint>, true)
            target = null
            status("Ruta calculada. Revisá las notas antes de salir.", false)
        }
    }
    private fun setPlan(points: List<GeoPoint>, save: Boolean) {
        val parsed = PaceNoteEngine.createPlan(points)
        plan = parsed; positionIndex = 0; lastSpoken = -1; currentNote = 0
        navigating = false; demo = false; demoPosition = 0.0
        action.text = "INICIAR GPS"; nextButton.text = "DEMO"
        val km = "%.1f".format(Locale.US, parsed.totalMetres / 1000.0)
        noteTitle.text = "${parsed.notes.size} NOTAS DETECTADAS"
        noteDetail.text = "${km} km · Revisá curvas y recorridos antes de iniciar"
        info.text = "$km KM  •  ${parsed.notes.size} CURVAS  •  1–6"
        drawPlan()
        if (save && points.size <= 30000) prefs.edit().putString("last_route", RouteStore.serialize(points)).apply()
        val center = parsed.points[parsed.points.size/2]
        map?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
            .target(LatLng(center.latitude, center.longitude)).zoom(12.7).tilt(0.0).build()), 600)
    }

    private fun startGpsNavigation() {
        if (gps == null) { requestGps(); status("Esperando GPS", true); return }
        navigating = true; demo = false; positionIndex = 0; lastSpoken = -1; currentNote = 0
        action.text = "FINALIZAR"; nextButton.text = "DEMO"
        status("GPS activo · notas estimadas del trazado · respetá la señalización", false)
        lastLocation?.let { updateProgress(GeoPoint(it.latitude,it.longitude), it.speed) }
    }
    private fun startDemo() {
        if (demo) { demo=false; nextButton.text="DEMO"; action.text="INICIAR GPS"; return }
        if (plan == null) {
            val center = gps ?: GeoPoint(-43.249, -65.309)
            setPlan(PaceNotes.synthetic(center), false)
            status("RUTA SIMULADA: no corresponde a calles reales", false)
        }
        demo = true; navigating = false; demoPosition = 0.0; positionIndex=0; lastSpoken=-1
        action.text = "FINALIZAR"; nextButton.text="PAUSA"
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
                if (index >= p.points.lastIndex) { demo=false; action.text="INICIAR GPS"; nextButton.text="DEMO"; return }
                ui.postDelayed(this, 500)
            }
        }
        ui.post(tick)
    }

    private fun updateProgress(point: GeoPoint, speed: Float) {
        val route = plan ?: return
        if (!navigating && !demo) return
        val (candidate, miss) = PaceNoteEngine.nearestProgress(route, point, positionIndex)
        if (miss > 70) {
            status("Fuera del recorrido (>70 m). No se cantan notas de curva.", true)
            return
        }
        positionIndex = maxOf(positionIndex, candidate)
        val progress = route.metresAtPoint[positionIndex]
        val remaining = (route.totalMetres - progress).coerceAtLeast(0.0)
        info.text = "${(speed*3.6).toInt()} km/h  •  ${"%.1f".format(Locale.US,remaining/1000)} km restantes"
        val notes = route.notes
        val nextIndex = notes.indexOfFirst { it.metresFromStart + 15 >= progress }
        if (nextIndex < 0) {
            noteTitle.text="FIN DE NOTAS"
            noteDetail.text="Mantené la atención en el camino"
            return
        }
        val next = notes[nextIndex]
        val dist = (next.metresFromStart - progress).coerceAtLeast(0.0)
        val follow = notes.getOrNull(nextIndex+1)
        noteTitle.text = "${if(next.side == PaceNote.Side.LEFT) "↖ IZQUIERDA" else "↗ DERECHA"} ${next.grade}"
        noteTitle.setTextColor(Color.parseColor(PaceNotes.color(next.grade)))
        noteDetail.text = "${dist.toInt()} m · ${PaceNotes.phrase(next, follow).replaceFirstChar { it.uppercase() }} · estimada"
        if (nextIndex > lastSpoken && dist <= PaceNotes.warningDistance(speed,warnFactor)) {
            lastSpoken = nextIndex
            val message = "En ${((dist/10).roundToInt()*10).coerceAtLeast(0)} metros, ${PaceNotes.phrase(next,follow)}"
            say(message)
        }
        if (remaining < 20) { say("Llegaste al final del recorrido"); navigating=false; demo=false; action.text="INICIAR GPS" }
    }
    private fun say(text: String) {
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
            Feature.fromGeometry(Point.fromLngLat(at.longitude,at.latitude)).apply { addStringProperty("tone", PaceNotes.color(n.grade)) }
        }
        val dotsCollection = FeatureCollection.fromFeatures(dots)
        val dotsSource = s.getSourceAs<GeoJsonSource>("rally_dots")
        if (dotsSource == null) {
            s.addSource(GeoJsonSource("rally_dots",dotsCollection))
            s.addLayer(CircleLayer("rally_dots_layer","rally_dots").withProperties(
                circleColor(Expression.get("tone")),circleRadius(5f),circleStrokeColor("#FFFFFF"),circleStrokeWidth(1.5f)))
        } else dotsSource.setGeoJson(dotsCollection)
    }
    private fun drawPosition(p: GeoPoint) {
        val s = map?.style ?: return
        val f = Feature.fromGeometry(Point.fromLngLat(p.longitude,p.latitude))
        val source = s.getSourceAs<GeoJsonSource>("rally_me")
        if (source == null) {
            s.addSource(GeoJsonSource("rally_me",f))
            s.addLayer(CircleLayer("rally_me_layer","rally_me").withProperties(
                circleColor(sky),circleRadius(9f),circleStrokeColor("#FFFFFF"),circleStrokeWidth(3f)))
        } else source.setGeoJson(f)
        if (navigating || demo) {
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .target(LatLng(p.latitude,p.longitude)).zoom(if(map3d) 16.2 else 15.5)
                .tilt(if(map3d) 45.0 else 0.0).build()),400)
        }
    }
    private fun drawLocation() { gps?.let { drawPosition(it) } }
    private fun toggle3D() {
        map3d = !map3d; modeButton.text=if(map3d) "3D" else "2D"
        (gps ?: plan?.points?.firstOrNull())?.let { point ->
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .target(LatLng(point.latitude,point.longitude)).zoom(15.0).tilt(if(map3d)45.0 else 0.0).build()),450)
        }
    }
    private fun centerGps() {
        val p = gps ?: run { requestGps(); status("Esperando ubicación GPS",true);return }
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(p.latitude,p.longitude),15.5),450)
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
        if (location.accuracy > 90f) return
        gps = GeoPoint(location.latitude,location.longitude); lastLocation=location
        if (!demo) drawPosition(gps!!)
        if (firstGpsCamera) { firstGpsCamera=false; centerGps() }
        if (navigating) updateProgress(gps!!, location.speed)
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
            101 -> try {
                val points=contentResolver.openInputStream(data.data!!)?.use { GpxIO.read(it) } ?: error("Archivo vacío")
                setPlan(points,true); status("GPX cargado: ${plan?.notes?.size} curvas aproximadas",false)
            } catch(e:Exception){status("GPX inválido: ${e.message}",true)}
            102 -> try {
                contentResolver.openOutputStream(data.data!!)?.bufferedWriter()?.use { it.write(GpxIO.write(plan!!.points)) }
                status("GPX guardado en tu dispositivo",false)
            } catch(e:Exception){status("No se pudo exportar: ${e.message}",true)}
        }
    }
    private fun showSettings() {
        val names=arrayOf("Aviso corto (4 s)","Aviso normal (7 s)","Aviso anticipado (10 s)","Volver a escuchar una nota", "Mostrar notas completas")
        android.app.AlertDialog.Builder(this).setTitle("COPILOTO · AJUSTES")
            .setItems(names) { _, i ->
                if(i in 0..2) { warnFactor=i; prefs.edit().putInt("anticipation",i).apply();status("Anticipación: ${names[i]}",false) }
                if(i==3) plan?.notes?.getOrNull(lastSpoken)?.let { say(PaceNotes.phrase(it,plan?.notes?.getOrNull(lastSpoken+1))) }
                if(i==4) {
                    val text=plan?.notes?.mapIndexed { j,n -> "${j+1}. ${n.metresFromStart.toInt()} m: ${PaceNotes.phrase(n)}" }?.joinToString("\n")
                        ?: "Todavía no hay un recorrido"
                    android.app.AlertDialog.Builder(this).setTitle("LISTA DE NOTAS · ESTIMADAS")
                        .setMessage(text.take(14000)).setPositiveButton("CERRAR",null).show()
                }
            }.setNegativeButton("CERRAR",null).show()
    }
    override fun onStart() { super.onStart();mapView.onStart() }
    override fun onResume() { super.onResume();mapView.onResume() }
    override fun onPause() { mapView.onPause();super.onPause() }
    override fun onStop() { mapView.onStop();super.onStop() }
    override fun onLowMemory() { super.onLowMemory();mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
    override fun onDestroy() {
        demo=false; navigating=false; ui.removeCallbacksAndMessages(null)
        locationManager.removeUpdates(this)
        tts?.stop();tts?.shutdown()
        mapView.onDestroy();super.onDestroy()
    }
}
