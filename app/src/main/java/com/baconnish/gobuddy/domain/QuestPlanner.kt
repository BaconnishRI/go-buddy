package com.baconnish.gobuddy.domain

import com.baconnish.gobuddy.data.TrainerSettings
import com.baconnish.gobuddy.data.db.TrackedPokemon

object QuestPlanner {

    data class Entry(
        val pokemon: TrackedPokemon,
        val plan: GoalPlan,
        val days: Int,
        val cumulativeDays: Int,
        val bestBuddyDay: Int?,
    )

    data class Quest(
        val entries: List<Entry>,
        val totalStardust: Int,
        val totalCandyShort: Int,
        val totalCandyXlShort: Int,
        val totalWalkKm: Double,
        val totalHeartsToBest: Int,
        val totalDays: Int,
    )

    fun plan(pokemon: List<TrackedPokemon>, settings: TrainerSettings): Quest {
        var cumulative = 0
        val entries = pokemon.map { p ->
            val goalPlan = GoalPlanner.plan(
                currentLevel = p.currentLevel,
                targetLevel = p.targetLevel,
                form = p.form,
                isLucky = p.isLucky,
                candyOwned = p.candyOwned,
                candyXlOwned = p.candyXlOwned,
                hearts = p.hearts,
                kmPerCandy = p.kmPerCandy,
                trainerLevel = settings.trainerLevel,
                heartsPerDay = settings.heartsPerDay,
                kmPerDay = settings.kmPerDay,
            )
            val days = maxOf(
                goalPlan.walkDaysNormal,
                if (p.wantBestBuddy) goalPlan.daysToBestBuddy else 0,
            )
            val start = cumulative
            cumulative += days
            val bestBuddyDay = if (
                p.wantBestBuddy && goalPlan.heartsToBest > 0 && goalPlan.daysToBestBuddy > 0
            ) {
                start + goalPlan.daysToBestBuddy
            } else {
                null
            }
            Entry(p, goalPlan, days, cumulative, bestBuddyDay)
        }
        return Quest(
            entries = entries,
            totalStardust = entries.sumOf { it.plan.cost.stardust },
            totalCandyShort = entries.sumOf { it.plan.candyShort },
            totalCandyXlShort = entries.sumOf { it.plan.candyXlShort },
            totalWalkKm = entries.sumOf { it.plan.walkKmNormal },
            totalHeartsToBest = entries.sumOf { if (it.pokemon.wantBestBuddy) it.plan.heartsToBest else 0 },
            totalDays = cumulative,
        )
    }
}
