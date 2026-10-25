import ar.com.rallygpsai.*
import kotlin.math.*

fun main() {
    val straight=(0..80).map{GeoPoint(0.0,it*0.00002)}
    val p1=PaceNoteEngine.createPlan(straight)
    check(p1.notes.isEmpty()) { "Recta: notas incorrectas ${p1.notes}" }
    val l=(0..50).map { GeoPoint(0.0,it*0.00002) } + (1..50).map { GeoPoint(it*0.00002,0.001) }
    val r=(0..50).map { GeoPoint(0.0,it*0.00002) } + (1..50).map { GeoPoint(-it*0.00002,0.001) }
    check(PaceNoteEngine.createPlan(l).notes.any {it.side==PaceNote.Side.LEFT})
    check(PaceNoteEngine.createPlan(r).notes.any {it.side==PaceNote.Side.RIGHT})
    val demo=PaceNoteEngine.createPlan(PaceNotes.synthetic(GeoPoint(-43.25,-65.31)))
    check(demo.notes.size >= 3) {"Ruta simulada sin suficientes notas: ${demo.notes}"}
    check(demo.notes.all{it.grade in 1..6})
    check(PaceNotes.warningDistance(50f,2)>PaceNotes.warningDistance(5f,0))
    println("TESTS OK: demo ${demo.notes.size} curvas, grados ${demo.notes.map{it.grade}}, longitud ${demo.totalMetres.toInt()}m")
}
