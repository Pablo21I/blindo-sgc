package com.example.blindo.ui.main

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.blindo.R
import com.example.blindo.data.model.OrdenActiva
import com.example.blindo.databinding.ItemOrdenActivaBinding

class OrdenesAdapter(
    private var ordenes: List<OrdenActiva>,
    private val onItemClick: (OrdenActiva) -> Unit,
    private val onFinalizarClick: (OrdenActiva) -> Unit
) : RecyclerView.Adapter<OrdenesAdapter.OrdenViewHolder>() {

    inner class OrdenViewHolder(private val binding: ItemOrdenActivaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(orden: OrdenActiva) {
            val context = binding.root.context

            binding.tvTrabajo.text = orden.nombreTrabajo
            binding.tvTrabajador.text = orden.trabajador

            val zonaCorta = orden.zona.split(" - ").firstOrNull() ?: orden.zona
            binding.tvZonaPill.text = zonaCorta

            val isVerde = orden.estado == "VERDE"
            val statusColor = ContextCompat.getColor(context, if (isVerde) R.color.status_green else R.color.status_yellow)
            val statusBgTint = if (isVerde) Color.parseColor("#1A00C896") else Color.parseColor("#1AFFB020")

            binding.viewEstadoStripe.setBackgroundColor(statusColor)
            binding.ivTaskIcon.backgroundTintList = ColorStateList.valueOf(statusBgTint)
            binding.ivTaskIcon.imageTintList = ColorStateList.valueOf(statusColor)

            val iconRes = when (orden.trabajoId) {
                1 -> R.drawable.ic_task_welding
                2 -> R.drawable.ic_task_painting
                3 -> R.drawable.ic_task_grinding
                4 -> R.drawable.ic_task_heights
                else -> R.drawable.ic_shield_check
            }
            binding.ivTaskIcon.setImageResource(iconRes)

            binding.root.setOnClickListener { onItemClick(orden) }
            binding.btnFinalizar.setOnClickListener { onFinalizarClick(orden) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdenViewHolder {
        val binding = ItemOrdenActivaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrdenViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrdenViewHolder, position: Int) {
        holder.bind(ordenes[position])
    }

    override fun getItemCount() = ordenes.size

    fun updateData(newOrdenes: List<OrdenActiva>) {
        ordenes = newOrdenes
        notifyDataSetChanged()
    }
}
