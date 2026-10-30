package com.gymmanager.android.nutrition.foods

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.gymmanager.android.nutrition.NutritionStatusBanner
import com.gymmanager.android.theme.GymTokens
import com.gymmanager.core.data.off.OffProductCandidate
import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.FoodServing
import kotlin.math.roundToInt

@Composable
fun FoodsScreen(viewModel: FoodsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(FoodsMode.List) }
    var editing by remember { mutableStateOf<Food?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = GymTokens.Space4),
        verticalArrangement = Arrangement.spacedBy(GymTokens.Space3),
    ) {
        Text("Foods", color = GymTokens.ColorTextPrimary, fontWeight = FontWeight.SemiBold)
        NutritionStatusBanner(state.banner)
        Row(horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2)) {
            FilterChip(
                selected = mode == FoodsMode.List,
                onClick = { mode = FoodsMode.List },
                label = { Text("Library") },
            )
            FilterChip(
                selected = mode == FoodsMode.Edit,
                onClick = {
                    mode = FoodsMode.Edit
                    editing = null
                },
                label = { Text("Create / edit") },
            )
            FilterChip(
                selected = mode == FoodsMode.Recipe,
                onClick = { mode = FoodsMode.Recipe },
                label = { Text("Recipe") },
            )
        }
        when (mode) {
            FoodsMode.List ->
                FoodList(
                    foods = state.foods,
                    busy = state.busy,
                    online = state.banner.online,
                    onEdit = {
                        editing = it
                        mode = FoodsMode.Edit
                        viewModel.loadServings(it.id)
                        if (it.isRecipe) {
                            viewModel.loadRecipeIngredients(it.id)
                        }
                    },
                    onFavorite = viewModel::toggleFavorite,
                    onDelete = viewModel::deleteFood,
                )
            FoodsMode.Edit ->
                FoodEditor(
                    food = editing,
                    servings = state.servings,
                    recipeIngredients = state.recipeIngredients,
                    componentFoods = state.foods,
                    offResults = state.offResults,
                    busy = state.busy,
                    online = state.banner.online,
                    onSave = { existing, name, p, c, f, serving ->
                        viewModel.saveFood(existing, name, p, c, f, serving)
                        mode = FoodsMode.List
                    },
                    onServing = viewModel::upsertServing,
                    onDeleteServing = viewModel::deleteServing,
                    onSearchOff = viewModel::searchOff,
                    onBarcode = viewModel::lookupBarcode,
                    onImportOff = { candidate ->
                        viewModel.importOff(candidate)
                        mode = FoodsMode.List
                    },
                )
            FoodsMode.Recipe ->
                RecipeBuilder(
                    foods = state.foods.filter { !it.isRecipe },
                    busy = state.busy,
                    online = state.banner.online,
                    onSave = { name, components, serving ->
                        viewModel.saveRecipe(null, name, components, serving)
                        mode = FoodsMode.List
                    },
                )
        }
    }
}

private enum class FoodsMode {
    List,
    Edit,
    Recipe,
}

@Composable
private fun FoodList(
    foods: List<Food>,
    busy: Boolean,
    online: Boolean,
    onEdit: (Food) -> Unit,
    onFavorite: (Food) -> Unit,
    onDelete: (Food) -> Unit,
) {
    if (foods.isEmpty()) {
        Text("No foods yet.", color = GymTokens.ColorTextSecondary)
    }
    foods.forEach { food ->
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onEdit(food) }
                    .padding(vertical = GymTokens.Space2),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${if (food.isFavorite) "★ " else ""}${food.name}",
                    color = GymTokens.ColorTextPrimary,
                )
                Text(
                    text =
                        buildString {
                            append(
                                "P ${food.macrosPer100g.proteinG.roundToInt()} · " +
                                    "C ${food.macrosPer100g.carbsG.roundToInt()} · " +
                                    "F ${food.macrosPer100g.fatG.roundToInt()} / 100g",
                            )
                            if (food.isRecipe) append(" · recipe")
                        },
                    color = GymTokens.ColorTextSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GymTokens.Space1)) {
                Button(onClick = { onFavorite(food) }, enabled = !busy && online) {
                    Text(if (food.isFavorite) "Unfav" else "Fav")
                }
                Button(onClick = { onDelete(food) }, enabled = !busy && online) {
                    Text("Del")
                }
            }
        }
    }
}

