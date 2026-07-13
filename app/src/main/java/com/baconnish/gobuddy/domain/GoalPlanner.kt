package com.baconnish.gobuddy.domain

import com.baconnish.gobuddy.data.BuddyLevel
import com.baconnish.gobuddy.data.GameData
import com.baconnish.gobuddy.data.PokemonForm
import kotlin.math.ceil
import kotlin.math.max

data class GoalPlan(
    val cost: Cost,
    val candyShort: Int,
    val candyXlShort: Int,
    val walkKmNormal: Double,
    val walkKmExcited: Double,
    val walkDaysNormal: Int,
    val walkDaysExcited: Int,
    val maxLevelAllowed: Double,
    val targetReachableNow: Boolean,
    val trainerLevelNeeded: Int?,
    val needsXlCandy: Boolean,
    val xlCandyUnlocked: Boolean,
    val buddyLevel: BuddyLevel,
    val heartsToBest: Int,
    val daysToBestBuddy: Int,
)

object GoalPlanner {

    fun plan(
        currentLevel: Double,
        targetLevel: Double,
        form: PokemonForm,
        isLucky: Boolean,
        candyOwned: Int,
        candyXlOwned: Int,
        hearts: Int,
        kmPerCandy: Double,
        trainerLevel: Int,
        heartsPerDay: Int,
        kmPerDay: Double,
    ): GoalPlan {
        val cost = PowerUpCalculator.costBetween(currentLevel, targetLevel, form, isLucky)
        val candyShort = max(0, cost.candy - candyOwned)
        val candyXlShort = max(0, cost.candyXl - candyXlOwned)

        val walkKmNormal = BuddyCalculator.distanceForCandy(candyShort, kmPerCandy, excited = false)
        val walkKmExcited = BuddyCalculator.distanceForCandy(candyShort, kmPerCandy, excited = true)
        val walkDaysNormal = if (kmPerDay > 0) ceil(walkKmNormal / kmPerDay).toInt() else 0
        val walkDaysExcited = if (kmPerDay > 0) ceil(walkKmExcited / kmPerDay).toInt() else 0

        val maxLevelAllowed = PowerUpCalculator.maxPokemonLevel(trainerLevel)
        val targetPowerUpLevel = minOf(targetLevel, GameData.MAX_POKEMON_LEVEL)
        val reachable = targetPowerUpLevel <= maxLevelAllowed
        val trainerLevelNeeded = if (reachable) {
            null
        } else {
            ceil(targetPowerUpLevel - GameData.ALLOWED_LEVELS_ABOVE_PLAYER).toInt()
        }

        val needsXl = cost.candyXl > 0
        val heartsToBest = BuddyCalculator.heartsRemaining(hearts)

        return GoalPlan(
            cost = cost,
            candyShort = candyShort,
            candyXlShort = candyXlShort,
            walkKmNormal = walkKmNormal,
            walkKmExcited = walkKmExcited,
            walkDaysNormal = walkDaysNormal,
            walkDaysExcited = walkDaysExcited,
            maxLevelAllowed = maxLevelAllowed,
            targetReachableNow = reachable,
            trainerLevelNeeded = trainerLevelNeeded,
            needsXlCandy = needsXl,
            xlCandyUnlocked = PowerUpCalculator.canUseXlCandy(trainerLevel),
            buddyLevel = BuddyLevel.fromHearts(hearts),
            heartsToBest = heartsToBest,
            daysToBestBuddy = if (heartsPerDay > 0) BuddyCalculator.daysForHearts(heartsToBest, heartsPerDay) else 0,
        )
    }
}
