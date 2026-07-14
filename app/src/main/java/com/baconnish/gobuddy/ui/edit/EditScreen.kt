package com.baconnish.gobuddy.ui.edit

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baconnish.gobuddy.data.BuddyLevel
import com.baconnish.gobuddy.data.HyperTrainingStat
import com.baconnish.gobuddy.data.PokemonForm
import com.baconnish.gobuddy.data.Species
import com.baconnish.gobuddy.data.db.TrackedPokemon
import com.baconnish.gobuddy.domain.LevelCalculator
import com.baconnish.gobuddy.domain.PowerUpCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    onDone: () -> Unit,
    viewModel: EditViewModel = viewModel(factory = EditViewModel.Factory),
) {
    val loaded by viewModel.pokemon.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "Track a Pokémon" else "Edit Pokémon") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { viewModel.delete(onDeleted = onDone) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        val pokemon = loaded
        if (pokemon != null) {
            EditForm(
                initial = pokemon,
                searchSpecies = viewModel::searchSpecies,
                scan = viewModel::scanScreenshot,
                onSave = { viewModel.save(it, onSaved = onDone) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@Composable
private fun EditForm(
    initial: TrackedPokemon,
    searchSpecies: (String) -> List<Species>,
    scan: (Uri, (ScanPrefill?) -> Unit) -> Unit,
    onSave: (TrackedPokemon) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var speciesName by remember { mutableStateOf(initial.speciesName) }
    var speciesDex by remember { mutableStateOf(initial.speciesDex) }
    var kmPerCandy by remember { mutableDoubleStateOf(initial.kmPerCandy) }
    var speciesStats by remember { mutableStateOf<Species?>(null) }
    var speciesQuery by remember { mutableStateOf(initial.speciesName) }
    var speciesPicked by remember { mutableStateOf(initial.speciesName.isNotBlank()) }
    var nickname by remember { mutableStateOf(initial.nickname) }
    var currentLevelText by remember { mutableStateOf(formatLevelInput(initial.currentLevel)) }
    var targetLevelText by remember { mutableStateOf(formatLevelInput(initial.targetLevel)) }
    var form by remember { mutableStateOf(initial.form) }
    var isLucky by remember { mutableStateOf(initial.isLucky) }
    var isCurrentBuddy by remember { mutableStateOf(initial.isCurrentBuddy) }
    var wantBestBuddy by remember { mutableStateOf(initial.wantBestBuddy) }
    var heartsText by remember { mutableStateOf(initial.hearts.toString()) }
    var heartsLeftText by remember { mutableStateOf("") }
    var candyText by remember { mutableStateOf(initial.candyOwned.toString()) }
    var candyXlText by remember { mutableStateOf(initial.candyXlOwned.toString()) }
    var hyperStat by remember { mutableStateOf(initial.hyperTrainingStat) }
    var cpText by remember { mutableStateOf("") }
    var ivAtkText by remember { mutableStateOf(initial.ivAtk?.toString() ?: "") }
    var ivDefText by remember { mutableStateOf(initial.ivDef?.toString() ?: "") }
    var ivStaText by remember { mutableStateOf(initial.ivSta?.toString() ?: "") }

    LaunchedEffect(initial.id) {
        if (initial.id > 0) {
            speciesName = initial.speciesName
            speciesQuery = initial.speciesName
            speciesStats = searchSpecies(initial.speciesName)
                .firstOrNull { it.name.equals(initial.speciesName, ignoreCase = true) }
            val stats = speciesStats
            if (cpText.isBlank() && stats != null &&
                initial.ivAtk != null && initial.ivDef != null && initial.ivSta != null &&
                PowerUpCalculator.isValidLevel(initial.currentLevel)
            ) {
                cpText = LevelCalculator.cpAt(
                    initial.currentLevel,
                    stats.baseAtk, stats.baseDef, stats.baseSta,
                    initial.ivAtk, initial.ivDef, initial.ivSta,
                ).toString()
            }
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scan(uri) { prefill ->
                if (prefill == null) {
                    Toast.makeText(context, "Couldn't read that screenshot.", Toast.LENGTH_LONG).show()
                } else {
                    prefill.species?.let { species ->
                        speciesName = species.name
                        speciesDex = species.dex
                        kmPerCandy = species.kmPerCandy
                        speciesStats = species
                        speciesQuery = species.name
                        speciesPicked = true
                    }
                    prefill.cp?.let { cpText = it.toString() }
                    prefill.candy?.let { candyText = it.toString() }
                    prefill.candyXl?.let { candyXlText = it.toString() }
                    prefill.level?.let { currentLevelText = formatLevelInput(it) }
                    prefill.ivAtk?.let { ivAtkText = it.toString() }
                    prefill.ivDef?.let { ivDefText = it.toString() }
                    prefill.ivSta?.let { ivStaText = it.toString() }
                    val message = when {
                        prefill.species == null ->
                            "Scanned: pick the species, then use CP to find the level."
                        prefill.level != null ->
                            "Scanned: ${prefill.species.name}, level ${formatLevelInput(prefill.level)}."
                        prefill.cp != null ->
                            "Scanned ${prefill.species.name} at CP ${prefill.cp}; add IVs to pin the level."
                        else -> "Scanned ${prefill.species.name}."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val currentLevel = currentLevelText.toDoubleOrNull()
    val targetLevel = targetLevelText.toDoubleOrNull()
    val levelsValid = currentLevel != null && targetLevel != null &&
        PowerUpCalculator.isValidLevel(currentLevel) &&
        PowerUpCalculator.isValidLevel(targetLevel) &&
        targetLevel >= currentLevel
    val canSave = speciesName.isNotBlank() && levelsValid

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = {
                scanLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Text("  Scan a screenshot")
        }
        Text(
            "Take a screenshot of the Pokémon's screen in the game, then pick it here: " +
                "species, CP, level, and candy fill in automatically.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = speciesQuery,
            onValueChange = {
                speciesQuery = it
                speciesPicked = false
            },
            label = { Text("Species") },
            supportingText = {
                if (speciesPicked && kmPerCandy > 0) {
                    Text("Buddy distance: ${formatKm(kmPerCandy)} km per candy")
                } else {
                    Text("Start typing and pick from the list")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!speciesPicked && speciesQuery.isNotBlank()) {
            val matches = searchSpecies(speciesQuery)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    matches.forEach { species ->
                        ListItem(
                            headlineContent = { Text(species.name) },
                            supportingContent = {
                                Text("#${species.dex} · ${formatKm(species.kmPerCandy)} km per candy")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    speciesName = species.name
                                    speciesDex = species.dex
                                    kmPerCandy = species.kmPerCandy
                                    speciesStats = species
                                    speciesQuery = species.name
                                    speciesPicked = true
                                },
                        )
                    }
                    if (matches.isEmpty()) {
                        ListItem(
                            headlineContent = { Text("No match; keep typing or use a custom name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    speciesName = speciesQuery.trim()
                                    speciesDex = null
                                    speciesStats = null
                                    speciesPicked = true
                                },
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("Nickname (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = currentLevelText,
                onValueChange = { currentLevelText = it },
                label = { Text("Current level") },
                isError = currentLevel == null || !PowerUpCalculator.isValidLevel(currentLevel),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = targetLevelText,
                onValueChange = { targetLevelText = it },
                label = { Text("Target level") },
                isError = targetLevel == null || !PowerUpCalculator.isValidLevel(targetLevel),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            "Levels go in 0.5 steps from 1 to 50.",
            style = MaterialTheme.typography.bodySmall,
        )

        speciesStats?.let { stats ->
            LevelFinderCard(
                species = stats,
                cpText = cpText,
                onCpChange = { cpText = it },
                ivAtkText = ivAtkText,
                onIvAtkChange = { ivAtkText = it },
                ivDefText = ivDefText,
                onIvDefChange = { ivDefText = it },
                ivStaText = ivStaText,
                onIvStaChange = { ivStaText = it },
                onLevelFound = { currentLevelText = formatLevelInput(it) },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PokemonForm.entries.forEach { option ->
                FilterChip(
                    selected = form == option,
                    onClick = { form = option },
                    label = { Text(option.label) },
                )
            }
        }

        LabeledSwitch("Lucky (half stardust)", isLucky) { isLucky = it }
        LabeledSwitch("Currently my buddy", isCurrentBuddy) { isCurrentBuddy = it }
        LabeledSwitch("Goal: make it my Best Buddy", wantBestBuddy) { wantBestBuddy = it }

        Text("Hyper Training (Bottle Cap)", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            FilterChip(
                selected = hyperStat == null,
                onClick = { hyperStat = null },
                label = { Text("None") },
            )
            HyperTrainingStat.entries.forEach { option ->
                FilterChip(
                    selected = hyperStat == option.name,
                    onClick = { hyperStat = option.name },
                    label = { Text(option.label) },
                )
            }
        }

        OutlinedTextField(
            value = heartsText,
            onValueChange = { heartsText = it },
            label = { Text("Buddy hearts earned so far") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Only know how many hearts are left? Type that number and tap the tier " +
                "you're working toward; the total fills in above.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = heartsLeftText,
                onValueChange = { heartsLeftText = it },
                label = { Text("Left") },
                singleLine = true,
                modifier = Modifier.weight(1.2f),
            )
            listOf(BuddyLevel.GREAT, BuddyLevel.ULTRA, BuddyLevel.BEST).forEach { tier ->
                OutlinedButton(
                    onClick = {
                        heartsLeftText.toIntOrNull()?.let { left ->
                            heartsText = (tier.requiredHearts - left).coerceAtLeast(0).toString()
                            heartsLeftText = ""
                        }
                    },
                    enabled = heartsLeftText.toIntOrNull() != null,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Text(tier.label.removeSuffix(" Buddy"))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = candyText,
                onValueChange = { candyText = it },
                label = { Text("Candy owned") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = candyXlText,
                onValueChange = { candyXlText = it },
                label = { Text("Candy XL owned") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        Button(
            onClick = {
                onSave(
                    initial.copy(
                        nickname = nickname.trim(),
                        speciesName = speciesName.trim(),
                        speciesDex = speciesDex,
                        kmPerCandy = kmPerCandy,
                        currentLevel = currentLevel ?: initial.currentLevel,
                        targetLevel = targetLevel ?: initial.targetLevel,
                        form = form,
                        isLucky = isLucky,
                        isCurrentBuddy = isCurrentBuddy,
                        wantBestBuddy = wantBestBuddy,
                        hearts = heartsText.toIntOrNull()?.coerceAtLeast(0) ?: initial.hearts,
                        candyOwned = candyText.toIntOrNull()?.coerceAtLeast(0) ?: initial.candyOwned,
                        candyXlOwned = candyXlText.toIntOrNull()?.coerceAtLeast(0)
                            ?: initial.candyXlOwned,
                        ivAtk = ivAtkText.toIntOrNull()?.takeIf { it in 0..15 },
                        ivDef = ivDefText.toIntOrNull()?.takeIf { it in 0..15 },
                        ivSta = ivStaText.toIntOrNull()?.takeIf { it in 0..15 },
                        hyperTrainingStat = hyperStat,
                    ),
                )
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
    }
}

@Composable
private fun LevelFinderCard(
    species: Species,
    cpText: String,
    onCpChange: (String) -> Unit,
    ivAtkText: String,
    onIvAtkChange: (String) -> Unit,
    ivDefText: String,
    onIvDefChange: (String) -> Unit,
    ivStaText: String,
    onIvStaChange: (String) -> Unit,
    onLevelFound: (Double) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var candidates by remember { mutableStateOf<List<Double>>(emptyList()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Don't know the level?", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Find it from CP and the appraisal bars",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(if (expanded) "▲" else "▼")
            }

            if (expanded) {
                OutlinedTextField(
                    value = cpText,
                    onValueChange = onCpChange,
                    label = { Text("CP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "IVs are the three bars on the in-game appraisal screen " +
                        "(tap your Pokémon → menu → Appraise), 0–15 each. " +
                        "Leave them empty to get a level range instead.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ivAtkText,
                        onValueChange = onIvAtkChange,
                        label = { Text("Attack") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = ivDefText,
                        onValueChange = onIvDefChange,
                        label = { Text("Defense") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = ivStaText,
                        onValueChange = onIvStaChange,
                        label = { Text("HP") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Button(
                    onClick = {
                        val cp = cpText.toIntOrNull()
                        val ivAtk = ivAtkText.toIntOrNull()
                        val ivDef = ivDefText.toIntOrNull()
                        val ivSta = ivStaText.toIntOrNull()
                        candidates = emptyList()
                        result = when {
                            cp == null || cp < 10 ->
                                "Enter a valid CP."

                            ivAtk != null && ivDef != null && ivSta != null -> {
                                if (ivAtk !in 0..15 || ivDef !in 0..15 || ivSta !in 0..15) {
                                    "IVs must be between 0 and 15."
                                } else {
                                    val levels = LevelCalculator.levelsForCp(
                                        cp, species.baseAtk, species.baseDef, species.baseSta,
                                        ivAtk, ivDef, ivSta,
                                    )
                                    when (levels.size) {
                                        0 -> "No level matches that CP and those IVs; double-check the numbers."
                                        1 -> {
                                            onLevelFound(levels.first())
                                            "Level ${formatLevelInput(levels.first())}; set above."
                                        }
                                        else -> {
                                            candidates = levels
                                            "A few levels match; pick one:"
                                        }
                                    }
                                }
                            }

                            else -> {
                                val range = LevelCalculator.levelRangeForCp(
                                    cp, species.baseAtk, species.baseDef, species.baseSta,
                                )
                                if (range == null) {
                                    "No level matches that CP for ${species.name}."
                                } else {
                                    "Level is between ${formatLevelInput(range.start)} and " +
                                        "${formatLevelInput(range.endInclusive)}. " +
                                        "Add the IVs to pin it down."
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Find level")
                }
                result?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (candidates.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        candidates.forEach { level ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onLevelFound(level)
                                    result = "Level ${formatLevelInput(level)}; set above."
                                    candidates = emptyList()
                                },
                                label = { Text(formatLevelInput(level)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatLevelInput(level: Double): String =
    if (level == level.toInt().toDouble()) level.toInt().toString() else level.toString()

private fun formatKm(km: Double): String =
    if (km == km.toInt().toDouble()) km.toInt().toString() else km.toString()
