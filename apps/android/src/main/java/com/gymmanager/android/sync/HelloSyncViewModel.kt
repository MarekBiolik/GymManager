package com.gymmanager.android.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gymmanager.core.application.measurements.MeasurementRepository
import com.gymmanager.core.application.profiles.ProfileRepository
import com.gymmanager.core.domain.Measurement
import com.gymmanager.core.domain.PreferredUnits
import com.gymmanager.core.domain.Profile
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HelloSyncUiState(
    val userId: String,
    val profile: Profile? = null,
    val measurements: List<Measurement> = emptyList(),
    val banner: String? = null,
    val errorMessage: String? = null,
    val busy: Boolean = false,
    val online: Boolean = true,
)

class HelloSyncViewModel(
    private val userId: String,
    private val profiles: ProfileRepository,
    private val measurements: MeasurementRepository,
    private val network: AndroidNetworkStatus,
    private val nowEpochMs: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(HelloSyncUiState(userId = userId, online = network.isOnline()))
    val uiState: StateFlow<HelloSyncUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, errorMessage = null, online = network.isOnline())
            val profileResult = profiles.readCurrent(userId)
            val listResult = measurements.list(userId)
            var banner: String? = null
            val profile =
                when (profileResult) {
                    is ReadResult.Fresh -> profileResult.data
                    is ReadResult.Stale -> {
                        banner = "Showing cached data (last updated ${profileResult.fetchedAtEpochMs})"
                        profileResult.data
                    }
                    is ReadResult.Empty -> null
                }
            val list =
                when (listResult) {
                    is ReadResult.Fresh -> listResult.data
                    is ReadResult.Stale -> {
                        banner =
                            banner
                                ?: "Showing cached data (last updated ${listResult.fetchedAtEpochMs})"
                        listResult.data
                    }
                    is ReadResult.Empty -> emptyList()
                }
            if (!network.isOnline() && profile == null && list.isEmpty()) {
                banner = "No network — no cached data."
            }
            _uiState.value =
                _uiState.value.copy(
                    profile = profile,
                    measurements = list,
                    banner = banner,
                    busy = false,
                    online = network.isOnline(),
                )
        }
    }

    fun addMeasurement(metric: String, valueText: String) {
        val value = valueText.toDoubleOrNull()
        if (value == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter a numeric value.")
            return
        }
        if (!network.isOnline()) {
            _uiState.value =
                _uiState.value.copy(errorMessage = "A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, errorMessage = null)
            val now = nowEpochMs()
            val draft =
                Measurement(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    metric = metric.ifBlank { "weight" },
                    value = value,
                    measuredAtEpochMs = now,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                )
            when (val result = measurements.insert(draft)) {
                is WriteResult.OfflineRejected ->
                    _uiState.value =
                        _uiState.value.copy(
                            busy = false,
                            errorMessage = "A network connection is required to save.",
                        )
                is WriteResult.Failed ->
                    _uiState.value =
                        _uiState.value.copy(busy = false, errorMessage = result.message)
                is WriteResult.Success -> {
                    _uiState.value = _uiState.value.copy(busy = false)
                    refresh()
                }
            }
        }
    }

    fun deleteMeasurement(id: String) {
        if (!network.isOnline()) {
            _uiState.value =
                _uiState.value.copy(errorMessage = "A network connection is required to save.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, errorMessage = null)
            when (val result = measurements.delete(userId, id)) {
                is WriteResult.OfflineRejected ->
                    _uiState.value =
                        _uiState.value.copy(
                            busy = false,
                            errorMessage = "A network connection is required to save.",
                        )
                is WriteResult.Failed ->
                    _uiState.value =
                        _uiState.value.copy(busy = false, errorMessage = result.message)
                is WriteResult.Success -> {
                    _uiState.value = _uiState.value.copy(busy = false)
                    refresh()
                }
            }
        }
    }

    fun preferredUnitsLabel(): String =
        when (_uiState.value.profile?.preferredUnits) {
            PreferredUnits.KG -> "kg"
            PreferredUnits.LB -> "lb"
            null -> "—"
        }

    fun onLogout() {
        profiles.onLogout(userId)
        measurements.onLogout(userId)
    }
}

class HelloSyncViewModelFactory(
    private val userId: String,
    private val profiles: ProfileRepository,
    private val measurements: MeasurementRepository,
    private val network: AndroidNetworkStatus,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HelloSyncViewModel::class.java)) {
            return HelloSyncViewModel(userId, profiles, measurements, network) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
