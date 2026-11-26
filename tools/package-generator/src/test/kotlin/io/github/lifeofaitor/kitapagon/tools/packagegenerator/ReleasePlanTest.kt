package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReleasePlanTest {
    private val published =
        listOf(
            release("2026-10-02"),
            release("2026-10-02-ES-20", isPrerelease = true),
            release("2026-11-02"),
            release("2026-11-02-ES-20", isPrerelease = true),
            release("2026-12-02"),
            release("2026-12-02-ES-20", isPrerelease = true),
            release("2026-12-02-ES-48", isPrerelease = true),
        )

    @Test
    fun readsTheListThatGhWrites() {
        val json =
            """[{"isDraft":false,"isPrerelease":true,"tagName":"2026-12-02-ES-20"},""" +
                """{"isDraft":true,"isPrerelease":false,"tagName":"2026-12-02"}]"""

        assertEquals(
            listOf(release("2026-12-02-ES-20", isPrerelease = true), release("2026-12-02", isDraft = true)),
            ReleasePlan.parse(json),
        )
        assertFailsWith<IllegalArgumentException> { ReleasePlan.parse("""{"tagName":"x"}""") }
        assertFailsWith<IllegalArgumentException> { ReleasePlan.parse("""[{"isDraft":false}]""") }
    }

    @Test
    fun aFailedRunOfTheSameDayLeavesReleasesToDeleteFirst() {
        val afterFailure =
            published + release("2027-01-02-ES-01", isPrerelease = true) + release("2027-01-02", isDraft = true)

        assertEquals(listOf("2027-01-02", "2027-01-02-ES-01"), ReleasePlan.leftoversOf(afterFailure, "2027-01-02"))
        assertEquals(emptyList(), ReleasePlan.leftoversOf(published, "2027-01-02"))
    }

    @Test
    fun aDayThatIsAlreadyPublishedIsNeverReplaced() {
        assertFailsWith<IllegalStateException> { ReleasePlan.leftoversOf(published, "2026-12-02") }
    }

    @Test
    fun keepsTheLastTwoPublishedVersionsAndDeletesTheRest() {
        assertEquals(listOf("2026-10-02", "2026-10-02-ES-20"), ReleasePlan.obsoleteOf(published, keep = 2))
    }

    @Test
    fun alsoDeletesTrialRunsThatNeverPublishedAnIndex() {
        val withTrial =
            published + release("2026-12-05-ES-20", isPrerelease = true) + release("2026-12-05", isDraft = true)

        assertEquals(
            listOf("2026-10-02", "2026-10-02-ES-20", "2026-12-05", "2026-12-05-ES-20"),
            ReleasePlan.obsoleteOf(withTrial, keep = 2),
        )
    }

    @Test
    fun neverTouchesReleasesWithOtherNames() {
        val others = listOf(release("v1.0"), release("2026-12-02-notas"), release("indice"))

        assertEquals(
            emptyList(),
            ReleasePlan.obsoleteOf(published + others, keep = 2).filter {
                it in
                    others.map(DataRelease::tag)
            },
        )
    }

    @Test
    fun deletesNothingWhileNoVersionIsPublished() {
        val onlyTrials = listOf(release("2026-12-05-ES-20", isPrerelease = true), release("2026-12-05", isDraft = true))

        assertEquals(emptyList(), ReleasePlan.obsoleteOf(onlyTrials, keep = 2))
        assertFailsWith<IllegalArgumentException> { ReleasePlan.obsoleteOf(published, keep = 0) }
    }

    private fun release(
        tag: String,
        isDraft: Boolean = false,
        isPrerelease: Boolean = false,
    ) = DataRelease(tag, isDraft, isPrerelease)
}
