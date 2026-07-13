package com.baconnish.gobuddy.domain

import com.baconnish.gobuddy.data.BuddyLevel
import com.baconnish.gobuddy.data.GameData
import kotlin.math.ceil
import kotlin.math.max

object BuddyCalculator {

    fun distanceForCandy(candyCount: Int, kmPerCandy: Double, excited: Boolean = false): Double {
        require(candyCount >= 0 && kmPerCandy > 0)
        val multiplier = if (excited) GameData.EXCITED_DISTANCE_MULTIPLIER else 1.0
        return candyCount * kmPerCandy * multiplier
    }

    fun candyFromDistance(km: Double, kmPerCandy: Double, excited: Boolean = false): Int {
        require(km >= 0 && kmPerCandy > 0)
        val effective = if (excited) kmPerCandy * GameData.EXCITED_DISTANCE_MULTIPLIER else kmPerCandy
        return (km / effective).toInt()
    }

    fun nextLevel(hearts: Int): BuddyLevel? =
        BuddyLevel.entries.firstOrNull { hearts < it.requiredHearts }

    fun heartsRemaining(hearts: Int, target: BuddyLevel = BuddyLevel.BEST): Int =
        max(0, target.requiredHearts - hearts)

    fun daysForHearts(heartsNeeded: Int, heartsPerDay: Int): Int {
        require(heartsPerDay > 0)
        return ceil(heartsNeeded.toDouble() / heartsPerDay).toInt()
    }
}
