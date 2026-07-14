package com.baconnish.gobuddy

import com.baconnish.gobuddy.data.TrainerSettings
import com.baconnish.gobuddy.data.db.TrackedPokemon
import com.baconnish.gobuddy.domain.QuestPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestPlannerTest {

    private val settings = TrainerSettings(trainerLevel = 40, heartsPerDay = 12, kmPerDay = 5.0)

    @Test
    fun `sequential quest takes the longer of walking and hearts per pokemon`() {
        val walker = TrackedPokemon(
            id = 1, speciesName = "A", currentLevel = 20.0, targetLevel = 30.0,
            kmPerCandy = 5.0, wantBestBuddy = false,
        )
        val cuddler = TrackedPokemon(
            id = 2, speciesName = "B", currentLevel = 20.0, targetLevel = 20.0,
            hearts = 0, wantBestBuddy = true,
        )
        val quest = QuestPlanner.plan(listOf(walker, cuddler), settings)

        assertEquals(2, quest.entries.size)
        assertEquals(quest.entries[0].plan.walkDaysNormal, quest.entries[0].days)
        assertEquals(25, quest.entries[1].days)
        assertEquals(
            quest.entries[0].days + quest.entries[1].days,
            quest.totalDays,
        )
        assertEquals(quest.entries[1].cumulativeDays, quest.totalDays)
        assertTrue(quest.totalStardust > 0)
        assertEquals(300, quest.totalHeartsToBest)
        assertNull(quest.entries[0].bestBuddyDay)
        assertEquals(
            quest.entries[0].days + quest.entries[1].plan.daysToBestBuddy,
            quest.entries[1].bestBuddyDay,
        )
    }

    @Test
    fun `finished pokemon contribute zero days`() {
        val done = TrackedPokemon(
            id = 1, speciesName = "A", currentLevel = 40.0, targetLevel = 40.0,
            hearts = 300, wantBestBuddy = true, candyOwned = 0,
        )
        val quest = QuestPlanner.plan(listOf(done), settings)
        assertEquals(0, quest.totalDays)
        assertEquals(0, quest.totalStardust)
        assertNull(quest.entries[0].bestBuddyDay)
    }
}
