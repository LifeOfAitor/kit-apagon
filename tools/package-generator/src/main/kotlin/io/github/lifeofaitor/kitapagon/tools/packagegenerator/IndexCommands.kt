package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.PackageIndex
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageIndexWriter
import java.io.File

/** The command that signs the index (decision 0013). */
class IndexCommands(
    private val environment: Map<String, String>,
    private val print: (String) -> Unit,
) {
    fun signIndex(command: GeneratorCommand.SignIndex) {
        val signingKey = environment[GeneratorCommand.SignIndex.SIGNING_KEY_VARIABLE]
        require(!signingKey.isNullOrBlank()) {
            "The signing key must be in ${GeneratorCommand.SignIndex.SIGNING_KEY_VARIABLE}."
        }
        val privateKey = IndexSigner.privateKeyFrom(signingKey)
        val partFiles =
            command.parts
                .listFiles { file ->
                    file.isFile && file.extension == "json"
                }.orEmpty()
                .sortedBy { it.name }
        val index = IndexParts.merge(partFiles.map { partFile -> partFile.readText() })
        val indexBytes = PackageIndexWriter.write(index).toByteArray(Charsets.UTF_8)
        check(command.output.isDirectory || command.output.mkdirs()) { "Cannot create ${command.output}" }
        File(command.output, PackageIndex.INDEX_FILE_NAME).writeBytes(indexBytes)
        File(command.output, PackageIndex.SIGNATURE_FILE_NAME).writeBytes(IndexSigner.sign(indexBytes, privateKey))
        print("packages: ${index.packages.size}\ndata version: ${index.dataVersion}")
    }
}
