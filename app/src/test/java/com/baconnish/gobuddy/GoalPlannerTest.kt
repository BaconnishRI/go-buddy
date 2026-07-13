package com.baconnish.gobuddy

import com.baconnish.gobuddy.data.PokemonForm
import com.baconnish.gobuddy.domain.GoalPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalPlannerTest {

    private fun plan(
        currentLevel: Double = 20.0,
        targetLevel: Double = 50.0,
        trainerLevel: Int = 40,
        candyOwned: Int = 0,
        candyXlOwned: Int = 0,
        hearts: Int = 0,
        kmPerCandy: Double = 3.0,
    ) = GoalPlanner.plan(
        currentLevel = currentLevel,
        targetLevel = targetLevel,
        form = PokemonForm.NORMAL,
        isLucky = false,
        candyOwned = candyOwned,
        candyXlOwned = candyXlOwned,
        hearts = hearts,
        kmPerCandy = kmPerCandy,
        trainerLevel = trainerLevel,
        heartsPerDay = 12,
        kmPerDay = 5.0,
    )

    @Test
    fun `candy shortfall subtracts owned candy`() {
        val p = plan(currentLevel = 20.0, targetLevel = 25.0, candyOwned = 10)
        assertEquals(2 * 2 + 3 * 8, p.cost.candy)
        assertEquals(p.cost.candy - 10, p.candyShort)
    }

    @Test
    fun `walking plan uses buddy distance and excited halves it`() {
        val p = plan(currentLevel = 20.0, targetLevel = 25.0, candyOwned = 0, kmPerCandy = 5.0)
        assertEquals(p.cost.candy * 5.0, p.walkKmNormal, 0.001)
        assertEquals(p.walkKmNormal / 2, p.walkKmExcited, 0.001)
    }

    @Test
    fun `target above trainer cap is flagged with the trainer level needed`() {
        val p = plan(targetLevel = 50.0, trainerLevel = 30)
        assertFalse(p.targetReachableNow)
        assertEquals(40.0, p.maxLevelAllowed, 0.0)
        assertEquals(40, p.trainerLevelNeeded)
        assertTrue(p.needsXlCandy)
        assertFalse(p.xlCandyUnlocked)
    }

    @Test
    fun `reachable target has no warnings`() {
        val p = plan(targetLevel = 50.0, trainerLevel = 40)
        assertTrue(p.targetReachableNow)
        assertNull(p.trainerLevelNeeded)
        assertTrue(p.xlCandyUnlocked)
    }

    @Test
    fun `best buddy plan counts hearts and days`() {
        val p = plan(hearts = 120)
        assertEquals(180, p.heartsToBest)
        assertEquals(15, p.daysToBestBuddy)
    }
}
