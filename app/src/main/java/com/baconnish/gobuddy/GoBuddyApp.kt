package com.baconnish.gobuddy

import android.app.Application
import com.baconnish.gobuddy.data.ScreenshotScanner
import com.baconnish.gobuddy.data.SettingsRepository
import com.baconnish.gobuddy.data.SpeciesRepository
import com.baconnish.gobuddy.data.db.GoBuddyDatabase

class AppContainer(app: Application) {
    val database by lazy { GoBuddyDatabase.get(app) }
    val pokemonDao by lazy { database.pokemonDao() }
    val speciesRepository by lazy { SpeciesRepository(app) }
    val settingsRepository by lazy { SettingsRepository(app) }
    val screenshotScanner by lazy { ScreenshotScanner(app, speciesRepository) }
}

class GoBuddyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
