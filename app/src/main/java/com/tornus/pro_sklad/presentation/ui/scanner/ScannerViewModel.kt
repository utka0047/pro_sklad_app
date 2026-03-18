package com.tornus.pro_sklad.presentation.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tornus.pro_sklad.domain.repository.ProductRepository
import com.tornus.pro_sklad.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToProduct: Int? = null
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onBarcodeScanned(sku: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = productRepository.getProductBySku(sku)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, navigateToProduct = result.data.id)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = "Товар не найден: $sku")
                }
                else -> {}
            }
        }
    }

    fun toggleFlash() { /* Flash toggle handled via CameraControl */ }
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearNavigation() = _uiState.update { it.copy(navigateToProduct = null) }
}
