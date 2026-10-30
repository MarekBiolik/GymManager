package com.gymmanager.android.nutrition.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gymmanager.android.nutrition.NutritionBannerState
import com.gymmanager.android.nutrition.errorMessageOrNull
import com.gymmanager.android.nutrition.parseIsoDay
import com.gymmanager.android.nutrition.toIsoDay
import com.gymmanager.android.nutrition.todayIso
import com.gymmanager.android.nutrition.unwrap
import com.gymmanager.android.nutrition.weekStrip
import com.gymmanager.android.sync.AndroidNetworkStatus
import com.gymmanager.core.GymManagerServices
import com.gymmanager.core.data.off.OffProductCandidate
import com.gymmanager.core.data.off.toFoodDraft
import com.gymmanager.core.domain.AmountUnit
import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.FoodEntry
import com.gymmanager.core.domain.GoalProgress
import com.gymmanager.core.domain.Macros
import com.gymmanager.core.domain.MealType
import com.gymmanager.core.domain.NutritionSettings
import com.gymmanager.core.sync.WriteResult
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiaryUiState(
    val selectedDay: String = todayIso(),
    val weekDays: List<String> = weekStrip(LocalDate.now()).map { it.toIsoDay() },
    val mealTypes: List<MealType> = emptyList(),
    val entries: List<FoodEntry> = emptyList(),
    val foods: List<Food> = emptyList(),
    val settings: NutritionSettings? = null,
    val consumed: Macros = Macros.zero(),
    val goalProgress: GoalProgress? = null,
    val banner: NutritionBannerState = NutritionBannerState(),
    val busy: Boolean = false,
    val offResults: List<OffProductCandidate> = emptyList(),
)

class DiaryViewModel(
    private val userId: String,
    private val services: GymManagerServices,
    private val network: AndroidNetworkStatus,
    private val nowEpochMs: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectDay(isoDay: String) {
        _uiState.update {
            val date = parseIsoDay(isoDay)
            it.copy(selectedDay = isoDay, weekDays = weekStrip(date).map { d -> d.toIsoDay() })
        }
        refresh()
    }

    fun shiftWeek(days: Long) {
        val date = parseIsoDay(_uiState.value.selectedDay).plusDays(days)
        selectDay(date.toIsoDay())
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

            fun noteBanner(msg: String?) {
                if (msg != null && cacheBanner == null) cacheBanner = msg
            }

            val mealTypes = services.mealTypes.list(userId).unwrap(::noteBanner).orEmpty()
            val day = _uiState.value.selectedDay
            val entries = services.foodEntries.listDay(userId, day).unwrap(::noteBanner).orEmpty()
            val foods = services.foods.list(userId).unwrap(::noteBanner).orEmpty()
            val settings = services.nutritionSettings.read(userId).unwrap(::noteBanner)
            val consumed = services.foodEntries.sumMacros(entries)
            val progress = settings?.let { GoalProgress.of(consumed, it) }

            _uiState.update {
                it.copy(
                    mealTypes = mealTypes,
                    entries = entries,
                    foods = foods,
                    settings = settings,
                    consumed = consumed,
                    goalProgress = progress,
                    busy = false,
                    banner =
                        NutritionBannerState(
                            cacheBanner = cacheBanner,
                            online = network.isOnline(),
                        ),
                )
            }
        }
    }

    fun foodName(foodId: String?): String =
        foodId?.let { id -> _uiState.value.foods.firstOrNull { it.id == id }?.name } ?: "Food"

    fun entriesForMeal(mealTypeId: String): List<FoodEntry> =
        _uiState.value.entries.filter { it.mealTypeId == mealTypeId }

    fun deleteEntry(entry: FoodEntry) {
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            when (
                val result =
                    services.foodEntries.deleteEntry(userId, entry.eatenOn, entry.id)
            ) {
                is WriteResult.Success -> refresh()
                else -> {
                    _uiState.update { it.copy(busy = false) }
                    setError(result.errorMessageOrNull())
                }
            }
        }
    }

    fun addEntry(
        foodId: String,
        mealTypeId: String,
        amount: Double,
        unit: AmountUnit,
        foodServingId: String?,
    ) {
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val day = _uiState.value.selectedDay
            when (
                val result =
                    services.foodEntries.addEntry(
                        userId = userId,
                        foodId = foodId,
                        mealTypeId = mealTypeId,
                        eatenOn = day,
                        amount = amount,
                        unit = unit,
                        foodServingId = foodServingId,
                    )
            ) {
                is WriteResult.Success -> refresh()
                else -> {
                    _uiState.update { it.copy(busy = false) }
                    setError(result.errorMessageOrNull())
                }
            }
        }
    }

    fun searchFoods(query: String): List<Food> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return _uiState.value.foods.take(30)
        return _uiState.value.foods.filter { it.name.lowercase().contains(q) }.take(30)
    }

    fun searchOff(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, offResults = emptyList()) }
            try {
                val results = services.offProducts.search(query)
                _uiState.update { it.copy(offResults = results, busy = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        busy = false,
                        banner = it.banner.copy(errorMessage = e.message ?: "OFF search failed"),
                    )
                }
            }
        }
    }

    fun lookupBarcode(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            try {
                val hit = services.offProducts.lookupBarcode(code)
                _uiState.update {
                    it.copy(
                        offResults = listOfNotNull(hit),
                        busy = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        busy = false,
                        banner = it.banner.copy(errorMessage = e.message ?: "Barcode lookup failed"),
                    )
                }
            }
        }
    }

    fun importOffAndAddEntry(
        candidate: OffProductCandidate,
        mealTypeId: String,
        amount: Double,
        unit: AmountUnit,
    ) {
        if (!network.isOnline()) {
            setError("A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val now = nowEpochMs()
            val existing = services.foods.findByExternal(userId, "openfoodfacts", candidate.code)
            val food =
                existing
                    ?: when (
                        val saved =
                            services.foods.upsertFood(
                                candidate.toFoodDraft(userId, now),
                            )
                    ) {
                        is WriteResult.Success -> saved.data
                        else -> {
                            _uiState.update { it.copy(busy = false) }
                            setError(saved.errorMessageOrNull())
                            return@launch
                        }
                    }
            addEntry(food.id, mealTypeId, amount, unit, foodServingId = null)
        }
    }

    fun clearOffResults() {
        _uiState.update { it.copy(offResults = emptyList()) }
    }

    private fun setError(message: String?) {
        _uiState.update {
            it.copy(banner = it.banner.copy(errorMessage = message, online = network.isOnline()))
        }
    }
}

class DiaryViewModelFactory(
    private val userId: String,
    private val services: GymManagerServices,
    private val network: AndroidNetworkStatus,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiaryViewModel::class.java)) {
            return DiaryViewModel(userId, services, network) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
