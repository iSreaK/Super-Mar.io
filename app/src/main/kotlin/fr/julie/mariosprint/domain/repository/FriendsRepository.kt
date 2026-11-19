package fr.julie.mariosprint.domain.repository

import fr.julie.mariosprint.domain.model.*
import kotlinx.coroutines.flow.Flow

interface FriendsRepository {
    /** Signal ponctuel après persistance, commun à toutes les actualisations du cache. */
    val incomingRequests: Flow<IncomingFriendRequests>
    fun observe(uid: String): Flow<FriendsSnapshot>
    fun observeProfile(viewerUid: String, playerUid: String): Flow<UserProfile?>
    /** Nombre de nouvelles demandes reçues, calculé après sauvegarde du snapshot. */
    suspend fun refresh(uid: String): Int
    suspend fun execute(uid: String, command: FriendCommand)
    suspend fun refreshProfile(viewerUid: String, playerUid: String)
    /** Flux froid : son annulation doit fermer la connexion et tous ses réessais. */
    fun liveEvents(uid: String): Flow<FriendsLiveEvent>
}
