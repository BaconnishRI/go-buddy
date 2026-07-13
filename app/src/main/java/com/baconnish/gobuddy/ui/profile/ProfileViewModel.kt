package com.baconnish.gobuddy.ui.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.baconnish.gobuddy.GoBuddyApp
import com.baconnish.gobuddy.data.BackupCodec
import com.baconnish.gobuddy.data.SettingsRepository
import com.baconnish.gobuddy.data.TrainerSettings
import com.baconnish.gobuddy.data.db.PokemonDao
import com.baconnish.gobuddy.domain.QuestPlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileUiState(
    val settings: TrainerSettings = TrainerSettings(),
    val quest: QuestPlanner.Quest? = null,
)

class ProfileViewModel(
    private val app: Application,
    private val dao: PokemonDao,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> =
        combine(dao.observeAll(), settingsRepository.settings) { pokemon, settings ->
            ProfileUiState(
                settings = settings,
                quest = QuestPlanner.plan(pokemon, settings),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun saveSettings(settings: TrainerSettings) = settingsRepository.update(settings)

    fun exportBackup(uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val message = try {
                val pokemon = dao.getAll()
                val json = BackupCodec.encode(settingsRepository.settings.value, pokemon)
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(json.toByteArray())
                    } ?: error("open failed")
                }
                "Backed up ${pokemon.size} Pokémon."
            } catch (_: Exception) {
                "Backup failed — try a different location."
            }
            onDone(message)
        }
    }

    fun importBackup(uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val message = try {
                val json = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().decodeToString()
                    } ?: error("open failed")
                }
                val backup = BackupCodec.decode(json)
                settingsRepository.update(backup.settings)
                backup.pokemon.forEach { dao.upsert(it) }
                "Restored ${backup.pokemon.size} Pokémon and your trainer settings."
            } catch (_: Exception) {
                "Couldn't read that backup file."
            }
            onDone(message)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GoBuddyApp
                ProfileViewModel(
                    app,
                    app.container.pokemonDao,
                    app.container.settingsRepository,
                )
            }
        }
    }
}
