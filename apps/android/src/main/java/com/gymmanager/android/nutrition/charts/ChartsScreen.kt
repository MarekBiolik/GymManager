package com.gymmanager.android.nutrition.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymmanager.android.nutrition.NutritionStatusBanner
import com.gymmanager.android.nutrition.parseIsoDay
import com.gymmanager.android.theme.GymTokens
import kotlin.math.roundToInt

@Composable
fun ChartsScreen(viewModel: ChartsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val maxKcal = viewModel.maxKcal()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(GymTokens.Space4),
        verticalArrangement = Arrangement.spacedBy(GymTokens.Space3),
    ) {
        Text("Charts", color = GymTokens.ColorTextPrimary, fontWeight = FontWeight.SemiBold)
        NutritionStatusBanner(state.banner)
        Button(onClick = viewModel::refresh, enabled = !state.busy) { Text("Refresh") }
        Text("Daily calories (last ${state.dailyTotals.size} days)", color = GymTokens.ColorTextPrimary)
        if (state.dailyTotals.isEmpty()) {
            Text("No entries in range.", color = GymTokens.ColorTextSecondary)
        } else {
            state.dailyTotals.forEach { day ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2),
                ) {
                    Text(
                        text = parseIsoDay(day.eatenOn).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd")),
                        color = GymTokens.ColorTextSecondary,
                        modifier = Modifier.weight(0.25f),
                    )
                    Box(
                        modifier =
                            Modifier
                                .weight(0.55f)
                                .height(16.dp)
                                .background(GymTokens.ColorBgMuted),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth((day.macros.kcal / maxKcal).toFloat().coerceIn(0f, 1f))
                                    .height(16.dp)
                                    .background(GymTokens.ColorActionPrimary),
                        )
                    }
                    Text(
                        text = "${day.macros.kcal.roundToInt()}",
                        color = GymTokens.ColorTextPrimary,
                        modifier = Modifier.weight(0.2f),
                    )
                }
            }
        }
        Text("Body weight (kg)", color = GymTokens.ColorTextPrimary, modifier = Modifier.padding(top = GymTokens.Space4))
        if (state.weightPoints.isEmpty()) {
            Text("Log weight in Settings.", color = GymTokens.ColorTextSecondary)
        } else {
            state.weightPoints.takeLast(10).forEach { m ->
                Text(
                    text = "${m.value} kg · ${m.measuredAtEpochMs}",
                    color = GymTokens.ColorTextSecondary,
                )
            }
        }
    }
}
