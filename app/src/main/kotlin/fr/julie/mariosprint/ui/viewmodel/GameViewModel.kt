package fr.julie.mariosprint.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.julie.mariosprint.domain.game.*
import fr.julie.mariosprint.domain.repository.*
import fr.julie.mariosprint.ui.mapping.toUi
import fr.julie.mariosprint.ui.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal class GameViewModel(private val records: RecordsRepository, private val accounts: AccountRepository,
    private val sync: RecordSyncRepository, private val level: LevelId = LevelId.MEADOW) : ViewModel() {
    private val _state = MutableStateFlow(GameState(level = level))
    val state = _state.asStateFlow()
    var engine = GameEngine(level); private set
    private var recorded = false
    private var runUid: String? = null
    init {
        viewModelScope.launch { records.observeStats(level.value).catch { _state.update { it.copy(recordStatus = RecordStatus.SAVE_FAILED) } }
            .collect { stats -> _state.update { it.copy(stats = stats.toUi()) } } }
        startRun()
    }
    fun onAction(action: GameAction) {
        when(action) {
            GameAction.Frame -> refresh()
            GameAction.Pause -> { engine.pause(); refresh() }
            GameAction.Resume -> { engine.resume(); refresh() }
            GameAction.Restart -> startRun()
            GameAction.Synchronize -> synchronize(_state.value.sessionId)
            GameAction.Quit, GameAction.Leaderboard -> { engine.pause(); refresh() }
        }
    }
    private fun startRun() {
        if (_state.value.loading && _state.value.sessionId > 0) return
        engine.pause()
        engine = GameEngine(level)
        recorded = false
        runUid = accounts.account.value?.uid
        _state.update { GameState(level = level, sessionId = it.sessionId + 1, stats = it.stats) }
        viewModelScope.launch {
            try {
                // Une petite transaction déjà engagée doit finir même si la destination est quittée.
                withContext(NonCancellable) { records.startAttempt(level.value) }
                _state.update { it.copy(loading = false) }
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) { _state.update { it.copy(loading = false, recordStatus = RecordStatus.SAVE_FAILED) } }
        }
    }
    private fun refresh() {
        _state.update { it.copy(phase = engine.phase, timeMs = engine.elapsedMs, coins = engine.coins, progress = engine.progress, eligible = engine.eligible, starSeconds = kotlin.math.ceil(engine.player.starSeconds).toInt(),
            magnetSeconds = kotlin.math.ceil(engine.player.magnetSeconds).toInt(), shield = engine.player.shield) }
        if (recorded || (engine.phase != Phase.FINISHED && engine.phase != Phase.DEAD)) return
        recorded = true
        val result = engine.result
        _state.update { it.copy(personalBest = result?.eligible == true && result.timeMs < (it.stats.bestMs ?: Long.MAX_VALUE)) }
        val coins = engine.coins
        val uid = runUid?.takeIf { it == accounts.account.value?.uid }
        val session = _state.value.sessionId
        viewModelScope.launch {
            try {
                withContext(NonCancellable) { records.finishAttempt(coins, result, uid, level.value) }
                if (session != _state.value.sessionId) return@launch
                _state.update { it.copy(recordStatus = when {
                    result == null -> RecordStatus.NONE
                    !result.eligible -> RecordStatus.PRACTICE
                    uid == null -> RecordStatus.LOCAL
                    else -> RecordStatus.SYNCING
                }) }
                if (result?.eligible == true && uid != null) synchronize(session)
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) { if (session == _state.value.sessionId) _state.update { it.copy(recordStatus = RecordStatus.SAVE_FAILED) } }
        }
    }
    private fun synchronize(session: Int) {
        viewModelScope.launch {
            try { sync.synchronize(); if (session == _state.value.sessionId) _state.update { it.copy(recordStatus = RecordStatus.SYNCED) } }
            catch (_: TimeoutCancellationException) { if (session == _state.value.sessionId) _state.update { it.copy(recordStatus = RecordStatus.PENDING) } }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { if (session == _state.value.sessionId) _state.update { it.copy(recordStatus = RecordStatus.PENDING) } }
        }
    }
}
