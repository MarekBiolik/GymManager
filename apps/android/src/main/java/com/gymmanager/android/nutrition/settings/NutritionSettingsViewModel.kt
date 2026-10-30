package com.gymmanager.android.nutrition.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gymmanager.android.nutrition.NutritionBannerState
import com.gymmanager.android.nutrition.errorMessageOrNull
import com.gymmanager.android.nutrition.unwrap
import com.gymmanager.android.sync.AndroidNetworkStatus
import com.gymmanager.core.GymManagerServices
import com.gymmanager.core.domain.MealType
import com.gymmanager.core.domain.Measurement
import com.gymmanager.core.domain.NutritionSettings
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NutritionSettingsUiState(
    val settings: NutritionSettings? = null,
    val mealTypes: List<MealType> = emptyList(),
    val banner: NutritionBannerState = NutritionBannerState(),
    val busy: Boolean = false,
)

class NutritionSettingsViewModel(
    private val userId: String,
    private val services: GymManagerServices,
    private val network: AndroidNetworkStatus,
    private val nowEpochMs: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(NutritionSettingsUiState())
    val uiState: StateFlow<NutritionSettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    busy = true,
                    banner = it.banner.copy(errorMessage = null, online = network.isOnline()),
                )
            }
            var cacheBanner: String? = null
            val settingsResult = services.nutritionSettings.read(userId)
            val settings =
                when (settingsResult) {
                    is ReadResult.Fresh -> settingsResult.data
                    is ReadResult.Stale -> {
                        cacheBanner = "Showing cached data (last updated ${settingsResult.fetchedAtEpochMs})"
                        settingsResult.data
                    }
                    is ReadResult.Empty -> null
                }
            val meals =
                services.mealTypes.list(userId).unwrap { msg ->
                    if (msg != null) cacheBanner = msg
                }.orEmpty()
            _uiState.update {
                it.copy(
                    settings = settings,
                    mealTypes = meals,
                    busy = false,
                    banner = NutritionBannerState(cacheBanner = cacheBanner, online = network.isOnline()),
                )
            }
        }
    }

    fun saveSettings(
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        heightCm: Double?,
        waterMl: Double?,
    ) {
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val now = nowEpochMs()
            val existing = _uiState.value.settings
            val draft =
                existing?.copy(
                    calorieTargetKcal = kcal,
                    proteinTargetG = protein,
                    carbsTargetG = carbs,
                    fatTargetG = fat,
                    heightCm = heightCm,
                    waterTargetMl = waterMl,
                    updatedAtEpochMs = now,
                )
                    ?: NutritionSettings(
                        userId = userId,
                        calorieTargetKcal = kcal,
                        proteinTargetG = protein,
                        carbsTargetG = carbs,
                        fatTargetG = fat,
                        heightCm = heightCm,
                        waterTargetMl = waterMl,
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now,
                    )
            when (val result = services.nutritionSettings.upsert(draft)) {
                is WriteResult.Success -> refresh()
                else -> {
                    _uiState.update { it.copy(busy = false) }
                    setError(result.errorMessageOrNull())
                }
            }
        }
    }

    fun addMealType(name: String) {
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val pos = (_uiState.value.mealTypes.maxOfOrNull { it.position } ?: -1) + 1
            when (val result = services.mealTypes.insert(userId, name, pos)) {
                is WriteResult.Success -> refresh()
                else -> {
                    _uiState.update { it.copy(busy = false) }
                    setError(result.errorMessageOrNull())
                }
            }
        }
    }

    fun renameMealType(
        meal: MealType,
        name: String,
    ) {
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            when (val result = services.mealTypes.update(meal.copy(name = name.trim()))) {
                is WriteResult.Success -> refresh()
                else -> {
                    _uiState.update { it.copy(busy = false) }
                    setError(result.errorMessageOrNull())
                }
            }
        }
    }

    fun deleteMealType(meal: MealType) {
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            when (val result = services.mealTypes.delete(userId, meal.id)) {
                is WriteResult.Success -> refresh()
                else -> {
                    _uiState.update { it.copy(busy = false) }
                    setError(result.errorMessageOrNull())
                }
            }
        }
    }

    fun moveMealType(
        meal: MealType,
        delta: Int,
    ) {
        val list = _uiState.value.mealTypes.sortedBy { it.position }
        val index = list.indexOfFirst { it.id == meal.id }
        if (index < 0) return
        val swapIndex = index + delta
        if (swapIndex !in list.indices) return
        val other = list[swapIndex]
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val a = services.mealTypes.update(meal.copy(position = other.position))
            val b = services.mealTypes.update(other.copy(position = meal.position))
            if (a is WriteResult.Success && b is WriteResult.Success) {
                refresh()
            } else {
                _uiState.update { it.copy(busy = false) }
                setError(a.errorMessageOrNull() ?: b.errorMessageOrNull())
            }
        }
    }

    fun logWeightKg(valueText: String) {
        val value = valueText.toDoubleOrNull()
        if (value == null) {
            setError("Enter a numeric weight.")
            return
        }
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val now = nowEpochMs()
            val draft =
                Measurement(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    metric = "body_weight_kg",
                    value = value,
                    measuredAtEpochMs = now,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                )
            when (val result = services.measurements.insert(draft)) {
                is WriteResult.Success -> {
                    _uiState.update { it.copy(busy = false) }
                }
                else -> {
                    _uiState.update { it.copy(busy = false) }
                    setError(result.errorMessageOrNull())
                }
            }
        }
    }

    private fun setError(message: String?) {
        _uiState.update {
            it.copy(banner = it.banner.copy(errorMessage = message, online = network.isOnline()))
        }
    }
}

class NutritionSettingsViewModelFactory(
    private val userId: String,
    private val services: GymManagerServices,
    private val network: AndroidNetworkStatus,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NutritionSettingsViewModel::class.java)) {
            return NutritionSettingsViewModel(userId, services, network) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
