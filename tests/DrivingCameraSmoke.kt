package ar.com.rallygpsai

import kotlin.math.*

fun main() {
    var passed=0
    fun verify(ok: Boolean, name: String) { check(ok) { "Falló: $name" }; passed++ }
    val geo = GeoPoint(-43.249, -65.307)
    for (angle in 0..359 step 3) {
        val p2 = GeoPoint(geo.latitude + cos(Math.toRadians(angle.toDouble())) * .003,
            geo.longitude + sin(Math.toRadians(angle.toDouble())) * .004)
        val plan = PaceNoteEngine.createPlan(listOf(geo,p2))
        val camera=DrivingCamera()
        val p = camera.pose(plan, 30.0, 12.0, true)
        verify(abs(DrivingCamera.shortestDelta(p.bearing, DrivingCamera.bearing(geo,p2))) < 5.0, "rumbo $angle")
        verify(p.tilt == 60.0, "inclinación $angle")
        verify(DrivingCamera.distance(p.focus,geo) > 20.0, "adelanto $angle")
        verify(camera.pose(plan,20.0,0.0,false).tilt==0.0, "plano $angle")
    }
    for (a in 0..359 step 3) for (b in 0..359 step 18) {
        val d=DrivingCamera.shortestDelta(a.toDouble(),b.toDouble())
        verify(d >= -180.0 && d <= 180.0, "wrap $a/$b")
    }
    verify(abs(DrivingCamera.shortestDelta(350.0,10.0)-20.0)<1e-5,"cruce norte derecha")
    verify(abs(DrivingCamera.shortestDelta(10.0,350.0)+20.0)<1e-5,"cruce norte izquierda")
    println("PASSED: $passed camera navigation verifications")
}
