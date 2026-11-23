package com.example.blindo.ui.historial

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.blindo.R
import com.example.blindo.data.model.RegistroAuditoria
import com.example.blindo.data.session.EvidenciaStore
import com.example.blindo.databinding.ItemHistorialBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistorialAdapter(
    private var registros: List<RegistroAuditoria>,
    private val onVerPdfClick: (RegistroAuditoria) -> Unit,
    private val onVerFotoClick: (RegistroAuditoria, String) -> Unit
) : RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy · hh:mm a", Locale.getDefault())

    inner class HistorialViewHolder(private val binding: ItemHistorialBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(registro: RegistroAuditoria) {
            val context = binding.root.context

            binding.tvFechaHora.text = dateFormat.format(Date(registro.fechaHora))
            binding.tvTrabajo.text = registro.trabajo
            binding.tvOperadorZona.text = "${registro.trabajador} · ${registro.zona}"
            binding.tvMotivo.text = registro.motivo
            binding.tvFirmaSupervisor.text = "Autorizó: ${registro.supervisor}"

            val fotoPath = EvidenciaStore.obtener(context, registro.id)?.evidenciaPath
            val fotoFile = fotoPath?.let { File(it) }?.takeIf { it.exists() }
            if (fotoFile != null) {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                val bmp = BitmapFactory.decodeFile(fotoFile.absolutePath, opts)
                    ?: BitmapFactory.decodeFile(fotoFile.absolutePath)
                if (bmp != null) {
                    binding.ivFotoEvidenciaBitacora.setImageBitmap(bmp)
                    binding.cardFotoEvidenciaBitacora.visibility = View.VISIBLE
                    binding.tvBadgeEvidencia.visibility = View.VISIBLE
                    binding.cardFotoEvidenciaBitacora.setOnClickListener {
                        onVerFotoClick(registro, fotoFile.absolutePath)
                    }
                } else {
                    binding.cardFotoEvidenciaBitacora.visibility = View.GONE
                    binding.tvBadgeEvidencia.visibility = View.GONE
                }
            } else {
                binding.ivFotoEvidenciaBitacora.setImageDrawable(null)
                binding.cardFotoEvidenciaBitacora.visibility = View.GONE
                binding.tvBadgeEvidencia.visibility = View.GONE
            }

            val (etiqueta, colorRes, pillBgRes) = when (registro.resultado) {
                "ROJO_BLOQUEADO" -> Triple("Bloqueado", R.color.status_red, R.drawable.bg_pill_red)
                "AMARILLO_PRECAUCION" -> Triple("Precaución", R.color.status_yellow, R.drawable.bg_pill_yellow)
                "VERDE_AUTORIZADO" -> Triple("Autorizado", R.color.status_green, R.drawable.bg_pill_green)
                else -> Triple(registro.resultado, R.color.industrial_text_secondary, R.drawable.bg_pill_accent)
            }

            val finalColor = ContextCompat.getColor(context, colorRes)
            binding.tvResultadoTag.text = etiqueta
            binding.tvResultadoTag.setTextColor(finalColor)
            binding.tvResultadoTag.setBackgroundResource(pillBgRes)
            binding.viewEstadoBitacoraStripe.setBackgroundColor(finalColor)

            binding.btnVerPdfBitacora.setOnClickListener { onVerPdfClick(registro) }
            binding.root.setOnClickListener { onVerPdfClick(registro) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val binding = ItemHistorialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistorialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        holder.bind(registros[position])
    }

    override fun getItemCount() = registros.size

    fun updateData(newRegistros: List<RegistroAuditoria>) {
        registros = newRegistros
        notifyDataSetChanged()
    }
}
