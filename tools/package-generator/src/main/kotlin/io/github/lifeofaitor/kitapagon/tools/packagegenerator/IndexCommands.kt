package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.IndexSignature
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageIndex
import io.github.lifeofaitor.kitapagon.core.packageformat.PackageIndexWriter
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

/** The commands that create the signing key and sign the index (decisions 0013 and 0014). */
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
        val publicKey = IndexSignature.publicKeyFrom(command.publicKey.readText().trim())
        val partFiles =
            command.parts
                .listFiles { file ->
                    file.isFile && file.extension == "json"
                }.orEmpty()
                .sortedBy { it.name }
        val index = IndexParts.merge(partFiles.map { partFile -> partFile.readText() })
        val indexBytes = PackageIndexWriter.write(index).toByteArray(Charsets.UTF_8)
        val signature = IndexSigner.sign(indexBytes, privateKey)
        // A secret that does not match the key inside the app would make every phone reject the index.
        check(IndexSignature.isValid(indexBytes, signature, publicKey)) {
            "The signing key does not match ${command.publicKey}, the public key of the app. Nothing was written."
        }
        check(command.output.isDirectory || command.output.mkdirs()) { "Cannot create ${command.output}" }
        File(command.output, PackageIndex.INDEX_FILE_NAME).writeBytes(indexBytes)
        File(command.output, PackageIndex.SIGNATURE_FILE_NAME).writeBytes(signature)
        print("packages: ${index.packages.size}\ndata version: ${index.dataVersion}")
    }

    fun newSigningKey(command: GeneratorCommand.NewSigningKey) {
        val privateKeyFile = File(command.output, SigningKeys.PRIVATE_KEY_FILE_NAME)
        val publicKeyFile = File(command.output, SigningKeys.PUBLIC_KEY_FILE_NAME)
        // A second key would silently replace the first one: the app would reject every index.
        check(
            !privateKeyFile.exists() && !publicKeyFile.exists(),
        ) { "There is already a signing key in ${command.output}." }
        check(command.output.isDirectory || command.output.mkdirs()) { "Cannot create ${command.output}" }
        val keyPair = SigningKeys.generate()
        privateKeyFile.writeText("")
        Files.setPosixFilePermissions(
            privateKeyFile.toPath(),
            setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
        )
        privateKeyFile.writeText(keyPair.privateKey + "\n")
        publicKeyFile.writeText(keyPair.publicKey + "\n")
        print("private key: $privateKeyFile\npublic key: $publicKeyFile")
    }
}
