package com.tornus.pro_sklad.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tornus.pro_sklad.domain.model.Product
import com.tornus.pro_sklad.domain.model.WarehouseSummary
import com.tornus.pro_sklad.domain.repository.MovementRepository
import com.tornus.pro_sklad.domain.repository.ProductRepository
import com.tornus.pro_sklad.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val summary: WarehouseSummary? = null,
    val lowStockProducts: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val syncSuccess: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val movementRepository: MovementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadSummary()
        observeLowStock()
    }

    private fun loadSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = movementRepository.getSummary()) {
                is Result.Success -> _uiState.update {
                    it.copy(summary = result.data, isLoading = false, error = null)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                else -> {}
            }
        }
    }

    private fun observeLowStock() {
        viewModelScope.launch {
            productRepository.getLowStockProducts().collect { products ->
                _uiState.update { it.copy(lowStockProducts = products) }
            }
        }
    }

    fun sync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val productsResult = productRepository.syncProducts()
            val movementsResult = movementRepository.syncMovements()
            if (productsResult is Result.Error) {
                _uiState.update { it.copy(isLoading = false, error = productsResult.message) }
                return@launch
            }
            if (movementsResult is Result.Error) {
                _uiState.update { it.copy(isLoading = false, error = movementsResult.message) }
                return@launch
            }
            loadSummary()
            _uiState.update { it.copy(isLoading = false, syncSuccess = true) }
        }
    }

    fun clearSyncFlag() = _uiState.update { it.copy(syncSuccess = false) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}
