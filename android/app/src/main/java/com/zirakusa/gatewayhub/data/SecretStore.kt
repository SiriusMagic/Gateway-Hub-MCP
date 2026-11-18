package com.zirakusa.gatewayhub.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Where the hub address and the owner token are kept between launches. */
interface SessionStore {
    fun load(): Pair<String, String>?
    fun save(hubUrl: String, token: String)
    fun clear()
    /** Non-secret preferences (kept after logout). */
    fun flag(name: String, default: Boolean): Boolean = default
    fun setFlag(name: String, value: Boolean) {}
    /** This phone's channel credential for [hubUrl] (encrypted): device id + secret. */
    fun device(hubUrl: String): Pair<String, String>? = null
    fun saveDevice(hubUrl: String, value: Pair<String, String>?) {}
    /** The hub this phone's channel credential belongs to (a satellite phone has only this, no owner token). */
    fun deviceHub(): String? = null
}


/**
 * The owner token encrypted with AES-GCM; the key lives in the Android Keystore and never leaves it.
 * Backups are disabled, so the token stays on this phone.
 */
class KeystoreSessionStore(context: Context) : SessionStore {
    private val prefs = context.getSharedPreferences("hub_session", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }

    /** AES-GCM with the Keystore key: iv + ciphertext, Base64. */
    fun seal(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray()), Base64.NO_WRAP)
    }
    fun open(stored: String): String? = runCatching {
        val sealed = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, sealed, 0, 12)) }
        String(cipher.doFinal(sealed, 12, sealed.size - 12))
    }.getOrNull()

    override fun load(): Pair<String, String>? {
        val url = prefs.getString("url", null) ?: return null
        return open(prefs.getString("token", null) ?: return null)?.let { url to it }
    }

    override fun save(hubUrl: String, token: String) {
        prefs.edit { putString("url", hubUrl); putString("token", seal(token)) }
    }

    override fun clear() { prefs.edit { clear() } }

    private val flags = context.getSharedPreferences("hub_prefs", Context.MODE_PRIVATE)
    init {
        // Gateway 0.0.6 kept the Cloudflare token: it is wiped on the first start of this version (never stored again).
        context.getSharedPreferences("hub_cloudflare", Context.MODE_PRIVATE).edit { clear() }
    }
    override fun flag(name: String, default: Boolean) = flags.getBoolean(name, default)
    override fun setFlag(name: String, value: Boolean) { flags.edit { putBoolean(name, value) } }

    private companion object {
        const val ALIAS = "gateway_hub_owner"
        const val TRANSFORM = "AES/GCM/NoPadding"
    }
}
