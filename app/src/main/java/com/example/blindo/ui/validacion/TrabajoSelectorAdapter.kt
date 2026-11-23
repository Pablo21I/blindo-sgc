package com.example.blindo.ui.validacion

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.blindo.R
import com.example.blindo.data.model.Trabajo
import com.example.blindo.databinding.ItemTrabajoSelectorBinding

class TrabajoSelectorAdapter(
    private val trabajos: List<Trabajo>,
    private val onTrabajoSelected: (Trabajo) -> Unit
) : RecyclerView.Adapter<TrabajoSelectorAdapter.TrabajoViewHolder>() {

    private var selectedIndex = 0

    inner class TrabajoViewHolder(private val binding: ItemTrabajoSelectorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(trabajo: Trabajo, isSelected: Boolean) {
            val context = binding.root.context

            binding.tvNombreTrabajo.text = trabajo.nombre
            binding.tvNorma.text = trabajo.norma

            // Set Task Icon
            val iconRes = when (trabajo.id) {
                1 -> R.drawable.ic_task_welding
                2 -> R.drawable.ic_task_painting
                3 -> R.drawable.ic_task_grinding
                4 -> R.drawable.ic_task_heights
                else -> R.drawable.ic_shield_check
            }
            binding.ivIconoTrabajo.setImageResource(iconRes)

            if (isSelected) {
                binding.cardTrabajo.setCardBackgroundColor(ContextCompat.getColor(context, R.color.industrial_card_elevated))
                binding.cardTrabajo.strokeColor = ContextCompat.getColor(context, R.color.industrial_accent)
                binding.cardTrabajo.strokeWidth = 4
                binding.tvNombreTrabajo.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                binding.cardTrabajo.setCardBackgroundColor(ContextCompat.getColor(context, R.color.industrial_card))
                binding.cardTrabajo.strokeColor = ContextCompat.getColor(context, R.color.industrial_border)
                binding.cardTrabajo.strokeWidth = 2
                binding.tvNombreTrabajo.setTextColor(ContextCompat.getColor(context, R.color.industrial_text_secondary))
            }

            binding.cardTrabajo.setOnClickListener {
                val previous = selectedIndex
                selectedIndex = bindingAdapterPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedIndex)
                onTrabajoSelected(trabajo)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrabajoViewHolder {
        val binding = ItemTrabajoSelectorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrabajoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrabajoViewHolder, position: Int) {
        holder.bind(trabajos[position], position == selectedIndex)
    }

    override fun getItemCount() = trabajos.size

    fun getSelectedTrabajo(): Trabajo? {
        return if (trabajos.isNotEmpty() && selectedIndex in trabajos.indices) trabajos[selectedIndex] else null
    }
}