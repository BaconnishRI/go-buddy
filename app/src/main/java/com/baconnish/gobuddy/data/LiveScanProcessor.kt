package com.baconnish.gobuddy.data

import com.baconnish.gobuddy.data.db.PokemonDao
import com.baconnish.gobuddy.data.db.TrackedPokemon
import com.baconnish.gobuddy.domain.LevelCalculator
import com.baconnish.gobuddy.domain.ScanApplier
import com.baconnish.gobuddy.domain.ScanMatcher
import com.baconnish.gobuddy.domain.ScanResult
import com.baconnish.gobuddy.domain.SpeciesResolver

class LiveScanProcessor(
    private val dao: PokemonDao,
    private val species: SpeciesRepository,
) {

    suspend fun process(capture: ScanCapture): String {
        val scan = capture.result
        if (scan.isEmpty) return "No Pokémon info found on screen"
        val tracked = dao.getAll()
        return when (val match = ScanMatcher.match(capture.lines, scan, tracked, species::byName)) {
            is ScanMatcher.Result.Matched -> {
                val outcome = ScanApplier.apply(
                    match.pokemon, scan, species.byName(match.pokemon.speciesName),
                )
                if (outcome.updated != match.pokemon) dao.upsert(outcome.updated)
                val parts = outcome.changes + outcome.notes
                "${match.pokemon.displayName}: " +
                    if (parts.isEmpty()) "already up to date" else parts.joinToString(", ")
            }
            is ScanMatcher.Result.Ambiguous ->
                "Could be ${match.candidates.joinToString(" or ") { it.displayName }} — " +
                    "update it in Go Buddy"
            ScanMatcher.Result.NoMatch ->
                if (scan.hpMax != null && (scan.cp != null || scan.ivAtk != null)) {
                    addNew(scan)
                } else {
                    "That didn't look like a Pokémon's info screen — open the Pokémon and tap again"
                }
        }
    }

    private suspend fun addNew(scan: ScanResult): String {
        val sp = SpeciesResolver.resolve(scan, species.all)
            ?: return "Not tracked — couldn't identify the species from this screen"
        var pokemon = TrackedPokemon(
            speciesName = sp.name,
            speciesDex = sp.dex,
            kmPerCandy = sp.kmPerCandy,
            candyOwned = scan.candy ?: 0,
            candyXlOwned = scan.candyXl ?: 0,
            ivAtk = scan.ivAtk,
            ivDef = scan.ivDef,
            ivSta = scan.ivSta,
        )
        if (scan.cp != null) {
            val dustLevels = scan.stardust
                ?.let { LevelCalculator.levelsForDustCost(it) }
                ?.takeIf { it.isNotEmpty() }
            val candidates = LevelCalculator.solve(
                scan.cp, scan.hpMax, sp.baseAtk, sp.baseDef, sp.baseSta, dustLevels,
            )
            val levels = if (scan.ivAtk != null && scan.ivDef != null && scan.ivSta != null) {
                val refined = LevelCalculator.nearestIvs(
                    candidates, scan.ivAtk, scan.ivDef, scan.ivSta,
                )
                if (refined != null) {
                    pokemon = pokemon.copy(
                        ivAtk = refined.first.first,
                        ivDef = refined.first.second,
                        ivSta = refined.first.third,
                    )
                    refined.second
                } else {
                    LevelCalculator.filterByDust(
                        LevelCalculator.levelsForCp(
                            scan.cp, sp.baseAtk, sp.baseDef, sp.baseSta,
                            scan.ivAtk, scan.ivDef, scan.ivSta,
                        ),
                        dustLevels,
                    )
                }
            } else {
                val ivs = candidates.map { Triple(it.ivAtk, it.ivDef, it.ivSta) }.distinct()
                if (ivs.size == 1) {
                    pokemon = pokemon.copy(
                        ivAtk = ivs.first().first,
                        ivDef = ivs.first().second,
                        ivSta = ivs.first().third,
                    )
                }
                candidates.map { it.level }.distinct()
            }
            if (levels.size == 1) pokemon = pokemon.copy(currentLevel = levels.first())
        } else if (scan.hpMax != null && scan.ivSta != null) {
            val levels = LevelCalculator.levelFromHp(scan.hpMax, sp.baseSta, scan.ivSta)
            if (levels.size == 1) pokemon = pokemon.copy(currentLevel = levels.first())
        }
        dao.upsert(pokemon)
        return "Added ${sp.name} to Go Buddy"
    }
}
