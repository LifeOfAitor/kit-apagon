package io.github.lifeofaitor.kitapagon.core.packageformat

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PackageIdTest {
    @Test
    fun acceptsASpanishMunicipalityCode() {
        assertEquals("ES-20045", PackageId.parseOrNull("ES-20045")?.value)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "ES", "ES-", "es-20045", "ESP-20045", "ES_20045", "ES-20045/..", "../ES-20045"])
    fun rejectsMalformedIdentifiers(candidate: String) {
        assertNull(PackageId.parseOrNull(candidate))
    }

    @Test
    fun rejectsCodesLongerThanTwentyCharacters() {
        assertNull(PackageId.parseOrNull("ES-" + "1".repeat(21)))
    }

    @Test
    fun printsAsItsValue() {
        assertEquals("ES-20045", PackageId.parseOrNull("ES-20045").toString())
    }
}
