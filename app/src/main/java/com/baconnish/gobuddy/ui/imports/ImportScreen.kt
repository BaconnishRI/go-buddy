package com.baconnish.gobuddy.ui.imports

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    uris: List<Uri>,
    onDone: () -> Unit,
    viewModel: ImportViewModel = viewModel(factory = ImportViewModel.Factory),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.process(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import screenshots") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
        bottomBar = {
            if (!state.processing) {
                Button(
                    onClick = {
                        viewModel.applySelected { message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            onDone()
                        }
                    },
                    enabled = state.selectedCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        if (state.selectedCount == 0) {
                            "Nothing to apply"
                        } else {
                            "Apply ${state.selectedCount} update${if (state.selectedCount == 1) "" else "s"}"
                        },
                    )
                }
            }
        },
    ) { padding ->
        if (state.processing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        "Reading ${uris.size} screenshot${if (uris.size == 1) "" else "s"}…",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(state.rows) { index, row ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Checkbox(
                                checked = row.selected,
                                onCheckedChange = if (row.updated != null) {
                                    { viewModel.toggle(index) }
                                } else {
                                    null
                                },
                                enabled = row.updated != null,
                            )
                            Column(Modifier.padding(vertical = 8.dp)) {
                                Text(row.title, style = MaterialTheme.typography.titleSmall)
                                Text(row.detail, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
