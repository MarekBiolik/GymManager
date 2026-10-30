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
import com.gymmanager.android.nutrition.toIsoDay
import com.gymmanager.android.theme.GymTokens
import com.gymmanager.core.data.off.OffProductCandidate
import com.gymmanager.core.domain.AmountUnit
import com.gymmanager.core.domain.Food
import kotlin.math.roundToInt

@Composable
fun DiaryScreen(viewModel: DiaryViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var showCopy by remember { mutableStateOf(false) }

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
                val selected = iso == state.selectedDay
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.selectDay(iso) },
                    label = { Text(dayLabel(parseIsoDay(iso))) },
                )
            }
        }
        MacroGoalSummary(state.consumed, state.settings, state.goalProgress)
        WaterSection(
            totalMl = state.waterTotalMl,
            targetMl = state.settings?.waterTargetMl,
            entries = state.waterEntries,
            busy = state.busy,
            online = state.banner.online,
            onAdd = viewModel::addWater,
            onDelete = viewModel::deleteWater,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2)) {
            Button(onClick = { showAdd = true }, enabled = !state.busy && state.banner.online) {
                Text("Add entry")
            }
            Button(onClick = { showCopy = true }, enabled = !state.busy && state.banner.online) {
                Text("Copy day")
            }
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
                onCopyMealFromYesterday = {
                    val yesterday = parseIsoDay(state.selectedDay).minusDays(1).toIsoDay()
                    viewModel.copyMeal(yesterday, meal.id)
                },
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
    if (showCopy) {
        CopyDayDialog(
            targetDay = state.selectedDay,
            onDismiss = { showCopy = false },
            onCopy = { from ->
                viewModel.copyDay(from)
                showCopy = false
            },
        )
    }
}

@Composable
private fun MealSection(
    title: String,
    entries: List<com.gymmanager.core.domain.FoodEntry>,
    foodName: (String?) -> String,
    busy: Boolean,
    online: Boolean,
    onDelete: (com.gymmanager.core.domain.FoodEntry) -> Unit,
    onCopyMealFromYesterday: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(GymTokens.ColorBgSurface)
                .padding(GymTokens.Space3),
        verticalArrangement = Arrangement.spacedBy(GymTokens.Space2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title, color = GymTokens.ColorTextBrand, fontWeight = FontWeight.SemiBold)
            Button(
                onClick = onCopyMealFromYesterday,
                enabled = !busy && online,
            ) {
                Text("Copy from yesterday")
            }
        }
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
private fun WaterSection(
    totalMl: Double,
    targetMl: Double?,
    entries: List<com.gymmanager.core.domain.WaterEntry>,
    busy: Boolean,
    online: Boolean,
    onAdd: (Double) -> Unit,
    onDelete: (com.gymmanager.core.domain.WaterEntry) -> Unit,
) {
    var amount by remember { mutableStateOf("250") }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(GymTokens.ColorBgSurfaceElevated)
                .padding(GymTokens.Space3),
    ) {
        val targetText = targetMl?.let { " / ${it.roundToInt()} ml" }.orEmpty()
        Text(
            text = "Water · ${totalMl.roundToInt()} ml$targetText",
            color = GymTokens.ColorTextPrimary,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = GymTokens.Space2),
            horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2),
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("ml") },
                modifier = Modifier.weight(1f),
                enabled = !busy,
            )
            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let { onAdd(it) }
                },
                enabled = !busy && online,
            ) {
                Text("Add")
            }
        }
        entries.forEach { w ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${w.amountMl.roundToInt()} ml", color = GymTokens.ColorTextSecondary)
                Text(
                    text = "Remove",
                    color = GymTokens.ColorActionSecondary,
                    modifier =
                        Modifier.clickable(enabled = !busy && online) {
                            onDelete(w)
                        },
                )
            }
        }
    }
}

@Composable
private fun AddEntryDialog(
    mealTypes: List<com.gymmanager.core.domain.MealType>,
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
            FilterChip(selected = tab == "off", onClick = { tab = "off" }, label = { Text("Open Food Facts") })
        }
        mealTypes.forEach { meal ->
            FilterChip(
                selected = mealId == meal.id,
                onClick = { mealId = meal.id },
                label = { Text(meal.name) },
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                selectedFood = null
                if (tab == "off") onSearchOff(it)
            },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        )
        if (tab == "off") {
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("Barcode") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            )
            Button(onClick = { onBarcode(barcode) }, enabled = !busy) { Text("Lookup barcode") }
        }
        if (tab == "mine") {
            ownResults.take(8).forEach { food ->
                Text(
                    text = food.name,
                    color = if (selectedFood?.id == food.id) GymTokens.ColorActionPrimary else GymTokens.ColorTextPrimary,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedFood = food },
                )
            }
        } else {
            offResults.forEach { off ->
                Text(
                    text = off.name,
                    color = GymTokens.ColorTextPrimary,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedFood = null
                                query = off.name
                            },
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
                        val off = offResults.firstOrNull { it.name == query } ?: offResults.firstOrNull()
                        if (off != null) onImportOff(off, mealId, amt, unit)
                    }
                },
                enabled = !busy && mealId.isNotBlank(),
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun CopyDayDialog(
    targetDay: String,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
) {
    var fromDay by remember { mutableStateOf("") }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(GymTokens.ColorBgSurfaceElevated)
                .padding(GymTokens.Space4),
        verticalArrangement = Arrangement.spacedBy(GymTokens.Space2),
    ) {
        Text("Copy entries to $targetDay", color = GymTokens.ColorTextPrimary)
        OutlinedTextField(
            value = fromDay,
            onValueChange = { fromDay = it },
            label = { Text("From date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2)) {
            Button(onClick = onDismiss) { Text("Cancel") }
            Button(onClick = { onCopy(fromDay.trim()) }) { Text("Copy") }
        }
    }
}
