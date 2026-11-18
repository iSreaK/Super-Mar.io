package fr.julie.mariosprint.ui.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.*
import androidx.credentials.exceptions.*
import com.google.android.libraries.identity.googleid.*
import fr.julie.mariosprint.ui.model.SignInFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import java.security.SecureRandom

/** Le sélecteur Android vit dans l'adaptateur UI ; aucune Activity conservée par un ViewModel. */
internal class GoogleSignInCoordinator(context: Context, private val webClientId: String) {
    private val manager = CredentialManager.create(context.applicationContext)
    suspend fun signIn(activity: Activity, onToken: (String, String) -> Unit, onFailure: (SignInFailure) -> Unit) {
        try {
            val rawNonce = withContext(Dispatchers.Default) {
                val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
                random.joinToString("") { "%02x".format(it) }
            }
            val hashedNonce = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            val option = GetSignInWithGoogleOption.Builder(webClientId).setNonce(hashedNonce).build()
            val response = manager.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
            val credential = response.credential
            check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
            onToken(GoogleIdTokenCredential.createFrom(credential.data).idToken, rawNonce)
        } catch (_: GetCredentialCancellationException) { onFailure(SignInFailure.CANCELED)
        } catch (_: NoCredentialException) { onFailure(SignInFailure.NO_ACCOUNT)
        } catch (error: CancellationException) { onFailure(SignInFailure.CANCELED); throw error
        } catch (_: Exception) { onFailure(SignInFailure.UNAVAILABLE) }
    }
    suspend fun clearSession() {
        // Supabase est déjà déconnecté ; le nettoyage des préférences Google ne doit pas bloquer l'UI.
        withTimeoutOrNull(GOOGLE_SESSION_CLEAR_TIMEOUT_MS) {
            manager.clearCredentialState(ClearCredentialStateRequest())
        }
    }
}

private const val GOOGLE_SESSION_CLEAR_TIMEOUT_MS = 15_000L
