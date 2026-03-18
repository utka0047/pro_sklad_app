package com.tornus.pro_sklad.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tornus.pro_sklad.R
import com.tornus.pro_sklad.databinding.ItemMovementBinding
import com.tornus.pro_sklad.domain.model.Movement
import com.tornus.pro_sklad.domain.model.MovementType

class MovementAdapter : ListAdapter<Movement, MovementAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemMovementBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(movement: Movement) {
            binding.tvType.text = movement.movementType.label
            binding.tvQuantity.text = when (movement.movementType) {
                MovementType.IN -> "+${movement.quantity}"
                MovementType.OUT -> "-${movement.quantity}"
                else -> movement.quantity
            }
            binding.tvProductName.text = movement.productName ?: "SKU: ${movement.productSku}"
            binding.tvDate.text = movement.createdAt.take(16).replace("T", " ")
            binding.tvComment.text = movement.comment ?: ""

            val color = when (movement.movementType) {
                MovementType.IN -> ContextCompat.getColor(binding.root.context, R.color.color_in)
                MovementType.OUT -> ContextCompat.getColor(binding.root.context, R.color.color_out)
                else -> ContextCompat.getColor(binding.root.context, R.color.color_transfer)
            }
            binding.tvQuantity.setTextColor(color)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemMovementBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<Movement>() {
        override fun areItemsTheSame(a: Movement, b: Movement) = a.id == b.id
        override fun areContentsTheSame(a: Movement, b: Movement) = a == b
    }
}
