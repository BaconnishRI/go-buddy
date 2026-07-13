package com.baconnish.gobuddy.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PokemonDao {
    @Query("SELECT * FROM tracked_pokemon ORDER BY priority ASC, createdAt DESC")
    fun observeAll(): Flow<List<TrackedPokemon>>

    @Query("SELECT * FROM tracked_pokemon WHERE id = :id")
    fun observeById(id: Long): Flow<TrackedPokemon?>

    @Query("SELECT * FROM tracked_pokemon WHERE id = :id")
    suspend fun getById(id: Long): TrackedPokemon?

    @Query("SELECT * FROM tracked_pokemon ORDER BY priority ASC, createdAt DESC")
    suspend fun getAll(): List<TrackedPokemon>

    @Upsert
    suspend fun upsert(pokemon: TrackedPokemon): Long

    @Delete
    suspend fun delete(pokemon: TrackedPokemon)
}
