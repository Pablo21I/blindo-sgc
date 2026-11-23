package com.example.blindo.ui.personal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.blindo.R
import com.example.blindo.data.model.Personal
import com.example.blindo.databinding.ItemPersonalBinding

class PersonalAdapter(
    private var listaPersonal: List<Personal>,
    private val onItemClick: (Personal) -> Unit
) : RecyclerView.Adapter<PersonalAdapter.PersonalViewHolder>() {

    inner class PersonalViewHolder(private val binding: ItemPersonalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(personal: Personal) {
            val context = binding.root.context

            binding.tvNombre.text = personal.nombre
            binding.tvNumEmpleado.text = "ID: ${personal.numeroEmpleado}"

            val partes = personal.nombre.trim().split(" ")
            val iniciales = if (partes.size >= 2) {
                "${partes[0].firstOrNull() ?: ""}${partes[1].firstOrNull() ?: ""}".uppercase()
            } else {
                personal.nombre.take(2).uppercase()
            }
            binding.tvAvatarInitials.text = iniciales

            val isOperador = personal.rol == "OPERADOR"
            binding.tvRolBadge.text = if (isOperador) "Operador" else "Supervisor"
            binding.tvRolBadge.setTextColor(
                ContextCompat.getColor(context, if (isOperador) R.color.industrial_accent_glow else R.color.status_yellow)
            )
            binding.tvRolBadge.setBackgroundResource(
                if (isOperador) R.drawable.bg_pill_accent else R.drawable.bg_pill_yellow
            )
            binding.tvAvatarInitials.setBackgroundResource(
                if (isOperador) R.drawable.circle_green else R.drawable.circle_yellow
            )

            binding.root.setOnClickListener { onItemClick(personal) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonalViewHolder {
        val binding = ItemPersonalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PersonalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PersonalViewHolder, position: Int) {
        holder.bind(listaPersonal[position])
    }

    override fun getItemCount() = listaPersonal.size

    fun updateData(nuevaLista: List<Personal>) {
        listaPersonal = nuevaLista
        notifyDataSetChanged()
    }
}
