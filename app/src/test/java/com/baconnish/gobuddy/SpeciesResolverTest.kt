package com.baconnish.gobuddy

import com.baconnish.gobuddy.data.Species
import com.baconnish.gobuddy.domain.LevelCalculator
import com.baconnish.gobuddy.domain.ScanResult
import com.baconnish.gobuddy.domain.SpeciesResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeciesResolverTest {

    private val small = Species(1, "Smallmon", 3.0, 100, 100, 100, "Smallmon")
    private val big = Species(2, "Bigmon", 3.0, 300, 300, 300, "Smallmon")
    private val lone = Species(3, "Lonely", 5.0, 150, 150, 150, "Lonely")
    private val all = listOf(small, big, lone)

    @Test
    fun `exact species name wins`() {
        assertEquals(
            big,
            SpeciesResolver.resolve(ScanResult(speciesName = "Bigmon"), all),
        )
    }

    @Test
    fun `single family member resolves without cp`() {
        assertEquals(
            lone,
            SpeciesResolver.resolve(ScanResult(candyFamily = "Lonely"), all),
        )
    }

    @Test
    fun `cp identifies the evolution within a family`() {
        val cp = LevelCalculator.cpAt(30.0, big.baseAtk, big.baseDef, big.baseSta, 10, 10, 10)
        val result = SpeciesResolver.resolve(
            ScanResult(cp = cp, candyFamily = "Smallmon"),
            all,
        )
        assertEquals(big, result)
    }

    @Test
    fun `family without cp stays unresolved when ambiguous`() {
        assertNull(SpeciesResolver.resolve(ScanResult(candyFamily = "Smallmon"), all))
    }

    @Test
    fun `unknown family resolves to nothing`() {
        assertNull(SpeciesResolver.resolve(ScanResult(candyFamily = "Mystery"), all))
    }
}
