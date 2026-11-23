package com.example.blindo.ui.validacion

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blindo.R
import com.example.blindo.data.model.Personal
import com.example.blindo.data.model.Trabajo
import com.example.blindo.data.remote.ApiException
import com.example.blindo.data.repository.MockBlindoRepository
import com.example.blindo.data.session.SessionManager
import com.example.blindo.databinding.ActivityValidacionBinding
import com.example.blindo.domain.ResultadoValidacion
import com.example.blindo.domain.ValidadorBlindo
import com.example.blindo.ui.resultado.ResultadoSemaforoActivity
import com.example.blindo.ui.showExpanded
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Formulario de validación de seguridad previo a iniciar un trabajo (Paso 1: datos, Paso 2: EPP, Paso 3: evidencia).
 * Evalúa las reglas Poka-Yoke + SIMOPS y envía el resultado a la pantalla de Semáforo.
 */
class ValidacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityValidacionBinding
    private lateinit var eppAdapter: EppAdapter
    private lateinit var trabajoSelectorAdapter: TrabajoSelectorAdapter

    private var zonas = listOf<String>()
    private val turnos = listOf("Matutino (1er Turno)", "Vespertino (2do Turno)", "Nocturno (3er Turno)")

    private var operadoresLista = listOf<Personal>()
    private var supervisoresLista = listOf<Personal>()
    private var trabajoSeleccionado: Trabajo? = null
    private var evidenciaPathActual: String? = null

    private val resultadoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

    // Cámara rápida (no requiere permisos de almacenamiento y funciona en Emulador Device Manager)
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            guardarYMostrarEvidencia(bitmap)
        }
    }

    // Galería (permite adjuntar foto de prueba desde el emulador o celular)
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    try {
                        contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
                if (bmp != null) {
                    guardarYMostrarEvidencia(bmp)
                } else {
                    Toast.makeText(this@ValidacionActivity, "No se pudo leer la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MockBlindoRepository.init(this)
        binding = ActivityValidacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViewEpp()
        setupListeners()
        cargarDatosIniciales()
    }

    override fun onResume() {
        super.onResume()
        cargarSpinnersPersonal()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun crearSpinnerAdapter(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, R.layout.item_spinner_industrial, items).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown_industrial)
        }
    }

    private fun guardarYMostrarEvidencia(bitmap: Bitmap) {
        lifecycleScope.launch {
            val scaledBitmap = withContext(Dispatchers.IO) {
                val maxDim = 1280
                val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
                if (ratio < 1f) {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * ratio).toInt().coerceAtLeast(1),
                        (bitmap.height * ratio).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    bitmap
                }
            }
            val savedPath = withContext(Dispatchers.IO) {
                try {
                    val dir = File(filesDir, "evidencias").apply { mkdirs() }
                    val outFile = File(dir, "ev_${UUID.randomUUID().toString().take(8)}.jpg")
                    FileOutputStream(outFile).use { fos ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                    }
                    outFile.absolutePath
                } catch (_: Exception) {
                    null
                }
            }
            if (savedPath != null) {
                evidenciaPathActual = savedPath
                binding.ivPreviewEvidencia.setImageBitmap(scaledBitmap)
                binding.ivPreviewEvidencia.visibility = View.VISIBLE
                binding.btnQuitarFoto.visibility = View.VISIBLE
                binding.tvEstadoEvidencia.text = "Evidencia adjunta · Visible en Bitácora y PDF STPS"
                binding.tvEstadoEvidencia.setTextColor(ContextCompat.getColor(this@ValidacionActivity, R.color.status_green))
            }
        }
    }

    private fun limpiarEvidencia() {
        evidenciaPathActual = null
        binding.ivPreviewEvidencia.setImageDrawable(null)
        binding.ivPreviewEvidencia.visibility = View.GONE
        binding.btnQuitarFoto.visibility = View.GONE
        binding.tvEstadoEvidencia.text = "Se incrusta en el Permiso PDF STPS"
        binding.tvEstadoEvidencia.setTextColor(ContextCompat.getColor(this, R.color.industrial_text_secondary))
    }

    private fun cargarDatosIniciales() {
        lifecycleScope.launch {
            cargarZonasYTurnos()
            cargarSpinnersPersonalInterno()
            cargarTrabajosSelectorVisual()
        }
    }

    private suspend fun cargarZonasYTurnos(seleccionarZona: String? = null) {
        zonas = MockBlindoRepository.getZonas()
        val displayZonas = if (zonas.isEmpty()) listOf("Sin zonas registradas") else zonas
        binding.spinnerZona.adapter = crearSpinnerAdapter(displayZonas)
        if (seleccionarZona != null) {
            val idx = zonas.indexOf(seleccionarZona)
            if (idx >= 0) binding.spinnerZona.setSelection(idx)
        }
        binding.spinnerTurno.adapter = crearSpinnerAdapter(turnos)
    }

    private fun cargarSpinnersPersonal(
        preseleccionarOperadorId: Int? = null,
        preseleccionarSupervisorId: Int? = null
    ) {
        lifecycleScope.launch {
            cargarSpinnersPersonalInterno(preseleccionarOperadorId, preseleccionarSupervisorId)
        }
    }

    private suspend fun cargarSpinnersPersonalInterno(
        preseleccionarOperadorId: Int? = null,
        preseleccionarSupervisorId: Int? = null
    ) {
        val personalRes = MockBlindoRepository.getPersonalResult()
        val todoPersonal = personalRes.getOrElse { err ->
            Toast.makeText(this, err.message ?: "No se pudo cargar el personal del servidor", Toast.LENGTH_LONG).show()
            emptyList()
        }

        operadoresLista = todoPersonal.filter { it.rol == "OPERADOR" }
        val opsNombres = if (operadoresLista.isNotEmpty()) {
            operadoresLista.map { "${it.nombre} (${it.numeroEmpleado})" }
        } else {
            listOf("Sin operadores — pulsa + Nuevo")
        }
        binding.spinnerOperador.adapter = crearSpinnerAdapter(opsNombres)
        if (preseleccionarOperadorId != null) {
            val idx = operadoresLista.indexOfFirst { it.id == preseleccionarOperadorId }
            if (idx >= 0) binding.spinnerOperador.setSelection(idx)
        }

        supervisoresLista = todoPersonal.filter { it.rol == "SUPERVISOR" }
        val supsNombres = if (supervisoresLista.isNotEmpty()) {
            supervisoresLista.map { "${it.nombre} (${it.numeroEmpleado})" }
        } else {
            listOf("Sin supervisores — pulsa + Nuevo")
        }
        binding.spinnerSupervisor.adapter = crearSpinnerAdapter(supsNombres)
        if (preseleccionarSupervisorId != null) {
            val idx = supervisoresLista.indexOfFirst { it.id == preseleccionarSupervisorId }
            if (idx >= 0) binding.spinnerSupervisor.setSelection(idx)
        } else {
            val empActual = SessionManager.getUserEmpNum(this)
            val idxPropio = supervisoresLista.indexOfFirst {
                it.numeroEmpleado.equals(empActual, ignoreCase = true)
            }
            if (idxPropio >= 0) binding.spinnerSupervisor.setSelection(idxPropio)
        }
    }

    private fun setupRecyclerViewEpp() {
        eppAdapter = EppAdapter(emptyList()) { _, _ ->
            actualizarContadorEpp()
        }
        binding.rvEpp.layoutManager = LinearLayoutManager(this)
        binding.rvEpp.adapter = eppAdapter
    }

    private fun actualizarContadorEpp() {
        val total = eppAdapter.getObligatoriosTotalCount()
        val checked = eppAdapter.getCheckedObligatoriosCount()
        binding.tvContadorEpp.text = "$checked de $total obligatorios"
        val colorRes = if (checked >= total && total > 0) R.color.status_green else R.color.status_yellow
        binding.tvContadorEpp.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    private suspend fun cargarTrabajosSelectorVisual() {
        val trabajos = MockBlindoRepository.getTrabajos()
        if (trabajos.isNotEmpty()) {
            val actual = trabajoSeleccionado?.let { sel -> trabajos.find { it.id == sel.id } } ?: trabajos.first()
            trabajoSeleccionado = actual
            actualizarEppParaTrabajo(actual.id)
        }

        trabajoSelectorAdapter = TrabajoSelectorAdapter(trabajos) { trabajoSelected ->
            trabajoSeleccionado = trabajoSelected
            lifecycleScope.launch {
                actualizarEppParaTrabajo(trabajoSelected.id)
            }
        }

        binding.rvTrabajosVisual.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvTrabajosVisual.adapter = trabajoSelectorAdapter
    }

    private suspend fun actualizarEppParaTrabajo(trabajoId: Int) {
        val eppsRequeridos = MockBlindoRepository.getEppsParaTrabajo(trabajoId)
        eppAdapter.updateData(eppsRequeridos)
        actualizarContadorEpp()
    }

    private fun setupListeners() {
        binding.btnValidar.setOnClickListener { ejecutarValidacion() }
        binding.btnNuevoTrabajo.setOnClickListener { mostrarDialogNuevoTrabajo() }
        binding.btnAgregarZona.setOnClickListener { mostrarDialogNuevaZona() }
        binding.btnAgregarOperador.setOnClickListener { mostrarDialogNuevoPersonalRapido("OPERADOR") }
        binding.btnAgregarSupervisor.setOnClickListener { mostrarDialogNuevoPersonalRapido("SUPERVISOR") }

        binding.btnTomarFoto.setOnClickListener {
            try {
                cameraLauncher.launch(null)
            } catch (_: Exception) {
                Toast.makeText(this, "Cámara no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnElegirGaleria.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnQuitarFoto.setOnClickListener {
            limpiarEvidencia()
        }

        binding.btnMarcarObligatorios.setOnClickListener {
            eppAdapter.checkAllObligatorios()
            actualizarContadorEpp()
            Toast.makeText(this, "Equipos obligatorios seleccionados", Toast.LENGTH_SHORT).show()
        }

        binding.switchEppEstado.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.cardSwitchPokaYoke.strokeColor = ContextCompat.getColor(this, R.color.status_green)
                binding.tvEstadoPokaYokeDetalle.text = "Inspección física confirmada: EPP en óptimas condiciones (NOM-017)."
                binding.tvEstadoPokaYokeDetalle.setTextColor(ContextCompat.getColor(this, R.color.status_green))
            } else {
                binding.cardSwitchPokaYoke.strokeColor = ContextCompat.getColor(this, R.color.status_yellow)
                binding.tvEstadoPokaYokeDetalle.text = "Requerido: confirma que el equipo no presenta daños ni desgaste crítico."
                binding.tvEstadoPokaYokeDetalle.setTextColor(ContextCompat.getColor(this, R.color.status_yellow))
            }
        }
    }

    // Corre el validador de seguridad (EPP, caducidad y SIMOPS) y abre la pantalla de Semáforo
    private fun ejecutarValidacion() {
        if (operadoresLista.isEmpty() || supervisoresLista.isEmpty()) {
            Toast.makeText(this, "Registra al menos 1 Operador y 1 Supervisor usando '+ Nuevo'", Toast.LENGTH_LONG).show()
            return
        }
        if (zonas.isEmpty()) {
            Toast.makeText(this, "Registra al menos una zona de trabajo", Toast.LENGTH_SHORT).show()
            return
        }

        val trabajo = trabajoSeleccionado ?: trabajoSelectorAdapter.getSelectedTrabajo() ?: return
        val opSeleccionado = operadoresLista.getOrNull(binding.spinnerOperador.selectedItemPosition) ?: return
        val supSeleccionado = supervisoresLista.getOrNull(binding.spinnerSupervisor.selectedItemPosition) ?: return

        val trabajador = "${opSeleccionado.nombre} (${opSeleccionado.numeroEmpleado})"
        val supervisor = "${supSeleccionado.nombre} (${supSeleccionado.numeroEmpleado})"
        val turnoSeleccionado = binding.spinnerTurno.selectedItem.toString()
        val zonaSeleccionada = binding.spinnerZona.selectedItem.toString()
        val eppsIds = eppAdapter.getCheckedEppsIds()
        val eppsNombres = ArrayList(eppAdapter.getCheckedEppsNames())
        val eppBuenEstado = binding.switchEppEstado.isChecked

        binding.btnValidar.isEnabled = false

        lifecycleScope.launch {
            val resultado = ValidadorBlindo.evaluarSeguridadTrabajo(
                personalId = opSeleccionado.id,
                zona = zonaSeleccionada,
                trabajo = trabajo,
                eppsSeleccionadosIds = eppsIds,
                eppBuenEstado = eppBuenEstado
            )

            binding.btnValidar.isEnabled = true

            val intent = Intent(this@ValidacionActivity, ResultadoSemaforoActivity::class.java).apply {
                putExtra("PERSONAL_ID", opSeleccionado.id)
                putExtra("TRABAJADOR", trabajador)
                putExtra("SUPERVISOR", supervisor)
                putExtra("TURNO", turnoSeleccionado)
                putExtra("ZONA", zonaSeleccionada)
                putExtra("TRABAJO_ID", trabajo.id)
                putStringArrayListExtra("EPPS_NOMBRES", eppsNombres)
                putExtra("EVIDENCIA_PATH", evidenciaPathActual)

                when (resultado) {
                    is ResultadoValidacion.RojoBloqueado -> {
                        putExtra("ESTADO", "ROJO")
                        putExtra("MENSAJE", resultado.mensaje)
                    }
                    is ResultadoValidacion.AmarilloPrecaucion -> {
                        putExtra("ESTADO", "AMARILLO")
                        putExtra("MENSAJE", resultado.mensaje)
                    }
                    is ResultadoValidacion.VerdeAutorizado -> {
                        putExtra("ESTADO", "VERDE")
                        putExtra("MENSAJE", resultado.mensaje)
                    }
                }
            }
            resultadoLauncher.launch(intent)
        }
    }

    private fun mostrarDialogNuevoTrabajo() {
        lifecycleScope.launch {
            val eppsOrdenados = MockBlindoRepository.getEppsGlobales().sortedBy { it.nombre }

            val dialog = BottomSheetDialog(this@ValidacionActivity)
            val dialogView = layoutInflater.inflate(R.layout.dialog_nuevo_trabajo, null)
            dialog.setContentView(dialogView)

            val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreTrabajo)
            val etNorma = dialogView.findViewById<TextInputEditText>(R.id.etNorma)
            val spinnerRiesgo = dialogView.findViewById<Spinner>(R.id.spinnerRiesgo)
            val spinnerCategoria = dialogView.findViewById<Spinner>(R.id.spinnerCategoriaSimops)
            val layoutCheckboxesEpp = dialogView.findViewById<LinearLayout>(R.id.layoutCheckboxesEpp)
            val etBuscarEpp = dialogView.findViewById<TextInputEditText>(R.id.etBuscarEpp)
            val btnGuardar = dialogView.findViewById<MaterialButton>(R.id.btnGuardarTrabajo)

            val riesgos = listOf("Alto", "Medio", "Bajo")
            spinnerRiesgo.adapter = crearSpinnerAdapter(riesgos)

            val categoriasSimops = listOf(
                "General (Sin choque térmico/inflamable)" to "GENERAL",
                "Fuego / Chispas / Calor intenso" to "FUEGO_CHISPAS",
                "Atmósfera o Solventes Inflamables" to "INFLAMABLE",
                "Trabajo en Alturas (>1.8m)" to "ALTURAS"
            )
            spinnerCategoria.adapter = crearSpinnerAdapter(categoriasSimops.map { it.first })

            val checkBoxesEpp = mutableMapOf<Int, CheckBox>()

            fun renderizarCheckboxes(filtro: String = "") {
                layoutCheckboxesEpp.removeAllViews()
                eppsOrdenados.forEach { epp ->
                    if (filtro.isEmpty() || epp.nombre.lowercase().contains(filtro.lowercase())) {
                        val checkBox = checkBoxesEpp.getOrPut(epp.id) {
                            CheckBox(this@ValidacionActivity).apply {
                                text = epp.nombre
                                setTextColor(ContextCompat.getColor(context, R.color.white))
                                setPadding(0, 8, 0, 8)
                            }
                        }
                        (checkBox.parent as? ViewGroup)?.removeView(checkBox)
                        layoutCheckboxesEpp.addView(checkBox)
                    }
                }
            }

            renderizarCheckboxes()

            etBuscarEpp.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    renderizarCheckboxes(s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            btnGuardar.setOnClickListener {
                val nombre = etNombre.text.toString().trim()
                val norma = etNorma.text.toString().trim()
                val riesgo = spinnerRiesgo.selectedItem.toString()
                val categoria = categoriasSimops[spinnerCategoria.selectedItemPosition].second

                if (nombre.isEmpty() || norma.isEmpty()) {
                    Toast.makeText(this@ValidacionActivity, "Complete nombre y norma de la tarea", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val eppsSeleccionados = checkBoxesEpp.filter { it.value.isChecked }.keys.toList()
                if (eppsSeleccionados.isEmpty()) {
                    Toast.makeText(this@ValidacionActivity, "Selecciona al menos un EPP obligatorio", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    MockBlindoRepository.agregarNuevoTrabajo(nombre, norma, riesgo, categoria, eppsSeleccionados)
                    Toast.makeText(this@ValidacionActivity, "Nueva tarea registrada: $nombre", Toast.LENGTH_SHORT).show()
                    trabajoSeleccionado = null
                    cargarTrabajosSelectorVisual()
                    dialog.dismiss()
                }
            }

            dialog.showExpanded()
        }
    }

    private fun mostrarDialogNuevaZona() {
        val dialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_nueva_zona, null)
        dialog.setContentView(dialogView)

        val etNombreZona = dialogView.findViewById<TextInputEditText>(R.id.etNombreZona)
        val btnGuardar = dialogView.findViewById<MaterialButton>(R.id.btnGuardarZona)

        btnGuardar.setOnClickListener {
            val nombre = etNombreZona.text.toString().trim()
            if (nombre.isEmpty()) {
                etNombreZona.error = "Ingrese el nombre de la zona"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val res = MockBlindoRepository.agregarZona(nombre)
                res.onSuccess {
                    cargarZonasYTurnos(seleccionarZona = nombre)
                    Toast.makeText(this@ValidacionActivity, "Zona agregada: $nombre", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.onFailure { err ->
                    etNombreZona.error = err.message ?: "No se pudo agregar"
                }
            }
        }

        dialog.showExpanded()
    }

    private fun mostrarDialogNuevoPersonalRapido(rolPorDefecto: String) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_personal_rapido, null)
        dialog.setContentView(view)

        val tvTitulo = view.findViewById<TextView>(R.id.tvTituloPersonalModal)
        val etNombre = view.findViewById<TextInputEditText>(R.id.etNombrePersonalModal)
        val etId = view.findViewById<TextInputEditText>(R.id.etNumEmpleadoModal)
        val toggleRol = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleRolPersonalModal)
        val btnGuardar = view.findViewById<MaterialButton>(R.id.btnGuardarPersonalModal)

        tvTitulo.text = if (rolPorDefecto == "OPERADOR") "Nuevo operador" else "Nuevo supervisor"
        btnGuardar.text = "Guardar y seleccionar"
        toggleRol.check(if (rolPorDefecto == "SUPERVISOR") R.id.btnRolSupModal else R.id.btnRolOpModal)

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val idStr = etId.text.toString().trim()
            if (nombre.length < 3) {
                etNombre.error = "Ingresa el nombre completo"
                return@setOnClickListener
            }
            if (idStr.isEmpty()) {
                etId.error = "Ingresa el No. de Empleado"
                return@setOnClickListener
            }

            val rol = if (toggleRol.checkedButtonId == R.id.btnRolSupModal) "SUPERVISOR" else "OPERADOR"

            btnGuardar.isEnabled = false
            btnGuardar.text = "Guardando..."

            lifecycleScope.launch {
                val res = MockBlindoRepository.agregarPersonal(nombre, rol, idStr)
                btnGuardar.isEnabled = true
                btnGuardar.text = "Guardar y seleccionar"
                res.onSuccess { nuevo ->
                    cargarSpinnersPersonal(
                        preseleccionarOperadorId = if (rol == "OPERADOR") nuevo.id else null,
                        preseleccionarSupervisorId = if (rol == "SUPERVISOR") nuevo.id else null
                    )
                    Toast.makeText(this@ValidacionActivity, "Registrado: ${nuevo.nombre}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.onFailure { err ->
                    if (err is ApiException && err.fieldErrors.isNotEmpty()) {
                        err.fieldErrors["nombre"]?.let { etNombre.error = it }
                        err.fieldErrors["numero_empleado"]?.let { etId.error = it }
                    }
                    Toast.makeText(this@ValidacionActivity, err.message ?: "No se pudo registrar", Toast.LENGTH_LONG).show()
                }
            }
        }

        dialog.showExpanded()
    }
}
