package fr.julie.mariosprint.data.repository

import fr.julie.mariosprint.data.local.SessionStore
import fr.julie.mariosprint.data.remote.*
import fr.julie.mariosprint.domain.model.PlayerAccount
import fr.julie.mariosprint.domain.repository.AccountRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

internal class SupabaseAccountRepository(private val api: SupabaseApi, private val store: SessionStore,
    override val configured: Boolean, private val now: () -> Long = { System.currentTimeMillis() / 1000 }) : AccountRepository, AccessTokenProvider {
    private val mutex = Mutex()
    private var initialized = false
    private var session: AuthSession? = null
    private val _account = MutableStateFlow<PlayerAccount?>(null)
    override val account = _account.asStateFlow()
    override suspend fun initialize() = mutex.withLock {
        if (initialized) return@withLock
        if (configured) {
            try { store.read()?.let { applySession(it, persist = false) } }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { store.clear() }
        }
        initialized = true
    }
    override suspend fun signIn(idToken: String, nonce: String) {
        initialize()
        withTimeout(15_000) { mutex.withLock {
            check(configured); require(idToken.isNotBlank() && nonce.isNotBlank())
            applySession(api.signIn(GoogleTokenRequest(idToken, nonce)))
        } }
    }
    override suspend fun tokenFor(uid: String, rejectedToken: String?): String {
        initialize()
        return withTimeout(15_000) { mutex.withLock {
            check(configured)
            var current = checkNotNull(session)
            check(current.user.id == uid)
            if (current.expiresAt <= now() + 60 || current.accessToken == rejectedToken) {
                try { applySession(api.refresh(RefreshRequest(current.refreshToken))) }
                catch (error: HttpException) {
                    if (error.code() in listOf(400, 401, 403)) {
                        // Effacer avant de publier null : l'observateur peut annuler la requête dès la déconnexion.
                        store.clear(); session = null; _account.value = null
                    }
                    throw error
                }
                current = checkNotNull(session)
                check(current.user.id == uid)
            }
            current.accessToken
        } }
    }
    override suspend fun signOut() {
        initialize()
        mutex.withLock {
            val token = session?.accessToken
            // La déconnexion locale réussit aussi hors connexion ; aucun autre appareil n'est déconnecté.
            store.clear(); session = null; _account.value = null
            if (token != null) try { withTimeout(5_000) { api.logout("Bearer $token") } }
            catch (error: CancellationException) { if (error !is kotlinx.coroutines.TimeoutCancellationException) throw error }
            catch (_: Exception) { /* Le serveur laisse expirer le jeton déjà supprimé localement. */ }
        }
    }
    private suspend fun applySession(value: AuthSession, persist: Boolean = true) {
        require(value.accessToken.isNotBlank() && value.refreshToken.isNotBlank() && value.user.id.isNotBlank())
        val normalized = AuthSession(value.accessToken, value.refreshToken, value.expiresIn,
            value.expiresAt.takeIf { it > 0 } ?: (now() + value.expiresIn), value.user)
        if (persist) store.save(normalized)
        session = normalized
        val name = value.user.metadata["full_name"] ?: value.user.metadata["name"]
        _account.value = PlayerAccount(value.user.id, name?.jsonPrimitive?.content.orEmpty().trim().take(40))
    }
}
