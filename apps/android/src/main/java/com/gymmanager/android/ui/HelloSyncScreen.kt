package com.gymmanager.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymmanager.android.sync.HelloSyncViewModel
import com.gymmanager.android.theme.GymTokens

@Composable
fun HelloSyncScreen(viewModel: HelloSyncViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var metric by remember { mutableStateOf("weight") }
    var value by remember { mutableStateOf("70") }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = GymTokens.Space4),
        verticalArrangement = Arrangement.spacedBy(GymTokens.Space3),
    ) {
        Text(text = "Hello sync", color = GymTokens.ColorTextPrimary)
        state.banner?.let {
            Text(text = it, color = GymTokens.ColorTextSecondary)
        }
        Text(
            text = "Profile units: ${viewModel.preferredUnitsLabel()}",
            color = GymTokens.ColorTextSecondary,
        )
        Text(text = "Measurements", color = GymTokens.ColorTextPrimary)
        if (state.measurements.isEmpty()) {
            Text(text = "No measurements yet.", color = GymTokens.ColorTextSecondary)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(GymTokens.Space2),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(state.measurements, key = { it.id }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${row.metric}: ${row.value}",
                            color = GymTokens.ColorTextPrimary,
                        )
                        Button(
                            onClick = { viewModel.deleteMeasurement(row.id) },
                            enabled = !state.busy && state.online,
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
        OutlinedTextField(
            value = metric,
            onValueChange = { metric = it },
            label = { Text("Metric") },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Value") },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.addMeasurement(metric, value) },
            enabled = !state.busy && state.online,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("Add measurement")
        }
        if (!state.online) {
            Text(
                text = "A network connection is required to save.",
                color = GymTokens.ColorTextSecondary,
            )
        }
        state.errorMessage?.let {
            Text(text = it, color = GymTokens.ColorTextSecondary)
        }
    }
}
