package com.tornus.pro_sklad.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tornus.pro_sklad.R
import com.tornus.pro_sklad.databinding.ItemProductBinding
import com.tornus.pro_sklad.domain.model.Product

class ProductAdapter(
    private val onItemClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.tvName.text = product.name
            binding.tvSku.text = product.sku
            binding.tvStock.text = "${product.currentStock} ${product.unit}"
            binding.tvCategory.text = product.category ?: "—"
            if (product.isLowStock) {
                binding.tvStock.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.color_low_stock)
                )
            } else {
                binding.tvStock.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.color_stock_ok)
                )
            }
            binding.root.setOnClickListener { onItemClick(product) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(a: Product, b: Product) = a.id == b.id
        override fun areContentsTheSame(a: Product, b: Product) = a == b
    }
}
