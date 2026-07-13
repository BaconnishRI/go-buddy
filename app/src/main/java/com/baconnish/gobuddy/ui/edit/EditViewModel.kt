package com.baconnish.gobuddy.ui.edit

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
import com.baconnish.gobuddy.data.ScreenshotScanner
import com.baconnish.gobuddy.data.Species
import com.baconnish.gobuddy.data.SpeciesRepository
import com.baconnish.gobuddy.data.db.PokemonDao
import com.baconnish.gobuddy.data.db.TrackedPokemon
import com.baconnish.gobuddy.domain.LevelCalculator
import com.baconnish.gobuddy.domain.SpeciesResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ScanPrefill(
    val species: Species?,
    val cp: Int?,
    val hpMax: Int?,
    val candy: Int?,
    val candyXl: Int?,
    val level: Double?,
    val ivAtk: Int?,
    val ivDef: Int?,
    val ivSta: Int?,
)

class EditViewModel(
    private val dao: PokemonDao,
    private val speciesRepository: SpeciesRepository,
    private val scanner: ScreenshotScanner,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val pokemonId: Long = savedStateHandle["pokemonId"] ?: -1L

    private val _pokemon = MutableStateFlow<TrackedPokemon?>(null)
    val pokemon: StateFlow<TrackedPokemon?> = _pokemon

    val isNew: Boolean get() = pokemonId <= 0

    init {
        viewModelScope.launch {
            _pokemon.value = if (pokemonId > 0) {
                dao.getById(pokemonId)
            } else {
                TrackedPokemon(speciesName = "")
            }
        }
    }

    fun searchSpecies(query: String): List<Species> = speciesRepository.search(query)

    fun scanScreenshot(uri: Uri, onResult: (ScanPrefill?) -> Unit) {
        viewModelScope.launch {
            val result = try {
                scanner.scan(uri).result
            } catch (_: Exception) {
                null
            }
            if (result == null || result.isEmpty) {
                onResult(null)
                return@launch
            }

            val species = SpeciesResolver.resolve(result, speciesRepository.all)

            var level: Double? = null
            var ivAtk: Int? = result.ivAtk
            var ivDef: Int? = result.ivDef
            var ivSta: Int? = result.ivSta
            if (species != null && result.cp != null) {
                val dustLevels = result.stardust
                    ?.let { LevelCalculator.levelsForDustCost(it) }
                    ?.takeIf { it.isNotEmpty() }
                val candidates = LevelCalculator.solve(
                    result.cp, result.hpMax,
                    species.baseAtk, species.baseDef, species.baseSta,
                    dustLevels,
                )
                val levels = if (ivAtk != null && ivDef != null && ivSta != null) {
                    val refined = LevelCalculator.nearestIvs(candidates, ivAtk!!, ivDef!!, ivSta!!)
                    if (refined != null) {
                        ivAtk = refined.first.first
                        ivDef = refined.first.second
                        ivSta = refined.first.third
                        refined.second
                    } else {
                        LevelCalculator.filterByDust(
                            LevelCalculator.levelsForCp(
                                result.cp, species.baseAtk, species.baseDef, species.baseSta,
                                ivAtk!!, ivDef!!, ivSta!!,
                            ),
                            dustLevels,
                        )
                    }
                } else {
                    val ivs = candidates.map { Triple(it.ivAtk, it.ivDef, it.ivSta) }.distinct()
                    if (ivs.size == 1) {
                        ivAtk = ivs.first().first
                        ivDef = ivs.first().second
                        ivSta = ivs.first().third
                    }
                    candidates.map { it.level }.distinct()
                }
                if (levels.size == 1) level = levels.first()
            } else if (species != null && result.hpMax != null && ivSta != null) {
                val levels = LevelCalculator.levelFromHp(result.hpMax, species.baseSta, ivSta!!)
                if (levels.size == 1) level = levels.first()
            }

            onResult(
                ScanPrefill(
                    species = species,
                    cp = result.cp,
                    hpMax = result.hpMax,
                    candy = result.candy,
                    candyXl = result.candyXl,
                    level = level,
                    ivAtk = ivAtk,
                    ivDef = ivDef,
                    ivSta = ivSta,
                ),
            )
        }
    }

    fun save(pokemon: TrackedPokemon, onSaved: () -> Unit) {
        viewModelScope.launch {
            dao.upsert(pokemon)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = _pokemon.value ?: return
        if (current.id <= 0) return
        viewModelScope.launch {
            dao.delete(current)
            onDeleted()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GoBuddyApp
                EditViewModel(
                    app.container.pokemonDao,
                    app.container.speciesRepository,
                    app.container.screenshotScanner,
                    createSavedStateHandle(),
                )
            }
        }
    }
}
