package fr.julie.mariosprint.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.julie.mariosprint.domain.game.LevelId
import fr.julie.mariosprint.domain.model.FriendCommand
import fr.julie.mariosprint.domain.repository.AccountRepository
import fr.julie.mariosprint.domain.repository.FriendsRepository
import fr.julie.mariosprint.ui.mapping.toFriendsUiError
import fr.julie.mariosprint.ui.mapping.toUi
import fr.julie.mariosprint.ui.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal class UserProfileViewModel(private val accounts: AccountRepository, private val friends: FriendsRepository,
    private val playerUid: String) : ViewModel() {
    private val _state = MutableStateFlow(UserProfileState(playerUid = playerUid))
    val state = _state.asStateFlow()
    private var operation: Job? = null
    private var generation = 0L
    init {
        viewModelScope.launch {
            accounts.account.map { it?.uid }.distinctUntilChanged().collectLatest { uid ->
                ++generation
                operation?.cancel()
                _state.value = UserProfileState(viewerUid = uid, playerUid = playerUid)
                if (uid != null) {
                    refresh()
                    combine(friends.observeProfile(uid, playerUid), friends.observe(uid)) { profile, snapshot -> profile to snapshot }
                        .catch { _state.update { state -> state.copy(error = it.toFriendsUiError()) } }
                        .collect { (profile, snapshot) ->
                            if (accounts.account.value?.uid == uid) _state.update { current ->
                                current.copy(player = profile?.player?.let { PublicPlayerUi(it.uid, it.name, it.skin) },
                                    friendCode = profile?.friendCode, records = profile?.records?.mapNotNull { record ->
                                        LevelId.entries.firstOrNull { it.value == record.levelId }?.let { ProfileRecordUi(it, record.timeMs) }
                                    }.orEmpty(), relation = snapshot.relations.firstOrNull { it.player.uid == playerUid }?.toUi())
                            }
                        }
                }
            }
        }
    }
    fun onAction(action: UserProfileAction) {
        when (action) {
            UserProfileAction.Refresh -> refresh()
            is UserProfileAction.Execute -> refresh(action.command)
            else -> Unit
        }
    }
    private fun refresh(command: FriendCommand? = null) {
        val uid = accounts.account.value?.uid ?: return
        if (operation?.isActive == true) return
        val ownerGeneration = generation
        _state.update { it.copy(loading = command == null, busy = command != null, error = null, cached = true) }
        operation = viewModelScope.launch {
            try {
                if (command == null) friends.refresh(uid) else friends.execute(uid, command)
                currentCoroutineContext().ensureActive()
                friends.refreshProfile(uid, playerUid)
                currentCoroutineContext().ensureActive()
                if (owns(uid, ownerGeneration)) _state.update { it.copy(cached = false) }
            } catch (error: TimeoutCancellationException) { if (owns(uid, ownerGeneration)) _state.update { it.copy(error = error.toFriendsUiError()) } }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (owns(uid, ownerGeneration)) _state.update { it.copy(error = error.toFriendsUiError()) } }
            finally { if (owns(uid, ownerGeneration)) _state.update { it.copy(loading = false, busy = false) } }
        }
    }
    private fun owns(uid: String, ownerGeneration: Long) = accounts.account.value?.uid == uid && generation == ownerGeneration
}
