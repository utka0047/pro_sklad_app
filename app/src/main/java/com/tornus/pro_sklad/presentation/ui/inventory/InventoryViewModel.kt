package com.tornus.pro_sklad.presentation.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tornus.pro_sklad.domain.model.MovementType
import com.tornus.pro_sklad.domain.model.Product
import com.tornus.pro_sklad.domain.repository.MovementRepository
import com.tornus.pro_sklad.domain.repository.ProductRepository
import com.tornus.pro_sklad.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryItem(
    val product: Product,
    val expectedQty: Double,
    val actualQty: Double
) {
    val difference: Double get() = actualQty - expectedQty
}

data class InventoryUiState(
    val items: List<InventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val completed: Boolean = false
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val movementRepository: MovementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    fun onScan(sku: String, actualQty: Double) {
        viewModelScope.launch {
            when (val result = productRepository.getProductBySku(sku)) {
                is Result.Success -> {
                    val product = result.data
                    val expected = product.currentStock.toDoubleOrNull() ?: 0.0
                    val items = _uiState.value.items.toMutableList()
                    val existing = items.indexOfFirst { it.product.id == product.id }
                    val newItem = InventoryItem(product, expected, actualQty)
                    if (existing >= 0) items[existing] = newItem else items.add(newItem)
                    _uiState.update { it.copy(items = items) }
                }
                is Result.Error -> _uiState.update { it.copy(error = result.message) }
                else -> {}
            }
        }
    }

    fun updateActualQty(productId: Int, qty: Double) {
        val items = _uiState.value.items.toMutableList()
        val idx = items.indexOfFirst { it.product.id == productId }
        if (idx >= 0) {
            items[idx] = items[idx].copy(actualQty = qty)
            _uiState.update { it.copy(items = items) }
        }
    }

    fun completeInventory() {
        val items = _uiState.value.items.filter { it.difference != 0.0 }
        if (items.isEmpty()) {
            _uiState.update { it.copy(completed = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            var hasError = false
            for (item in items) {
                val result = movementRepository.createMovement(
                    productId = item.product.id,
                    type = MovementType.INVENTORY,
                    quantity = item.actualQty,
                    comment = "Инвентаризация: факт ${item.actualQty}, ожидалось ${item.expectedQty}"
                )
                if (result is Result.Error) {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    hasError = true
                    break
                }
            }
            if (!hasError) {
                _uiState.update { it.copy(isLoading = false, completed = true) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearCompleted() = _uiState.update { it.copy(completed = false) }
}
