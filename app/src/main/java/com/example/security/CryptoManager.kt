package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/**
 * Hardware-backed Android Keystore AES-256-GCM encryption & PBKDF2WithHmacSHA256
 * PIN hashing for protecting sensitive business records and staff credentials.
 */
object CryptoManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "stockpulse_vault_master_key_v1"
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ALGO = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 12_000
    private const val SALT_BYTES = 16

    private val secureRandom = SecureRandom()

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts plaintext into "v1:<base64_iv>:<base64_ciphertext>" using AES-256-GCM.
     */
    fun encrypt(plaintext: String): String {
        if (plaintext.isBlank()) return ""
        return try {
            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val dataB64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            "v1:$ivB64:$dataB64"
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Decrypts a payload produced by [encrypt]. Returns empty string on tampering or error.
     */
    fun decrypt(payload: String): String {
        if (payload.isBlank()) return ""
        if (!payload.startsWith("v1:")) return payload
        return try {
            val parts = payload.split(":")
            if (parts.size != 3) return ""
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[2], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_MODE)
            val spec = GCMParameterSpec(GCM_TAG_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    fun generateSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hashPin(pin: String, saltB64: String): String {
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGO)
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun verifyPin(pin: String, saltB64: String, expectedHashB64: String): Boolean {
        if (pin.isBlank() || saltB64.isBlank() || expectedHashB64.isBlank()) return false
        return try {
            val actual = Base64.decode(hashPin(pin, saltB64), Base64.NO_WRAP)
            val expected = Base64.decode(expectedHashB64, Base64.NO_WRAP)
            MessageDigest.isEqual(actual, expected)
        } catch (_: Exception) {
            false
        }
    }
}
