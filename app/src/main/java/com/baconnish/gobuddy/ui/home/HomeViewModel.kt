package com.baconnish.gobuddy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.baconnish.gobuddy.GoBuddyApp
import com.baconnish.gobuddy.data.SettingsRepository
import com.baconnish.gobuddy.data.TrainerSettings
import com.baconnish.gobuddy.data.db.PokemonDao
import com.baconnish.gobuddy.data.db.TrackedPokemon
import com.baconnish.gobuddy.domain.QuestPlanner
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val dao: PokemonDao,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val quest: StateFlow<QuestPlanner.Quest?> = combine(
        dao.observeAll(),
        settingsRepository.settings,
    ) { list, settings -> QuestPlanner.plan(list, settings) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val showOnboarding: StateFlow<Boolean> = settingsRepository.onboarded
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val settings: StateFlow<TrainerSettings> = settingsRepository.settings

    fun completeOnboarding(settings: TrainerSettings?) {
        settings?.let(settingsRepository::update)
        settingsRepository.markOnboarded()
    }

    fun addHeart(pokemon: TrackedPokemon) {
        viewModelScope.launch {
            dao.upsert(pokemon.copy(hearts = (pokemon.hearts + 1).coerceAtLeast(0)))
        }
    }

    fun move(target: TrackedPokemon, up: Boolean) {
        val list = quest.value?.entries?.map { it.pokemon } ?: return
        val index = list.indexOfFirst { it.id == target.id }
        if (index < 0) return
        val neighbor = list.getOrNull(index + if (up) -1 else 1) ?: return
        viewModelScope.launch {
            if (target.priority == neighbor.priority) {
                dao.upsert(target.copy(priority = neighbor.priority + if (up) -1 else 1))
            } else {
                dao.upsert(target.copy(priority = neighbor.priority))
                dao.upsert(neighbor.copy(priority = target.priority))
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GoBuddyApp
                HomeViewModel(app.container.pokemonDao, app.container.settingsRepository)
            }
        }
    }
}
