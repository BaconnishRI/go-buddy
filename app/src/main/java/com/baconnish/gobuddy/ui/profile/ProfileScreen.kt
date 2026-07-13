package com.baconnish.gobuddy.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baconnish.gobuddy.BuildConfig
import com.baconnish.gobuddy.data.GameData
import com.baconnish.gobuddy.data.TrainerSettings
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri) { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri) { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trainer profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TrainerCard(state.settings, onSave = viewModel::saveSettings)
            state.quest?.let { quest ->
                TotalsCard(quest)
                if (quest.entries.isNotEmpty()) QuestCard(quest)
            }
            BackupCard(
                onExport = { exportLauncher.launch("gobuddy-backup.json") },
                onImport = { importLauncher.launch(arrayOf("*/*")) },
            )
            AboutCard()
        }
    }
}

@Composable
private fun AboutCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("About", style = MaterialTheme.typography.titleMedium)
            StatRow(
                "Version",
                "${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
            )
            StatRow("Build type", BuildConfig.BUILD_TYPE)
            StatRow("Game data updated", GameData.DATA_UPDATED)
            Text(
                "Go Buddy is in early development — expect rough edges, and export a " +
                    "backup before updating.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Free, noncommercial fan project (PolyForm Noncommercial 1.0.0). Not " +
                    "affiliated with Niantic or The Pokémon Company. All scanning runs " +
                    "on-device; nothing leaves your phone.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TrainerCard(settings: TrainerSettings, onSave: (TrainerSettings) -> Unit) {
    val context = LocalContext.current
    var trainerLevel by remember { mutableStateOf(settings.trainerLevel.toString()) }
    var heartsPerDay by remember { mutableStateOf(settings.heartsPerDay.toString()) }
    var kmPerDay by remember { mutableStateOf(settings.kmPerDay.toString()) }

    LaunchedEffect(settings) {
        trainerLevel = settings.trainerLevel.toString()
        heartsPerDay = settings.heartsPerDay.toString()
        kmPerDay = fmt(settings.kmPerDay)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Trainer", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = trainerLevel,
                onValueChange = { trainerLevel = it },
                label = { Text("Trainer level") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = heartsPerDay,
                onValueChange = { heartsPerDay = it },
                label = { Text("Buddy hearts you earn per day") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = kmPerDay,
                onValueChange = { kmPerDay = it },
                label = { Text("Km you walk per day") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    onSave(
                        TrainerSettings(
                            trainerLevel = trainerLevel.toIntOrNull()?.coerceIn(1, 80)
                                ?: settings.trainerLevel,
                            heartsPerDay = heartsPerDay.toIntOrNull()?.coerceIn(1, 100)
                                ?: settings.heartsPerDay,
                            kmPerDay = kmPerDay.toDoubleOrNull()?.coerceIn(0.1, 200.0)
                                ?: settings.kmPerDay,
                        ),
                    )
                    Toast.makeText(context, "Saved.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun TotalsCard(quest: com.baconnish.gobuddy.domain.QuestPlanner.Quest) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Everything you're tracking", style = MaterialTheme.typography.titleMedium)
            StatRow("Pokémon", quest.entries.size.toString())
            StatRow("Stardust needed", grouped(quest.totalStardust))
            StatRow("Candy short", grouped(quest.totalCandyShort))
            if (quest.totalCandyXlShort > 0) {
                StatRow("Candy XL short", grouped(quest.totalCandyXlShort))
            }
            StatRow("Walking left", "${fmt(quest.totalWalkKm)} km")
            StatRow("Hearts to Best", grouped(quest.totalHeartsToBest))
        }
    }
}

@Composable
private fun QuestCard(quest: com.baconnish.gobuddy.domain.QuestPlanner.Quest) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("The full quest, in your order", style = MaterialTheme.typography.titleMedium)
            Text(
                "One buddy at a time: each Pokémon needs the longer of its walking and " +
                    "hearts estimates. Reorder with the arrows on the home screen.",
                style = MaterialTheme.typography.bodySmall,
            )
            quest.entries.forEachIndexed { index, entry ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${index + 1}. ${entry.pokemon.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (entry.days == 0) "done" else "~${entry.days}d · day ${entry.cumulativeDays}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            StatRow("Total", "~${quest.totalDays} days")
        }
    }
}

@Composable
private fun BackupCard(onExport: () -> Unit, onImport: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Backup", style = MaterialTheme.typography.titleMedium)
            Text(
                "Save everything to a file (keep it in Drive or Downloads) and restore it " +
                    "after a reinstall or on a new phone.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onExport, modifier = Modifier.weight(1f)) {
                    Text("Export")
                }
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                    Text("Import")
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun fmt(value: Double): String =
    if (value == value.toInt().toDouble()) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }

private fun grouped(value: Int): String = String.format(Locale.US, "%,d", value)
