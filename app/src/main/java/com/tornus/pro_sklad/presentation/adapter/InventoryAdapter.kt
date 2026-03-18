package com.tornus.pro_sklad.presentation.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tornus.pro_sklad.R
import com.tornus.pro_sklad.databinding.ItemInventoryBinding
import com.tornus.pro_sklad.presentation.ui.inventory.InventoryItem

class InventoryAdapter(
    private val onQtyChanged: (InventoryItem, Double) -> Unit
) : ListAdapter<InventoryItem, InventoryAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var currentItem: InventoryItem? = null
        private val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val qty = s.toString().toDoubleOrNull() ?: 0.0
                currentItem?.let { onQtyChanged(it, qty) }
            }
        }

        fun bind(item: InventoryItem) {
            currentItem = null
            binding.etActualQty.removeTextChangedListener(watcher)

            binding.tvName.text = item.product.name
            binding.tvSku.text = item.product.sku
            binding.tvExpected.text = "${item.expectedQty} ${item.product.unit}"
            binding.etActualQty.setText(item.actualQty.toString())

            val diff = item.difference
            binding.tvDiff.text = if (diff == 0.0) "0" else (if (diff > 0) "+$diff" else "$diff")
            val color = when {
                diff > 0 -> ContextCompat.getColor(binding.root.context, R.color.color_in)
                diff < 0 -> ContextCompat.getColor(binding.root.context, R.color.color_out)
                else -> ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)
            }
            binding.tvDiff.setTextColor(color)

            currentItem = item
            binding.etActualQty.addTextChangedListener(watcher)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<InventoryItem>() {
        override fun areItemsTheSame(a: InventoryItem, b: InventoryItem) = a.product.id == b.product.id
        override fun areContentsTheSame(a: InventoryItem, b: InventoryItem) = a == b
    }
}
