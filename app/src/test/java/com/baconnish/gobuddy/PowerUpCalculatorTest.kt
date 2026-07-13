package com.baconnish.gobuddy

import com.baconnish.gobuddy.data.PokemonForm
import com.baconnish.gobuddy.domain.Cost
import com.baconnish.gobuddy.domain.PowerUpCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerUpCalculatorTest {

    @Test
    fun `level validation accepts half steps between 1 and 51`() {
        assertTrue(PowerUpCalculator.isValidLevel(1.0))
        assertTrue(PowerUpCalculator.isValidLevel(25.5))
        assertTrue(PowerUpCalculator.isValidLevel(50.0))
        assertTrue(PowerUpCalculator.isValidLevel(51.0))
        assertFalse(PowerUpCalculator.isValidLevel(0.5))
        assertFalse(PowerUpCalculator.isValidLevel(25.25))
        assertFalse(PowerUpCalculator.isValidLevel(51.5))
    }

    @Test
    fun `single power up matches game master values`() {
        assertEquals(Cost(stardust = 2500, candy = 2), PowerUpCalculator.singlePowerUpCost(20.0))
        assertEquals(Cost(stardust = 4000, candy = 3), PowerUpCalculator.singlePowerUpCost(25.5))
        assertEquals(Cost(stardust = 10000, candyXl = 10), PowerUpCalculator.singlePowerUpCost(40.0))
        assertEquals(Cost(stardust = 15000, candyXl = 20), PowerUpCalculator.singlePowerUpCost(49.5))
    }

    @Test
    fun `full climb from 1 to 40 costs 270k dust and 304 candy`() {
        assertEquals(
            Cost(stardust = 270_000, candy = 304),
            PowerUpCalculator.costBetween(1.0, 40.0),
        )
    }

    @Test
    fun `XL climb from 40 to 50 costs 250k dust and 296 XL`() {
        assertEquals(
            Cost(stardust = 250_000, candyXl = 296),
            PowerUpCalculator.costBetween(40.0, 50.0),
        )
    }

    @Test
    fun `full climb 1 to 50 is the sum of both phases`() {
        assertEquals(
            Cost(stardust = 520_000, candy = 304, candyXl = 296),
            PowerUpCalculator.costBetween(1.0, 50.0),
        )
    }

    @Test
    fun `same from and to level costs nothing`() {
        assertEquals(Cost(), PowerUpCalculator.costBetween(30.0, 30.0))
    }

    @Test
    fun `best buddy target above 50 is capped - the boost is free`() {
        assertEquals(
            PowerUpCalculator.costBetween(49.0, 50.0),
            PowerUpCalculator.costBetween(49.0, 51.0),
        )
    }

    @Test
    fun `shadow pokemon pay 20 percent more, candy rounds up`() {
        assertEquals(
            Cost(stardust = 240, candy = 2),
            PowerUpCalculator.singlePowerUpCost(1.0, form = PokemonForm.SHADOW),
        )
    }

    @Test
    fun `purified pokemon pay 10 percent less, candy rounds up`() {
        assertEquals(
            Cost(stardust = 180, candy = 1),
            PowerUpCalculator.singlePowerUpCost(1.0, form = PokemonForm.PURIFIED),
        )
    }

    @Test
    fun `lucky pokemon pay half stardust`() {
        assertEquals(
            Cost(stardust = 7500, candyXl = 20),
            PowerUpCalculator.singlePowerUpCost(49.5, isLucky = true),
        )
    }

    @Test
    fun `max pokemon level is trainer level plus 10 capped at 50`() {
        assertEquals(40.0, PowerUpCalculator.maxPokemonLevel(30), 0.0)
        assertEquals(45.0, PowerUpCalculator.maxPokemonLevel(35), 0.0)
        assertEquals(50.0, PowerUpCalculator.maxPokemonLevel(40), 0.0)
        assertEquals(50.0, PowerUpCalculator.maxPokemonLevel(50), 0.0)
    }

    @Test
    fun `xl candy unlocks at trainer level 31`() {
        assertFalse(PowerUpCalculator.canUseXlCandy(30))
        assertTrue(PowerUpCalculator.canUseXlCandy(31))
    }
}
