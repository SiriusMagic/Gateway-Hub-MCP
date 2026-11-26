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
class KeystoreSessionStore(context: Context, agent: Boolean = false) : SessionStore {
    private val prefs = context.getSharedPreferences(if (agent) "lucia_mcp" else "hub_session", Context.MODE_PRIVATE)
    private val alias = if (agent) "gateway_hub_lucia_agent" else "gateway_hub_owner"

    private val encryption = PrivateValues(alias)

    override fun load(): Pair<String, String>? = runCatching {
        val url = prefs.getString("url", null) ?: return null
        url to encryption.open(prefs.getString("token", null) ?: return null)
    }.getOrNull()

    override fun save(hubUrl: String, token: String) {
        prefs.edit { putString("url", hubUrl); putString("token", encryption.seal(token)) }
    }

    override fun clear() { prefs.edit { clear() } }

    private val flags = context.getSharedPreferences("hub_prefs", Context.MODE_PRIVATE)
    override fun flag(name: String, default: Boolean) = flags.getBoolean(name, default)
    override fun setFlag(name: String, value: Boolean) { flags.edit { putBoolean(name, value) } }

}

internal interface RecordCipher {
    fun seal(text: String): String
    fun open(value: String): String
}

/** AES-GCM with a separate Android Keystore key for each private store. */
internal class PrivateValues(private val alias: String) : RecordCipher {
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }

    override fun seal(text: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(text.toByteArray()), Base64.NO_WRAP)
    }

    override fun open(value: String): String {
        val sealed = Base64.decode(value, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, sealed, 0, 12))
        }
        return String(cipher.doFinal(sealed, 12, sealed.size - 12))
    }
}
