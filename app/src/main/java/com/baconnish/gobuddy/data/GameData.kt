package com.baconnish.gobuddy.data

object GameData {

    const val DATA_UPDATED = "2026-07-12"

    const val UPGRADES_PER_LEVEL = 2
    const val ALLOWED_LEVELS_ABOVE_PLAYER = 10
    const val MAX_POKEMON_LEVEL = 50.0
    const val BEST_BUDDY_BONUS_LEVELS = 1.0
    const val XL_CANDY_FROM_POKEMON_LEVEL = 40.0
    const val XL_CANDY_MIN_TRAINER_LEVEL = 31

    val STARDUST_COST = intArrayOf(
        200, 200, 400, 400, 600, 600, 800, 800, 1000, 1000,
        1300, 1300, 1600, 1600, 1900, 1900, 2200, 2200, 2500, 2500,
        3000, 3000, 3500, 3500, 4000, 4000, 4500, 4500, 5000, 5000,
        6000, 6000, 7000, 7000, 8000, 8000, 9000, 9000, 10000, 10000,
        11000, 11000, 12000, 12000, 13000, 13000, 14000, 14000, 15000,
    )

    val CANDY_COST = intArrayOf(
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        3, 3, 3, 3, 3, 4, 4, 4, 4, 4,
        6, 6, 8, 8, 10, 10, 12, 12, 15,
    )

    val XL_CANDY_COST = intArrayOf(10, 10, 12, 12, 15, 15, 17, 17, 20, 20)

    const val SHADOW_STARDUST_MULTIPLIER = 1.2
    const val SHADOW_CANDY_MULTIPLIER = 1.2
    const val PURIFIED_STARDUST_MULTIPLIER = 0.9
    const val PURIFIED_CANDY_MULTIPLIER = 0.9
    const val LUCKY_STARDUST_MULTIPLIER = 0.5

    const val EXCITED_DISTANCE_MULTIPLIER = 0.5
    const val KM_PER_WALK_HEART = 2.0

    val BUDDY_DISTANCE_TIERS = doubleArrayOf(1.0, 3.0, 5.0, 20.0)

    val CP_MULTIPLIER = doubleArrayOf(
        0.09400000, 0.13513743, 0.16639787, 0.19265091, 0.21573247,
        0.23657266, 0.25572005, 0.27353038, 0.29024988, 0.30605738,
        0.32108760, 0.33544503, 0.34921268, 0.36245776, 0.37523560,
        0.38759242, 0.39956728, 0.41119354, 0.42250000, 0.43292641,
        0.44310755, 0.45305996, 0.46279840, 0.47233608, 0.48168495,
        0.49085581, 0.49985844, 0.50870176, 0.51739395, 0.52594250,
        0.53435430, 0.54263575, 0.55079270, 0.55883059, 0.56675450,
        0.57456913, 0.58227890, 0.58988790, 0.59740000, 0.60482366,
        0.61215730, 0.61940411, 0.62656710, 0.63364917, 0.64065295,
        0.64758096, 0.65443563, 0.66121926, 0.66793400, 0.67458189,
        0.68116490, 0.68768489, 0.69414365, 0.70054289, 0.70688420,
        0.71316910, 0.71939910, 0.72557562, 0.73170000, 0.73474102,
        0.73776950, 0.74078558, 0.74378943, 0.74678120, 0.74976104,
        0.75272910, 0.75568550, 0.75863036, 0.76156384, 0.76448607,
        0.76739717, 0.77029727, 0.77318650, 0.77606494, 0.77893275,
        0.78179008, 0.78463700, 0.78747359, 0.79030000, 0.79280394,
        0.79530000, 0.79780392, 0.80030000, 0.80280389, 0.80530000,
        0.80780387, 0.81030000, 0.81280384, 0.81530000, 0.81780382,
        0.82030000, 0.82280380, 0.82530000, 0.82780378, 0.83030000,
        0.83280375, 0.83530000, 0.83780373, 0.84030000, 0.84280371,
        0.84530000,
    )

    fun cpMultiplier(level: Double): Double = CP_MULTIPLIER[((level - 1) * 2).toInt()]
}

enum class BuddyLevel(val requiredHearts: Int, val label: String) {
    BUDDY(0, "Buddy"),
    GOOD(1, "Good Buddy"),
    GREAT(70, "Great Buddy"),
    ULTRA(150, "Ultra Buddy"),
    BEST(300, "Best Buddy");

    companion object {
        fun fromHearts(hearts: Int): BuddyLevel =
            entries.last { hearts >= it.requiredHearts }
    }
}

enum class PokemonForm(val label: String) {
    NORMAL("Normal"),
    SHADOW("Shadow"),
    PURIFIED("Purified"),
}

enum class HyperTrainingStat(val label: String) {
    ATK("Attack"),
    DEF("Defense"),
    STA("HP"),
    ALL("All three (Gold Cap)"),
}
