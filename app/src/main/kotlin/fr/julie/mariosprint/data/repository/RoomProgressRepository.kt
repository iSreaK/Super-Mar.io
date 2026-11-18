package fr.julie.mariosprint.data.repository

import androidx.room.withTransaction
import fr.julie.mariosprint.data.local.SprintDatabase
import fr.julie.mariosprint.data.local.model.AppearanceEntity
import fr.julie.mariosprint.data.local.model.CompletedLevelEntity
import fr.julie.mariosprint.data.mapping.progressOwner
import fr.julie.mariosprint.data.mapping.toProgress
import fr.julie.mariosprint.data.mapping.acceptsRemoteSelection
import fr.julie.mariosprint.data.remote.SupabaseProgressSource
import fr.julie.mariosprint.domain.model.CharacterSkin
import fr.julie.mariosprint.domain.model.PlayerAccount
import fr.julie.mariosprint.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

internal class RoomProgressRepository(
    private val database: SprintDatabase,
    private val remote: SupabaseProgressSource,
) : ProgressRepository {
    private val dao = database.progressDao()
    private val syncMutex = Mutex()

    override fun observe(uid: String?) = progressOwner(uid).let { owner ->
        combine(dao.observeCompleted(owner), dao.observeAppearance(owner)) { completed, appearance ->
            completed.toProgress(appearance)
        }
    }

    override suspend fun equip(uid: String?, skin: CharacterSkin) {
        val owner = progressOwner(uid)
        database.withTransaction {
            val previous = dao.appearance(owner) ?: AppearanceEntity(owner)
            require(skin in dao.completed(owner).toProgress(previous).unlockedSkins)
            dao.saveAppearance(previous.copy(skinId = skin.value, revision = previous.revision + 1, pending = uid != null))
        }
    }

    override suspend fun synchronize(account: PlayerAccount) = syncMutex.withLock {
        withTimeout(12_000) {
            val owner = progressOwner(account.uid)
            val selection = dao.appearance(owner) ?: AppearanceEntity(owner)
            var server = remote.load(account.uid).toProgress()
            val requestedSkin = CharacterSkin.find(selection.skinId)
            if (selection.pending && requestedSkin != null && requestedSkin in server.unlockedSkins) {
                server = remote.equip(account.uid, requestedSkin).toProgress()
            }
            database.withTransaction {
                dao.insertCompleted(server.completedLevels.map { CompletedLevelEntity(owner, it.value) })
                val current = dao.appearance(owner) ?: AppearanceEntity(owner)
                // Un choix plus récent fait pendant l'appel réseau reste en attente.
                if (current.acceptsRemoteSelection(selection, server.equippedSkin)) {
                    dao.saveAppearance(current.copy(skinId = server.equippedSkin.value, pending = false))
                }
            }
        }
    }
}
