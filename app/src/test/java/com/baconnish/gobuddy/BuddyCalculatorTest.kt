package com.baconnish.gobuddy

import com.baconnish.gobuddy.data.BuddyLevel
import com.baconnish.gobuddy.domain.BuddyCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuddyCalculatorTest {

    @Test
    fun `walking distance scales with candy and species distance`() {
        assertEquals(30.0, BuddyCalculator.distanceForCandy(10, 3.0), 0.0)
        assertEquals(200.0, BuddyCalculator.distanceForCandy(10, 20.0), 0.0)
    }

    @Test
    fun `excited mood halves the distance`() {
        assertEquals(15.0, BuddyCalculator.distanceForCandy(10, 3.0, excited = true), 0.0)
        assertEquals(2.5, BuddyCalculator.distanceForCandy(1, 5.0, excited = true), 0.0)
    }

    @Test
    fun `candy earned from km walked`() {
        assertEquals(10, BuddyCalculator.candyFromDistance(30.0, 3.0))
        assertEquals(20, BuddyCalculator.candyFromDistance(30.0, 3.0, excited = true))
        assertEquals(9, BuddyCalculator.candyFromDistance(29.9, 3.0))
    }

    @Test
    fun `buddy tiers match heart thresholds`() {
        assertEquals(BuddyLevel.BUDDY, BuddyLevel.fromHearts(0))
        assertEquals(BuddyLevel.GOOD, BuddyLevel.fromHearts(1))
        assertEquals(BuddyLevel.GOOD, BuddyLevel.fromHearts(69))
        assertEquals(BuddyLevel.GREAT, BuddyLevel.fromHearts(70))
        assertEquals(BuddyLevel.ULTRA, BuddyLevel.fromHearts(150))
        assertEquals(BuddyLevel.ULTRA, BuddyLevel.fromHearts(299))
        assertEquals(BuddyLevel.BEST, BuddyLevel.fromHearts(300))
        assertEquals(BuddyLevel.BEST, BuddyLevel.fromHearts(999))
    }

    @Test
    fun `next level and hearts remaining`() {
        assertEquals(BuddyLevel.GOOD, BuddyCalculator.nextLevel(0))
        assertEquals(BuddyLevel.GREAT, BuddyCalculator.nextLevel(30))
        assertEquals(BuddyLevel.BEST, BuddyCalculator.nextLevel(299))
        assertNull(BuddyCalculator.nextLevel(300))

        assertEquals(300, BuddyCalculator.heartsRemaining(0))
        assertEquals(180, BuddyCalculator.heartsRemaining(120))
        assertEquals(0, BuddyCalculator.heartsRemaining(400))
    }

    @Test
    fun `days estimate rounds up`() {
        assertEquals(15, BuddyCalculator.daysForHearts(180, 12))
        assertEquals(1, BuddyCalculator.daysForHearts(1, 12))
        assertEquals(0, BuddyCalculator.daysForHearts(0, 12))
    }
}
