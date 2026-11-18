package fr.julie.mariosprint.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import fr.julie.mariosprint.data.remote.AuthSession
import fr.julie.mariosprint.data.remote.SupabaseClient
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface SessionStore {
    suspend fun read(): AuthSession?
    suspend fun save(session: AuthSession)
    suspend fun clear()
}
/** Android Keystore garde la clé AES non exportable ; le disque ne reçoit que IV + chiffré. */
internal class EncryptedSessionStore(context: Context, private val io: CoroutineDispatcher = Dispatchers.IO) : SessionStore {
    private val appContext = context.applicationContext
    private val alias = "mario_sprint_supabase_session_v1"
    private fun preferences() = appContext.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return store.getKey(alias, null) as? SecretKey ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    override suspend fun read(): AuthSession? = withContext(io) {
        val encoded = preferences().getString("encrypted", null) ?: return@withContext null
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size > 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        SupabaseClient.json.decodeFromString<AuthSession>(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8))
    }
    override suspend fun save(session: AuthSession) = withContext(io) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.iv + cipher.doFinal(SupabaseClient.json.encodeToString(session).toByteArray(Charsets.UTF_8))
        check(preferences().edit().putString("encrypted", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit())
    }
    override suspend fun clear() = withContext(io) { check(preferences().edit().clear().commit()) }
}
