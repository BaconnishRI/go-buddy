package com.baconnish.gobuddy.domain

import com.baconnish.gobuddy.data.PokemonForm
import com.baconnish.gobuddy.data.Species
import com.baconnish.gobuddy.data.db.TrackedPokemon

object ScanApplier {

    data class Outcome(
        val updated: TrackedPokemon,
        val changes: List<String>,
        val notes: List<String>,
    ) {
        val hasChanges: Boolean get() = changes.isNotEmpty()
    }

    fun apply(pokemon: TrackedPokemon, scan: ScanResult, species: Species?): Outcome {
        val changes = mutableListOf<String>()
        val notes = mutableListOf<String>()
        var updated = pokemon

        val dustMultiplier =
            (if (pokemon.form == PokemonForm.SHADOW) 1.2 else if (pokemon.form == PokemonForm.PURIFIED) 0.9 else 1.0) *
                (if (pokemon.isLucky) 0.5 else 1.0)
        val dustLevels = scan.stardust
            ?.let { LevelCalculator.levelsForDustCost(it, dustMultiplier) }
            ?.takeIf { it.isNotEmpty() }

        var barLevels: List<Double>? = null
        if (scan.ivAtk != null && scan.ivDef != null && scan.ivSta != null) {
            var ivs = Triple(scan.ivAtk, scan.ivDef, scan.ivSta)
            if (species != null && scan.cp != null) {
                val candidates = LevelCalculator.solve(
                    scan.cp, scan.hpMax,
                    species.baseAtk, species.baseDef, species.baseSta,
                    dustLevels,
                )
                LevelCalculator.nearestIvs(candidates, ivs.first, ivs.second, ivs.third)
                    ?.let { (refined, levels) ->
                        ivs = refined
                        barLevels = levels
                    }
            }
            if (ivs.first != updated.ivAtk || ivs.second != updated.ivDef || ivs.third != updated.ivSta) {
                if (updated.ivAtk != null) {
                    notes.add("IVs corrected from appraisal to ${ivs.first}/${ivs.second}/${ivs.third}")
                } else {
                    changes.add("IVs ${ivs.first}/${ivs.second}/${ivs.third} from appraisal")
                }
                updated = updated.copy(ivAtk = ivs.first, ivDef = ivs.second, ivSta = ivs.third)
            }
        }

        if (scan.cp != null) {
            if (species == null) {
                notes.add("can't compute level for a custom species")
            } else {
                val ivAtk = updated.ivAtk
                val ivDef = updated.ivDef
                val ivSta = updated.ivSta
                val levels = barLevels ?: if (ivAtk != null && ivDef != null && ivSta != null) {
                    LevelCalculator.filterByDust(
                        LevelCalculator.levelsForCp(
                            scan.cp, species.baseAtk, species.baseDef, species.baseSta,
                            ivAtk, ivDef, ivSta,
                        ),
                        dustLevels,
                    )
                } else {
                    val candidates = LevelCalculator.solve(
                        scan.cp, scan.hpMax,
                        species.baseAtk, species.baseDef, species.baseSta,
                        dustLevels,
                    )
                    val ivs = candidates.map { Triple(it.ivAtk, it.ivDef, it.ivSta) }.distinct()
                    if (ivs.size == 1) {
                        updated = updated.copy(
                            ivAtk = ivs.first().first,
                            ivDef = ivs.first().second,
                            ivSta = ivs.first().third,
                        )
                    }
                    candidates.map { it.level }.distinct()
                }
                when (levels.size) {
                    1 -> {
                        if (levels.first() != updated.currentLevel) {
                            changes.add("level ${format(updated.currentLevel)} → ${format(levels.first())}")
                            updated = updated.copy(currentLevel = levels.first())
                        }
                    }
                    0 -> notes.add("CP ${scan.cp} doesn't match the stored IVs")
                    else -> notes.add("level ambiguous — set IVs to pin it down")
                }
            }
        } else if (species != null && scan.hpMax != null) {
            val ivSta = updated.ivSta
            if (ivSta != null) {
                val levels = LevelCalculator.levelFromHp(
                    scan.hpMax, species.baseSta, ivSta, dustLevels,
                )
                if (levels.size == 1 && levels.first() != updated.currentLevel) {
                    changes.add(
                        "level ${format(updated.currentLevel)} → ${format(levels.first())} (from HP)",
                    )
                    updated = updated.copy(currentLevel = levels.first())
                }
            }
        }

        if (scan.candy != null && scan.candy != updated.candyOwned) {
            changes.add("candy ${updated.candyOwned} → ${scan.candy}")
            updated = updated.copy(candyOwned = scan.candy)
        }
        if (scan.candyXl != null && scan.candyXl != updated.candyXlOwned) {
            changes.add("candy XL ${updated.candyXlOwned} → ${scan.candyXl}")
            updated = updated.copy(candyXlOwned = scan.candyXl)
        }

        return Outcome(updated, changes, notes)
    }

    private fun format(level: Double): String =
        if (level == level.toInt().toDouble()) level.toInt().toString() else level.toString()
}
