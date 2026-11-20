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

    override fun load(): Pair<String, String>? = runCatching {
        val url = prefs.getString("url", null) ?: return null
        val sealed = Base64.decode(prefs.getString("token", null) ?: return null, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, sealed, 0, 12)) }
        url to String(cipher.doFinal(sealed, 12, sealed.size - 12))
    }.getOrNull()

    override fun save(hubUrl: String, token: String) {
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val sealed = cipher.iv + cipher.doFinal(token.toByteArray())
        prefs.edit { putString("url", hubUrl); putString("token", Base64.encodeToString(sealed, Base64.NO_WRAP)) }
    }

    override fun clear() { prefs.edit { clear() } }

    private val flags = context.getSharedPreferences("hub_prefs", Context.MODE_PRIVATE)
    override fun flag(name: String, default: Boolean) = flags.getBoolean(name, default)
    override fun setFlag(name: String, value: Boolean) { flags.edit { putBoolean(name, value) } }

    private companion object {
        const val ALIAS = "gateway_hub_owner"
        const val TRANSFORM = "AES/GCM/NoPadding"
    }
}
