package com.baconnish.gobuddy.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.baconnish.gobuddy.data.PokemonForm

class Converters {
    @TypeConverter
    fun formToString(form: PokemonForm): String = form.name

    @TypeConverter
    fun stringToForm(value: String): PokemonForm =
        PokemonForm.entries.firstOrNull { it.name == value } ?: PokemonForm.NORMAL
}

@Database(entities = [TrackedPokemon::class], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class GoBuddyDatabase : RoomDatabase() {

    abstract fun pokemonDao(): PokemonDao

    companion object {
        @Volatile
        private var instance: GoBuddyDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracked_pokemon ADD COLUMN ivAtk INTEGER")
                db.execSQL("ALTER TABLE tracked_pokemon ADD COLUMN ivDef INTEGER")
                db.execSQL("ALTER TABLE tracked_pokemon ADD COLUMN ivSta INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracked_pokemon ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE tracked_pokemon SET priority = createdAt")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracked_pokemon ADD COLUMN hyperTrainingStat TEXT")
            }
        }

        fun get(context: Context): GoBuddyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GoBuddyDatabase::class.java,
                    "gobuddy.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}
