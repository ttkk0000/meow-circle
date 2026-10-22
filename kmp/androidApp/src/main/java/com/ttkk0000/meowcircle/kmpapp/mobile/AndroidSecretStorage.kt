package com.ttkk0000.meowcircle.kmpapp.mobile

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.ttkk0000.meowcircle.client.MobileSecretStorage
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Only an encrypted token pair enters preferences. The key never leaves Android Keystore. */
class AndroidSecretStorage(context: Context) : MobileSecretStorage {
    private val preferences = context.applicationContext.getSharedPreferences("mnd_v2_secrets", Context.MODE_PRIVATE)
    private val alias = "mnd.v2.session"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override fun read(): String? {
        val raw = preferences.getString("session", null) ?: return null
        val parts = raw.split('.', limit = 2)
        check(parts.size == 2) { "Invalid encrypted session" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }
    override fun write(value: String?) {
        val editor = preferences.edit()
        if (value == null) editor.remove("session") else {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val encrypted = Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
            editor.putString("session", "$iv.$encrypted")
        }
        check(editor.commit()) { "Secure session write failed" }
    }
}
