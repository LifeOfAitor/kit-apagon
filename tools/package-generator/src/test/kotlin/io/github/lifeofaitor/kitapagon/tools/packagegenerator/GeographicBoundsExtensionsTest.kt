package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicBounds
import io.github.lifeofaitor.kitapagon.core.packageformat.GeographicPoint
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeographicBoundsExtensionsTest {
    @Test
    fun theMarginIsAboutThreeKilometresOnEverySide() {
        val expanded = IRUN_BOUNDS.expandedBy(3.0)

        val latitudeDegrees = 3.0 / 111.32
        val longitudeDegrees = 3.0 / (111.32 * cos(Math.toRadians((IRUN_BOUNDS.south + IRUN_BOUNDS.north) / 2)))
        assertEquals(IRUN_BOUNDS.north + latitudeDegrees, expanded.north, absoluteTolerance = 1e-9)
        assertEquals(IRUN_BOUNDS.south - latitudeDegrees, expanded.south, absoluteTolerance = 1e-9)
        assertEquals(IRUN_BOUNDS.east + longitudeDegrees, expanded.east, absoluteTolerance = 1e-9)
        assertEquals(IRUN_BOUNDS.west - longitudeDegrees, expanded.west, absoluteTolerance = 1e-9)
    }

    @Test
    fun aDegreeOfLongitudeIsShorterFurtherNorth() {
        val canarias = GeographicBounds(-15.5, 28.0, -15.4, 28.1).expandedBy(3.0)
        val gipuzkoa = GeographicBounds(-2.0, 43.0, -1.9, 43.1).expandedBy(3.0)

        assertTrue(gipuzkoa.east - gipuzkoa.west > canarias.east - canarias.west)
    }

    @Test
    fun neverGrowsPastTheEdgesOfTheWorld() {
        assertEquals(
            GeographicBounds(-180.0, -90.0, 180.0, 90.0),
            GeographicBounds(-179.99, -89.99, 179.99, 89.99).expandedBy(3.0),
        )
    }

    @Test
    fun theUnionContainsBothRectangles() {
        val union = GeographicBounds(-2.0, 43.0, -1.0, 44.0).union(GeographicBounds(-3.0, 43.5, -1.5, 45.0))

        assertEquals(GeographicBounds(-3.0, 43.0, -1.0, 45.0), union)
    }

    @Test
    fun theUnionOfNoRectanglesIsNull() {
        assertNull(emptyList<GeographicBounds>().unionOrNull())
        assertEquals(IRUN_BOUNDS, listOf(IRUN_BOUNDS).unionOrNull())
    }

    @Test
    fun containsPointsOnTheEdgeButNotOutside() {
        assertTrue(IRUN_BOUNDS.contains(GeographicPoint(IRUN_BOUNDS.north, IRUN_BOUNDS.west)))
        assertTrue(IRUN_BOUNDS.contains(GeographicPoint(43.32, -1.79)))
        assertFalse(IRUN_BOUNDS.contains(GeographicPoint(43.32, -1.70)))
        assertFalse(IRUN_BOUNDS.contains(GeographicPoint(43.40, -1.79)))
    }
}
