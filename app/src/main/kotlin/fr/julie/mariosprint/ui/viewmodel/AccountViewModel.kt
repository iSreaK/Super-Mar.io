package fr.julie.mariosprint.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.repository.*
import fr.julie.mariosprint.ui.mapping.toUi
import fr.julie.mariosprint.ui.model.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class AccountViewModel(private val accounts: AccountRepository, private val records: RecordsRepository,
    private val sync: RecordSyncRepository) : ViewModel() {
    private val flags = MutableStateFlow(ProfileState(configured = accounts.configured))
    val state = combine(accounts.account, records.observeSound().catch { message(R.string.local_save_failed); emit(false) }, flags) { user, sound, status ->
        status.copy(player = user?.toUi(), sound = sound)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileState(configured = accounts.configured, sound = false))
    private val _events = MutableSharedFlow<AccountEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()
    init {
        flags.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { accounts.initialize() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { message(R.string.sign_in_failed) }
            finally { flags.update { it.copy(busy = false) } }
        }
    }
    fun onAction(action: AccountAction) {
        when (action) {
            AccountAction.SignIn -> if (!flags.value.busy) {
                if (accounts.configured) {
                    flags.update { it.copy(busy = true) }
                    _events.tryEmit(AccountEvent.RequestGoogleSignIn)
                } else message(R.string.supabase_setup_required)
            }
            is AccountAction.TokenReceived -> viewModelScope.launch {
                try { accounts.signIn(action.token, action.nonce); message(R.string.sign_in_success); synchronize() }
                catch (_: TimeoutCancellationException) { message(R.string.network_timeout) }
                catch (error: CancellationException) { throw error }
                catch (_: Exception) { message(R.string.sign_in_failed) }
                finally { flags.update { it.copy(busy = false) } }
            }
            is AccountAction.SignInFailed -> {
                flags.update { it.copy(busy = false) }
                message(when(action.reason) { SignInFailure.CANCELED -> R.string.sign_in_canceled
                    SignInFailure.NO_ACCOUNT -> R.string.google_account_missing
                    SignInFailure.UNAVAILABLE -> R.string.sign_in_failed })
            }
            AccountAction.SignOut -> if (!flags.value.busy) {
                flags.update { it.copy(busy = true) }
                viewModelScope.launch {
                    try { accounts.signOut(); _events.emit(AccountEvent.ClearGoogleSession) }
                    catch (error: CancellationException) { throw error }
                    catch (_: Exception) { flags.update { it.copy(busy = false) }; message(R.string.sign_out_failed) }
                }
            }
            AccountAction.SessionCleared -> flags.update { it.copy(busy = false) }
            AccountAction.ToggleSound -> viewModelScope.launch {
                try { records.toggleSound() }
                catch (error: CancellationException) { throw error }
                catch (_: Exception) { message(R.string.local_save_failed) }
            }
            AccountAction.Synchronize -> synchronize()
        }
    }
    private fun synchronize() {
        if (flags.value.syncing || accounts.account.value == null) return
        flags.update { it.copy(syncing = true) }
        viewModelScope.launch {
            try { message(if (sync.synchronize()) R.string.record_synced else R.string.no_pending_record) }
            catch (_: TimeoutCancellationException) { message(R.string.record_pending) }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { message(R.string.record_pending) }
            finally { flags.update { it.copy(syncing = false) } }
        }
    }
    private fun message(resource: Int) { _events.tryEmit(AccountEvent.ShowMessage(UiText(resource))) }
}
