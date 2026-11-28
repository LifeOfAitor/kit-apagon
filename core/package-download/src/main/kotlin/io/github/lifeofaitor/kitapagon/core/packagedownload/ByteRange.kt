package io.github.lifeofaitor.kitapagon.core.packagedownload

/** The bytes a partial request asks for, both ends included, as HTTP writes them. */
data class ByteRange(
    val first: Long,
    val last: Long,
) {
    init {
        require(first in 0..last) { "Not a range of bytes: $first-$last" }
    }

    val length: Long get() = last - first + 1

    /** The value of the Range header of the request. */
    val rangeHeader: String get() = "bytes=$first-$last"

    /**
     * True when a Content-Range header describes exactly these bytes. The app accepts nothing else:
     * a server that answers with other bytes, or with the whole tar of the province, is refused
     * (decision 0017).
     */
    fun isDescribedBy(contentRange: String?): Boolean {
        val match = contentRange?.let { CONTENT_RANGE.matchEntire(it.trim()) } ?: return false
        val (start, end, total) = match.destructured
        return start.toLongOrNull() == first && end.toLongOrNull() == last && (total == "*" || isAfterLast(total))
    }

    private fun isAfterLast(total: String): Boolean = (total.toLongOrNull() ?: 0) > last

    private companion object {
        val CONTENT_RANGE = Regex("^bytes ([0-9]{1,19})-([0-9]{1,19})/([0-9]{1,19}|\\*)$")
    }
}
