package io.github.lifeofaitor.kitapagon.core.packageformat

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeographicBoundsTest {
    @Test
    fun acceptsARectangleInsideTheWorld() {
        assertTrue(GeographicBounds(west = -1.9, south = 43.2, east = -1.6, north = 43.4).isValid)
    }

    @Test
    fun rejectsLongitudesOutsideTheWorld() {
        assertFalse(GeographicBounds(west = -181.0, south = 0.0, east = 0.0, north = 1.0).isValid)
        assertFalse(GeographicBounds(west = 0.0, south = 0.0, east = 181.0, north = 1.0).isValid)
    }

    @Test
    fun rejectsLatitudesOutsideTheWorld() {
        assertFalse(GeographicBounds(west = 0.0, south = -91.0, east = 1.0, north = 1.0).isValid)
        assertFalse(GeographicBounds(west = 0.0, south = 0.0, east = 1.0, north = 91.0).isValid)
    }

    @Test
    fun rejectsEmptyOrInvertedRectangles() {
        assertFalse(GeographicBounds(west = 1.0, south = 0.0, east = 1.0, north = 1.0).isValid)
        assertFalse(GeographicBounds(west = 0.0, south = 1.0, east = 1.0, north = 0.0).isValid)
    }
}
