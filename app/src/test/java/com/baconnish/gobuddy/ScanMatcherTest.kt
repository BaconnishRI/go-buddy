package com.baconnish.gobuddy

import com.baconnish.gobuddy.data.Species
import com.baconnish.gobuddy.data.db.TrackedPokemon
import com.baconnish.gobuddy.domain.LevelCalculator
import com.baconnish.gobuddy.domain.ScanMatcher
import com.baconnish.gobuddy.domain.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanMatcherTest {

    private val mewtwo = Species(150, "Mewtwo", 20.0, 300, 182, 214, "Mewtwo")
    private val charizard = Species(6, "Charizard", 3.0, 223, 173, 186, "Charmander")
    private val speciesOf: (String) -> Species? = { name ->
        listOf(mewtwo, charizard).firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    @Test
    fun `nickname wins over everything`() {
        val zippy = TrackedPokemon(id = 1, nickname = "Zippy", speciesName = "Charizard")
        val other = TrackedPokemon(id = 2, speciesName = "Charizard")
        val result = ScanMatcher.match(
            listOf("Zippy", "CP 1420"),
            ScanResult(cp = 1420, candyFamily = "Charmander"),
            listOf(other, zippy),
            speciesOf,
        )
        assertEquals(ScanMatcher.Result.Matched(zippy), result)
    }

    @Test
    fun `single species match`() {
        val tracked = TrackedPokemon(id = 1, speciesName = "Mewtwo")
        val result = ScanMatcher.match(
            listOf("Mewtwo"),
            ScanResult(cp = 3000, speciesName = "Mewtwo"),
            listOf(tracked, TrackedPokemon(id = 2, speciesName = "Charizard")),
            speciesOf,
        )
        assertEquals(ScanMatcher.Result.Matched(tracked), result)
    }

    @Test
    fun `candy family matches an evolved nicknamed pokemon`() {
        val zard = TrackedPokemon(id = 1, nickname = "Flame", speciesName = "Charizard")
        val result = ScanMatcher.match(
            listOf("SomethingUnreadable"),
            ScanResult(cp = 2100, candyFamily = "Charmander"),
            listOf(zard, TrackedPokemon(id = 2, speciesName = "Mewtwo")),
            speciesOf,
        )
        assertEquals(ScanMatcher.Result.Matched(zard), result)
    }

    @Test
    fun `stored ivs disambiguate two of the same species`() {
        val hundo = TrackedPokemon(
            id = 1, speciesName = "Mewtwo", ivAtk = 15, ivDef = 15, ivSta = 15,
        )
        val zero = TrackedPokemon(
            id = 2, speciesName = "Mewtwo", ivAtk = 0, ivDef = 0, ivSta = 0,
        )
        assertTrue(
            LevelCalculator.levelsForCp(4178, 300, 182, 214, 0, 0, 0).isEmpty(),
        )
        val result = ScanMatcher.match(
            listOf("Mewtwo"),
            ScanResult(cp = 4178, speciesName = "Mewtwo"),
            listOf(zero, hundo),
            speciesOf,
        )
        assertEquals(ScanMatcher.Result.Matched(hundo), result)
    }

    @Test
    fun `same species without ivs is ambiguous`() {
        val a = TrackedPokemon(id = 1, speciesName = "Mewtwo")
        val b = TrackedPokemon(id = 2, speciesName = "Mewtwo")
        val result = ScanMatcher.match(
            listOf("Mewtwo"),
            ScanResult(cp = 3000, speciesName = "Mewtwo"),
            listOf(a, b),
            speciesOf,
        )
        assertTrue(result is ScanMatcher.Result.Ambiguous)
    }

    @Test
    fun `nothing tracked matches`() {
        val result = ScanMatcher.match(
            listOf("Pikachu"),
            ScanResult(cp = 500, speciesName = "Pikachu"),
            listOf(TrackedPokemon(id = 1, speciesName = "Mewtwo")),
            speciesOf,
        )
        assertEquals(ScanMatcher.Result.NoMatch, result)
    }
}
