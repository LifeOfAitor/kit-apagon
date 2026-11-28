package io.github.lifeofaitor.kitapagon.feature.packages

import io.github.lifeofaitor.kitapagon.core.packageformat.IndexSignature
import java.security.PublicKey

/** The key that checks the signature of the list of municipalities, copied in when building. */
object IndexPublicKey {
    fun load(): PublicKey = IndexSignature.publicKeyFrom(BuildConfig.INDEX_PUBLIC_KEY)
}
