package fr.julie.mariosprint.domain.repository

import fr.julie.mariosprint.domain.model.CharacterSkin
import fr.julie.mariosprint.domain.model.PlayerAccount
import fr.julie.mariosprint.domain.model.PlayerProgress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observe(uid: String?): Flow<PlayerProgress>
    suspend fun equip(uid: String?, skin: CharacterSkin)
    suspend fun synchronize(account: PlayerAccount)
}
