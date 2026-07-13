package com.baconnish.gobuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.baconnish.gobuddy.data.PokemonForm

@Entity(tableName = "tracked_pokemon")
data class TrackedPokemon(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String = "",
    val speciesName: String,
    val speciesDex: Int? = null,
    val kmPerCandy: Double = 3.0,
    val currentLevel: Double = 20.0,
    val targetLevel: Double = 50.0,
    val form: PokemonForm = PokemonForm.NORMAL,
    val isLucky: Boolean = false,
    val isCurrentBuddy: Boolean = false,
    val hearts: Int = 0,
    val wantBestBuddy: Boolean = true,
    val candyOwned: Int = 0,
    val candyXlOwned: Int = 0,
    val ivAtk: Int? = null,
    val ivDef: Int? = null,
    val ivSta: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val priority: Long = System.currentTimeMillis(),
    val hyperTrainingStat: String? = null,
) {
    val displayName: String
        get() = nickname.ifBlank { speciesName }
}
