package com.gymmanager.android.nutrition.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymmanager.android.nutrition.MacroGoalSummary
import com.gymmanager.android.nutrition.NutritionStatusBanner
import com.gymmanager.android.nutrition.dayLabel
import com.gymmanager.android.nutrition.parseIsoDay
import com.gymmanager.android.theme.GymTokens
import com.gymmanager.core.data.off.OffProductCandidate
import com.gymmanager.core.domain.AmountUnit
import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.FoodEntry
import com.gymmanager.core.domain.MealType
import kotlin.math.roundToInt

@Composable
fun DiaryScreen(viewModel: DiaryViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(GymTokens.Space4),
        verticalArrangement = Arrangement.spacedBy(GymTokens.Space3),
    ) {
        Text(
            text = "Diary",
            color = GymTokens.ColorTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        NutritionStatusBanner(state.banner)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(onClick = { viewModel.shiftWeek(-7) }, enabled = !state.busy) {
                Text("◀ Week")
            }
            Button(onClick = { viewModel.refresh() }, enabled = !state.busy) {
                Text("Refresh")
            }
            Button(onClick = { viewModel.shiftWeek(7) }, enabled = !state.busy) {
                Text("Week ▶")
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2),
        ) {
            state.weekDays.forEach { iso ->
                FilterChip(
                    selected = iso == state.selectedDay,
                    onClick = { viewModel.selectDay(iso) },
                    label = { Text(dayLabel(parseIsoDay(iso))) },
                )
            }
        }
        MacroGoalSummary(state.consumed, state.settings, state.goalProgress)
        Button(onClick = { showAdd = true }, enabled = !state.busy && state.banner.online) {
            Text("Add entry")
        }
        if (state.mealTypes.isEmpty()) {
            Text("No meal types — add them in Settings.", color = GymTokens.ColorTextSecondary)
        }
        state.mealTypes.forEach { meal ->
            MealSection(
                title = meal.name,
                entries = viewModel.entriesForMeal(meal.id),
                foodName = viewModel::foodName,
                busy = state.busy,
                online = state.banner.online,
                onDelete = viewModel::deleteEntry,
            )
        }
    }

    if (showAdd) {
        AddEntryDialog(
            mealTypes = state.mealTypes,
            searchFoods = viewModel::searchFoods,
            offResults = state.offResults,
            busy = state.busy,
            onDismiss = {
                showAdd = false
                viewModel.clearOffResults()
            },
            onSearchOff = viewModel::searchOff,
            onBarcode = viewModel::lookupBarcode,
            onAddOwn = { foodId, mealId, amount, unit ->
                viewModel.addEntry(foodId, mealId, amount, unit, null)
                showAdd = false
            },
            onImportOff = { candidate, mealId, amount, unit ->
                viewModel.importOffAndAddEntry(candidate, mealId, amount, unit)
                showAdd = false
                viewModel.clearOffResults()
            },
        )
    }
}

