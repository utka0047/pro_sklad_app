package com.tornus.pro_sklad.presentation.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.tornus.pro_sklad.databinding.FragmentProductDetailBinding
import com.tornus.pro_sklad.presentation.adapter.MovementAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductDetailViewModel by viewModels()
    private val movementAdapter = MovementAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvMovements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = movementAdapter
        }

        setupListeners()
        observeState()
        viewModel.loadProduct(arguments?.getInt("productId") ?: 0)
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.btnIncome.setOnClickListener {
            val productId = viewModel.uiState.value.product?.id ?: return@setOnClickListener
            findNavController().navigate(
                com.tornus.pro_sklad.R.id.action_product_detail_to_movement,
                android.os.Bundle().apply {
                    putInt("productId", productId)
                    putString("movementType", "IN")
                }
            )
        }
        binding.btnOutcome.setOnClickListener {
            val productId = viewModel.uiState.value.product?.id ?: return@setOnClickListener
            findNavController().navigate(
                com.tornus.pro_sklad.R.id.action_product_detail_to_movement,
                android.os.Bundle().apply {
                    putInt("productId", productId)
                    putString("movementType", "OUT")
                }
            )
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                state.product?.let { product ->
                    binding.toolbar.title = product.name
                    binding.tvSku.text = "SKU: ${product.sku}"
                    binding.tvCategory.text = product.category ?: "—"
                    binding.tvStock.text = "${product.currentStock} ${product.unit}"
                    binding.tvMinStock.text = "Мин: ${product.minStock} ${product.unit}"
                    binding.tvPrice.text = "${product.price} ₽"
                    binding.tvDescription.text = product.description ?: ""
                    binding.tvDescription.visibility =
                        if (product.description.isNullOrEmpty()) View.GONE else View.VISIBLE

                    if (product.isLowStock) {
                        binding.tvLowStockWarning.visibility = View.VISIBLE
                    }
                }

                movementAdapter.submitList(state.movements)

                state.error?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
