package ar.com.rallygpsai

import kotlin.math.*

/**
 * Rejects impossible/unsafe route responses and makes snap uncertainty explicit.
 * Geometric checks cannot guarantee OpenStreetMap road completeness or legality.
 */
data class RoadInspection(
    val rejected: Boolean,
    val reviewRequired: Boolean,
    val reasons: List<String>,
    val maximumSnapMetres: Double,
    val totalMetres: Double
)

data class CheckedRoadRoute(val points: List<GeoPoint>, val inspection: RoadInspection)

object RoadRouteChecker {
    private fun distance(a: GeoPoint,b:GeoPoint): Double {
        val r=Math.PI/180.0
        val x=sin((b.latitude-a.latitude)*r/2).pow(2)+
            cos(a.latitude*r)*cos(b.latitude*r)*sin((b.longitude-a.longitude)*r/2).pow(2)
        return 12_742_000.0*asin(sqrt(x.coerceIn(0.0,1.0)))
    }
    private fun valid(p:GeoPoint) =
        p.latitude.isFinite() && p.longitude.isFinite() &&
        p.latitude in -90.0..90.0 && p.longitude in -180.0..180.0

    fun inspect(requested: List<GeoPoint>, geometry: List<GeoPoint>,
                snapped: List<GeoPoint>, reportedMetres: Double): RoadInspection {
        val errors=mutableListOf<String>()
        val warnings=mutableListOf<String>()
        if(requested.size<2 || requested.size>6 || snapped.size!=requested.size)
            errors+="Cantidad de puntos inválida"
        if(geometry.size<2 || geometry.size>30_000)
            errors+="Geometría vacía o demasiado grande"
        if(requested.any { !valid(it) } || snapped.any { !valid(it) } ||
            geometry.any { !valid(it) }) errors+="Coordenadas inválidas"
        if(!reportedMetres.isFinite() || reportedMetres <= 15.0)
            errors+="Distancia de ruta inválida"
        if(errors.isNotEmpty()) return RoadInspection(true,false,errors,Double.POSITIVE_INFINITY,0.0)

        val legs=geometry.zipWithNext().map { (a,b)->distance(a,b) }
        val total=legs.sum()
        if(!total.isFinite() || total<15.0) errors+="El recorrido no tiene longitud útil"
        if(legs.any { it>25_000.0 }) errors+="La geometría salta más de 25 km sin puntos intermedios"
        if(legs.any { it>5_000.0 }) warnings+="Hay segmentos demasiado largos para revisar visualmente"
        if(reportedMetres>0 && abs(total-reportedMetres) / reportedMetres > 0.28)
            warnings+="Distancia informada y dibujada no coinciden"
        val snaps=requested.zip(snapped).map { (a,b)->distance(a,b) }
        val maxSnap=snaps.maxOrNull()?:0.0
        if(maxSnap>1_200.0) errors+="Un punto está a más de 1,2 km de la calle más cercana"
        else if(maxSnap>120.0) warnings+="Algún punto fue trasladado a una calle a más de 120 m"
        if(distance(geometry.first(),snapped.first())>250.0 ||
            distance(geometry.last(),snapped.last())>250.0)
            errors+="Inicio o destino de la ruta no coinciden con los puntos de OSRM"
        // Check that ordered stops really occur along the returned geometry.
        for(stop in snapped.drop(1).dropLast(1)) {
            if(geometry.minOf { distance(it,stop) }>250.0) {
                warnings+="Una parada intermedia no se encuentra cerca de la polilínea"
                break
            }
        }
        if(geometry.size<3 && total>300.0)
            warnings+="El trazado tiene muy pocos puntos para generar notas confiables"
        return RoadInspection(errors.isNotEmpty(),errors.isEmpty()&&warnings.isNotEmpty(),
            errors+warnings,maxSnap,total)
    }
}
