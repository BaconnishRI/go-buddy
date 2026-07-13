package com.baconnish.gobuddy.domain

import com.baconnish.gobuddy.data.Species

object SpeciesResolver {

    fun resolve(scan: ScanResult, allSpecies: List<Species>): Species? {
        scan.speciesName?.let { name ->
            allSpecies.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }
        }
        val family = scan.candyFamily ?: return null
        val members = allSpecies.filter { it.family.equals(family, ignoreCase = true) }
        if (members.size == 1) return members.first()
        if (members.isEmpty() || scan.cp == null) return null

        val dustLevels = scan.stardust
            ?.let { LevelCalculator.levelsForDustCost(it) }
            ?.takeIf { it.isNotEmpty() }
        val fits = members.filter { member ->
            if (scan.ivAtk != null && scan.ivDef != null && scan.ivSta != null) {
                LevelCalculator.filterByDust(
                    LevelCalculator.levelsForCp(
                        scan.cp, member.baseAtk, member.baseDef, member.baseSta,
                        scan.ivAtk, scan.ivDef, scan.ivSta,
                    ),
                    dustLevels,
                ).isNotEmpty()
            } else {
                LevelCalculator.solve(
                    scan.cp, scan.hpMax,
                    member.baseAtk, member.baseDef, member.baseSta,
                    dustLevels,
                ).isNotEmpty()
            }
        }
        return fits.singleOrNull()
    }
}
