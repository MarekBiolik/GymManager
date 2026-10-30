package com.gymmanager.android.nutrition.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.gymmanager.android.sync.HelloSyncViewModel
import com.gymmanager.android.theme.GymTokens
import com.gymmanager.android.ui.AccountSettingsSection
import com.gymmanager.android.ui.HelloSyncScreen
import com.gymmanager.core.application.account.DeleteAccount
import com.gymmanager.core.domain.MealType

@Composable
fun NutritionSettingsScreen(
    viewModel: NutritionSettingsViewModel,
    helloSyncViewModel: HelloSyncViewModel,
    deleteAccount: DeleteAccount,
    onAccountDeleted: () -> Unit,
    onSignOut: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    var kcal by remember(settings) { mutableStateOf(settings?.calorieTargetKcal?.toString() ?: "2000") }
    var protein by remember(settings) { mutableStateOf(settings?.proteinTargetG?.toString() ?: "150") }
    var carbs by remember(settings) { mutableStateOf(settings?.carbsTargetG?.toString() ?: "200") }
    var fat by remember(settings) { mutableStateOf(settings?.fatTargetG?.toString() ?: "65") }
    var height by remember(settings) { mutableStateOf(settings?.heightCm?.toString().orEmpty()) }
    var water by remember(settings) { mutableStateOf(settings?.waterTargetMl?.toString().orEmpty()) }
    var weight by remember { mutableStateOf("") }
    var showDevSync by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(GymTokens.Space4),
        verticalArrangement = Arrangement.spacedBy(GymTokens.Space3),
    ) {
        Text("Settings", color = GymTokens.ColorTextPrimary, fontWeight = FontWeight.SemiBold)
        NutritionStatusBanner(state.banner)
        Text("Nutrition targets", color = GymTokens.ColorTextPrimary)
        OutlinedTextField(value = kcal, onValueChange = { kcal = it }, label = { Text("Calories (kcal)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs (g)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (cm)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = water, onValueChange = { water = it }, label = { Text("Water target (ml)") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                viewModel.saveSettings(
                    kcal = kcal.toDoubleOrNull() ?: 0.0,
                    protein = protein.toDoubleOrNull() ?: 0.0,
                    carbs = carbs.toDoubleOrNull() ?: 0.0,
                    fat = fat.toDoubleOrNull() ?: 0.0,
                    heightCm = height.toDoubleOrNull(),
                    waterMl = water.toDoubleOrNull(),
                )
            },
            enabled = !state.busy && state.banner.online,
        ) {
            Text("Save nutrition settings")
        }
        Text("Body weight", color = GymTokens.ColorTextPrimary)
        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                viewModel.logWeightKg(weight)
                weight = ""
            },
            enabled = !state.busy && state.banner.online,
        ) {
            Text("Log weight")
        }
        MealTypesSection(
            mealTypes = state.mealTypes,
            busy = state.busy,
            online = state.banner.online,
            onAdd = viewModel::addMealType,
            onRename = viewModel::renameMealType,
            onDelete = viewModel::deleteMealType,
            onMove = viewModel::moveMealType,
        )
        Button(onClick = { showDevSync = !showDevSync }) {
            Text(if (showDevSync) "Hide dev sync" else "Dev sync")
        }
        if (showDevSync) {
            HelloSyncScreen(viewModel = helloSyncViewModel)
        }
        AccountSettingsSection(deleteAccount = deleteAccount, onDeleted = onAccountDeleted)
        Button(onClick = onSignOut, modifier = Modifier.padding(top = GymTokens.Space2)) {
            Text("Sign out")
        }
    }
}

@Composable
private fun MealTypesSection(
    mealTypes: List<MealType>,
    busy: Boolean,
    online: Boolean,
    onAdd: (String) -> Unit,
    onRename: (MealType, String) -> Unit,
    onDelete: (MealType) -> Unit,
    onMove: (MealType, Int) -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    Text("Meal types", color = GymTokens.ColorTextPrimary)
    mealTypes.forEach { meal ->
        var editName by remember(meal.id) { mutableStateOf(meal.name) }
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = GymTokens.Space1)) {
            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text("Meal name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(GymTokens.Space2)) {
                Button(onClick = { onRename(meal, editName) }, enabled = !busy && online) { Text("Save") }
                Button(onClick = { onMove(meal, -1) }, enabled = !busy && online) { Text("Up") }
                Button(onClick = { onMove(meal, 1) }, enabled = !busy && online) { Text("Down") }
                Button(onClick = { onDelete(meal) }, enabled = !busy && online) { Text("Delete") }
            }
        }
    }
    OutlinedTextField(
        value = newName,
        onValueChange = { newName = it },
        label = { Text("New meal type") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy,
    )
    Button(
        onClick = {
            onAdd(newName)
            newName = ""
        },
        enabled = !busy && online && newName.isNotBlank(),
    ) {
        Text("Add meal type")
    }
}
