package com.tornus.pro_sklad.presentation.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.tornus.pro_sklad.R
import com.tornus.pro_sklad.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnScan.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scanner)
        }
        binding.btnIncome.setOnClickListener {
            findNavController().navigate(
                R.id.action_home_to_movement,
                Bundle().apply {
                    putString("movementType", "IN")
                    putInt("productId", -1)
                }
            )
        }
        binding.btnOutcome.setOnClickListener {
            findNavController().navigate(
                R.id.action_home_to_movement,
                Bundle().apply {
                    putString("movementType", "OUT")
                    putInt("productId", -1)
                }
            )
        }
        binding.btnInventory.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_inventory)
        }
        binding.fabSync.setOnClickListener {
            viewModel.sync()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                state.summary?.let { summary ->
                    binding.tvTotalProducts.text = summary.totalProducts.toString()
                    binding.tvLowStockCount.text = summary.lowStockCount.toString()
                    binding.tvMovementsToday.text = summary.movementsToday.toString()
                    val stockValue = summary.totalStockValue.toDoubleOrNull()?.toLong()
                    binding.tvStockValue.text = if (stockValue != null) "$stockValue ₽" else "${summary.totalStockValue} ₽"
                }

                state.error?.let { error ->
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }

                if (state.syncSuccess) {
                    Snackbar.make(binding.root, "Данные синхронизированы", Snackbar.LENGTH_SHORT).show()
                    viewModel.clearSyncFlag()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
