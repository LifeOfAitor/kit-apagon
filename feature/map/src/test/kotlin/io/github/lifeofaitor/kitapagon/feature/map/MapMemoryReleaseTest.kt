package io.github.lifeofaitor.kitapagon.feature.map

import android.content.ComponentCallbacks2
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("DEPRECATION") // The levels below are still sent by Android 8 to 13.
class MapMemoryReleaseTest {
    @Test
    fun keepsTheMapCachesWhileMemoryIsOnlyModeratelyLow() {
        assertFalse(shouldReleaseMapMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE))
    }

    @Test
    fun releasesTheMapCachesWhenMemoryRunsLow() {
        assertTrue(shouldReleaseMapMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW))
        assertTrue(shouldReleaseMapMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL))
    }

    @Test
    fun releasesTheMapCachesWhenTheAppIsNoLongerVisible() {
        assertTrue(shouldReleaseMapMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN))
        assertTrue(shouldReleaseMapMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND))
    }
}
