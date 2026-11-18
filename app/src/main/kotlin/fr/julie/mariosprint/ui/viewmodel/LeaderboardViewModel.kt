package fr.julie.mariosprint.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.game.LevelId
import fr.julie.mariosprint.domain.repository.*
import fr.julie.mariosprint.ui.mapping.toUi
import fr.julie.mariosprint.ui.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal class LeaderboardViewModel(private val accounts: AccountRepository, private val ranking: RankingRepository,
    records: RecordsRepository, private val level: LevelId = LevelId.MEADOW) : ViewModel() {
    private val _state = MutableStateFlow(LeaderboardState(level = level))
    val state = _state.asStateFlow()
    private var refreshJob: Job? = null
    init {
        viewModelScope.launch { records.observeStats(level.value).catch { _state.update { it.copy(error = UiText(R.string.local_save_failed)) } }.collect { stats -> _state.update { it.copy(stats = stats.toUi()) } } }
        viewModelScope.launch {
            accounts.account.collectLatest { user ->
                refreshJob?.cancel()
                _state.update { it.copy(entries = emptyList(), error = null, loading = false, cached = true) }
                if (user != null) {
                    refresh()
                    ranking.observeEntries(user.uid, level.value).catch { _state.update { it.copy(error = UiText(R.string.local_save_failed)) } }.collect { entries -> _state.update { it.copy(entries = entries.map { row -> row.toUi() }) } }
                }
            }
        }
    }
    fun onAction(action: LeaderboardAction) {
        if (action == LeaderboardAction.Refresh) refresh()
    }
    private fun refresh() {
        val uid = accounts.account.value?.uid ?: return
        if (refreshJob?.isActive == true) return
        _state.update { it.copy(loading = true, error = null) }
        refreshJob = viewModelScope.launch {
            try {
                ranking.refresh(uid, level.value)
                if (accounts.account.value?.uid == uid) _state.update { it.copy(cached = false) }
            } catch (_: TimeoutCancellationException) {
                if (accounts.account.value?.uid == uid) _state.update { it.copy(error = UiText(R.string.network_timeout)) }
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) {
                if (accounts.account.value?.uid == uid) _state.update { it.copy(error = UiText(R.string.ranking_unavailable)) }
            } finally { if (accounts.account.value?.uid == uid) _state.update { it.copy(loading = false) } }
        }
    }
}
