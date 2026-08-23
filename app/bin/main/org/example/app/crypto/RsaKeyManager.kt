package org.example.app.crypto

import net.rsprot.crypto.rsa.RsaKeyPair
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

data class RsaMaterial(
    val rsProtKey: RsaKeyPair,
    val modulus: BigInteger,
    val publicExponent: BigInteger,
)

object RsaKeyManager {
    fun loadOrCreate(
        privateKeyFile: Path,
        publicInfoFile: Path,
    ): RsaMaterial {
        Files.createDirectories(
            privateKeyFile.parent
        )

        val privateKey =
            if (Files.isRegularFile(privateKeyFile)) {
                println(
                    "[RSA] Loading existing private key."
                )

                loadPrivateKey(privateKeyFile)
            } else {
                println(
                    "[RSA] Generating persistent 1024-bit RSA key."
                )

                generatePrivateKey()
                    .also {
                        savePrivateKey(
                            privateKeyFile,
                            it,
                        )
                    }
            }

        val material =
            RsaMaterial(
                rsProtKey =
                    RsaKeyPair(
                        privateKey.privateExponent,
                        privateKey.modulus,
                    ),
                modulus = privateKey.modulus,
                publicExponent =
                    privateKey.publicExponent,
            )

        writePublicInformation(
            publicInfoFile,
            material,
        )

        return material
    }

    private fun generatePrivateKey(): RSAPrivateCrtKey {
        val generator =
            KeyPairGenerator.getInstance("RSA")

        generator.initialize(1024)

        return generator
            .generateKeyPair()
            .private as RSAPrivateCrtKey
    }

    private fun loadPrivateKey(
        file: Path,
    ): RSAPrivateCrtKey {
        val pem =
            Files.readString(file)

        val base64 =
            pem.lineSequence()
                .filterNot {
                    it.startsWith("-----")
                }
                .joinToString("")

        val encoded =
            Base64.getDecoder()
                .decode(base64)

        val spec =
            PKCS8EncodedKeySpec(encoded)

        return KeyFactory
            .getInstance("RSA")
            .generatePrivate(spec) as RSAPrivateCrtKey
    }

    private fun savePrivateKey(
        file: Path,
        key: RSAPrivateCrtKey,
    ) {
        val encoded =
            Base64.getMimeEncoder(
                64,
                "\n".toByteArray(),
            ).encodeToString(
                key.encoded
            )

        val pem =
            buildString {
                appendLine(
                    "-----BEGIN PRIVATE KEY-----"
                )
                appendLine(encoded)
                appendLine(
                    "-----END PRIVATE KEY-----"
                )
            }

        Files.writeString(
            file,
            pem,
        )
    }

    private fun writePublicInformation(
        file: Path,
        material: RsaMaterial,
    ) {
        Files.createDirectories(
            file.parent
        )

        Files.writeString(
            file,
            buildString {
                appendLine(
                    "modulus.decimal=${material.modulus}"
                )
                appendLine(
                    "modulus.hex=${material.modulus.toString(16)}"
                )
                appendLine(
                    "publicExponent=${material.publicExponent}"
                )
            },
        )
    }
}