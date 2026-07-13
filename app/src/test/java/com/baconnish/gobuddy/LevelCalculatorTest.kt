package com.baconnish.gobuddy

import com.baconnish.gobuddy.domain.LevelCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCalculatorTest {

    private val mewtwoAtk = 300
    private val mewtwoDef = 182
    private val mewtwoSta = 214

    @Test
    fun `perfect mewtwo at level 40 is the famous 4178`() {
        assertEquals(
            4178,
            LevelCalculator.cpAt(40.0, mewtwoAtk, mewtwoDef, mewtwoSta, 15, 15, 15),
        )
    }

    @Test
    fun `cp and ivs resolve to the exact level`() {
        assertEquals(
            listOf(40.0),
            LevelCalculator.levelsForCp(4178, mewtwoAtk, mewtwoDef, mewtwoSta, 15, 15, 15),
        )
    }

    @Test
    fun `round trip works across levels and iv spreads`() {
        val ivSpreads = listOf(
            Triple(0, 0, 0),
            Triple(15, 15, 15),
            Triple(10, 7, 12),
            Triple(4, 15, 9),
        )
        val levels = listOf(1.0, 7.5, 20.0, 25.5, 34.0, 40.0, 47.5, 51.0)
        for ((ivAtk, ivDef, ivSta) in ivSpreads) {
            for (level in levels) {
                val cp = LevelCalculator.cpAt(level, mewtwoAtk, mewtwoDef, mewtwoSta, ivAtk, ivDef, ivSta)
                val found = LevelCalculator.levelsForCp(cp, mewtwoAtk, mewtwoDef, mewtwoSta, ivAtk, ivDef, ivSta)
                assertTrue(
                    "level $level ivs $ivAtk/$ivDef/$ivSta cp $cp -> $found",
                    found.contains(level),
                )
            }
        }
    }

    @Test
    fun `cp never goes below 10`() {
        assertEquals(10, LevelCalculator.cpAt(1.0, 10, 10, 10, 0, 0, 0))
    }

    @Test
    fun `range without ivs brackets the true level`() {
        val range = LevelCalculator.levelRangeForCp(4178, mewtwoAtk, mewtwoDef, mewtwoSta)!!
        assertTrue(range.start <= 40.0 && 40.0 <= range.endInclusive)
        assertTrue(range.start >= 35.0 && range.endInclusive <= 51.0)
    }

    @Test
    fun `hp matches the known formula`() {
        assertEquals(180, LevelCalculator.hpAt(40.0, mewtwoSta, 15))
    }

    @Test
    fun `solver narrows level with cp and hp`() {
        val cp = LevelCalculator.cpAt(33.5, mewtwoAtk, mewtwoDef, mewtwoSta, 10, 7, 12)
        val hp = LevelCalculator.hpAt(33.5, mewtwoSta, 12)
        val candidates = LevelCalculator.solve(cp, hp, mewtwoAtk, mewtwoDef, mewtwoSta)
        assertTrue(candidates.any { it.level == 33.5 && it.ivAtk == 10 && it.ivDef == 7 && it.ivSta == 12 })
        val withHp = candidates.map { it.level }.distinct()
        val withoutHp = LevelCalculator.solve(cp, null, mewtwoAtk, mewtwoDef, mewtwoSta)
            .map { it.level }.distinct()
        assertTrue(withHp.isNotEmpty())
        assertTrue(withoutHp.containsAll(withHp))
        assertTrue(withHp.size <= withoutHp.size)
    }

    @Test
    fun `dust cost pins the level to its tier`() {
        assertEquals(setOf(39, 40), LevelCalculator.levelsForDustCost(10_000, 1.0))
        assertEquals(setOf(19, 20), LevelCalculator.levelsForDustCost(2_500, 1.0))
    }

    @Test
    fun `dust cost without known form tries all modifiers`() {
        val levels = LevelCalculator.levelsForDustCost(5_000)
        assertTrue(levels.containsAll(setOf(29, 30)))
        assertTrue(levels.containsAll(setOf(39, 40)))
    }

    @Test
    fun `dust levels restrict the solver`() {
        val cp = LevelCalculator.cpAt(33.5, mewtwoAtk, mewtwoDef, mewtwoSta, 10, 7, 12)
        val dust = LevelCalculator.levelsForDustCost(7_000, 1.0)
        assertEquals(setOf(33, 34), dust)
        val candidates = LevelCalculator.solve(cp, null, mewtwoAtk, mewtwoDef, mewtwoSta, dust)
        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.all { it.level.toInt() in dust })
        assertTrue(candidates.any { it.level == 33.5 })
    }

    @Test
    fun `nearest ivs picks the closest candidate within tolerance`() {
        val candidates = LevelCalculator.solve(
            3013, 154, 287, 210, 221,
            LevelCalculator.levelsForDustCost(4000, 1.0),
        )
        val refined = LevelCalculator.nearestIvs(candidates, 12, 10, 12)
        assertEquals(Triple(12, 10, 11), refined?.first)
        assertEquals(listOf(25.0), refined?.second)
    }

    @Test
    fun `nearest ivs gives up when nothing is close`() {
        val candidates = LevelCalculator.solve(
            3013, 154, 287, 210, 221,
            LevelCalculator.levelsForDustCost(4000, 1.0),
        )
        assertNull(LevelCalculator.nearestIvs(candidates, 0, 15, 0))
    }

    @Test
    fun `hp alone can pin the level given the hp iv`() {
        assertEquals(listOf(20.0), LevelCalculator.levelFromHp(134, mewtwoSta, 11))
    }

    @Test
    fun `impossible cp returns no results`() {
        assertEquals(
            emptyList<Double>(),
            LevelCalculator.levelsForCp(9999, mewtwoAtk, mewtwoDef, mewtwoSta, 15, 15, 15),
        )
        assertNull(LevelCalculator.levelRangeForCp(9999, mewtwoAtk, mewtwoDef, mewtwoSta))
    }
}