@Composable
private fun FoodEditor(
    food: Food?,
    servings: List<FoodServing>,
    recipeIngredients: List<com.gymmanager.core.domain.RecipeIngredient>,
    componentFoods: List<Food>,
    offResults: List<OffProductCandidate>,
    busy: Boolean,
    online: Boolean,
    onSave: (Food?, String, Double, Double, Double, Double?) -> Unit,
    onServing: (String, FoodServing?, String, Double, Int) -> Unit,
    onDeleteServing: (String, String) -> Unit,
    onSearchOff: (String) -> Unit,
    onBarcode: (String) -> Unit,
    onImportOff: (OffProductCandidate) -> Unit,
) {
    var name by remember(food?.id) { mutableStateOf(food?.name.orEmpty()) }
    var protein by remember(food?.id) { mutableStateOf(food?.macrosPer100g?.proteinG?.toString() ?: "0") }
    var carbs by remember(food?.id) { mutableStateOf(food?.macrosPer100g?.carbsG?.toString() ?: "0") }
    var fat by remember(food?.id) { mutableStateOf(food?.macrosPer100g?.fatG?.toString() ?: "0") }
    var serving by remember(food?.id) { mutableStateOf(food?.defaultServingG?.toString() ?: "100") }
    var servingLabel by remember { mutableStateOf("Serving") }
    var servingGrams by remember { mutableStateOf("100") }
    var offQuery by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(GymTokens.Space2)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = protein,
            onValueChange = { protein = it },
            label = { Text("Protein / 100g") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = carbs,
            onValueChange = { carbs = it },
            label = { Text("Carbs / 100g") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = fat,
            onValueChange = { fat = it },
            label = { Text("Fat / 100g") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = serving,
            onValueChange = { serving = it },
            label = { Text("Default serving (g)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                onSave(
                    food,
                    name,
                    protein.toDoubleOrNull() ?: 0.0,
                    carbs.toDoubleOrNull() ?: 0.0,
                    fat.toDoubleOrNull() ?: 0.0,
                    serving.toDoubleOrNull(),
                )
            },
            enabled = !busy && online && name.isNotBlank() && food?.isRecipe != true,
        ) {
            Text("Save food")
        }
        if (food != null && food.isRecipe) {
            Text("Recipe ingredients", color = GymTokens.ColorTextPrimary)
            if (recipeIngredients.isEmpty()) {
                Text("No ingredients loaded.", color = GymTokens.ColorTextSecondary)
            }
            recipeIngredients.forEach { ing ->
                val label = componentFoods.firstOrNull { it.id == ing.componentFoodId }?.name ?: ing.componentFoodId
                Text("$label · ${ing.grams.roundToInt()} g", color = GymTokens.ColorTextSecondary)
            }
        }
        if (food != null && !food.isRecipe) {
            Text("Servings", color = GymTokens.ColorTextPrimary)
            servings.forEach { s ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${s.label}: ${s.grams} g", color = GymTokens.ColorTextSecondary)
                    Button(
                        onClick = { onDeleteServing(food.id, s.id) },
                        enabled = !busy && online,
                    ) {
                        Text("Remove")
                    }
                }
            }
            OutlinedTextField(
                value = servingLabel,
                onValueChange = { servingLabel = it },
                label = { Text("Label") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = servingGrams,
                onValueChange = { servingGrams = it },
                label = { Text("Grams") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    onServing(
                        food.id,
                        null,
                        servingLabel,
                        servingGrams.toDoubleOrNull() ?: return@Button,
                        servings.size,
                    )
                },
                enabled = !busy && online && servingLabel.isNotBlank(),
            ) {
                Text("Add serving")
            }
        }
        if (food?.isRecipe != true) {
            Text("Import from Open Food Facts", color = GymTokens.ColorTextPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                "Data from Open Food Facts — saved as your private food copy.",
                color = GymTokens.ColorTextSecondary,
            )
            OutlinedTextField(
                value = offQuery,
                onValueChange = {
                    offQuery = it
                    onSearchOff(it)
                },
                label = { Text("Search OFF") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            )
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("Barcode (paste)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            )
            Button(onClick = { onBarcode(barcode) }, enabled = !busy) { Text("Lookup barcode") }
            offResults.forEach { off ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(off.name, color = GymTokens.ColorTextPrimary)
                        Text(
                            "P ${off.macrosPer100g.proteinG.roundToInt()} · " +
                                "C ${off.macrosPer100g.carbsG.roundToInt()} · " +
                                "F ${off.macrosPer100g.fatG.roundToInt()} / 100g",
                            color = GymTokens.ColorTextSecondary,
                        )
                    }
                    Button(onClick = { onImportOff(off) }, enabled = !busy && online) {
                        Text("Import")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeBuilder(
    foods: List<Food>,
    busy: Boolean,
    online: Boolean,
    onSave: (String, List<Pair<String, Double>>, Double?) -> Unit,
) {
    var name by remember { mutableStateOf("My recipe") }
    var serving by remember { mutableStateOf("") }
    var pickFoodId by remember { mutableStateOf(foods.firstOrNull()?.id.orEmpty()) }
    var grams by remember { mutableStateOf("100") }
    var components by remember { mutableStateOf(listOf<Pair<String, Double>>()) }

    Column(verticalArrangement = Arrangement.spacedBy(GymTokens.Space2)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Recipe name") },
            modifier = Modifier.fillMaxWidth(),
        )
        foods.forEach { food ->
            FilterChip(
                selected = pickFoodId == food.id,
                onClick = { pickFoodId = food.id },
                label = { Text(food.name) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2)) {
            OutlinedTextField(
                value = grams,
                onValueChange = { grams = it },
                label = { Text("Grams") },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    val g = grams.toDoubleOrNull() ?: return@Button
                    if (pickFoodId.isNotBlank()) {
                        components = components + (pickFoodId to g)
                    }
                },
                enabled = !busy,
            ) {
                Text("Add component")
            }
        }
        components.forEach { (id, g) ->
            val label = foods.firstOrNull { it.id == id }?.name ?: id
            Text("$label · ${g.roundToInt()} g", color = GymTokens.ColorTextSecondary)
        }
        OutlinedTextField(
            value = serving,
            onValueChange = { serving = it },
            label = { Text("Default serving g (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onSave(name, components, serving.toDoubleOrNull()) },
            enabled = !busy && online && components.isNotEmpty() && name.isNotBlank(),
        ) {
            Text("Save recipe")
        }
    }
}
