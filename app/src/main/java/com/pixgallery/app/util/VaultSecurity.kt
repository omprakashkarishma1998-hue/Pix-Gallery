package com.pixgallery.app.util

import java.security.MessageDigest

/**
 * Small helper for the Vault PIN. We never store the PIN itself - only a
 * salted SHA-256 hash of it, so a rooted-device / backup-file read can't
 * recover the actual PIN the user typed.
 */
object VaultSecurity {

    // App-specific static salt. This isn't meant to defend against a
    // sophisticated attacker with device access (that's what the Android
    // permission model / lock screen already do) - it just means the PIN
    // isn't sitting in prefs as recoverable plaintext.
    private const val SALT = "pixgallery_vault_salt_v1"

    fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((SALT + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }
}
