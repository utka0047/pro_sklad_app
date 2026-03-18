package com.tornus.pro_sklad.presentation.ui.movement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.*
import com.google.android.material.snackbar.Snackbar
import com.tornus.pro_sklad.R
import com.tornus.pro_sklad.databinding.FragmentMovementBinding
import com.tornus.pro_sklad.domain.model.MovementType
import com.tornus.pro_sklad.worker.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MovementFragment : Fragment() {

    private var _binding: FragmentMovementBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MovementViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMovementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val movementTypeStr = arguments?.getString("movementType") ?: "IN"
        val productIdArg = arguments?.getInt("productId", -1) ?: -1
        val movementType = MovementType.valueOf(movementTypeStr)
        binding.tvOperationType.text = movementType.label
        binding.toolbar.title = movementType.label

        if (productIdArg != -1) {
            viewModel.loadProduct(productIdArg)
        }

        setupListeners(movementType, productIdArg)
        observeState()
    }

    private fun setupListeners(movementType: MovementType, productIdArg: Int) {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.btnCancel.setOnClickListener { findNavController().popBackStack() }

        binding.btnConfirm.setOnClickListener {
            val qty = binding.etQuantity.text.toString().toDoubleOrNull()
            if (qty == null || qty <= 0) {
                binding.etQuantity.error = "Введите количество"
                return@setOnClickListener
            }
            val productId = if (productIdArg != -1) productIdArg
            else {
                Snackbar.make(binding.root, "Выберите товар", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val comment = binding.etComment.text.toString().takeIf { it.isNotBlank() }
            viewModel.submit(productId, movementType, qty, comment)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                state.product?.let { product ->
                    binding.tvProductName.text = product.name
                    binding.tvProductSku.text = "SKU: ${product.sku}"
                    binding.tvCurrentStock.text = "Остаток: ${product.currentStock} ${product.unit}"
                }

                state.error?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }

                if (state.success) {
                    Snackbar.make(binding.root, "Операция выполнена", Snackbar.LENGTH_SHORT).show()
                    viewModel.clearSuccess()
                    enqueueSync()
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun enqueueSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(requireContext()).enqueueUniqueWork(
            SyncWorker.WORK_NAME_ONE_TIME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
