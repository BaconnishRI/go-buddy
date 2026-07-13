package com.baconnish.gobuddy.domain

import com.baconnish.gobuddy.data.GameData
import com.baconnish.gobuddy.data.PokemonForm
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

data class Cost(val stardust: Int = 0, val candy: Int = 0, val candyXl: Int = 0) {
    operator fun plus(other: Cost) = Cost(
        stardust + other.stardust,
        candy + other.candy,
        candyXl + other.candyXl,
    )
}

object PowerUpCalculator {

    fun isValidLevel(level: Double): Boolean =
        level in 1.0..(GameData.MAX_POKEMON_LEVEL + GameData.BEST_BUDDY_BONUS_LEVELS) &&
            (level * 2) == (level * 2).toLong().toDouble()

    fun maxPokemonLevel(trainerLevel: Int): Double =
        min((trainerLevel + GameData.ALLOWED_LEVELS_ABOVE_PLAYER).toDouble(), GameData.MAX_POKEMON_LEVEL)

    fun canUseXlCandy(trainerLevel: Int): Boolean =
        trainerLevel >= GameData.XL_CANDY_MIN_TRAINER_LEVEL

    fun singlePowerUpCost(
        level: Double,
        form: PokemonForm = PokemonForm.NORMAL,
        isLucky: Boolean = false,
    ): Cost {
        require(isValidLevel(level) && level < GameData.MAX_POKEMON_LEVEL) {
            "No power-up exists at level $level"
        }
        val index = level.toInt() - 1
        var stardust = GameData.STARDUST_COST[index].toDouble()
        val usesXl = level >= GameData.XL_CANDY_FROM_POKEMON_LEVEL
        var candy = (if (usesXl) {
            GameData.XL_CANDY_COST[level.toInt() - GameData.XL_CANDY_FROM_POKEMON_LEVEL.toInt()]
        } else {
            GameData.CANDY_COST[index]
        }).toDouble()

        when (form) {
            PokemonForm.SHADOW -> {
                stardust *= GameData.SHADOW_STARDUST_MULTIPLIER
                candy *= GameData.SHADOW_CANDY_MULTIPLIER
            }
            PokemonForm.PURIFIED -> {
                stardust *= GameData.PURIFIED_STARDUST_MULTIPLIER
                candy *= GameData.PURIFIED_CANDY_MULTIPLIER
            }
            PokemonForm.NORMAL -> Unit
        }
        if (isLucky) stardust *= GameData.LUCKY_STARDUST_MULTIPLIER

        val candyWhole = ceil(candy - 1e-9).toInt()
        return if (usesXl) {
            Cost(stardust = stardust.roundToInt(), candyXl = candyWhole)
        } else {
            Cost(stardust = stardust.roundToInt(), candy = candyWhole)
        }
    }

    fun costBetween(
        fromLevel: Double,
        toLevel: Double,
        form: PokemonForm = PokemonForm.NORMAL,
        isLucky: Boolean = false,
    ): Cost {
        require(isValidLevel(fromLevel)) { "Invalid from level $fromLevel" }
        require(isValidLevel(toLevel)) { "Invalid to level $toLevel" }
        val cappedTarget = min(toLevel, GameData.MAX_POKEMON_LEVEL)
        var total = Cost()
        var level = fromLevel
        while (level < cappedTarget) {
            total += singlePowerUpCost(level, form, isLucky)
            level += 0.5
        }
        return total
    }
}
