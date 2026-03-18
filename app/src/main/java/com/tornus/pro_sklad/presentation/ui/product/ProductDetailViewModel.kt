package com.tornus.pro_sklad.presentation.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tornus.pro_sklad.domain.model.Movement
import com.tornus.pro_sklad.domain.model.Product
import com.tornus.pro_sklad.domain.repository.MovementRepository
import com.tornus.pro_sklad.domain.repository.ProductRepository
import com.tornus.pro_sklad.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val product: Product? = null,
    val movements: List<Movement> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val movementRepository: MovementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = productRepository.getProductById(productId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(product = result.data, isLoading = false) }
                    observeMovements(productId)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                else -> {}
            }
        }
    }

    private fun observeMovements(productId: Int) {
        viewModelScope.launch {
            movementRepository.getMovementsForProduct(productId).collect { movements ->
                _uiState.update { it.copy(movements = movements.take(10)) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
