package com.baconnish.gobuddy

import com.baconnish.gobuddy.domain.ScreenshotParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotParserTest {

    private val species = listOf("Charmander", "Charizard", "Mewtwo", "Mew", "Snorlax", "Pikachu", "Regigigas")

    @Test
    fun `parses a typical pokemon info screen`() {
        val lines = listOf(
            "CP 892",
            "Charmander",
            "129 / 129 HP",
            "153,000",
            "STARDUST",
            "25",
            "CHARMANDER CANDY",
            "POWER UP",
            "1,000",
            "1",
        )
        val result = ScreenshotParser.parse(lines, species)
        assertEquals(892, result.cp)
        assertEquals(129, result.hpMax)
        assertEquals(25, result.candy)
        assertEquals(1000, result.stardust)
        assertEquals("Charmander", result.speciesName)
        assertEquals("Charmander", result.candyFamily)
    }

    @Test
    fun `parses the real regigigas info screen layout`() {
        val lines = listOf(
            "CP3013",
            "Regigigas",
            "154 / 154 HP",
            "Lego",
            "684.4kg",
            "WEIGHT",
            "NORMAL",
            "4.49m",
            "HEIGHT",
            "2,558,817",
            "STARDUST",
            "21",
            "REGIGIGAS CANDY",
            "POWER UP",
            "4,000",
            "3",
            "GYMS & RAIDS",
            "Zen Headbutt",
            "NEW ATTACK",
            "100,000",
            "100",
        )
        val result = ScreenshotParser.parse(lines, species)
        assertEquals(3013, result.cp)
        assertEquals(154, result.hpMax)
        assertEquals(21, result.candy)
        assertEquals(4000, result.stardust)
        assertEquals("Regigigas", result.speciesName)
        assertEquals("Regigigas", result.candyFamily)
    }

    @Test
    fun `total stardust owned is not mistaken for the power up cost`() {
        val lines = listOf("CP 1000", "2,558,817", "STARDUST", "Pikachu")
        val result = ScreenshotParser.parse(lines, species)
        assertNull(result.stardust)
    }

    @Test
    fun `parses cp glued to the label and numbers with spaces`() {
        val lines = listOf("CP2 831", "Snorlax", "175/175 HP")
        val result = ScreenshotParser.parse(lines, species)
        assertEquals(2831, result.cp)
        assertEquals(175, result.hpMax)
        assertEquals("Snorlax", result.speciesName)
    }

    @Test
    fun `nicknamed pokemon falls back to the candy family`() {
        val lines = listOf(
            "CP 1420",
            "Zippy",
            "98 / 98 HP",
            "112",
            "CHARMANDER CANDY",
            "36",
            "CHARMANDER CANDY XL",
        )
        val result = ScreenshotParser.parse(lines, species)
        assertNull(result.speciesName)
        assertEquals("Charmander", result.candyFamily)
        assertEquals(112, result.candy)
        assertEquals(36, result.candyXl)
    }

    @Test
    fun `does not mistake the hp fraction for cp`() {
        val lines = listOf("175/175 HP", "CP 2831")
        val result = ScreenshotParser.parse(lines, species)
        assertEquals(2831, result.cp)
        assertEquals(175, result.hpMax)
    }

    @Test
    fun `prefers the exact species line over substrings`() {
        val lines = listOf("CP 4178", "Mewtwo", "180/180 HP")
        val result = ScreenshotParser.parse(lines, species)
        assertEquals("Mewtwo", result.speciesName)
    }

    @Test
    fun `cp split across lines by the stylized font is still found`() {
        val result = ScreenshotParser.parse(listOf("CP", "3013", "Regigigas", "154/154 HP"), species)
        assertEquals(3013, result.cp)
    }

    @Test
    fun `cp with spaced letters is still found`() {
        val result = ScreenshotParser.parse(listOf("C P 2831", "Snorlax"), species)
        assertEquals(2831, result.cp)
    }

    @Test
    fun `occluded candy label fragment is ignored`() {
        val lines = listOf(
            "CP3013",
            "Regigigas",
            "154 / 154 HP",
            "1",
            "S CANDY",
        )
        val result = ScreenshotParser.parse(lines, species)
        assertNull(result.candy)
        assertNull(result.candyFamily)
        assertEquals(3013, result.cp)
    }

    @Test
    fun `garbage input yields an empty result`() {
        val result = ScreenshotParser.parse(listOf("Settings", "Music", "Vibration"), species)
        assertTrue(result.isEmpty)
    }
}
