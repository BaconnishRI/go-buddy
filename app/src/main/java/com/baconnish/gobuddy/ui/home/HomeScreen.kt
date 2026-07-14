package com.baconnish.gobuddy.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baconnish.gobuddy.data.BuddyLevel
import com.baconnish.gobuddy.data.TrainerSettings
import com.baconnish.gobuddy.data.db.TrackedPokemon
import com.baconnish.gobuddy.domain.BuddyCalculator
import com.baconnish.gobuddy.ui.theme.buddyTierColor
import com.baconnish.gobuddy.ui.theme.buddyTierIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onPokemonClick: (Long) -> Unit,
    onOverlayClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val pokemon by viewModel.pokemon.collectAsState()
    val showOnboarding by viewModel.showOnboarding.collectAsState()
    val settings by viewModel.settings.collectAsState()

    if (showOnboarding) {
        OnboardingDialog(
            defaults = settings,
            onDone = viewModel::completeOnboarding,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Go Buddy") },
                actions = {
                    IconButton(onClick = onOverlayClick) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Game overlay")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Trainer profile")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Track a Pokémon")
            }
        },
    ) { padding ->
        if (pokemon.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No Pokémon tracked yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap + to add a Pokémon and set a goal: a target level, Best Buddy, or both.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(pokemon, key = { it.id }) { p ->
                    PokemonCard(
                        pokemon = p,
                        isFirst = p.id == pokemon.first().id,
                        isLast = p.id == pokemon.last().id,
                        onClick = { onPokemonClick(p.id) },
                        onAddHeart = { viewModel.addHeart(p) },
                        onMove = { up -> viewModel.move(p, up) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PokemonCard(
    pokemon: TrackedPokemon,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onAddHeart: () -> Unit,
    onMove: (Boolean) -> Unit,
) {
    val buddyLevel = BuddyLevel.fromHearts(pokemon.hearts)
    val heartsToBest = BuddyCalculator.heartsRemaining(pokemon.hearts)

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pokemon.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Lv ${formatLevel(pokemon.currentLevel)} → ${formatLevel(pokemon.targetLevel)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Column {
                    IconButton(
                        onClick = { onMove(true) },
                        enabled = !isFirst,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                    }
                    IconButton(
                        onClick = { onMove(false) },
                        enabled = !isLast,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                    }
                }
            }
            if (pokemon.nickname.isNotBlank()) {
                Text(pokemon.speciesName, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            if (pokemon.wantBestBuddy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        buddyTierIcon(buddyLevel),
                        contentDescription = buddyLevel.label,
                        tint = buddyTierColor(buddyLevel),
                    )
                    Spacer(Modifier.padding(4.dp))
                    Text(
                        if (heartsToBest == 0) {
                            "Best Buddy!"
                        } else {
                            "${buddyLevel.label}: $heartsToBest hearts to Best"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (heartsToBest > 0) {
                        TextButton(onClick = onAddHeart) {
                            Text("+1 ♥")
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = {
                        (pokemon.hearts.toFloat() / BuddyLevel.BEST.requiredHearts).coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    color = buddyTierColor(buddyLevel),
                    drawStopIndicator = {},
                )
            }
        }
    }
}

@Composable
private fun OnboardingDialog(
    defaults: TrainerSettings,
    onDone: (TrainerSettings?) -> Unit,
) {
    var trainerLevel by remember { mutableStateOf(defaults.trainerLevel.toString()) }
    var heartsPerDay by remember { mutableStateOf(defaults.heartsPerDay.toString()) }
    var kmPerDay by remember { mutableStateOf(formatLevel(defaults.kmPerDay)) }

    AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text("Welcome to Go Buddy!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Three quick things about you; they drive every cost and time " +
                        "estimate in the app. You can change them anytime from the " +
                        "profile page.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = trainerLevel,
                    onValueChange = { trainerLevel = it },
                    label = { Text("Your trainer level") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = heartsPerDay,
                    onValueChange = { heartsPerDay = it },
                    label = { Text("Buddy hearts you earn per day") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = kmPerDay,
                    onValueChange = { kmPerDay = it },
                    label = { Text("Km you walk per day") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDone(
                    TrainerSettings(
                        trainerLevel = trainerLevel.toIntOrNull()?.coerceIn(1, 80)
                            ?: defaults.trainerLevel,
                        heartsPerDay = heartsPerDay.toIntOrNull()?.coerceIn(1, 100)
                            ?: defaults.heartsPerDay,
                        kmPerDay = kmPerDay.toDoubleOrNull()?.coerceIn(0.1, 200.0)
                            ?: defaults.kmPerDay,
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { onDone(null) }) { Text("Later") }
        },
    )
}

internal fun formatLevel(level: Double): String =
    if (level == level.toInt().toDouble()) level.toInt().toString() else level.toString()
