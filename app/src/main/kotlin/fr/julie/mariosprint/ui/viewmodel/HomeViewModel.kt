package fr.julie.mariosprint.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.julie.mariosprint.domain.repository.RecordsRepository
import fr.julie.mariosprint.domain.game.*
import fr.julie.mariosprint.ui.mapping.toUi
import fr.julie.mariosprint.R
import fr.julie.mariosprint.ui.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
internal class HomeViewModel(records: RecordsRepository) : ViewModel() {
    private val selected = MutableStateFlow(LevelId.MEADOW)
    val state = selected.flatMapLatest { level ->
        combine(records.observeStats(level.value), records.observeStats(Level.LEGACY_ID)) { stats, legacy ->
            HomeState(selectedLevel = level, stats = stats.toUi(), legacyBestMs = legacy.bestMs)
        }.catch { emit(HomeState(selectedLevel = level, error = UiText(R.string.local_save_failed))) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
    fun selectLevel(level: LevelId) { selected.value = level }
}
