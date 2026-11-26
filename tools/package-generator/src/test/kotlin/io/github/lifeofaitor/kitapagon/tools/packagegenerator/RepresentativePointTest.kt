package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicPoint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RepresentativePointTest {
    @Test
    fun keepsAPointAsItIs() {
        assertEquals(GeographicPoint(43.3, -1.8), pointOf("""{"type":"Point","coordinates":[-1.8,43.3]}"""))
    }

    @Test
    fun usesTheCentreOfASquareOutline() {
        val square = """{"type":"Polygon","coordinates":[[[0,0],[2,0],[2,2],[0,2],[0,0]]]}"""

        assertClose(GeographicPoint(1.0, 1.0), pointOf(square))
    }

    @Test
    fun weighsTheCentreByAreaInsteadOfCountingCorners() {
        // An L shape: the average of its corners would be (1.0, 1.0); its real centroid is (5/6, 5/6).
        val lShape = """{"type":"Polygon","coordinates":[[[0,0],[2,0],[2,1],[1,1],[1,2],[0,2],[0,0]]]}"""

        assertClose(GeographicPoint(latitude = 5.0 / 6, longitude = 5.0 / 6), pointOf(lShape))
    }

    @Test
    fun usesTheLargestOutlineOfSeveral() {
        val small = "[[[0,0],[1,0],[1,1],[0,1],[0,0]]]"
        val large = "[[[10,10],[14,10],[14,14],[10,14],[10,10]]]"

        val point = pointOf("""{"type":"MultiPolygon","coordinates":[$small,$large]}""")

        assertClose(GeographicPoint(12.0, 12.0), point)
    }

    @Test
    fun averagesTheCornersOfAnOutlineWithoutArea() {
        val flat = """{"type":"Polygon","coordinates":[[[0,0],[2,0],[4,0],[0,0]]]}"""

        assertClose(GeographicPoint(latitude = 0.0, longitude = 1.5), pointOf(flat))
    }

    @Test
    fun rejectsGeometriesItCannotReduce() {
        assertNull(pointOf("""{"type":"LineString","coordinates":[[0,0],[1,1]]}"""))
        assertNull(pointOf("""{"type":"Polygon","coordinates":[[[0,0],[1,1]]]}"""))
        assertNull(pointOf("""{"type":"Point","coordinates":[-1.8]}"""))
        assertNull(pointOf("""{"type":"Point"}"""))
    }

    private fun pointOf(geometry: String): GeographicPoint? =
        RepresentativePoint.of(Json.parseToJsonElement(geometry).jsonObject)

    private fun assertClose(
        expected: GeographicPoint,
        actual: GeographicPoint?,
    ) {
        assertNotNull(actual)
        assertEquals(expected.latitude, actual.latitude, TOLERANCE)
        assertEquals(expected.longitude, actual.longitude, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
