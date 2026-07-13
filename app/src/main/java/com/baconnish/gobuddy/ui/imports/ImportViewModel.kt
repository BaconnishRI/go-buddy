package com.baconnish.gobuddy.ui.imports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.baconnish.gobuddy.GoBuddyApp
import com.baconnish.gobuddy.data.ScreenshotScanner
import com.baconnish.gobuddy.data.SpeciesRepository
import com.baconnish.gobuddy.data.db.PokemonDao
import com.baconnish.gobuddy.data.db.TrackedPokemon
import com.baconnish.gobuddy.domain.ScanApplier
import com.baconnish.gobuddy.domain.ScanMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ImportRow(
    val title: String,
    val detail: String,
    val updated: TrackedPokemon? = null,
    val selected: Boolean = false,
)

data class ImportUiState(
    val processing: Boolean = true,
    val rows: List<ImportRow> = emptyList(),
) {
    val selectedCount: Int get() = rows.count { it.selected }
}

class ImportViewModel(
    private val dao: PokemonDao,
    private val speciesRepository: SpeciesRepository,
    private val scanner: ScreenshotScanner,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState

    private var started = false

    fun process(uris: List<Uri>) {
        if (started) return
        started = true
        viewModelScope.launch {
            val tracked = dao.getAll()
            val rows = uris.mapIndexed { index, uri ->
                val label = "Screenshot ${index + 1}"
                try {
                    val capture = scanner.scan(uri)
                    val scan = capture.result
                    if (scan.isEmpty) {
                        return@mapIndexed ImportRow(label, "Couldn't read anything useful from this image.")
                    }
                    when (val match = ScanMatcher.match(capture.lines, scan, tracked, speciesRepository::byName)) {
                        is ScanMatcher.Result.NoMatch -> ImportRow(
                            label,
                            "No tracked Pokémon matches" +
                                (scan.speciesName?.let { " ($it)" } ?: "") + ".",
                        )
                        is ScanMatcher.Result.Ambiguous -> ImportRow(
                            label,
                            "Could be ${match.candidates.joinToString(" or ") { it.displayName }} — " +
                                "update it from its own screen instead.",
                        )
                        is ScanMatcher.Result.Matched -> {
                            val outcome = ScanApplier.apply(
                                match.pokemon, scan, speciesRepository.byName(match.pokemon.speciesName),
                            )
                            if (outcome.hasChanges) {
                                ImportRow(
                                    match.pokemon.displayName,
                                    (outcome.changes + outcome.notes).joinToString(", "),
                                    updated = outcome.updated,
                                    selected = true,
                                )
                            } else {
                                ImportRow(
                                    match.pokemon.displayName,
                                    (listOf("Already up to date") + outcome.notes).joinToString(", ") + ".",
                                )
                            }
                        }
                    }
                } catch (_: Exception) {
                    ImportRow(label, "Couldn't read this image.")
                }
            }
            _uiState.value = ImportUiState(processing = false, rows = rows)
        }
    }

    fun toggle(index: Int) {
        val rows = _uiState.value.rows.toMutableList()
        val row = rows.getOrNull(index) ?: return
        if (row.updated == null) return
        rows[index] = row.copy(selected = !row.selected)
        _uiState.value = _uiState.value.copy(rows = rows)
    }

    fun applySelected(onDone: (String) -> Unit) {
        viewModelScope.launch {
            val selected = _uiState.value.rows.filter { it.selected && it.updated != null }
            selected.forEach { dao.upsert(it.updated!!) }
            onDone(
                when (selected.size) {
                    0 -> "Nothing applied."
                    1 -> "Updated ${selected.first().title}."
                    else -> "Updated ${selected.size} Pokémon."
                },
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GoBuddyApp
                ImportViewModel(
                    app.container.pokemonDao,
                    app.container.speciesRepository,
                    app.container.screenshotScanner,
                )
            }
        }
    }
}
