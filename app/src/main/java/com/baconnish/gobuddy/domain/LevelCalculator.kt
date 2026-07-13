package com.baconnish.gobuddy.domain

import com.baconnish.gobuddy.data.GameData
import kotlin.math.max
import kotlin.math.sqrt

data class LevelIvCandidate(
    val level: Double,
    val ivAtk: Int,
    val ivDef: Int,
    val ivSta: Int,
)

object LevelCalculator {

    const val MIN_LEVEL = 1.0
    const val MAX_LEVEL = 51.0

    fun cpAt(
        level: Double,
        baseAtk: Int,
        baseDef: Int,
        baseSta: Int,
        ivAtk: Int,
        ivDef: Int,
        ivSta: Int,
    ): Int {
        val cpm = GameData.cpMultiplier(level)
        val cp = (baseAtk + ivAtk) *
            sqrt((baseDef + ivDef).toDouble()) *
            sqrt((baseSta + ivSta).toDouble()) *
            cpm * cpm / 10.0
        return max(10, cp.toInt())
    }

    fun levelsForCp(
        cp: Int,
        baseAtk: Int,
        baseDef: Int,
        baseSta: Int,
        ivAtk: Int,
        ivDef: Int,
        ivSta: Int,
    ): List<Double> {
        require(ivAtk in 0..15 && ivDef in 0..15 && ivSta in 0..15)
        val matches = mutableListOf<Double>()
        var level = MIN_LEVEL
        while (level <= MAX_LEVEL) {
            if (cpAt(level, baseAtk, baseDef, baseSta, ivAtk, ivDef, ivSta) == cp) {
                matches.add(level)
            }
            level += 0.5
        }
        return matches
    }

    fun hpAt(level: Double, baseSta: Int, ivSta: Int): Int =
        max(10, ((baseSta + ivSta) * GameData.cpMultiplier(level)).toInt())

    private val DUST_MULTIPLIERS = listOf(1.0, 0.5, 1.2, 0.9, 0.6, 0.45)

    fun levelsForDustCost(dustCost: Int, multiplier: Double? = null): Set<Int> {
        val multipliers = multiplier?.let { listOf(it) } ?: DUST_MULTIPLIERS
        val levels = mutableSetOf<Int>()
        GameData.STARDUST_COST.forEachIndexed { index, base ->
            if (multipliers.any { (base * it).toInt() == dustCost }) levels.add(index + 1)
        }
        return levels
    }

    fun filterByDust(levels: List<Double>, dustLevels: Set<Int>?): List<Double> =
        if (dustLevels.isNullOrEmpty()) {
            levels
        } else {
            levels.filter { it >= GameData.MAX_POKEMON_LEVEL || it.toInt() in dustLevels }
        }

    fun solve(
        cp: Int,
        hpMax: Int?,
        baseAtk: Int,
        baseDef: Int,
        baseSta: Int,
        dustLevels: Set<Int>? = null,
    ): List<LevelIvCandidate> {
        val out = mutableListOf<LevelIvCandidate>()
        var level = MIN_LEVEL
        while (level <= MAX_LEVEL) {
            if (!dustLevels.isNullOrEmpty() &&
                level < GameData.MAX_POKEMON_LEVEL &&
                level.toInt() !in dustLevels
            ) {
                level += 0.5
                continue
            }
            for (ivSta in 0..15) {
                if (hpMax != null && hpAt(level, baseSta, ivSta) != hpMax) continue
                for (ivAtk in 0..15) {
                    for (ivDef in 0..15) {
                        if (cpAt(level, baseAtk, baseDef, baseSta, ivAtk, ivDef, ivSta) == cp) {
                            out.add(LevelIvCandidate(level, ivAtk, ivDef, ivSta))
                        }
                    }
                }
            }
            level += 0.5
        }
        return out
    }

    fun solveByHp(hpMax: Int, baseSta: Int): List<Pair<Double, Int>> {
        val out = mutableListOf<Pair<Double, Int>>()
        var level = MIN_LEVEL
        while (level <= MAX_LEVEL) {
            for (ivSta in 0..15) {
                if (hpAt(level, baseSta, ivSta) == hpMax) out.add(level to ivSta)
            }
            level += 0.5
        }
        return out
    }

    fun levelFromHp(
        hpMax: Int,
        baseSta: Int,
        ivSta: Int,
        dustLevels: Set<Int>? = null,
    ): List<Double> {
        val pairs = solveByHp(hpMax, baseSta)
        if (pairs.isEmpty()) return emptyList()
        val byDistance = pairs.groupBy { kotlin.math.abs(it.second - ivSta) }
        val bestDistance = byDistance.keys.min()
        if (bestDistance > 2) return emptyList()
        return filterByDust(
            byDistance.getValue(bestDistance).map { it.first }.distinct(),
            dustLevels,
        )
    }

    fun nearestIvs(
        candidates: List<LevelIvCandidate>,
        ivAtk: Int,
        ivDef: Int,
        ivSta: Int,
        maxDistance: Int = 3,
    ): Pair<Triple<Int, Int, Int>, List<Double>>? {
        if (candidates.isEmpty()) return null
        val byDistance = candidates.groupBy {
            kotlin.math.abs(it.ivAtk - ivAtk) +
                kotlin.math.abs(it.ivDef - ivDef) +
                kotlin.math.abs(it.ivSta - ivSta)
        }
        val bestDistance = byDistance.keys.min()
        if (bestDistance > maxDistance) return null
        val best = byDistance.getValue(bestDistance)
        val triple = best.map { Triple(it.ivAtk, it.ivDef, it.ivSta) }.distinct().singleOrNull()
            ?: return null
        return triple to best.map { it.level }.distinct()
    }

    fun levelRangeForCp(cp: Int, baseAtk: Int, baseDef: Int, baseSta: Int): ClosedRange<Double>? {
        var lowest: Double? = null
        var highest: Double? = null
        var level = MIN_LEVEL
        while (level <= MAX_LEVEL) {
            val maxCp = cpAt(level, baseAtk, baseDef, baseSta, 15, 15, 15)
            val minCp = cpAt(level, baseAtk, baseDef, baseSta, 0, 0, 0)
            if (cp in minCp..maxCp) {
                if (lowest == null) lowest = level
                highest = level
            }
            level += 0.5
        }
        val low = lowest ?: return null
        return low..highest!!
    }
}
