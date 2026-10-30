package com.gymmanager.android.nutrition.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gymmanager.android.nutrition.NutritionBannerState
import com.gymmanager.android.nutrition.parseIsoDay
import com.gymmanager.android.nutrition.todayIso
import com.gymmanager.android.nutrition.toIsoDay
import com.gymmanager.android.nutrition.unwrap
import com.gymmanager.android.sync.AndroidNetworkStatus
import com.gymmanager.core.GymManagerServices
import com.gymmanager.core.domain.DailyMacroTotals
import com.gymmanager.core.domain.Measurement
import com.gymmanager.core.domain.NutritionAggregations
import com.gymmanager.core.sync.ReadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChartsUiState(
    val dailyTotals: List<DailyMacroTotals> = emptyList(),
    val weightPoints: List<Measurement> = emptyList(),
    val banner: NutritionBannerState = NutritionBannerState(),
    val busy: Boolean = false,
)

class ChartsViewModel(
    private val userId: String,
    private val services: GymManagerServices,
    private val network: AndroidNetworkStatus,
    private val dayRange: Int = 14,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChartsUiState())
    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()

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
            val today = parseIsoDay(todayIso())
            val from = today.minusDays(dayRange.toLong() - 1).toIsoDay()
            val to = today.toIsoDay()
            val entries =
                when (val result = services.foodEntries.listRange(userId, from, to)) {
                    is ReadResult.Fresh -> result.data
                    is ReadResult.Stale -> {
                        cacheBanner = "Showing cached data (last updated ${result.fetchedAtEpochMs})"
                        result.data
                    }
                    is ReadResult.Empty -> emptyList()
                }
            val daily = NutritionAggregations.dailyTotals(entries)
            val measurements =
                services.measurements.list(userId).unwrap { msg ->
                    if (msg != null) cacheBanner = msg
                }.orEmpty()
            val weights =
                measurements
                    .filter { it.metric == "body_weight_kg" }
                    .sortedBy { it.measuredAtEpochMs }
            _uiState.update {
                it.copy(
                    dailyTotals = daily,
                    weightPoints = weights,
                    busy = false,
                    banner = NutritionBannerState(cacheBanner = cacheBanner, online = network.isOnline()),
                )
            }
        }
    }

    fun maxKcal(): Double =
        _uiState.value.dailyTotals.maxOfOrNull { it.macros.kcal }?.coerceAtLeast(1.0) ?: 1.0
}

class ChartsViewModelFactory(
    private val userId: String,
    private val services: GymManagerServices,
    private val network: AndroidNetworkStatus,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChartsViewModel::class.java)) {
            return ChartsViewModel(userId, services, network) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
