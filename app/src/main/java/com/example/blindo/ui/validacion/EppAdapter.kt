package com.example.blindo.ui.validacion

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.blindo.R
import com.example.blindo.data.model.Epp
import com.example.blindo.databinding.ItemEppCheckboxBinding

class EppAdapter(
    private var items: List<Pair<Epp, Boolean>>,
    private val onCheckedChange: (Epp, Boolean) -> Unit
) : RecyclerView.Adapter<EppAdapter.EppViewHolder>() {

    private val checkedEpps = mutableSetOf<Int>()

    inner class EppViewHolder(private val binding: ItemEppCheckboxBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(epp: Epp, esObligatorio: Boolean) {
            val context = binding.root.context
            
            binding.tvEppNombre.text = epp.nombre
            binding.tvObligatorio.visibility = if (esObligatorio) View.VISIBLE else View.GONE
            
            val isChecked = checkedEpps.contains(epp.id)
            actualizarEstilo(isChecked, context)

            binding.cardEppTile.setOnClickListener {
                val newCheckedState = !checkedEpps.contains(epp.id)
                if (newCheckedState) {
                    checkedEpps.add(epp.id)
                } else {
                    checkedEpps.remove(epp.id)
                }
                actualizarEstilo(newCheckedState, context)
                onCheckedChange(epp, newCheckedState)
            }
        }
        
        private fun actualizarEstilo(isChecked: Boolean, context: android.content.Context) {
            if (isChecked) {
                binding.cardEppTile.setCardBackgroundColor(ContextCompat.getColor(context, R.color.industrial_card_elevated))
                binding.cardEppTile.strokeColor = ContextCompat.getColor(context, R.color.status_green)
                binding.cardEppTile.strokeWidth = 3
                binding.ivCheckIcon.setImageResource(R.drawable.ic_check_circle)
                binding.ivCheckIcon.imageTintList = null // Eliminar tint gris
                binding.tvEppNombre.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                binding.cardEppTile.setCardBackgroundColor(ContextCompat.getColor(context, R.color.industrial_card))
                binding.cardEppTile.strokeColor = ContextCompat.getColor(context, R.color.industrial_border)
                binding.cardEppTile.strokeWidth = 1
                binding.ivCheckIcon.setImageResource(R.drawable.ic_circle_outline)
                binding.ivCheckIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.industrial_text_secondary))
                binding.tvEppNombre.setTextColor(ContextCompat.getColor(context, R.color.industrial_text_secondary))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EppViewHolder {
        val binding = ItemEppCheckboxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EppViewHolder, position: Int) {
        val (epp, esObligatorio) = items[position]
        holder.bind(epp, esObligatorio)
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Pair<Epp, Boolean>>) {
        items = newItems
        checkedEpps.clear()
        notifyDataSetChanged()
    }

    fun getCheckedEppsIds(): List<Int> {
        return checkedEpps.toList()
    }

    fun getCheckedEppsNames(): List<String> {
        return items.filter { checkedEpps.contains(it.first.id) }.map { it.first.nombre }
    }

    fun checkAllObligatorios() {
        items.filter { it.second }.forEach { (epp, _) ->
            checkedEpps.add(epp.id)
        }
        notifyDataSetChanged()
    }

    fun getObligatoriosTotalCount(): Int {
        return items.count { it.second }
    }

    fun getCheckedObligatoriosCount(): Int {
        val obligatoriosIds = items.filter { it.second }.map { it.first.id }.toSet()
        return checkedEpps.count { it in obligatoriosIds }
    }
}