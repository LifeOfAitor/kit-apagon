package io.github.lifeofaitor.kitapagon.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Remembers which installed municipality the map shows, also after the app is closed (decision 0017).
 * It is a convenience: if the phone cannot read or save it, the map shows the first municipality.
 */
class ShownMunicipality(
    private val dataStore: DataStore<Preferences>,
    /** Lives as long as the app, so leaving a screen right after choosing never loses the choice. */
    private val scope: CoroutineScope,
) {
    val id: Flow<PackageId?> =
        dataStore.data
            .catch { problem -> if (problem is IOException) emit(emptyPreferences()) else throw problem }
            .map { preferences -> preferences[SHOWN_PACKAGE]?.let(PackageId::parseOrNull) }
            .distinctUntilChanged()

    fun show(id: PackageId) {
        scope.launch {
            try {
                dataStore.edit { preferences -> preferences[SHOWN_PACKAGE] = id.value }
            } catch (expectedWhenThePhoneCannotWrite: IOException) {
                // The map keeps showing what it showed; nothing else depends on this choice.
            }
        }
    }

    companion object {
        const val FILE_NAME = "municipio-mostrado"
        private val SHOWN_PACKAGE = stringPreferencesKey("shown_package_id")
    }
}
