package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.cos

class TrafficCalmingTest {
    private val latitude = -43.25
    private fun point(metersEast: Double, metersNorth: Double=0.0): GeoPoint =
        GeoPoint(latitude + metersNorth/111195.0,
            -65.31 + metersEast/(111320.0 * cos(Math.toRadians(latitude))))
    private val p = listOf(point(0.0),point(200.0),point(400.0))
    private val route = PaceNoteEngine.createPlan(p)

    @Test fun recognizedOSMTagsAndUnknownValues() {
        assertEquals("bump", TrafficCalming.normalize("bump"))
        assertEquals("hump", TrafficCalming.normalize("hump"))
        assertEquals("table", TrafficCalming.normalize("table"))
        assertEquals("rumble_strip", TrafficCalming.normalize("rumble_strip"))
        assertEquals("cushion", TrafficCalming.normalize("cushion"))
        assertEquals("bump", TrafficCalming.normalize("choker;bump"))
        assertNull(TrafficCalming.normalize("unconfirmed_hazard"))
    }

    @Test fun mappedBumpIsPlacedAlongRouteAndFoundAhead() {
        val one = RawBump("osm_12",point(152.0,3.0),"bump",BumpSource.OSM)
        val result=TrafficCalming.onRoute(route,listOf(one))
        assertEquals(1,result.size)
        assertEquals(152.0,result[0].routeMetres,12.0)
        assertEquals("osm_12",TrafficCalming.upcoming(result,15.0)?.raw?.id)
        assertNull(TrafficCalming.upcoming(result,270.0))
    }

    @Test fun nearbyParallelRoadAndDuplicatesAreIgnored() {
        val accepted = RawBump("manual_a",point(120.0,2.0),"hump",BumpSource.MANUAL)
        val duplicate = RawBump("osm_b",point(125.0,2.0),"hump",BumpSource.OSM)
        val distant = RawBump("osm_c",point(130.0,35.0),"table",BumpSource.OSM)
        val matched=TrafficCalming.onRoute(route,listOf(accepted,duplicate,distant))
        assertEquals(1,matched.size)
        assertEquals("manual_a",matched.single().raw.id)
    }

    @Test fun parseOverpassNodesForMappedBumpsOnly() {
        val json = """{"elements":[
          {"type":"node","id":12,"lat":-43.25,"lon":-65.31,
            "tags":{"traffic_calming":"table"}},
          {"type":"node","id":13,"lat":-43.25,"lon":-65.30,
            "tags":{"hazard":"bump"}},
          {"type":"node","id":14,"lat":-43.25,"lon":-65.29,
            "tags":{"name":"nearby street"}}
        ]}"""
        val found=TrafficCalming.readOverpassJson(json)
        assertEquals(2,found.size)
        assertEquals("table",found[0].kind)
        assertEquals("bump",found[1].kind)
        assertTrue(found.all { it.source == BumpSource.OSM })
    }

    @Test fun noCandidatesCannotInventHazards() {
        assertTrue(TrafficCalming.onRoute(route,emptyList()).isEmpty())
        assertNull(TrafficCalming.upcoming(emptyList(),100.0))
    }

    @Test fun roadSurfaceIsNeverInferredFromCurveGeometry() {
        val sharp = PaceNoteEngine.createPlan(listOf(point(0.0),point(100.0),
            point(100.0,100.0),point(200.0,100.0)))
        assertTrue(TrafficCalming.onRoute(sharp,emptyList()).isEmpty())
    }
}
