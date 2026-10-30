package com.gymmanager.android.nutrition.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gymmanager.android.nutrition.NutritionBannerState
import com.gymmanager.android.nutrition.errorMessageOrNull
import com.gymmanager.android.nutrition.unwrap
import com.gymmanager.android.sync.AndroidNetworkStatus
import com.gymmanager.core.GymManagerServices
import com.gymmanager.core.data.off.OffProductCandidate
import com.gymmanager.core.data.off.toFoodDraft
import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.FoodServing
import com.gymmanager.core.domain.Macros
import com.gymmanager.core.sync.WriteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoodsUiState(
    val foods: List<Food> = emptyList(),
    val servings: List<FoodServing> = emptyList(),
    val banner: NutritionBannerState = NutritionBannerState(),
    val busy: Boolean = false,
    val offResults: List<OffProductCandidate> = emptyList(),
)

class FoodsViewModel(
    private val userId: String,
    private val services: GymManagerServices,
    private val network: AndroidNetworkStatus,
    private val nowEpochMs: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(FoodsUiState())
    val uiState: StateFlow<FoodsUiState> = _uiState.asStateFlow()

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
            val foods = services.foods.list(userId).unwrap { cacheBanner = it ?: cacheBanner }.orEmpty()
            _uiState.update {
                it.copy(
                    foods = foods,
                    busy = false,
                    banner = NutritionBannerState(cacheBanner = cacheBanner, online = network.isOnline()),
                )
            }
        }
    }

    fun loadServings(foodId: String) {
        viewModelScope.launch {
            val servings = services.foods.listServings(foodId).unwrap { }.orEmpty()
            _uiState.update { it.copy(servings = servings) }
        }
    }

    fun saveFood(
        existing: Food?,
        name: String,
        protein: Double,
        carbs: Double,
        fat: Double,
        defaultServingG: Double?,
    ) {
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val now = nowEpochMs()
            val draft =
                (existing ?: blankFood(now)).copy(
                    name = name.trim(),
                    macrosPer100g = Macros(protein, carbs, fat),
                    defaultServingG = defaultServingG,
                    isRecipe = false,
                    updatedAtEpochMs = now,
                )
            when (val result = services.foods.upsertFood(draft)) {
                is WriteResult.Success -> refresh()
                else -> {
                    _uiState.update { it.copy(busy = false) }
                    setError(result.errorMessageOrNull())
                }
            }
        }
    }

    fun toggleFavorite(food: Food) {
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            when (val result = services.foods.upsertFood(food.copy(isFavorite = !food.isFavorite))) {
                is WriteResult.Success -> refresh()
                else -> {
                    _uiState.update { it.copy(busy = false) }
                    setError(result.errorMessageOrNull())
                }
            }
        }
    }

    fun deleteFood(food: Food) {
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            when (val result = services.foods.deleteFood(userId, food.id)) {
                is WriteResult.Success -> {
                    _uiState.update { it.copy(servings = emptyList()) }
                    refresh()
                }
                else -> {
                    _uiState.update { it.copy(busy = false) }
                    setError(result.errorMessageOrNull())
                }
            }
        }
    }

    fun upsertServing(
        foodId: String,
        existing: FoodServing?,
        label: String,
        grams: Double,
        position: Int,
    ) {
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val now = nowEpochMs()
            val draft =
                existing?.copy(label = label, grams = grams, position = position)
                    ?: FoodServing(
                        id = "",
                        userId = userId,
                        foodId = foodId,
                        label = label,
                        grams = grams,
                        position = position,
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now,
                    )
            when (val result = services.foods.upsertServing(draft)) {
                is WriteResult.Success -> {
                    loadServings(foodId)
                    _uiState.update { it.copy(busy = false) }
                }
                else -> {
                    _uiState.update { it.copy(busy = false) }
                    setError(result.errorMessageOrNull())
                }
            }
        }
    }

    fun deleteServing(
        foodId: String,
        servingId: String,
    ) {
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            when (val result = services.foods.deleteServing(servingId)) {
                is WriteResult.Success -> {
                    loadServings(foodId)
                    _uiState.update { it.copy(busy = false) }
                }
                else -> {
                    _uiState.update { it.copy(busy = false) }
                    setError(result.errorMessageOrNull())
                }
            }
        }
    }

    fun searchOff(query: String) {
        viewModelScope.launch {
            try {
                val results = services.offProducts.search(query)
                _uiState.update { it.copy(offResults = results) }
            } catch (e: Exception) {
                setError(e.message ?: "OFF search failed")
            }
        }
    }

    fun lookupBarcode(code: String) {
        viewModelScope.launch {
            try {
                val hit = services.offProducts.lookupBarcode(code)
                _uiState.update { it.copy(offResults = listOfNotNull(hit)) }
            } catch (e: Exception) {
                setError(e.message ?: "Barcode lookup failed")
            }
        }
    }

    fun importOff(candidate: OffProductCandidate) {
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val now = nowEpochMs()
            when (val result = services.foods.upsertFood(candidate.toFoodDraft(userId, now))) {
                is WriteResult.Success -> refresh()
                else -> {
                    _uiState.update { it.copy(busy = false) }
                    setError(result.errorMessageOrNull())
                }
            }
        }
    }

    private fun blankFood(now: Long): Food =
        Food(
            id = "",
            userId = userId,
            name = "",
            macrosPer100g = Macros.zero(),
            defaultServingG = 100.0,
            isRecipe = false,
            isFavorite = false,
            lastUsedAtEpochMs = null,
            externalSource = null,
            externalId = null,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )

    private fun setError(message: String?) {
        _uiState.update {
            it.copy(banner = it.banner.copy(errorMessage = message, online = network.isOnline()))
        }
    }
}

class FoodsViewModelFactory(
    private val userId: String,
    private val services: GymManagerServices,
    private val network: AndroidNetworkStatus,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FoodsViewModel::class.java)) {
            return FoodsViewModel(userId, services, network) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
