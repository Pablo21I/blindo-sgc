package com.example.blindo.ui.resultado

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.blindo.R
import com.example.blindo.data.repository.MockBlindoRepository
import com.example.blindo.data.session.EvidenciaStore
import com.example.blindo.databinding.ActivityResultadoBinding
import com.example.blindo.domain.DatosPermisoPdf
import com.example.blindo.domain.PermisoPdfGenerator
import com.example.blindo.ui.showExpanded
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Muestra el resultado final del semáforo (VERDE, AMARILLO o ROJO),
 * permite previsualizar/compartir el Permiso STPS en PDF y guarda el registro en MySQL.
 */
class ResultadoSemaforoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultadoBinding
    private var registroEnviado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MockBlindoRepository.init(this)
        binding = ActivityResultadoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val estado = intent.getStringExtra("ESTADO") ?: "ROJO"
        val mensaje = intent.getStringExtra("MENSAJE") ?: ""
        val personalId = intent.getIntExtra("PERSONAL_ID", 0)
        val trabajador = intent.getStringExtra("TRABAJADOR") ?: ""
        val zona = intent.getStringExtra("ZONA") ?: ""
        val supervisor = intent.getStringExtra("SUPERVISOR") ?: "Supervisor SGC"
        val turno = intent.getStringExtra("TURNO") ?: ""
        val trabajoId = intent.getIntExtra("TRABAJO_ID", -1)
        val eppsNombres = intent.getStringArrayListExtra("EPPS_NOMBRES") ?: arrayListOf()
        val evidenciaPath = intent.getStringExtra("EVIDENCIA_PATH")

        var nombreTrabajo = "Trabajo no especificado"
        var normaTrabajo = "NOM-017-STPS"
        var riesgoTrabajo = "Medio"
        val folio = "PTW-${UUID.randomUUID().toString().take(6).uppercase()}"

        lifecycleScope.launch {
            val trabajo = MockBlindoRepository.getTrabajoPorId(trabajoId)
            nombreTrabajo = trabajo?.nombre ?: "Trabajo no especificado"
            normaTrabajo = trabajo?.norma ?: "NOM-017-STPS"
            riesgoTrabajo = trabajo?.riesgo ?: "Medio"
            binding.tvResumenTrabajo.text = nombreTrabajo
            binding.tvResumenNorma.text = "$normaTrabajo · Riesgo $riesgoTrabajo"
        }

        binding.tvResumenOperador.text = "Operador: $trabajador"
        binding.tvResumenSupervisor.text = "Supervisor: $supervisor"
        binding.tvResumenUbicacion.text = "Ubicación: $zona · $turno"

        configurarUiSegunEstado(estado, mensaje)

        binding.btnExportarPdf.setOnClickListener {
            val datosPdf = DatosPermisoPdf(
                folio = folio,
                estado = estado,
                trabajo = nombreTrabajo,
                norma = normaTrabajo,
                riesgo = riesgoTrabajo,
                operador = trabajador,
                supervisor = supervisor,
                zona = zona,
                turno = turno,
                diagnostico = mensaje,
                eppsVerificados = eppsNombres,
                evidenciaPath = evidenciaPath
            )
            generarYMostrarVistaPreviaPdf(datosPdf)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lifecycleScope.launch {
                    enviarValidacionAlServidor(
                        personalId, trabajador, supervisor, turno, zona, trabajoId,
                        nombreTrabajo, estado, activa = false, notas = "Salida sin confirmar: $mensaje",
                        folio = folio, evidenciaPath = evidenciaPath, eppsNombres = eppsNombres
                    )
                    setResult(if (estado == "ROJO") Activity.RESULT_CANCELED else Activity.RESULT_OK)
                    finish()
                }
            }
        })

        binding.btnAccionSecundaria.setOnClickListener {
            binding.btnAccionSecundaria.isEnabled = false
            lifecycleScope.launch {
                val notaCierre = if (estado == "ROJO") "Bloqueo devuelto a corrección: $mensaje" else "Cancelado por supervisor: $mensaje"
                enviarValidacionAlServidor(
                    personalId, trabajador, supervisor, turno, zona, trabajoId,
                    nombreTrabajo, estado, activa = false, notas = notaCierre,
                    folio = folio, evidenciaPath = evidenciaPath, eppsNombres = eppsNombres
                )
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }

        binding.btnAccionPrimaria.setOnClickListener {
            binding.btnAccionPrimaria.isEnabled = false
            lifecycleScope.launch {
                val iniciarEnPiso = (estado == "VERDE" || estado == "AMARILLO")
                val nota = if (iniciarEnPiso) "Orden iniciada en piso: $mensaje" else mensaje
                val res = enviarValidacionAlServidor(
                    personalId, trabajador, supervisor, turno, zona, trabajoId,
                    nombreTrabajo, estado, activa = iniciarEnPiso, notas = nota,
                    folio = folio, evidenciaPath = evidenciaPath, eppsNombres = eppsNombres
                )
                res.onFailure { err ->
                    Toast.makeText(this@ResultadoSemaforoActivity, err.message ?: "Error al guardar en servidor", Toast.LENGTH_LONG).show()
                }
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private suspend fun enviarValidacionAlServidor(
        personalId: Int,
        trabajador: String,
        supervisor: String,
        turno: String,
        zona: String,
        trabajoId: Int,
        nombreTrabajo: String,
        estado: String,
        activa: Boolean,
        notas: String,
        folio: String,
        evidenciaPath: String?,
        eppsNombres: List<String>
    ): Result<Unit> {
        if (registroEnviado) return Result.success(Unit)
        registroEnviado = true
        return MockBlindoRepository.registrarValidacionRemota(
            personalId = personalId,
            trabajador = trabajador,
            supervisor = supervisor,
            turno = turno,
            zona = zona,
            trabajoId = trabajoId,
            nombreTrabajo = nombreTrabajo,
            estado = estado,
            activa = activa,
            notas = notas
        ).onSuccess { ordenGuardada ->
            EvidenciaStore.guardar(
                context = this,
                validacionId = ordenGuardada.id,
                folio = folio,
                evidenciaPath = evidenciaPath,
                eppsVerificados = eppsNombres
            )
        }.map { Unit }
    }

    private fun generarYMostrarVistaPreviaPdf(datos: DatosPermisoPdf) {
        lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                try {
                    val file = PermisoPdfGenerator.generarPermisoPdf(this@ResultadoSemaforoActivity, datos)
                    val bitmap = PermisoPdfGenerator.renderizarPrimeraPagina(file)
                    Pair(file, bitmap)
                } catch (_: Exception) {
                    null
                }
            }

            if (resultado == null) {
                Toast.makeText(this@ResultadoSemaforoActivity, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val (pdfFile, previewBitmap) = resultado
            val dialog = BottomSheetDialog(this@ResultadoSemaforoActivity)
            val view = layoutInflater.inflate(R.layout.dialog_preview_pdf, null)
            dialog.setContentView(view)

            view.findViewById<TextView>(R.id.tvPdfFolioSub).text = "Folio ${datos.folio} · Listo para auditoría STPS"
            val ivPreview = view.findViewById<ImageView>(R.id.ivPdfPreview)
            if (previewBitmap != null) ivPreview.setImageBitmap(previewBitmap)

            view.findViewById<MaterialButton>(R.id.btnCompartirPdf).setOnClickListener {
                PermisoPdfGenerator.compartirPdf(this@ResultadoSemaforoActivity, pdfFile)
            }
            view.findViewById<MaterialButton>(R.id.btnCerrarPdfPreview).setOnClickListener {
                dialog.dismiss()
            }

            dialog.showExpanded()
        }
    }

    private fun configurarUiSegunEstado(estado: String, mensaje: String) {
        binding.tvMensaje.text = mensaje

        when (estado) {
            "ROJO" -> {
                val red = ContextCompat.getColor(this, R.color.status_red)
                binding.rootView.setBackgroundResource(R.drawable.bg_resultado_red)
                binding.frameIconBadge.setBackgroundResource(R.drawable.bg_pill_red)
                binding.ivIcono.setImageResource(R.drawable.ic_block)
                binding.ivIcono.imageTintList = ColorStateList.valueOf(red)
                binding.tvTitulo.text = "Trabajo bloqueado"
                binding.btnAccionPrimaria.text = "Registrar bloqueo y salir"
                binding.btnAccionPrimaria.backgroundTintList = ColorStateList.valueOf(red)
                binding.btnAccionPrimaria.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.btnExportarPdf.text = "Ver reporte de bloqueo (PDF)"
                binding.btnAccionSecundaria.visibility = View.VISIBLE
                binding.btnAccionSecundaria.text = "Volver y corregir formulario"
            }
            "AMARILLO" -> {
                val yellow = ContextCompat.getColor(this, R.color.status_yellow)
                binding.rootView.setBackgroundResource(R.drawable.bg_resultado_yellow)
                binding.frameIconBadge.setBackgroundResource(R.drawable.bg_pill_yellow)
                binding.ivIcono.setImageResource(R.drawable.ic_warning_triangle)
                binding.ivIcono.imageTintList = ColorStateList.valueOf(yellow)
                binding.tvTitulo.text = "Precaución SGC"
                binding.btnAccionPrimaria.text = "Autorizar bajo supervisión"
                binding.btnAccionPrimaria.backgroundTintList = ColorStateList.valueOf(yellow)
                binding.btnAccionPrimaria.setTextColor(ContextCompat.getColor(this, R.color.industrial_bg))
                binding.btnExportarPdf.text = "Ver / Exportar Permiso STPS (PDF)"
                binding.btnAccionSecundaria.visibility = View.VISIBLE
                binding.btnAccionSecundaria.text = "Volver y corregir"
            }
            "VERDE" -> {
                val green = ContextCompat.getColor(this, R.color.status_green)
                binding.rootView.setBackgroundResource(R.drawable.bg_resultado_green)
                binding.frameIconBadge.setBackgroundResource(R.drawable.bg_pill_green)
                binding.ivIcono.setImageResource(R.drawable.ic_check_circle)
                binding.ivIcono.imageTintList = ColorStateList.valueOf(green)
                binding.tvTitulo.text = "Autorizado y blindado"
                binding.btnAccionPrimaria.text = "Iniciar orden en piso"
                binding.btnAccionPrimaria.backgroundTintList = ColorStateList.valueOf(green)
                binding.btnAccionPrimaria.setTextColor(ContextCompat.getColor(this, R.color.industrial_bg))
                binding.btnExportarPdf.text = "Ver / Exportar Permiso STPS (PDF)"
                binding.btnAccionSecundaria.visibility = View.GONE
            }
        }
    }
}
