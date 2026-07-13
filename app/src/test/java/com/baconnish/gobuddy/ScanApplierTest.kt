package com.baconnish.gobuddy

import com.baconnish.gobuddy.data.Species
import com.baconnish.gobuddy.data.db.TrackedPokemon
import com.baconnish.gobuddy.domain.ScanApplier
import com.baconnish.gobuddy.domain.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanApplierTest {

    private val mewtwo = Species(150, "Mewtwo", 20.0, 300, 182, 214, "Mewtwo")
    private val regigigas = Species(486, "Regigigas", 20.0, 287, 210, 221, "Regigigas")

    @Test
    fun `slightly misread appraisal bars snap to the only possible spread`() {
        val pokemon = TrackedPokemon(id = 1, speciesName = "Regigigas", currentLevel = 20.0)
        val outcome = ScanApplier.apply(
            pokemon,
            ScanResult(cp = 3013, hpMax = 154, stardust = 4000, ivAtk = 12, ivDef = 10, ivSta = 12),
            regigigas,
        )
        assertEquals(12, outcome.updated.ivAtk)
        assertEquals(10, outcome.updated.ivDef)
        assertEquals(11, outcome.updated.ivSta)
        assertEquals(25.0, outcome.updated.currentLevel, 0.0)
    }

    @Test
    fun `stored ivs plus new cp updates the level and candy`() {
        val pokemon = TrackedPokemon(
            id = 1, speciesName = "Mewtwo", currentLevel = 30.0,
            ivAtk = 15, ivDef = 15, ivSta = 15, candyOwned = 10,
        )
        val outcome = ScanApplier.apply(pokemon, ScanResult(cp = 4178, candy = 50), mewtwo)
        assertEquals(40.0, outcome.updated.currentLevel, 0.0)
        assertEquals(50, outcome.updated.candyOwned)
        assertEquals(2, outcome.changes.size)
        assertTrue(outcome.notes.isEmpty())
    }

    @Test
    fun `custom species still gets candy but no level`() {
        val pokemon = TrackedPokemon(id = 1, speciesName = "Homemade", currentLevel = 20.0)
        val outcome = ScanApplier.apply(pokemon, ScanResult(cp = 1500, candy = 33), null)
        assertEquals(20.0, outcome.updated.currentLevel, 0.0)
        assertEquals(33, outcome.updated.candyOwned)
        assertTrue(outcome.notes.isNotEmpty())
    }

    @Test
    fun `no changes when the scan matches current state`() {
        val pokemon = TrackedPokemon(
            id = 1, speciesName = "Mewtwo", currentLevel = 40.0,
            ivAtk = 15, ivDef = 15, ivSta = 15, candyOwned = 50,
        )
        val outcome = ScanApplier.apply(pokemon, ScanResult(cp = 4178, candy = 50), mewtwo)
        assertFalse(outcome.hasChanges)
        assertEquals(pokemon, outcome.updated)
    }

    @Test
    fun `cp that contradicts stored ivs is flagged`() {
        val pokemon = TrackedPokemon(
            id = 1, speciesName = "Mewtwo", currentLevel = 40.0,
            ivAtk = 0, ivDef = 0, ivSta = 0,
        )
        val outcome = ScanApplier.apply(pokemon, ScanResult(cp = 4178), mewtwo)
        assertEquals(40.0, outcome.updated.currentLevel, 0.0)
        assertTrue(outcome.notes.any { it.contains("doesn't match") })
    }

    @Test
    fun `appraisal ivs are stored and pin the level without prior ivs`() {
        val pokemon = TrackedPokemon(id = 1, speciesName = "Mewtwo", currentLevel = 20.0)
        val outcome = ScanApplier.apply(
            pokemon,
            ScanResult(cp = 4178, ivAtk = 15, ivDef = 15, ivSta = 15),
            mewtwo,
        )
        assertEquals(15, outcome.updated.ivAtk)
        assertEquals(40.0, outcome.updated.currentLevel, 0.0)
        assertTrue(outcome.changes.any { it.contains("IVs") })
    }

    @Test
    fun `appraisal ivs override stale stored ivs with a note`() {
        val pokemon = TrackedPokemon(
            id = 1, speciesName = "Mewtwo", currentLevel = 40.0,
            ivAtk = 1, ivDef = 2, ivSta = 3,
        )
        val outcome = ScanApplier.apply(
            pokemon,
            ScanResult(ivAtk = 15, ivDef = 15, ivSta = 15),
            mewtwo,
        )
        assertEquals(15, outcome.updated.ivAtk)
        assertTrue(outcome.notes.any { it.contains("corrected") })
    }

    @Test
    fun `appraisal screen without readable cp still sets ivs and level from hp`() {
        val pokemon = TrackedPokemon(id = 1, speciesName = "Mewtwo", currentLevel = 30.0)
        val outcome = ScanApplier.apply(
            pokemon,
            ScanResult(hpMax = 134, ivAtk = 11, ivDef = 15, ivSta = 11),
            mewtwo,
        )
        assertEquals(11, outcome.updated.ivSta)
        assertEquals(20.0, outcome.updated.currentLevel, 0.0)
        assertTrue(outcome.changes.any { it.contains("from HP") })
    }

    @Test
    fun `xl candy updates too`() {
        val pokemon = TrackedPokemon(id = 1, speciesName = "Mewtwo", candyXlOwned = 5)
        val outcome = ScanApplier.apply(pokemon, ScanResult(candyXl = 40), mewtwo)
        assertEquals(40, outcome.updated.candyXlOwned)
        assertTrue(outcome.changes.any { it.contains("XL") })
    }
}
