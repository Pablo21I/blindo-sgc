package com.example.blindo.ui.main

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.blindo.R
import com.example.blindo.data.model.AsignacionEppDetalle
import com.example.blindo.databinding.ItemCaducidadEppBinding
import com.example.blindo.domain.EppDateUtils
import com.example.blindo.domain.EstadoEpp

class InventarioEppAdapter(
    private var items: List<AsignacionEppDetalle>,
    private val onItemClick: (AsignacionEppDetalle) -> Unit,
    private val onRenovarClick: (AsignacionEppDetalle) -> Unit
) : RecyclerView.Adapter<InventarioEppAdapter.InventarioViewHolder>() {

    inner class InventarioViewHolder(private val binding: ItemCaducidadEppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(detalle: AsignacionEppDetalle) {
            val context = binding.root.context
            val vidaMeses = if (detalle.asignacion.vidaUtilMesesAsignada > 0) {
                detalle.asignacion.vidaUtilMesesAsignada
            } else {
                detalle.epp.vidaUtilMeses
            }
            val vigenciaTexto = EppDateUtils.formatearVidaUtil(vidaMeses)

            binding.tvEquipoAsignado.text = "${detalle.epp.nombre} · $vigenciaTexto"
            binding.tvNombreEmpleado.text = "${detalle.personal.nombre} (${detalle.personal.numeroEmpleado})"

            val stripeView = binding.ivEstatusEpp

            when (detalle.estado) {
                EstadoEpp.PERMANENTE -> {
                    val green = ContextCompat.getColor(context, R.color.status_green)
                    binding.tvDiasRestantes.text = "Permanente · Sin caducidad"
                    binding.tvDiasRestantes.setTextColor(green)
                    binding.tvDiasRestantes.setBackgroundResource(R.drawable.bg_pill_green)
                    stripeView.backgroundTintList = ColorStateList.valueOf(green)
                    binding.btnRenovarEpp.visibility = View.GONE
                }
                EstadoEpp.CADUCADO -> {
                    val red = ContextCompat.getColor(context, R.color.status_red)
                    val diasPasados = (-detalle.diasRestantes).coerceAtLeast(1)
                    val txtDias = if (diasPasados == 1) "1 día" else "$diasPasados días"
                    binding.tvDiasRestantes.text = "Caducado hace $txtDias"
                    binding.tvDiasRestantes.setTextColor(red)
                    binding.tvDiasRestantes.setBackgroundResource(R.drawable.bg_pill_red)
                    stripeView.backgroundTintList = ColorStateList.valueOf(red)
                    binding.btnRenovarEpp.visibility = View.VISIBLE
                    binding.btnRenovarEpp.setTextColor(red)
                    binding.btnRenovarEpp.strokeColor = ColorStateList.valueOf(red)
                }
                EstadoEpp.ALERTA -> {
                    val yellow = ContextCompat.getColor(context, R.color.status_yellow)
                    val txtDias = if (detalle.diasRestantes == 1) "1 día" else "${detalle.diasRestantes} días"
                    binding.tvDiasRestantes.text = "Por vencer · $txtDias"
                    binding.tvDiasRestantes.setTextColor(yellow)
                    binding.tvDiasRestantes.setBackgroundResource(R.drawable.bg_pill_yellow)
                    stripeView.backgroundTintList = ColorStateList.valueOf(yellow)
                    binding.btnRenovarEpp.visibility = View.VISIBLE
                    binding.btnRenovarEpp.setTextColor(yellow)
                    binding.btnRenovarEpp.strokeColor = ColorStateList.valueOf(yellow)
                }
                EstadoEpp.VIGENTE -> {
                    val green = ContextCompat.getColor(context, R.color.status_green)
                    val accent = ContextCompat.getColor(context, R.color.industrial_accent_glow)
                    val mesesRestantes = detalle.diasRestantes / 30
                    val textoTiempo = when {
                        mesesRestantes == 1 -> "~1 mes"
                        mesesRestantes > 1 -> "~$mesesRestantes meses"
                        detalle.diasRestantes == 1 -> "1 día"
                        else -> "${detalle.diasRestantes} días"
                    }
                    binding.tvDiasRestantes.text = "Vigente · $textoTiempo restantes"
                    binding.tvDiasRestantes.setTextColor(green)
                    binding.tvDiasRestantes.setBackgroundResource(R.drawable.bg_pill_green)
                    stripeView.backgroundTintList = ColorStateList.valueOf(green)
                    binding.btnRenovarEpp.visibility = View.VISIBLE
                    binding.btnRenovarEpp.setTextColor(accent)
                    binding.btnRenovarEpp.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.industrial_accent))
                }
            }

            binding.root.setOnClickListener { onItemClick(detalle) }
            binding.btnRenovarEpp.setOnClickListener { onRenovarClick(detalle) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventarioViewHolder {
        val binding = ItemCaducidadEppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InventarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InventarioViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun updateData(nuevosItems: List<AsignacionEppDetalle>) {
        items = nuevosItems
        notifyDataSetChanged()
    }
}
