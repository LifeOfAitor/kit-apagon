package io.github.lifeofaitor.kitapagon.core.packageformat

/**
 * Identifier of a municipal package: ISO 3166 country code, a hyphen and the official municipality
 * code of that country. In Spain it is the five digit INE code, for example `ES-20045` (decision 0006).
 */
@JvmInline
value class PackageId private constructor(
    val value: String,
) {
    override fun toString(): String = value

    companion object {
        private val VALID_FORMAT = Regex("^[A-Z]{2}-[0-9A-Za-z]{1,20}$")

        /** Returns null when [value] does not follow the package identifier format. */
        fun parseOrNull(value: String): PackageId? = if (VALID_FORMAT.matches(value)) PackageId(value) else null
    }
}
