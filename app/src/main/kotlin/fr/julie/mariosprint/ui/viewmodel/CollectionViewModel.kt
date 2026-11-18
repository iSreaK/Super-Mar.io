package fr.julie.mariosprint.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.model.CharacterSkin
import fr.julie.mariosprint.domain.model.MissionId
import fr.julie.mariosprint.domain.model.PlayerProgress
import fr.julie.mariosprint.domain.repository.AccountRepository
import fr.julie.mariosprint.domain.repository.PlayerSyncRepository
import fr.julie.mariosprint.domain.repository.ProgressRepository
import fr.julie.mariosprint.ui.model.CollectionAction
import fr.julie.mariosprint.ui.model.CollectionState
import fr.julie.mariosprint.ui.model.MissionUi
import fr.julie.mariosprint.ui.model.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
internal class CollectionViewModel(
    private val accounts: AccountRepository,
    private val progress: ProgressRepository,
    private val sync: PlayerSyncRepository,
) : ViewModel() {
    private val flags = MutableStateFlow(CollectionState())
    private val collection = accounts.account.flatMapLatest { account ->
        progress.observe(account?.uid).onStart { emit(PlayerProgress()) }.map { account to it }
    }.catch { flags.update { it.copy(error = UiText(R.string.local_save_failed)) }; emit(null to PlayerProgress()) }

    val state = combine(collection, flags) { (account, value), status ->
        status.copy(signedIn = account != null, equippedSkin = value.equippedSkin, unlockedSkins = value.unlockedSkins,
            pendingSelection = value.pendingSelection, missions = MissionId.entries.map {
                MissionUi(it, it.progress(value.completedLevels), it.target, it.reward)
            })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionState())

    fun onAction(action: CollectionAction) {
        if (action == CollectionAction.Back || flags.value.busy) return
        val uid = accounts.account.value?.uid
        flags.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                if (action is CollectionAction.Equip && !equip(uid, action.skin)) return@launch
                if (uid != null && accounts.account.value?.uid == uid) sync.synchronize()
            } catch (_: TimeoutCancellationException) { flags.update { it.copy(error = UiText(R.string.progress_sync_pending)) }
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) { flags.update { it.copy(error = UiText(R.string.progress_sync_pending)) }
            } finally { flags.update { it.copy(busy = false) } }
        }
    }

    /** Seul le refus d'équiper signale un personnage verrouillé ; une erreur de synchronisation a son propre message. */
    private suspend fun equip(uid: String?, skin: CharacterSkin): Boolean = try {
        progress.equip(uid, skin)
        true
    } catch (_: IllegalArgumentException) {
        flags.update { it.copy(error = UiText(R.string.skin_not_unlocked)) }
        false
    }
}
