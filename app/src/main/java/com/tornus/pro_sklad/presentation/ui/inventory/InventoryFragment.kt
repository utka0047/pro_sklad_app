package com.tornus.pro_sklad.presentation.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.tornus.pro_sklad.R
import com.tornus.pro_sklad.databinding.FragmentInventoryBinding
import com.tornus.pro_sklad.presentation.adapter.InventoryAdapter
import com.tornus.pro_sklad.worker.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InventoryViewModel by viewModels()
    private val adapter = InventoryAdapter { item, newQty ->
        viewModel.updateActualQty(item.product.id, newQty)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvInventory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@InventoryFragment.adapter
        }

        binding.btnAddScan.setOnClickListener { showScanDialog() }
        binding.btnComplete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Завершить инвентаризацию?")
                .setMessage("Будут применены все расхождения.")
                .setPositiveButton("Завершить") { _, _ -> viewModel.completeInventory() }
                .setNegativeButton("Отмена", null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                adapter.submitList(state.items)
                binding.tvEmpty.visibility = if (state.items.isEmpty()) View.VISIBLE else View.GONE

                state.error?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
                if (state.completed) {
                    Snackbar.make(binding.root, "Инвентаризация завершена", Snackbar.LENGTH_SHORT).show()
                    viewModel.clearCompleted()
                    enqueueSync()
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun showScanDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_scan_input, null)
        val etSku = dialogView.findViewById<TextInputEditText>(R.id.et_sku)
        val etQty = dialogView.findViewById<TextInputEditText>(R.id.et_qty)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить товар")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val sku = etSku.text.toString().trim()
                val qty = etQty.text.toString().toDoubleOrNull() ?: 0.0
                if (sku.isNotEmpty()) viewModel.onScan(sku, qty)
            }
            .setNegativeButton("Отмена", null)
            .show()
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
