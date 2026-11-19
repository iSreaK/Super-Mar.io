package fr.julie.mariosprint.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.julie.mariosprint.domain.model.*
import fr.julie.mariosprint.domain.repository.AccountRepository
import fr.julie.mariosprint.domain.repository.FriendsRepository
import fr.julie.mariosprint.ui.mapping.toFriendsUiError
import fr.julie.mariosprint.ui.mapping.toUi
import fr.julie.mariosprint.ui.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
internal class FriendsViewModel(private val accounts: AccountRepository, private val friends: FriendsRepository) : ViewModel() {
    private data class Flags(val uid: String? = null, val refreshing: Boolean = false, val busy: Boolean = false,
        val connected: Boolean = false, val error: UiText? = null)
    private val flags = MutableStateFlow(Flags())
    private val visible = MutableStateFlow(false)
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _events = MutableSharedFlow<FriendsEvent>(extraBufferCapacity = 2)
    val events = _events.asSharedFlow()
    private var generation = 0L
    private var mutation: Job? = null
    private val snapshots = accounts.account.map { it?.uid }.distinctUntilChanged().flatMapLatest { uid ->
        if (uid == null) flowOf(FriendsSnapshot("")) else friends.observe(uid)
            .onStart { emit(FriendsSnapshot(uid)) }
            .catch { flags.update { value -> if (value.uid == uid) value.copy(error = it.toFriendsUiError()) else value }; emit(FriendsSnapshot(uid)) }
    }
    val state = combine(accounts.account, snapshots, flags) { account, snapshot, status ->
        val uid = account?.uid
        val owned = snapshot.takeIf { it.viewerUid == uid }
        val ownedFlags = status.takeIf { it.uid == uid } ?: Flags()
        FriendsState(uid, owned?.friendCode.orEmpty(), owned?.relations?.map { it.toUi() }.orEmpty(),
            ownedFlags.refreshing, ownedFlags.busy, ownedFlags.error, ownedFlags.connected,
            cached = !ownedFlags.connected || ownedFlags.refreshing || ownedFlags.error != null || owned == null || owned.refreshedAtMs == 0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendsState())

    init {
        viewModelScope.launch {
            accounts.account.map { it?.uid }.distinctUntilChanged().onEach { uid ->
                ++generation
                mutation?.cancel()
                flags.value = Flags(uid)
            }.collectLatest { uid ->
                val ownerGeneration = generation
                if (uid != null && accounts.account.value?.uid == uid) visible.collectLatest { active ->
                    flags.update { it.copy(connected = false, refreshing = false) }
                    if (active) coroutineScope {
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            friends.incomingRequests.collect { notice ->
                                if (notice.viewerUid == uid && owns(uid, ownerGeneration))
                                    _events.tryEmit(FriendsEvent.IncomingRequests(uid, notice.count))
                            }
                        }
                        launch {
                            refreshRequests.onStart { emit(Unit) }.conflate().collect { refresh(uid, ownerGeneration) }
                        }
                        friends.liveEvents(uid).collect { event ->
                            if (owns(uid, ownerGeneration)) {
                                flags.update { it.copy(connected = event != FriendsLiveEvent.DISCONNECTED) }
                                if (event != FriendsLiveEvent.DISCONNECTED) refreshRequests.tryEmit(Unit)
                            }
                        }
                    }
                }
            }
        }
    }
    fun onAction(action: FriendsAction) {
        when (action) {
            is FriendsAction.VisibilityChanged -> visible.value = action.visible
            FriendsAction.Refresh -> refreshRequests.tryEmit(Unit)
            is FriendsAction.Execute -> execute(action.command)
            else -> Unit // Navigation et presse-papiers appartiennent au Root.
        }
    }
    private fun execute(command: FriendCommand) {
        val uid = accounts.account.value?.uid ?: return
        val ownerGeneration = generation
        if (flags.value.uid != uid || flags.value.busy) return
        flags.update { it.copy(busy = true, error = null) }
        mutation = viewModelScope.launch {
            try { friends.execute(uid, command); currentCoroutineContext().ensureActive() }
            catch (error: TimeoutCancellationException) { if (owns(uid, ownerGeneration)) flags.update { it.copy(error = error.toFriendsUiError()) } }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (owns(uid, ownerGeneration)) flags.update { it.copy(error = error.toFriendsUiError()) } }
            finally { if (owns(uid, ownerGeneration)) flags.update { it.copy(busy = false) } }
        }
    }
    private suspend fun refresh(uid: String, ownerGeneration: Long) {
        if (!owns(uid, ownerGeneration)) return
        flags.update { it.copy(refreshing = true, error = null) }
        try {
            friends.refresh(uid)
            currentCoroutineContext().ensureActive()
        } catch (error: TimeoutCancellationException) { if (owns(uid, ownerGeneration)) flags.update { it.copy(error = error.toFriendsUiError()) } }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { if (owns(uid, ownerGeneration)) flags.update { it.copy(error = error.toFriendsUiError()) } }
        finally { if (owns(uid, ownerGeneration)) flags.update { it.copy(refreshing = false) } }
    }
    private fun owns(uid: String, ownerGeneration: Long) = accounts.account.value?.uid == uid && generation == ownerGeneration
}
