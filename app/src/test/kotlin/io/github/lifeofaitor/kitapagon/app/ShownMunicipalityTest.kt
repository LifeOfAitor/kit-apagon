package io.github.lifeofaitor.kitapagon.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// Advancing the test scheduler is still marked experimental, but it is the documented way to let the
// store close before the app opens it again.
@OptIn(ExperimentalCoroutinesApi::class)
class ShownMunicipalityTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val bilbao = PackageId.parseOrNull("ES-48020")!!

    @Test
    fun remembersTheMunicipalityShownAfterTheAppIsClosed() =
        runTest {
            val file = File(temporaryFolder.root, "shown.preferences_pb")
            val appBeforeClosing = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
            shownMunicipality(file, appBeforeClosing).show(bilbao)
            advanceUntilIdle()
            appBeforeClosing.cancel()
            advanceUntilIdle()

            assertEquals(bilbao, shownMunicipality(file).id.first())
        }

    @Test
    fun withoutAChoiceThereIsNone() =
        runTest {
            assertNull(shownMunicipality(File(temporaryFolder.root, "new.preferences_pb")).id.first())
        }

    @Test
    fun aFileItCannotReadMeansNoChoice() =
        runTest {
            val damaged = File(temporaryFolder.root, "damaged.preferences_pb").apply { writeText("not preferences") }

            assertNull(shownMunicipality(damaged).id.first())
        }

    // DataStore allows one store per file while its scope lives, as in the app, which keeps one per process.
    private fun TestScope.shownMunicipality(
        file: File,
        scope: CoroutineScope = backgroundScope,
    ): ShownMunicipality = ShownMunicipality(PreferenceDataStoreFactory.create(scope = scope) { file }, scope)
}
