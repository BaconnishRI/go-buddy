package com.baconnish.gobuddy.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baconnish.gobuddy.data.BuddyLevel
import com.baconnish.gobuddy.data.GameData
import com.baconnish.gobuddy.data.HyperTrainingStat
import com.baconnish.gobuddy.data.PokemonForm
import com.baconnish.gobuddy.data.db.TrackedPokemon
import com.baconnish.gobuddy.domain.BuddyCalculator
import com.baconnish.gobuddy.domain.GoalPlan
import com.baconnish.gobuddy.ui.theme.buddyTierColor
import com.baconnish.gobuddy.ui.theme.buddyTierIcon
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    val pokemon = state.pokemon
    val plan = state.plan
    val context = LocalContext.current

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.updateFromScan(uri) { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pokemon?.displayName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scanLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Update from screenshot")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                },
            )
        },
    ) { padding ->
        if (pokemon != null && plan != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HeaderCard(
                    pokemon,
                    currentCp = state.currentCp,
                    targetCp = state.targetCp,
                    onAdjustLevel = viewModel::adjustLevel,
                )
                ReachabilityCard(pokemon, plan)
                if (pokemon.hyperTrainingStat != null) {
                    HyperTrainingCard(
                        pokemon,
                        hyperTrainedCp = state.hyperTrainedCp,
                        onIncrement = viewModel::incrementIv,
                    )
                }
                CostCard(pokemon, plan)
                if (plan.candyShort > 0) WalkingCard(pokemon, plan)
                if (pokemon.wantBestBuddy) {
                    BestBuddyCard(pokemon, plan, onAddHearts = viewModel::addHearts)
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(
    pokemon: TrackedPokemon,
    currentCp: Int?,
    targetCp: Int?,
    onAdjustLevel: (Double) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Level ${fmt(pokemon.currentLevel)} → ${fmt(pokemon.targetLevel)}",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onAdjustLevel(-0.5) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Lower level by half")
                }
                IconButton(onClick = { onAdjustLevel(0.5) }) {
                    Icon(Icons.Default.Add, contentDescription = "Raise level by half")
                }
            }
            if (currentCp != null && targetCp != null) {
                Text(
                    "CP ${grouped(currentCp)} now → ${grouped(targetCp)} at the target",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            val tags = buildList {
                add(pokemon.speciesName)
                if (pokemon.form != PokemonForm.NORMAL) add(pokemon.form.label)
                if (pokemon.isLucky) add("Lucky")
                if (pokemon.isCurrentBuddy) add("Current buddy")
                if (pokemon.ivAtk != null) {
                    add("IVs ${pokemon.ivAtk}/${pokemon.ivDef}/${pokemon.ivSta}")
                }
            }
            Text(tags.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            Text(
                "Buddy distance: ${fmt(pokemon.kmPerCandy)} km per candy" +
                    " (${fmt(pokemon.kmPerCandy * GameData.EXCITED_DISTANCE_MULTIPLIER)} km when excited)",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ReachabilityCard(pokemon: TrackedPokemon, plan: GoalPlan) {
    val warnings = buildList {
        if (!plan.targetReachableNow) {
            add(
                "Level ${fmt(pokemon.targetLevel)} is out of reach right now; a Pokémon can only " +
                    "be powered up to your trainer level + ${GameData.ALLOWED_LEVELS_ABOVE_PLAYER} " +
                    "(cap ${fmt(GameData.MAX_POKEMON_LEVEL)}). You can currently reach " +
                    "${fmt(plan.maxLevelAllowed)}; you'd need trainer level ${plan.trainerLevelNeeded}.",
            )
        }
        if (plan.needsXlCandy && !plan.xlCandyUnlocked) {
            add(
                "Powering up past level ${fmt(GameData.XL_CANDY_FROM_POKEMON_LEVEL)} uses Candy XL, " +
                    "which unlocks at trainer level ${GameData.XL_CANDY_MIN_TRAINER_LEVEL}.",
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (warnings.isEmpty()) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (warnings.isEmpty()) "Goal is reachable" else "Heads up",
                style = MaterialTheme.typography.titleMedium,
            )
            if (warnings.isEmpty()) {
                Text(
                    "Your trainer level allows powering up to level ${fmt(plan.maxLevelAllowed)}." +
                        if (pokemon.wantBestBuddy) {
                            " A Best Buddy set as your buddy gets +1 boosted level on top."
                        } else {
                            ""
                        },
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                warnings.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun HyperTrainingCard(
    pokemon: TrackedPokemon,
    hyperTrainedCp: Int?,
    onIncrement: (HyperTrainingStat) -> Unit,
) {
    val stat = pokemon.hyperTrainingStat
        ?.let { name -> HyperTrainingStat.entries.firstOrNull { it.name == name } }
        ?: return
    val trained = if (stat == HyperTrainingStat.ALL) {
        listOf(HyperTrainingStat.ATK, HyperTrainingStat.DEF, HyperTrainingStat.STA)
    } else {
        listOf(stat)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Hyper Training: ${stat.label}", style = MaterialTheme.typography.titleMedium)
            when {
                pokemon.form == PokemonForm.SHADOW ->
                    Text(
                        "Shadow Pokémon can't be Hyper Trained; purify it first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                pokemon.hearts < 1 ->
                    Text(
                        "Needs at least Good Buddy (1 heart) before training can start.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                pokemon.ivAtk == null ->
                    Text(
                        "Scan the appraisal screen first so the IVs are known.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                else -> {
                    trained.forEach { target ->
                        val current = when (target) {
                            HyperTrainingStat.ATK -> pokemon.ivAtk
                            HyperTrainingStat.DEF -> pokemon.ivDef
                            HyperTrainingStat.STA -> pokemon.ivSta
                            HyperTrainingStat.ALL -> null
                        } ?: 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "${target.label} IV: $current / 15",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(
                                onClick = { onIncrement(target) },
                                enabled = current < 15,
                            ) {
                                Text(if (current < 15) "+1 stage" else "Maxed!")
                            }
                        }
                    }
                    if (hyperTrainedCp != null) {
                        StatRow("CP when maxed", grouped(hyperTrainedCp))
                    }
                    Text(
                        "Each completed training stage in the game raises the IV by 1; " +
                            "tap +1 here when you finish one, or just rescan the appraisal screen.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun CostCard(pokemon: TrackedPokemon, plan: GoalPlan) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Power-up cost", style = MaterialTheme.typography.titleMedium)
            StatRow("Stardust", grouped(plan.cost.stardust))
            if (plan.cost.candy > 0) {
                StatRow(
                    "Candy",
                    "${grouped(plan.cost.candy)} needed · ${grouped(pokemon.candyOwned)} owned · " +
                        if (plan.candyShort == 0) "covered ✓" else "${grouped(plan.candyShort)} short",
                )
            }
            if (plan.cost.candyXl > 0) {
                StatRow(
                    "Candy XL",
                    "${grouped(plan.cost.candyXl)} needed · ${grouped(pokemon.candyXlOwned)} owned · " +
                        if (plan.candyXlShort == 0) "covered ✓" else "${grouped(plan.candyXlShort)} short",
                )
            }
            if (plan.cost.stardust == 0) {
                Text("Already at the target level!", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun WalkingCard(pokemon: TrackedPokemon, plan: GoalPlan) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Walking for the missing candy", style = MaterialTheme.typography.titleMedium)
            Text(
                "${grouped(plan.candyShort)} candy × ${fmt(pokemon.kmPerCandy)} km:",
                style = MaterialTheme.typography.bodyMedium,
            )
            StatRow("Normal mood", "${fmt(plan.walkKmNormal)} km · ~${plan.walkDaysNormal} days")
            StatRow("Excited mood", "${fmt(plan.walkKmExcited)} km · ~${plan.walkDaysExcited} days")
            Text(
                "Excited halves the distance per candy. Keeping your buddy excited (or feeding a " +
                    "Poffin) pays off on long grinds. Day estimates use your km/day from settings.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BestBuddyCard(pokemon: TrackedPokemon, plan: GoalPlan, onAddHearts: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    buddyTierIcon(plan.buddyLevel),
                    contentDescription = null,
                    tint = buddyTierColor(plan.buddyLevel),
                )
                Text(
                    "  Road to Best Buddy",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            StatRow("Current tier", plan.buddyLevel.label)
            LinearProgressIndicator(
                progress = {
                    (pokemon.hearts.toFloat() / BuddyLevel.BEST.requiredHearts).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth(),
                color = buddyTierColor(plan.buddyLevel),
                drawStopIndicator = {},
            )
            if (plan.heartsToBest == 0) {
                Text("Best Buddy reached! 🎉", style = MaterialTheme.typography.bodyMedium)
            } else {
                StatRow(
                    "Hearts",
                    "${grouped(pokemon.hearts)} / ${BuddyLevel.BEST.requiredHearts} · " +
                        "${grouped(plan.heartsToBest)} to go",
                )
                val next = BuddyCalculator.nextLevel(pokemon.hearts)
                if (next != null && next != BuddyLevel.BEST) {
                    StatRow(
                        "Next tier",
                        "${BuddyCalculator.heartsRemaining(pokemon.hearts, next)} hearts to ${next.label}",
                    )
                }
                StatRow("Estimate", "~${plan.daysToBestBuddy} days at your hearts/day")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(-1, 1, 5, 12).forEach { delta ->
                        OutlinedButton(
                            onClick = { onAddHearts(delta) },
                            enabled = delta > 0 || pokemon.hearts > 0,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        ) {
                            Text(if (delta > 0) "+$delta" else "$delta")
                        }
                    }
                }
                Text(
                    "An excited buddy earns double hearts, so Poffins and busy play days " +
                        "cut this roughly in half.",
                    style = MaterialTheme.typography.bodySmall,
                )
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
            modifier = Modifier.weight(0.35f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.65f),
        )
    }
}

private fun fmt(value: Double): String =
    if (value == value.toInt().toDouble()) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }

private fun grouped(value: Int): String = String.format(Locale.US, "%,d", value)
