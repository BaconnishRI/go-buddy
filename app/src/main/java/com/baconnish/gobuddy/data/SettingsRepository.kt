package com.baconnish.gobuddy.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TrainerSettings(
    val trainerLevel: Int = 40,
    val heartsPerDay: Int = 12,
    val kmPerDay: Double = 5.0,
)

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("gobuddy_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<TrainerSettings> = _settings

    private val _onboarded = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDED, false))
    val onboarded: StateFlow<Boolean> = _onboarded

    fun markOnboarded() {
        prefs.edit { putBoolean(KEY_ONBOARDED, true) }
        _onboarded.value = true
    }

    private fun load() = TrainerSettings(
        trainerLevel = prefs.getInt(KEY_TRAINER_LEVEL, 40),
        heartsPerDay = prefs.getInt(KEY_HEARTS_PER_DAY, 12),
        kmPerDay = prefs.getFloat(KEY_KM_PER_DAY, 5.0f).toDouble(),
    )

    fun update(settings: TrainerSettings) {
        prefs.edit {
            putInt(KEY_TRAINER_LEVEL, settings.trainerLevel)
            putInt(KEY_HEARTS_PER_DAY, settings.heartsPerDay)
            putFloat(KEY_KM_PER_DAY, settings.kmPerDay.toFloat())
        }
        _settings.value = settings
    }

    private companion object {
        const val KEY_TRAINER_LEVEL = "trainer_level"
        const val KEY_HEARTS_PER_DAY = "hearts_per_day"
        const val KEY_KM_PER_DAY = "km_per_day"
        const val KEY_ONBOARDED = "onboarded"
    }
}