@Composable
private fun MealSection(
    title: String,
    entries: List<FoodEntry>,
    foodName: (String?) -> String,
    busy: Boolean,
    online: Boolean,
    onDelete: (FoodEntry) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(GymTokens.ColorBgSurface)
                .padding(GymTokens.Space3),
        verticalArrangement = Arrangement.spacedBy(GymTokens.Space2),
    ) {
        Text(text = title, color = GymTokens.ColorTextBrand, fontWeight = FontWeight.SemiBold)
        if (entries.isEmpty()) {
            Text("No entries", color = GymTokens.ColorTextSecondary)
        } else {
            entries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(foodName(entry.foodId), color = GymTokens.ColorTextPrimary)
                        Text(
                            "${entry.amount} ${entry.unit.name.lowercase()} · " +
                                "${entry.macros.kcal.roundToInt()} kcal",
                            color = GymTokens.ColorTextSecondary,
                        )
                    }
                    Button(
                        onClick = { onDelete(entry) },
                        enabled = !busy && online,
                    ) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEntryDialog(
    mealTypes: List<MealType>,
    searchFoods: (String) -> List<Food>,
    offResults: List<OffProductCandidate>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSearchOff: (String) -> Unit,
    onBarcode: (String) -> Unit,
    onAddOwn: (String, String, Double, AmountUnit) -> Unit,
    onImportOff: (OffProductCandidate, String, Double, AmountUnit) -> Unit,
) {
    var mealId by remember { mutableStateOf(mealTypes.firstOrNull()?.id.orEmpty()) }
    var query by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var selectedOff by remember { mutableStateOf<OffProductCandidate?>(null) }
    var amount by remember { mutableStateOf("100") }
    var unit by remember { mutableStateOf(AmountUnit.G) }
    var barcode by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("mine") }
    val ownResults = remember(query, searchFoods) { searchFoods(query) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(GymTokens.ColorBgSurfaceElevated)
                .padding(GymTokens.Space4),
        verticalArrangement = Arrangement.spacedBy(GymTokens.Space2),
    ) {
        Text("Add food entry", color = GymTokens.ColorTextPrimary, fontWeight = FontWeight.SemiBold)
        if (mealTypes.isEmpty()) {
            Text("Configure meal types in Settings first.", color = GymTokens.ColorFeedbackWarn)
            Button(onClick = onDismiss) { Text("Close") }
            return
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2)) {
            FilterChip(selected = tab == "mine", onClick = { tab = "mine" }, label = { Text("My foods") })
            FilterChip(
                selected = tab == "off",
                onClick = { tab = "off" },
                label = { Text("Open Food Facts") },
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2),
        ) {
            mealTypes.forEach { meal ->
                FilterChip(
                    selected = mealId == meal.id,
                    onClick = { mealId = meal.id },
                    label = { Text(meal.name) },
                )
            }
        }
        if (tab == "off") {
            Text(
                "Data from Open Food Facts — import creates your private food copy.",
                color = GymTokens.ColorTextSecondary,
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                selectedFood = null
                selectedOff = null
                if (tab == "off") onSearchOff(it)
            },
            label = { Text(if (tab == "mine") "Search my foods" else "Search Open Food Facts") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        )
        if (tab == "off") {
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("Barcode (paste)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            )
            Button(onClick = { onBarcode(barcode) }, enabled = !busy) { Text("Lookup barcode") }
        }
        if (tab == "mine") {
            ownResults.take(8).forEach { food ->
                Text(
                    text = food.name,
                    color =
                        if (selectedFood?.id == food.id) {
                            GymTokens.ColorActionPrimary
                        } else {
                            GymTokens.ColorTextPrimary
                        },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedFood = food },
                )
            }
        } else {
            offResults.take(8).forEach { off ->
                Text(
                    text =
                        "${off.name} · P ${off.macrosPer100g.proteinG.roundToInt()} / " +
                            "C ${off.macrosPer100g.carbsG.roundToInt()} / " +
                            "F ${off.macrosPer100g.fatG.roundToInt()}",
                    color =
                        if (selectedOff?.code == off.code) {
                            GymTokens.ColorActionPrimary
                        } else {
                            GymTokens.ColorTextPrimary
                        },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedOff = off },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2)) {
            FilterChip(selected = unit == AmountUnit.G, onClick = { unit = AmountUnit.G }, label = { Text("g") })
            FilterChip(
                selected = unit == AmountUnit.SERVING,
                onClick = { unit = AmountUnit.SERVING },
                label = { Text("serving") },
            )
        }
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2)) {
            Button(onClick = onDismiss) { Text("Cancel") }
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    if (mealId.isBlank()) return@Button
                    if (tab == "mine") {
                        val food = selectedFood ?: ownResults.firstOrNull() ?: return@Button
                        onAddOwn(food.id, mealId, amt, unit)
                    } else {
                        val off = selectedOff ?: offResults.firstOrNull() ?: return@Button
                        onImportOff(off, mealId, amt, unit)
                    }
                },
                enabled = !busy && mealId.isNotBlank(),
            ) {
                Text(if (tab == "mine") "Save" else "Import & add")
            }
        }
    }
}
