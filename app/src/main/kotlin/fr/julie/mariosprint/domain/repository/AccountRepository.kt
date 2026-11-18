package fr.julie.mariosprint.domain.repository

import fr.julie.mariosprint.domain.model.PlayerAccount
import kotlinx.coroutines.flow.StateFlow

interface AccountRepository {
    val configured: Boolean
    val account: StateFlow<PlayerAccount?>
    suspend fun initialize() {}
    suspend fun signIn(idToken: String, nonce: String)
    suspend fun signOut()
}
