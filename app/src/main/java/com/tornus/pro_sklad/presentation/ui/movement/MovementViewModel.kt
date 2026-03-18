package com.tornus.pro_sklad.presentation.ui.movement

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

data class MovementUiState(
    val product: Product? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class MovementViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val movementRepository: MovementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovementUiState())
    val uiState: StateFlow<MovementUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: Int) {
        if (productId == -1) return
        viewModelScope.launch {
            when (val result = productRepository.getProductById(productId)) {
                is Result.Success -> _uiState.update { it.copy(product = result.data) }
                is Result.Error -> _uiState.update { it.copy(error = result.message) }
                else -> {}
            }
        }
    }

    fun submit(productId: Int, movementType: MovementType, quantity: Double, comment: String?) {
        if (quantity <= 0) {
            _uiState.update { it.copy(error = "Введите количество больше 0") }
            return
        }
        val currentStock = _uiState.value.product?.currentStock?.toDoubleOrNull() ?: 0.0
        if (movementType == MovementType.OUT && quantity > currentStock) {
            _uiState.update { it.copy(error = "Недостаточно остатков (доступно: $currentStock)") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = movementRepository.createMovement(productId, movementType, quantity, comment)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, success = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearSuccess() = _uiState.update { it.copy(success = false) }
}
