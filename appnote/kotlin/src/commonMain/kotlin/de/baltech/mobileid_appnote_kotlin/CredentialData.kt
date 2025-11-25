package de.baltech.mobileid_appnote_kotlin

import kotlinx.serialization.Serializable

@Serializable
data class CredentialData(
    val projectKeyHex: String,    // 32 hex characters (16 bytes)
    val credentialId: String      // ASCII string
)

fun String.hexToByteArray(): ByteArray {
    require(length == 32) { "Hex string must be exactly 32 characters" }
    require(all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) { "Hex string must contain only 0-9, A-F characters" }

    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}