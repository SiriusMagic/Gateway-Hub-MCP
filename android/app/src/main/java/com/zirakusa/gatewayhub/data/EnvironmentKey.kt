package com.zirakusa.gatewayhub.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/** Signs execution packages for this environment. Only the public key ever leaves the phone. */
interface PackageSigner {
    val envId: String
    /** X.509 (SubjectPublicKeyInfo), Base64: what providers keep to verify packages. */
    fun publicKey(): String
    fun sign(bytes: ByteArray): ByteArray
}

private fun newEnvId() = "env_" + ByteArray(12).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }

/**
 * ECDSA P-256 key in the Android Keystore (not exportable). P-256 rather than Ed25519 because providers
 * verify it on Android 8+ (Ed25519 needs Android 13).
 */
class KeystoreSigner(context: Context) : PackageSigner {
    private val prefs = context.getSharedPreferences("hub_environment", Context.MODE_PRIVATE)
    override val envId: String = prefs.getString("env", null) ?: newEnvId().also { id -> prefs.edit { putString("env", id) } }

    private fun entry(): KeyStore.PrivateKeyEntry {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.PrivateKeyEntry)?.let { return it }
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")).setDigests(KeyProperties.DIGEST_SHA256).build())
        }.generateKeyPair()
        return store.getEntry(ALIAS, null) as KeyStore.PrivateKeyEntry
    }

    override fun publicKey(): String = Base64.getEncoder().encodeToString(entry().certificate.publicKey.encoded)
    override fun sign(bytes: ByteArray): ByteArray = sign(entry().privateKey, bytes)

    private companion object { const val ALIAS = "gateway_env_p256" }
}

/** Same algorithm with a software key (tests). */
class SoftwareSigner(override val envId: String = newEnvId(),
    private val keys: KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()) : PackageSigner {
    override fun publicKey(): String = Base64.getEncoder().encodeToString(keys.public.encoded)
    override fun sign(bytes: ByteArray): ByteArray = sign(keys.private, bytes)
}

private fun sign(key: PrivateKey, bytes: ByteArray): ByteArray = Signature.getInstance("SHA256withECDSA").run { initSign(key); update(bytes); sign() }
