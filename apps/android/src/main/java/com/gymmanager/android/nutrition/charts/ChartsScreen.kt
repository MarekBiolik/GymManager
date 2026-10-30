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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val dayLabelFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd")

@Composable
fun ChartsScreen(viewModel: ChartsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val maxKcal = state.dailyTotals.maxOfOrNull { it.macros.kcal }?.coerceAtLeast(1.0) ?: 1.0

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
                        text = parseIsoDay(day.eatenOn).format(dayLabelFormat),
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

        Text(
            "Macro averages (per logged day)",
            color = GymTokens.ColorTextPrimary,
            modifier = Modifier.padding(top = GymTokens.Space4),
        )
        if (state.dailyTotals.isEmpty()) {
            Text("No entries in range.", color = GymTokens.ColorTextSecondary)
        } else {
            val avg = state.averageMacros
            val (pShare, cShare, fShare) = state.macroShares
            Text(
                text = "${avg.kcal.roundToInt()} kcal avg",
                color = GymTokens.ColorTextSecondary,
            )
            MacroShareRow("Protein", avg.proteinG, pShare)
            MacroShareRow("Carbs", avg.carbsG, cShare)
            MacroShareRow("Fat", avg.fatG, fShare)
        }

        Text(
            "Body weight (kg)",
            color = GymTokens.ColorTextPrimary,
            modifier = Modifier.padding(top = GymTokens.Space4),
        )
        if (state.weightPoints.isEmpty()) {
            Text("Log weight in Settings.", color = GymTokens.ColorTextSecondary)
        } else {
            val recent = state.weightPoints.takeLast(10)
            val minW = recent.minOf { it.value }
            val maxW = recent.maxOf { it.value }
            val span = (maxW - minW).coerceAtLeast(0.1)
            recent.forEach { m ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2),
                ) {
                    Text(
                        text =
                            Instant.ofEpochMilli(m.measuredAtEpochMs)
                                .atZone(ZoneId.systemDefault())
                                .format(dayLabelFormat),
                        color = GymTokens.ColorTextSecondary,
                        modifier = Modifier.weight(0.25f),
                    )
                    Box(
                        modifier =
                            Modifier
                                .weight(0.5f)
                                .height(12.dp)
                                .background(GymTokens.ColorBgMuted),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth((0.15 + 0.85 * (m.value - minW) / span).toFloat().coerceIn(0f, 1f))
                                    .height(12.dp)
                                    .background(GymTokens.ColorActionPrimary),
                        )
                    }
                    Text(
                        text = "%.1f".format(m.value),
                        color = GymTokens.ColorTextPrimary,
                        modifier = Modifier.weight(0.25f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroShareRow(
    label: String,
    grams: Double,
    share: Double,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2),
    ) {
        Text(label, color = GymTokens.ColorTextSecondary, modifier = Modifier.weight(0.25f))
        Box(
            modifier =
                Modifier
                    .weight(0.5f)
                    .height(12.dp)
                    .background(GymTokens.ColorBgMuted),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(share.toFloat().coerceIn(0f, 1f))
                        .height(12.dp)
                        .background(GymTokens.ColorActionPrimary),
            )
        }
        Text(
            text = "${grams.roundToInt()} g · ${(share * 100).roundToInt()}%",
            color = GymTokens.ColorTextPrimary,
            modifier = Modifier.weight(0.25f),
        )
    }
}
