import ar.com.rallygpsai.*
import kotlin.math.*

fun main() {
    var checks = 0
    fun verify(ok: Boolean, why: String) { check(ok) { why }; checks++ }
    val straight = (0..200).map { GeoPoint(-43.25, -65.31 + it * 0.000015) }
    val straightPlan = PaceNoteEngine.createPlan(straight)
    verify(straightPlan.notes.isEmpty(), "Recta no debería producir notas")
    verify(straightPlan.totalMetres > 200, "Longitud de recta")
    val left = (0..50).map { GeoPoint(0.0, it*.00002) } + (1..50).map { GeoPoint(it*.00002,.001) }
    val right = (0..50).map { GeoPoint(0.0, it*.00002) } + (1..50).map { GeoPoint(-it*.00002,.001) }
    verify(PaceNoteEngine.createPlan(left).notes.any { it.side == PaceNote.Side.LEFT }, "Esquina izquierda")
    verify(PaceNoteEngine.createPlan(right).notes.any { it.side == PaceNote.Side.RIGHT }, "Esquina derecha")
    val origin = GeoPoint(-43.25, -65.31)
    val demo = PaceNoteEngine.createPlan(PaceNotes.synthetic(origin))
    verify(demo.notes.size >= 3, "Ruta demo sin notas")
    verify(demo.notes.any { it.side == PaceNote.Side.LEFT } && demo.notes.any { it.side == PaceNote.Side.RIGHT }, "Ambas direcciones")
    verify(demo.notes.all { it.grade in 1..6 && it.metresFromStart in 0.0..demo.totalMetres }, "Grados fuera de rango")
    verify(demo.notes.zipWithNext().all { (a,b) -> a.metresFromStart < b.metresFromStart }, "Notas desordenadas o duplicadas")
    verify(demo.points.size == demo.metresAtPoint.size && demo.points.size > 50, "Proyección de ruta")
    verify((demo.metresAtPoint.last() - demo.totalMetres).absoluteValue < 0.0001, "Distancias acumuladas")
    verify(PaceNotes.warningDistance(27f, 2) > PaceNotes.warningDistance(27f,0), "Antelación configurable")
    verify(PaceNotes.color(1) != PaceNotes.color(6), "Colores de grados")
    verify(PaceNotes.phrase(demo.notes[0]).contains("${demo.notes[0].grade}"), "Voz de nota")
    val navigator = NavigationEngine(demo) { 1 }
    val invalid = navigator.update(GeoPoint(origin.latitude + .05,origin.longitude + .05), 20f, 8f)
    verify(invalid.offRoute && invalid.announcement == null, "Desvío nunca debe producir voz")
    navigator.reset()
    val speech = mutableListOf<String>()
    var lastDistance = -1.0
    for ((i,p) in demo.points.withIndex()) {
        val g = navigator.update(p,16f,4f)
        verify(!g.offRoute, "Posición de ruta $i inválida")
        verify(g.fix.metres + 0.01 >= lastDistance, "Progreso retrocedió $i")
        lastDistance = g.fix.metres
        if (g.announcement != null) speech.add(g.announcement)
    }
    verify(speech.isNotEmpty(), "Faltan anuncios")
    verify(speech.size <= demo.notes.size + 1, "Avisos duplicados")
    val distantFix = navigator.update(GeoPoint(origin.latitude + .06,origin.longitude),8f,5f)
    verify(distantFix.announcement == null && distantFix.offRoute, "Desvío de ruta")
    val badGps = navigator.update(demo.points.last(),10f,100f)
    verify(badGps.offRoute && badGps.announcement == null,"GPS impreciso")
    val noisyStraight = (0..500).map { i ->
        GeoPoint(-43.25 + (if(i%2==0) 0.0000004 else -0.0000004), -65.31 + i*0.000006)
    }
    verify(PaceNoteEngine.createPlan(noisyStraight).notes.isEmpty(), "Ruido GPS pequeño")
    val dense = (0..15000).map { GeoPoint(-43.25,-65.31 + it * 0.0000005) }
    val densePlan = PaceNoteEngine.createPlan(dense)
    verify(densePlan.notes.isEmpty() && densePlan.totalMetres > 400,"GPX densamente muestreado")
    println("ETAPAS 1–8: $checks comprobaciones OK; curvas demo=${demo.notes.size}; voces=${speech.size}; distancia=${demo.totalMetres.toInt()}m")
}
