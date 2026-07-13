package com.baconnish.gobuddy.domain

import com.baconnish.gobuddy.data.Species
import com.baconnish.gobuddy.data.db.TrackedPokemon

object ScanMatcher {

    sealed interface Result {
        data class Matched(val pokemon: TrackedPokemon) : Result
        data class Ambiguous(val candidates: List<TrackedPokemon>) : Result
        data object NoMatch : Result
    }

    fun match(
        lines: List<String>,
        scan: ScanResult,
        tracked: List<TrackedPokemon>,
        speciesOf: (String) -> Species?,
    ): Result {
        val cleaned = lines.map { it.trim().trimEnd('♂', '♀', ' ') }

        val byNickname = tracked.filter { pokemon ->
            pokemon.nickname.isNotBlank() &&
                cleaned.any { it.equals(pokemon.nickname, ignoreCase = true) }
        }
        if (byNickname.size == 1) return Result.Matched(byNickname.first())

        val pool = byNickname.ifEmpty { tracked }
        val bySpecies = if (scan.speciesName != null) {
            pool.filter { it.speciesName.equals(scan.speciesName, ignoreCase = true) }
        } else {
            emptyList()
        }
        val byFamily = if (bySpecies.isEmpty() && scan.candyFamily != null) {
            pool.filter { pokemon ->
                speciesOf(pokemon.speciesName)?.family
                    ?.equals(scan.candyFamily, ignoreCase = true) == true
            }
        } else {
            emptyList()
        }

        val candidates = bySpecies.ifEmpty { byFamily }
        if (candidates.isEmpty()) return Result.NoMatch
        if (candidates.size == 1) return Result.Matched(candidates.first())

        val consistent = candidates.filter { pokemon ->
            isIvConsistent(pokemon, scan, speciesOf(pokemon.speciesName))
        }
        return when (consistent.size) {
            1 -> Result.Matched(consistent.first())
            0 -> Result.Ambiguous(candidates)
            else -> Result.Ambiguous(consistent)
        }
    }

    private fun isIvConsistent(pokemon: TrackedPokemon, scan: ScanResult, species: Species?): Boolean {
        if (species == null || scan.cp == null) return true
        val ivAtk = pokemon.ivAtk ?: return true
        val ivDef = pokemon.ivDef ?: return true
        val ivSta = pokemon.ivSta ?: return true
        val levels = LevelCalculator.levelsForCp(
            scan.cp, species.baseAtk, species.baseDef, species.baseSta,
            ivAtk, ivDef, ivSta,
        )
        if (levels.isEmpty()) return false
        val hpMax = scan.hpMax ?: return true
        return levels.any { LevelCalculator.hpAt(it, species.baseSta, ivSta) == hpMax }
    }
}
