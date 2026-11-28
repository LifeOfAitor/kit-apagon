package io.github.lifeofaitor.kitapagon.tools.packagegenerator

import io.github.lifeofaitor.kitapagon.core.packageformat.AddressTemplates
import io.github.lifeofaitor.kitapagon.core.packageformat.IndexSignature
import io.github.lifeofaitor.kitapagon.core.packageformat.MunicipalityList
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

/** The commands that create the signing key and sign the index (decisions 0013, 0014 and 0017). */
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
        val index = IndexParts.merge(partTextsIn(command.parts), addressTemplatesOf(command))
        val signature = IndexSigner.sign(index.compressedList, privateKey)
        // A secret that does not match the key inside the app would make every phone reject the index.
        check(IndexSignature.isValid(index.compressedList, signature, publicKey)) {
            "The signing key does not match ${command.publicKey}, the public key of the app. Nothing was written."
        }
        write(index, signature, command.output)
        print(
            listOf(
                "packages: ${index.list.municipalities.size}",
                "provinces: ${index.list.provinces.size}",
                "data version: ${index.list.dataVersion}",
                "list: ${index.compressedList.size} bytes",
            ).joinToString("\n"),
        )
    }

    private fun partTextsIn(directory: File): List<String> =
        directory
            .listFiles { file -> file.isFile && file.extension == "json" }
            .orEmpty()
            .sortedBy { it.name }
            .map { partFile -> partFile.readText() }

    private fun write(
        index: PublishedIndex,
        signature: ByteArray,
        output: File,
    ) {
        check(output.isDirectory || output.mkdirs()) { "Cannot create $output" }
        index.compressedProvinceDetails.forEach { (fileName, bytes) -> File(output, fileName).writeBytes(bytes) }
        File(output, MunicipalityList.FILE_NAME).writeBytes(index.compressedList)
        File(output, MunicipalityList.SIGNATURE_FILE_NAME).writeBytes(signature)
    }

    // The app ignores addresses that are not https or have no {file} (IndexJson), so they are refused here.
    private fun addressTemplatesOf(command: GeneratorCommand.SignIndex): AddressTemplates {
        val templates = listOf(command.detailsAddress, command.bundleAddress)
        require(templates.all { it.startsWith("https://") && "{file}" in it }) {
            "Download addresses must start with https:// and contain {file}: $templates"
        }
        return AddressTemplates(details = listOf(command.detailsAddress), bundles = listOf(command.bundleAddress))
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
