package com.baconnish.gobuddy.ui.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.baconnish.gobuddy.GoBuddyApp
import com.baconnish.gobuddy.data.HyperTrainingStat
import com.baconnish.gobuddy.data.ScreenshotScanner
import com.baconnish.gobuddy.data.SettingsRepository
import com.baconnish.gobuddy.data.SpeciesRepository
import com.baconnish.gobuddy.data.TrainerSettings
import com.baconnish.gobuddy.data.db.PokemonDao
import com.baconnish.gobuddy.data.db.TrackedPokemon
import com.baconnish.gobuddy.domain.GoalPlan
import com.baconnish.gobuddy.domain.GoalPlanner
import com.baconnish.gobuddy.domain.LevelCalculator
import com.baconnish.gobuddy.domain.ScanApplier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val pokemon: TrackedPokemon? = null,
    val settings: TrainerSettings = TrainerSettings(),
    val plan: GoalPlan? = null,
    val currentCp: Int? = null,
    val targetCp: Int? = null,
    val hyperTrainedCp: Int? = null,
)

class DetailViewModel(
    private val dao: PokemonDao,
    settingsRepository: SettingsRepository,
    private val speciesRepository: SpeciesRepository,
    private val scanner: ScreenshotScanner,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val pokemonId: Long = savedStateHandle["pokemonId"] ?: -1L

    val uiState: StateFlow<DetailUiState> =
        combine(dao.observeById(pokemonId), settingsRepository.settings) { pokemon, settings ->
            val species = pokemon?.let { speciesRepository.byName(it.speciesName) }
            val ivAtk = pokemon?.ivAtk
            val ivDef = pokemon?.ivDef
            val ivSta = pokemon?.ivSta
            val hasIvs = species != null && ivAtk != null && ivDef != null && ivSta != null
            DetailUiState(
                pokemon = pokemon,
                settings = settings,
                plan = pokemon?.let {
                    GoalPlanner.plan(
                        currentLevel = it.currentLevel,
                        targetLevel = it.targetLevel,
                        form = it.form,
                        isLucky = it.isLucky,
                        candyOwned = it.candyOwned,
                        candyXlOwned = it.candyXlOwned,
                        hearts = it.hearts,
                        kmPerCandy = it.kmPerCandy,
                        trainerLevel = settings.trainerLevel,
                        heartsPerDay = settings.heartsPerDay,
                        kmPerDay = settings.kmPerDay,
                    )
                },
                currentCp = if (hasIvs) {
                    LevelCalculator.cpAt(
                        pokemon.currentLevel,
                        species.baseAtk, species.baseDef, species.baseSta,
                        ivAtk, ivDef, ivSta,
                    )
                } else {
                    null
                },
                targetCp = if (hasIvs) {
                    LevelCalculator.cpAt(
                        minOf(pokemon.targetLevel, LevelCalculator.MAX_LEVEL),
                        species.baseAtk, species.baseDef, species.baseSta,
                        ivAtk, ivDef, ivSta,
                    )
                } else {
                    null
                },
                hyperTrainedCp = if (hasIvs && pokemon.hyperTrainingStat != null) {
                    val stat = runCatching {
                        HyperTrainingStat.valueOf(pokemon.hyperTrainingStat)
                    }.getOrNull()
                    stat?.let {
                        LevelCalculator.cpAt(
                            pokemon.currentLevel,
                            species.baseAtk, species.baseDef, species.baseSta,
                            if (it == HyperTrainingStat.ATK || it == HyperTrainingStat.ALL) 15 else ivAtk,
                            if (it == HyperTrainingStat.DEF || it == HyperTrainingStat.ALL) 15 else ivDef,
                            if (it == HyperTrainingStat.STA || it == HyperTrainingStat.ALL) 15 else ivSta,
                        )
                    }
                } else {
                    null
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun incrementIv(stat: HyperTrainingStat) {
        val pokemon = uiState.value.pokemon ?: return
        val updated = when (stat) {
            HyperTrainingStat.ATK ->
                pokemon.copy(ivAtk = ((pokemon.ivAtk ?: 0) + 1).coerceAtMost(15))
            HyperTrainingStat.DEF ->
                pokemon.copy(ivDef = ((pokemon.ivDef ?: 0) + 1).coerceAtMost(15))
            HyperTrainingStat.STA ->
                pokemon.copy(ivSta = ((pokemon.ivSta ?: 0) + 1).coerceAtMost(15))
            HyperTrainingStat.ALL -> return
        }
        if (updated != pokemon) {
            viewModelScope.launch { dao.upsert(updated) }
        }
    }

    fun addHearts(delta: Int) {
        val pokemon = uiState.value.pokemon ?: return
        viewModelScope.launch {
            dao.upsert(pokemon.copy(hearts = (pokemon.hearts + delta).coerceAtLeast(0)))
        }
    }

    fun adjustLevel(delta: Double) {
        val pokemon = uiState.value.pokemon ?: return
        val newLevel = (pokemon.currentLevel + delta).coerceIn(1.0, 50.0)
        if (newLevel == pokemon.currentLevel) return
        viewModelScope.launch {
            dao.upsert(pokemon.copy(currentLevel = newLevel))
        }
    }

    fun updateFromScan(uri: Uri, onDone: (String) -> Unit) {
        val pokemon = uiState.value.pokemon ?: return
        viewModelScope.launch {
            val result = try {
                scanner.scan(uri).result
            } catch (_: Exception) {
                null
            }
            if (result == null || result.isEmpty) {
                onDone("Couldn't read that screenshot.")
                return@launch
            }

            val outcome = ScanApplier.apply(
                pokemon, result, speciesRepository.byName(pokemon.speciesName),
            )
            if (outcome.updated != pokemon) dao.upsert(outcome.updated)
            val parts = outcome.changes + outcome.notes
            onDone(if (parts.isEmpty()) "Everything already up to date." else "Updated: ${parts.joinToString(", ")}.")
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GoBuddyApp
                DetailViewModel(
                    app.container.pokemonDao,
                    app.container.settingsRepository,
                    app.container.speciesRepository,
                    app.container.screenshotScanner,
                    createSavedStateHandle(),
                )
            }
        }
    }
}
