package fr.julie.mariosprint.data.repository

import fr.julie.mariosprint.data.local.FriendsLocalStore
import fr.julie.mariosprint.data.local.model.*
import fr.julie.mariosprint.data.mapping.toDomain
import fr.julie.mariosprint.data.remote.*
import fr.julie.mariosprint.domain.model.*
import fr.julie.mariosprint.domain.repository.AccountRepository
import fr.julie.mariosprint.domain.repository.FriendsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

internal class CachedFriendsRepository(
    private val local: FriendsLocalStore, private val remote: SupabaseFriendsSource,
    private val realtime: FriendsRealtimeSource, private val accounts: AccountRepository,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : FriendsRepository {
    private val mutex = Mutex()
    private val notices = MutableSharedFlow<IncomingFriendRequests>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingRequests = notices.asSharedFlow()
    override fun observe(uid: String) = local.observe(uid).map { it.toDomain(uid) }
    override fun observeProfile(viewerUid: String, playerUid: String) = local.observeProfile(viewerUid, playerUid)
        .map { it.toDomain(viewerUid, playerUid) }
    override fun liveEvents(uid: String) = realtime.events(uid)
    override suspend fun refresh(uid: String): Int = operation(uid) { save(uid, remote.snapshot(uid)) }
    override suspend fun execute(uid: String, command: FriendCommand) {
        operation(uid) { save(uid, remote.execute(uid, command)) }
    }
    override suspend fun refreshProfile(viewerUid: String, playerUid: String) {
        operation(viewerUid) {
            val profile = remote.profile(viewerUid, playerUid)
            currentCoroutineContext().ensureActive()
            require(profile.uid == playerUid && profile.records.size <= 100)
            require(profile.records.map { it.levelId }.distinct().size == profile.records.size)
            require(profile.records.all { it.timeMs in 10_000..600_000 })
            requireOwner(viewerUid)
            try {
                local.saveProfile(SocialProfileEntity(viewerUid, profile.uid, profile.displayName, profile.skinId,
                    profile.friendCode.takeIf { viewerUid == playerUid }, nowMs()), profile.records.map {
                    SocialProfileRecordEntity(viewerUid, playerUid, it.levelId, it.timeMs)
                })
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { throw FriendsException(FriendsFailure.LOCAL) }
        }
    }
    private suspend fun save(uid: String, value: FriendsSnapshotRemote): Int {
        currentCoroutineContext().ensureActive()
        requireOwner(uid)
        require(value.viewerUid == uid && value.revision >= 0 && value.relations.size <= 300)
        require(FriendCode.normalize(value.friendCode) == value.friendCode)
        require(value.relations.map { it.uid }.distinct().size == value.relations.size)
        require(value.relations.all { it.uid != uid && it.id.isNotBlank() && it.status in setOf("friend", "incoming", "outgoing") })
        return try {
            val added = local.replace(FriendsCacheEntity(uid, value.friendCode, value.revision, nowMs()), value.relations.map {
                FriendRelationEntity(uid, it.uid, it.id, it.displayName, it.skinId, it.status)
            })
            currentCoroutineContext().ensureActive()
            if (added > 0 && accounts.account.value?.uid == uid) notices.tryEmit(IncomingFriendRequests(uid, added))
            added
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { throw FriendsException(FriendsFailure.LOCAL) }
    }
    private suspend fun <T> operation(uid: String, block: suspend () -> T): T = withTimeout(12_000) {
        mutex.withLock { requireOwner(uid); block() }
    }
    private fun requireOwner(uid: String) {
        if (accounts.account.value?.uid != uid) throw CancellationException("Social account changed")
    }
}
