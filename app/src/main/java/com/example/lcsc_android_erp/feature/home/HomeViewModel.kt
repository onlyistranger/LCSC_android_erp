package com.example.lcsc_android_erp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.lcsc_android_erp.core.AppContainer
import com.example.lcsc_android_erp.core.datastore.UserPreferencesRepository
import com.example.lcsc_android_erp.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    inventoryRepository: InventoryRepository,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        inventoryRepository.observeDashboardSummary(),
        inventoryRepository.observeStorageLocations(),
        userPreferencesRepository.preferences
    ) { summary, locations, preferences ->
        HomeUiState(
            summary = summary,
            locations = locations,
            defaultLocationCode = preferences.defaultLocationCode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    init {
        viewModelScope.launch {
            inventoryRepository.bootstrapDefaults()
        }
    }

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    inventoryRepository = appContainer.inventoryRepository,
                    userPreferencesRepository = appContainer.userPreferencesRepository
                )
            }
        }
    }
}
