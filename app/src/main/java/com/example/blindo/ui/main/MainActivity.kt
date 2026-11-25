package com.example.blindo.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blindo.R
import com.example.blindo.data.model.AsignacionEppDetalle
import com.example.blindo.data.model.OrdenActiva
import com.example.blindo.data.model.Personal
import com.example.blindo.data.model.RegistroAuditoria
import com.example.blindo.data.remote.ApiException
import com.example.blindo.data.repository.MockBlindoRepository
import com.example.blindo.data.session.EvidenciaStore
import com.example.blindo.data.session.SessionManager
import com.example.blindo.databinding.ActivityMainBinding
import com.example.blindo.databinding.DialogDetalleOrdenBinding
import com.example.blindo.databinding.DialogDetallePersonalBinding
import com.example.blindo.domain.DatosPermisoPdf
import com.example.blindo.domain.EppDateUtils
import com.example.blindo.domain.EstadoEpp
import com.example.blindo.domain.PermisoPdfGenerator
import com.example.blindo.ui.PdfPreviewDialogHelper
import com.example.blindo.ui.historial.HistorialAdapter
import com.example.blindo.ui.login.LoginActivity
import com.example.blindo.ui.personal.PersonalAdapter
import com.example.blindo.ui.personal.RegistroPersonalActivity
import com.example.blindo.ui.showExpanded
import com.example.blindo.ui.validacion.ValidacionActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla principal de Blindo SGC con 4 pestañas:
 * Taller (órdenes activas), Personal (CRUD), EPP (inventario/caducidades) y Bitácora (historial + fotos + PDF).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ordenesAdapter: OrdenesAdapter
    private lateinit var personalTabAdapter: PersonalAdapter
    private lateinit var bitacoraTabAdapter: HistorialAdapter
    private lateinit var inventarioEppAdapter: InventarioEppAdapter

    private var zonaFiltroSeleccionada: String = "Todas"
    private var busquedaPersonalTexto: String = ""
    private var estadoEppFiltro: String = "Todos"
    private var busquedaEppTexto: String = ""
    private var estadoBitacoraFiltro: String = "Todos"
    private var busquedaBitacoraTexto: String = ""

    // Cachés en memoria para que las búsquedas y filtros sean instantáneos sin saturar la API
    private var cacheOrdenes: List<OrdenActiva> = emptyList()
    private var cacheBitacora: List<RegistroAuditoria> = emptyList()
    private var cachePersonal: List<Personal> = emptyList()
    private var cacheInventario: List<AsignacionEppDetalle> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MockBlindoRepository.init(this)

        if (!SessionManager.isLoggedIn(this)) {
            irALoginPorSesionExpirada(null)
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val nombreUsuario = SessionManager.getUserName(this)
        // Mostramos los dos primeros nombres en el saludo para evitar que se corte con "..."
        val saludoCorto = nombreUsuario.trim().split(Regex("\\s+")).take(2).joinToString(" ")
        binding.tvHeaderTitle.text = "Hola, $saludoCorto"

        setupRecyclerViews()
        setupListeners()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        recargarTodo()
    }

    private fun recargarTodo() {
        lifecycleScope.launch {
            MockBlindoRepository.ensureCatalogosNormativos()
            setupChips()
            updateDashboardYBitacora()
            refrescarTabPersonal()
            refrescarTabInventario()
        }
    }

    private fun manejarErrorApi(err: Throwable) {
        val apiErr = err as? ApiException
        if (apiErr?.statusCode == 401) {
            irALoginPorSesionExpirada("Tu sesión expiró (401). Vuelve a iniciar sesión.")
            return
        }
        Snackbar.make(binding.root, err.message ?: "Error de conexión con el servidor", Snackbar.LENGTH_LONG).show()
    }

    private fun irALoginPorSesionExpirada(mensaje: String?) {
        SessionManager.clearSession(this)
        if (!mensaje.isNullOrBlank()) {
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        }
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setupRecyclerViews() {
        ordenesAdapter = OrdenesAdapter(
            ordenes = emptyList(),
            onItemClick = { orden -> mostrarDetalleOrdenModal(orden) },
            onFinalizarClick = { orden -> confirmarFinalizarOrden(orden) }
        )
        binding.rvOrdenes.layoutManager = LinearLayoutManager(this)
        binding.rvOrdenes.adapter = ordenesAdapter

        personalTabAdapter = PersonalAdapter(emptyList()) { personal ->
            mostrarFichaPersonalModal(personal)
        }
        binding.rvPersonalTab.layoutManager = LinearLayoutManager(this)
        binding.rvPersonalTab.adapter = personalTabAdapter

        inventarioEppAdapter = InventarioEppAdapter(
            items = emptyList(),
            onItemClick = { detalle -> mostrarDialogEditarEpp(detalle) },
            onRenovarClick = { detalle -> confirmarRenovarEpp(detalle) }
        )
        binding.rvInventarioEpp.layoutManager = LinearLayoutManager(this)
        binding.rvInventarioEpp.adapter = inventarioEppAdapter

        bitacoraTabAdapter = HistorialAdapter(
            registros = emptyList(),
            onVerPdfClick = { registro -> abrirPdfDesdeBitacora(registro) },
            onVerFotoClick = { registro, fotoPath -> mostrarFotoEvidenciaAmpliada(registro, fotoPath) }
        )
        binding.rvBitacoraTab.layoutManager = LinearLayoutManager(this)
        binding.rvBitacoraTab.adapter = bitacoraTabAdapter
    }

    private fun setupListeners() {
        binding.btnNuevaValidacion.setOnClickListener {
            startActivity(Intent(this, ValidacionActivity::class.java))
        }

        binding.btnAbrirRegistroPersonalModal.setOnClickListener {
            startActivity(Intent(this, RegistroPersonalActivity::class.java))
        }

        binding.btnAbrirAsignarEppModal.setOnClickListener {
            mostrarDialogAsignarEpp()
        }

        // Atajos interactivos en los KPIs del panel principal para navegar con un toque
        binding.kpiActivasContainer.setOnClickListener {
            zonaFiltroSeleccionada = "Todas"
            (binding.chipGroupZonas.getChildAt(0) as? Chip)?.isChecked = true
            aplicarFiltroTaller()
            Toast.makeText(this, "Mostrando todas las órdenes activas", Toast.LENGTH_SHORT).show()
        }

        binding.kpiAlertasContainer.setOnClickListener {
            binding.bottomNavigation.selectedItemId = R.id.nav_bitacora
            binding.chipBitacoraAmarillo.isChecked = true
        }

        binding.kpiBloqueosContainer.setOnClickListener {
            binding.bottomNavigation.selectedItemId = R.id.nav_bitacora
            binding.chipBitacoraRojo.isChecked = true
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Deseas cerrar tu sesión actual y revocar el token Sanctum?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Cerrar sesión") { _, _ ->
                    lifecycleScope.launch {
                        MockBlindoRepository.cerrarSesionServidor()
                        irALoginPorSesionExpirada(null)
                    }
                }
                .show()
        }

        binding.etBuscarPersonalTab.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                busquedaPersonalTexto = s?.toString()?.trim()?.lowercase() ?: ""
                aplicarFiltroPersonal()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etBuscarInventario.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                busquedaEppTexto = s?.toString()?.trim()?.lowercase() ?: ""
                aplicarFiltroInventario()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupEstadoEpp.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                estadoEppFiltro = when (checkedIds.first()) {
                    binding.chipEppCaducados.id -> "Caducados"
                    binding.chipEppAlerta.id -> "En Alerta"
                    binding.chipEppVigentes.id -> "Vigentes"
                    else -> "Todos"
                }
                aplicarFiltroInventario()
            }
        }

        binding.etBuscarBitacora.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                busquedaBitacoraTexto = s?.toString()?.trim()?.lowercase() ?: ""
                aplicarFiltroBitacora()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupEstadoBitacora.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                estadoBitacoraFiltro = when (checkedIds.first()) {
                    binding.chipBitacoraRojo.id -> "ROJO_BLOQUEADO"
                    binding.chipBitacoraAmarillo.id -> "AMARILLO_PRECAUCION"
                    binding.chipBitacoraVerde.id -> "VERDE_AUTORIZADO"
                    else -> "Todos"
                }
                aplicarFiltroBitacora()
            }
        }
    }

    private suspend fun setupChips() {
        val chipGroup = binding.chipGroupZonas
        chipGroup.removeAllViews()

        val todasZonas = listOf("Todas") + MockBlindoRepository.getZonas()
        if (zonaFiltroSeleccionada !in todasZonas) {
            zonaFiltroSeleccionada = "Todas"
        }

        todasZonas.forEach { zona ->
            val nombreCorto = if (zona == "Todas") "Todas" else (zona.split(" - ").firstOrNull() ?: zona)
            val chip = Chip(this).apply {
                text = nombreCorto
                isCheckable = true
                isChecked = (zonaFiltroSeleccionada == zona)
                setChipBackgroundColorResource(R.color.chip_bg_selector)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.industrial_text_primary))
                setChipStrokeColorResource(R.color.industrial_border)
                chipStrokeWidth = 2f
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        zonaFiltroSeleccionada = zona
                        aplicarFiltroTaller()
                    }
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_taller -> {
                    mostrarTab(binding.layoutTabTaller)
                    binding.tvHeaderSubtitle.text = "Supervisión de piso en vivo"
                    lifecycleScope.launch { updateDashboardYBitacora() }
                    true
                }
                R.id.nav_personal -> {
                    mostrarTab(binding.layoutTabPersonal)
                    binding.tvHeaderSubtitle.text = "Plantilla de personal SGC"
                    lifecycleScope.launch { refrescarTabPersonal() }
                    true
                }
                R.id.nav_epp -> {
                    mostrarTab(binding.layoutTabEpp)
                    binding.tvHeaderSubtitle.text = "Control y caducidad de EPP"
                    lifecycleScope.launch { refrescarTabInventario() }
                    true
                }
                R.id.nav_bitacora -> {
                    mostrarTab(binding.layoutTabBitacora)
                    binding.tvHeaderSubtitle.text = "Historial de auditoría"
                    lifecycleScope.launch { updateDashboardYBitacora() }
                    true
                }
                else -> false
            }
        }
    }

    private fun mostrarTab(targetTab: View) {
        binding.layoutTabTaller.visibility = View.GONE
        binding.layoutTabPersonal.visibility = View.GONE
        binding.layoutTabEpp.visibility = View.GONE
        binding.layoutTabBitacora.visibility = View.GONE

        targetTab.visibility = View.VISIBLE

        when (targetTab) {
            binding.layoutTabTaller -> binding.rvOrdenes.scheduleLayoutAnimation()
            binding.layoutTabPersonal -> binding.rvPersonalTab.scheduleLayoutAnimation()
            binding.layoutTabEpp -> binding.rvInventarioEpp.scheduleLayoutAnimation()
            binding.layoutTabBitacora -> binding.rvBitacoraTab.scheduleLayoutAnimation()
        }
    }

    private suspend fun updateDashboardYBitacora() {
        MockBlindoRepository.getValidacionesSeparadasResult()
            .onSuccess { (activas, auditorias) ->
                cacheOrdenes = activas
                cacheBitacora = auditorias
                aplicarFiltroTaller()
                aplicarFiltroBitacora()
            }
            .onFailure { err ->
                manejarErrorApi(err)
            }
    }

    private fun aplicarFiltroTaller() {
        val todasLasActivas = cacheOrdenes
        val bloqueosTotales = cacheBitacora.count { it.resultado == "ROJO_BLOQUEADO" }
        val alertasActivas = todasLasActivas.count { it.estado == "AMARILLO" }

        val filtradas = if (zonaFiltroSeleccionada == "Todas") {
            todasLasActivas
        } else {
            todasLasActivas.filter { it.zona == zonaFiltroSeleccionada }
        }

        ordenesAdapter.updateData(filtradas)

        if (filtradas.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvOrdenes.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvOrdenes.visibility = View.VISIBLE
        }

        when {
            alertasActivas > 0 -> {
                val yellow = ContextCompat.getColor(this, R.color.status_yellow)
                binding.cardHeroBanner.strokeColor = yellow
                binding.viewHeroPulse.setBackgroundResource(R.drawable.circle_yellow)
                binding.tvHeroTitle.text = "Precaución activa en piso"
                binding.tvHeroTitle.setTextColor(yellow)
                binding.tvHeroSubtitle.text = "$alertasActivas orden(es) condicionada(s) bajo supervisión"
            }
            todasLasActivas.isNotEmpty() -> {
                val green = ContextCompat.getColor(this, R.color.status_green)
                binding.cardHeroBanner.strokeColor = green
                binding.viewHeroPulse.setBackgroundResource(R.drawable.circle_green)
                binding.tvHeroTitle.text = "Piso blindado y operando"
                binding.tvHeroTitle.setTextColor(green)
                binding.tvHeroSubtitle.text = "${todasLasActivas.size} orden(es) verificada(s) bajo norma STPS"
            }
            else -> {
                val green = ContextCompat.getColor(this, R.color.status_green)
                binding.cardHeroBanner.strokeColor = green
                binding.viewHeroPulse.setBackgroundResource(R.drawable.circle_green)
                binding.tvHeroTitle.text = "Piso blindado y despejado"
                binding.tvHeroTitle.setTextColor(green)
                binding.tvHeroSubtitle.text = "Sin riesgos activos ni choques SIMOPS detectados"
            }
        }

        binding.tvActivosKpi.text = todasLasActivas.size.toString()
        binding.tvAlertasKpi.text = alertasActivas.toString()
        binding.tvBloqueosKpi.text = bloqueosTotales.toString()
    }

    private fun aplicarFiltroBitacora() {
        val filtrados = cacheBitacora.filter { reg ->
            val coincideTexto = busquedaBitacoraTexto.isEmpty() ||
                reg.trabajador.lowercase().contains(busquedaBitacoraTexto) ||
                reg.trabajo.lowercase().contains(busquedaBitacoraTexto) ||
                reg.supervisor.lowercase().contains(busquedaBitacoraTexto) ||
                reg.zona.lowercase().contains(busquedaBitacoraTexto) ||
                reg.motivo.lowercase().contains(busquedaBitacoraTexto)

            val coincideEstado = estadoBitacoraFiltro == "Todos" || reg.resultado == estadoBitacoraFiltro
            coincideTexto && coincideEstado
        }
        bitacoraTabAdapter.updateData(filtrados)
        binding.layoutEmptyBitacora.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
        binding.rvBitacoraTab.visibility = if (filtrados.isEmpty()) View.GONE else View.VISIBLE
    }

    // Descarga la plantilla de personal desde Laravel y aplica el filtro actual
    private suspend fun refrescarTabPersonal() {
        MockBlindoRepository.getPersonalResult()
            .onSuccess { todos ->
                cachePersonal = todos
                aplicarFiltroPersonal()
            }
            .onFailure { err ->
                manejarErrorApi(err)
            }
    }

    // Filtra la lista de personal en memoria sin volver a consultar la red
    private fun aplicarFiltroPersonal() {
        val filtrados = if (busquedaPersonalTexto.isEmpty()) {
            cachePersonal
        } else {
            cachePersonal.filter {
                it.nombre.lowercase().contains(busquedaPersonalTexto) ||
                    it.numeroEmpleado.lowercase().contains(busquedaPersonalTexto)
            }
        }
        personalTabAdapter.updateData(filtrados)
        binding.layoutEmptyPersonal.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
        binding.rvPersonalTab.visibility = if (filtrados.isEmpty()) View.GONE else View.VISIBLE
    }

    // Descarga las asignaciones de EPP desde Laravel y aplica búsqueda + filtro de caducidad
    private suspend fun refrescarTabInventario() {
        MockBlindoRepository.getAsignacionesConDetalleResult()
            .onSuccess { todos ->
                cacheInventario = todos
                aplicarFiltroInventario()
            }
            .onFailure { err ->
                manejarErrorApi(err)
            }
    }

    // Filtra el inventario de EPP en memoria según búsqueda y estado (Caducados, Alerta, Vigentes)
    private fun aplicarFiltroInventario() {
        val filtrados = cacheInventario.filter { det ->
            val coincideTexto = busquedaEppTexto.isEmpty() ||
                det.epp.nombre.lowercase().contains(busquedaEppTexto) ||
                det.personal.nombre.lowercase().contains(busquedaEppTexto) ||
                det.personal.numeroEmpleado.lowercase().contains(busquedaEppTexto)

            val coincideEstado = when (estadoEppFiltro) {
                "Caducados" -> det.estado == EstadoEpp.CADUCADO
                "En Alerta" -> det.estado == EstadoEpp.ALERTA
                "Vigentes" -> det.estado == EstadoEpp.VIGENTE || det.estado == EstadoEpp.PERMANENTE
                else -> true
            }
            coincideTexto && coincideEstado
        }

        inventarioEppAdapter.updateData(filtrados)
        binding.layoutEmptyEpp.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
        binding.rvInventarioEpp.visibility = if (filtrados.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun mostrarDetalleOrdenModal(orden: OrdenActiva) {
        lifecycleScope.launch {
            val trabajo = MockBlindoRepository.getTrabajoPorId(orden.trabajoId)
            val dialog = BottomSheetDialog(this@MainActivity)
            val dialogBinding = DialogDetalleOrdenBinding.inflate(layoutInflater)
            dialog.setContentView(dialogBinding.root)

            dialogBinding.tvDialogTrabajo.text = orden.nombreTrabajo
            dialogBinding.tvDialogNormaRiesgo.text = "${trabajo?.norma ?: "STPS"} · Riesgo ${trabajo?.riesgo ?: "Medio"}"
            dialogBinding.tvDialogOperador.text = orden.trabajador
            dialogBinding.tvDialogSupervisor.text = orden.supervisor
            dialogBinding.tvDialogZona.text = orden.zona
            dialogBinding.tvDialogTurno.text = orden.turno
            dialogBinding.etDialogNotas.setText(orden.notas)

            if (orden.estado == "VERDE") {
                dialogBinding.tvDialogEstado.text = "Autorizado y blindado"
                dialogBinding.tvDialogEstado.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_green))
            } else {
                dialogBinding.tvDialogEstado.text = "Trabajo condicionado (precaución)"
                dialogBinding.tvDialogEstado.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_yellow))
            }

            dialogBinding.btnDialogGuardarNotas.setOnClickListener {
                val nuevasNotas = dialogBinding.etDialogNotas.text.toString().trim()
                lifecycleScope.launch {
                    MockBlindoRepository.actualizarOrdenNotas(orden.id, nuevasNotas)
                        .onSuccess {
                            updateDashboardYBitacora()
                            Snackbar.make(binding.root, "Notas actualizadas en servidor", Snackbar.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .onFailure { err -> manejarErrorApi(err) }
                }
            }

            dialogBinding.btnDialogExportarPdf.setOnClickListener {
                lifecycleScope.launch {
                    val evidencia = EvidenciaStore.obtener(this@MainActivity, orden.id)
                    val epps = evidencia?.eppsVerificados?.takeIf { it.isNotEmpty() }
                        ?: MockBlindoRepository.getEppsParaTrabajo(orden.trabajoId)
                            .filter { it.second }
                            .map { it.first.nombre }
                    val datosPdf = DatosPermisoPdf(
                        folio = evidencia?.folio ?: "PTW-${orden.id.padStart(4, '0')}",
                        estado = orden.estado,
                        trabajo = orden.nombreTrabajo,
                        norma = trabajo?.norma ?: "NOM-017-STPS",
                        riesgo = trabajo?.riesgo ?: "Medio",
                        operador = orden.trabajador,
                        supervisor = orden.supervisor,
                        zona = orden.zona,
                        turno = orden.turno,
                        diagnostico = dialogBinding.etDialogNotas.text.toString().trim().ifEmpty { "Orden verificada bajo norma STPS." },
                        eppsVerificados = epps,
                        evidenciaPath = evidencia?.evidenciaPath
                    )
                    dialog.dismiss()
                    mostrarVistaPreviaPdf(datosPdf)
                }
            }

            dialogBinding.btnDialogFinalizar.setOnClickListener {
                dialog.dismiss()
                confirmarFinalizarOrden(orden)
            }

            dialogBinding.btnDialogCerrar.setOnClickListener { dialog.dismiss() }
            dialog.showExpanded()
        }
    }

    private fun mostrarVistaPreviaPdf(datos: DatosPermisoPdf) {
        lifecycleScope.launch {
            val res = withContext(Dispatchers.IO) {
                try {
                    val file = PermisoPdfGenerator.generarPermisoPdf(this@MainActivity, datos)
                    val bmp = PermisoPdfGenerator.renderizarPrimeraPagina(file)
                    Pair(file, bmp)
                } catch (_: Exception) {
                    null
                }
            }
            if (res == null) {
                Toast.makeText(this@MainActivity, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val (pdfFile, previewBitmap) = res
            if (previewBitmap == null) {
                Toast.makeText(this@MainActivity, "No se pudo renderizar la vista previa", Toast.LENGTH_SHORT).show()
                return@launch
            }
            PdfPreviewDialogHelper.mostrar(
                activity = this@MainActivity,
                titulo = "Permiso de Trabajo STPS",
                subtitulo = "Folio ${datos.folio} · Respaldo STPS",
                bitmap = previewBitmap,
                textoBotonPrincipal = "Compartir / Guardar PDF"
            ) {
                PermisoPdfGenerator.compartirPdf(this@MainActivity, pdfFile)
            }
        }
    }

    private fun abrirPdfDesdeBitacora(registro: RegistroAuditoria) {
        lifecycleScope.launch {
            val trabajo = if (registro.trabajoId > 0) MockBlindoRepository.getTrabajoPorId(registro.trabajoId) else null
            val evidencia = EvidenciaStore.obtener(this@MainActivity, registro.id)
            val epps = evidencia?.eppsVerificados?.takeIf { it.isNotEmpty() }
                ?: if (registro.trabajoId > 0) {
                    MockBlindoRepository.getEppsParaTrabajo(registro.trabajoId)
                        .filter { it.second }
                        .map { it.first.nombre }
                } else {
                    emptyList()
                }

            val estadoCorto = when (registro.resultado) {
                "VERDE_AUTORIZADO" -> "VERDE"
                "AMARILLO_PRECAUCION" -> "AMARILLO"
                "ROJO_BLOQUEADO" -> "ROJO"
                else -> registro.resultado
            }

            val datosPdf = DatosPermisoPdf(
                folio = evidencia?.folio ?: "PTW-${registro.id.padStart(4, '0')}",
                estado = estadoCorto,
                trabajo = registro.trabajo,
                norma = trabajo?.norma ?: "NOM-017-STPS",
                riesgo = trabajo?.riesgo ?: "Medio",
                operador = registro.trabajador,
                supervisor = registro.supervisor,
                zona = registro.zona,
                turno = registro.turno.ifBlank { "No especificado" },
                diagnostico = registro.motivo.ifBlank { "Registro de auditoría SGC." },
                eppsVerificados = epps,
                evidenciaPath = evidencia?.evidenciaPath,
                fechaMillis = registro.fechaHora
            )
            mostrarVistaPreviaPdf(datosPdf)
        }
    }

    private fun mostrarFotoEvidenciaAmpliada(registro: RegistroAuditoria, fotoPath: String) {
        val bmp = android.graphics.BitmapFactory.decodeFile(fotoPath)
        if (bmp == null) {
            Toast.makeText(this, "No se pudo cargar la foto de evidencia", Toast.LENGTH_SHORT).show()
            return
        }
        PdfPreviewDialogHelper.mostrar(
            activity = this,
            titulo = "Evidencia fotográfica",
            subtitulo = "${registro.trabajo} · ${registro.trabajador}",
            bitmap = bmp,
            textoBotonPrincipal = "Ver / Descargar PDF completo"
        ) { dialog ->
            dialog.dismiss()
            abrirPdfDesdeBitacora(registro)
        }
    }

    private fun confirmarFinalizarOrden(orden: OrdenActiva) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Finalizar orden en piso")
            .setMessage("¿Confirmas que '${orden.nombreTrabajo}' (${orden.trabajador}) concluyó y puede retirarse de ${orden.zona}?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Finalizar") { _, _ ->
                lifecycleScope.launch {
                    MockBlindoRepository.finalizarOrden(orden.id)
                        .onSuccess {
                            updateDashboardYBitacora()
                            Snackbar.make(binding.root, "Orden finalizada y archivada en Bitácora", Snackbar.LENGTH_SHORT).show()
                        }
                        .onFailure { err -> manejarErrorApi(err) }
                }
            }
            .show()
    }

    private fun mostrarFichaPersonalModal(personal: Personal) {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogDetallePersonalBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val partes = personal.nombre.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val iniciales = if (partes.size >= 2) {
            "${partes[0].firstOrNull() ?: ""}${partes[1].firstOrNull() ?: ""}".uppercase()
        } else {
            personal.nombre.take(2).uppercase()
        }

        dialogBinding.tvDialogAvatar.text = iniciales
        dialogBinding.tvDialogNombre.text = personal.nombre
        dialogBinding.tvDialogRol.text = if (personal.rol == "OPERADOR") "Operador de piso" else "Supervisor de seguridad SGC"
        dialogBinding.tvDialogNumEmpleado.text = personal.numeroEmpleado

        val primerNombre = personal.nombre.split(" ").firstOrNull() ?: "personal"

        dialogBinding.btnDialogAsignarEppDirecto.setOnClickListener {
            dialog.dismiss()
            mostrarDialogAsignarEpp(personal.id)
        }

        dialogBinding.btnDialogEppCaducidad.text = "Ver EPP de $primerNombre"
        dialogBinding.btnDialogEppCaducidad.setOnClickListener {
            dialog.dismiss()
            binding.bottomNavigation.selectedItemId = R.id.nav_epp
            binding.etBuscarInventario.setText(personal.nombre)
        }

        dialogBinding.btnDialogEditarPersonal.setOnClickListener {
            dialog.dismiss()
            mostrarDialogEditarPersonal(personal)
        }

        dialogBinding.btnDialogEliminarPersonal.setOnClickListener {
            dialog.dismiss()
            confirmarEliminarPersonal(personal)
        }

        dialogBinding.btnDialogCerrarPersonal.setOnClickListener { dialog.dismiss() }
        dialog.showExpanded()
    }

    private fun mostrarDialogEditarPersonal(personal: Personal) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_personal_rapido, null)
        dialog.setContentView(view)

        val tvTitulo = view.findViewById<TextView>(R.id.tvTituloPersonalModal)
        val etNombre = view.findViewById<TextInputEditText>(R.id.etNombrePersonalModal)
        val etNum = view.findViewById<TextInputEditText>(R.id.etNumEmpleadoModal)
        val toggleRol = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleRolPersonalModal)
        val btnGuardar = view.findViewById<MaterialButton>(R.id.btnGuardarPersonalModal)

        tvTitulo.text = "Editar personal"
        etNombre.setText(personal.nombre)
        etNum.setText(personal.numeroEmpleado)
        toggleRol.check(if (personal.rol == "SUPERVISOR") R.id.btnRolSupModal else R.id.btnRolOpModal)

        btnGuardar.setOnClickListener {
            val nuevoNombre = etNombre.text.toString().trim()
            val nuevoNum = etNum.text.toString().trim()
            val nuevoRol = if (toggleRol.checkedButtonId == R.id.btnRolSupModal) "SUPERVISOR" else "OPERADOR"

            btnGuardar.isEnabled = false
            lifecycleScope.launch {
                val res = MockBlindoRepository.editarPersonal(
                    personal.copy(nombre = nuevoNombre, numeroEmpleado = nuevoNum, rol = nuevoRol)
                )
                btnGuardar.isEnabled = true

                res.onSuccess {
                    refrescarTabPersonal()
                    refrescarTabInventario()
                    Snackbar.make(binding.root, "Trabajador actualizado: $nuevoNombre", Snackbar.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.onFailure { err ->
                    val apiErr = err as? ApiException
                    if (apiErr != null && apiErr.statusCode == 422) {
                        apiErr.fieldErrors["nombre"]?.let { etNombre.error = it }
                        apiErr.fieldErrors["numero_empleado"]?.let { etNum.error = it }
                    }
                    Toast.makeText(this@MainActivity, err.message ?: "No se pudo actualizar", Toast.LENGTH_LONG).show()
                }
            }
        }

        dialog.showExpanded()
    }

    private fun confirmarEliminarPersonal(personal: Personal) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar trabajador")
            .setMessage("¿Seguro que deseas eliminar a ${personal.nombre} (${personal.numeroEmpleado}) de la base de datos? También se borrarán sus asignaciones de EPP.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    MockBlindoRepository.eliminarPersonal(personal.id)
                        .onSuccess {
                            refrescarTabPersonal()
                            refrescarTabInventario()
                            Snackbar.make(binding.root, "${personal.nombre} eliminado", Snackbar.LENGTH_SHORT).show()
                        }
                        .onFailure { err -> manejarErrorApi(err) }
                }
            }
            .show()
    }

    private fun mostrarDialogAsignarEpp(trabajadorPreseleccionadoId: Int? = null) {
        lifecycleScope.launch {
            val personalRes = MockBlindoRepository.getPersonalResult()
            val personal = personalRes.getOrElse { err ->
                manejarErrorApi(err)
                return@launch
            }
            if (personal.isEmpty()) {
                Toast.makeText(this@MainActivity, "Primero registra al menos un trabajador en la pestaña Personal", Toast.LENGTH_LONG).show()
                return@launch
            }
            val eppsOrdenados = MockBlindoRepository.getEppsGlobales().sortedBy { it.nombre }

            val dialog = BottomSheetDialog(this@MainActivity)
            val view = layoutInflater.inflate(R.layout.dialog_asignar_epp, null)
            dialog.setContentView(view)

            val spinnerTrabajador = view.findViewById<Spinner>(R.id.spinnerTrabajadorAsignar)
            val spinnerEpp = view.findViewById<Spinner>(R.id.spinnerEppAsignar)
            val tvSugerenciaNorma = view.findViewById<TextView>(R.id.tvSugerenciaNorma)
            val switchTieneCaducidad = view.findViewById<SwitchMaterial>(R.id.switchTieneCaducidad)
            val layoutOpcionesCaducidad = view.findViewById<View>(R.id.layoutOpcionesCaducidad)
            val etVidaUtil = view.findViewById<TextInputEditText>(R.id.etVidaUtilPersonalizada)
            val spinnerTipoTiempo = view.findViewById<Spinner>(R.id.spinnerTipoTiempo)
            val btnConfirmar = view.findViewById<MaterialButton>(R.id.btnConfirmarAsignacionEpp)

            spinnerTrabajador.adapter = ArrayAdapter(
                this@MainActivity,
                R.layout.item_spinner_industrial,
                personal.map { "${it.nombre} (${it.numeroEmpleado})" }
            ).apply { setDropDownViewResource(R.layout.item_spinner_dropdown_industrial) }

            if (trabajadorPreseleccionadoId != null) {
                val idx = personal.indexOfFirst { it.id == trabajadorPreseleccionadoId }
                if (idx >= 0) spinnerTrabajador.setSelection(idx)
            }

            spinnerEpp.adapter = ArrayAdapter(
                this@MainActivity,
                R.layout.item_spinner_industrial,
                eppsOrdenados.map { it.nombre }
            ).apply { setDropDownViewResource(R.layout.item_spinner_dropdown_industrial) }

            spinnerTipoTiempo.adapter = ArrayAdapter(
                this@MainActivity,
                R.layout.item_spinner_industrial,
                listOf("Meses", "Años")
            ).apply { setDropDownViewResource(R.layout.item_spinner_dropdown_industrial) }

            fun actualizarSugerenciaParaEpp(pos: Int) {
                if (pos !in eppsOrdenados.indices) return
                val epp = eppsOrdenados[pos]

                if (EppDateUtils.esPermanente(epp.vidaUtilMeses)) {
                    switchTieneCaducidad.isChecked = false
                    layoutOpcionesCaducidad.visibility = View.GONE
                    tvSugerenciaNorma.text = "Equipo permanente (sin caducidad obligatoria). Activa el switch si el fabricante indica vencimiento."
                    tvSugerenciaNorma.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.industrial_text_secondary))
                } else {
                    switchTieneCaducidad.isChecked = true
                    layoutOpcionesCaducidad.visibility = View.VISIBLE
                    tvSugerenciaNorma.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.industrial_accent_glow))

                    if (epp.vidaUtilMeses >= 12 && epp.vidaUtilMeses % 12 == 0) {
                        val anios = epp.vidaUtilMeses / 12
                        etVidaUtil.setText(anios.toString())
                        spinnerTipoTiempo.setSelection(1)
                        tvSugerenciaNorma.text = "Sugerencia STPS: $anios ${if (anios == 1) "año" else "años"} (${epp.vidaUtilMeses}m). Ajusta según marca."
                    } else {
                        etVidaUtil.setText(epp.vidaUtilMeses.toString())
                        spinnerTipoTiempo.setSelection(0)
                        tvSugerenciaNorma.text = "Sugerencia STPS: ${epp.vidaUtilMeses} meses. Ajusta según marca."
                    }
                }
            }

            spinnerEpp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                    actualizarSugerenciaParaEpp(position)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            actualizarSugerenciaParaEpp(0)

            switchTieneCaducidad.setOnCheckedChangeListener { _, isChecked ->
                layoutOpcionesCaducidad.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (!isChecked) {
                    tvSugerenciaNorma.text = "Este equipo quedará registrado como permanente (sin caducidad)."
                } else {
                    actualizarSugerenciaParaEpp(spinnerEpp.selectedItemPosition)
                }
            }

            btnConfirmar.setOnClickListener {
                val trabPos = spinnerTrabajador.selectedItemPosition
                val eppPos = spinnerEpp.selectedItemPosition
                if (trabPos !in personal.indices || eppPos !in eppsOrdenados.indices) return@setOnClickListener

                val trabajador = personal[trabPos]
                val epp = eppsOrdenados[eppPos]

                val vidaUtilFinal = if (!switchTieneCaducidad.isChecked) {
                    9999
                } else {
                    val cantidad = etVidaUtil.text.toString().trim().toIntOrNull() ?: 0
                    if (cantidad <= 0) {
                        Toast.makeText(this@MainActivity, "Ingresa una cantidad válida mayor a 0", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (spinnerTipoTiempo.selectedItemPosition == 1) cantidad * 12 else cantidad
                }

                btnConfirmar.isEnabled = false
                lifecycleScope.launch {
                    val res = MockBlindoRepository.asignarOActualizarEpp(
                        personalId = trabajador.id,
                        eppId = epp.id,
                        nombreEpp = epp.nombre,
                        vidaUtilMeses = vidaUtilFinal
                    )
                    btnConfirmar.isEnabled = true

                    res.onSuccess { actualizado ->
                        refrescarTabInventario()
                        val msg = if (actualizado) {
                            "${epp.nombre} actualizado para ${trabajador.nombre}"
                        } else {
                            "${epp.nombre} asignado a ${trabajador.nombre}"
                        }
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }.onFailure { err ->
                        Toast.makeText(this@MainActivity, err.message ?: "Error al asignar EPP", Toast.LENGTH_LONG).show()
                    }
                }
            }

            dialog.showExpanded()
        }
    }

    private fun mostrarDialogEditarEpp(detalle: AsignacionEppDetalle) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_editar_epp, null)
        dialog.setContentView(view)

        val tvSubtitulo = view.findViewById<TextView>(R.id.tvEditEppSubtitulo)
        val switchCaducidad = view.findViewById<SwitchMaterial>(R.id.switchEditCaducidad)
        val layoutCaducidad = view.findViewById<View>(R.id.layoutEditCaducidad)
        val etVida = view.findViewById<TextInputEditText>(R.id.etEditVidaUtil)
        val spinnerTiempo = view.findViewById<Spinner>(R.id.spinnerEditTipoTiempo)
        val btnGuardar = view.findViewById<MaterialButton>(R.id.btnGuardarEdicionEpp)
        val btnEliminar = view.findViewById<MaterialButton>(R.id.btnEliminarAsignacionEpp)

        tvSubtitulo.text = "${detalle.personal.nombre} · ${detalle.epp.nombre}"

        spinnerTiempo.adapter = ArrayAdapter(
            this,
            R.layout.item_spinner_industrial,
            listOf("Meses", "Años")
        ).apply { setDropDownViewResource(R.layout.item_spinner_dropdown_industrial) }

        val vidaActual = detalle.asignacion.vidaUtilMesesAsignada
        if (EppDateUtils.esPermanente(vidaActual)) {
            switchCaducidad.isChecked = false
            layoutCaducidad.visibility = View.GONE
            etVida.setText("12")
            spinnerTiempo.setSelection(0)
        } else {
            switchCaducidad.isChecked = true
            layoutCaducidad.visibility = View.VISIBLE
            if (vidaActual >= 12 && vidaActual % 12 == 0) {
                etVida.setText((vidaActual / 12).toString())
                spinnerTiempo.setSelection(1)
            } else {
                etVida.setText(vidaActual.toString())
                spinnerTiempo.setSelection(0)
            }
        }

        switchCaducidad.setOnCheckedChangeListener { _, isChecked ->
            layoutCaducidad.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        btnGuardar.setOnClickListener {
            val nuevaVida = if (!switchCaducidad.isChecked) {
                9999
            } else {
                val cant = etVida.text.toString().trim().toIntOrNull() ?: 0
                if (cant <= 0) {
                    Toast.makeText(this, "Ingresa una cantidad válida mayor a 0", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (spinnerTiempo.selectedItemPosition == 1) cant * 12 else cant
            }

            btnGuardar.isEnabled = false
            lifecycleScope.launch {
                val res = MockBlindoRepository.editarVigenciaAsignacion(detalle.asignacion, nuevaVida)
                btnGuardar.isEnabled = true

                res.onSuccess {
                    refrescarTabInventario()
                    Snackbar.make(binding.root, "Vigencia actualizada en servidor", Snackbar.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.onFailure { err ->
                    Toast.makeText(this@MainActivity, err.message ?: "Error al actualizar", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnEliminar.setOnClickListener {
            dialog.dismiss()
            MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar asignación de EPP")
                .setMessage("¿Eliminar '${detalle.epp.nombre}' del inventario de ${detalle.personal.nombre}?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar") { _, _ ->
                    lifecycleScope.launch {
                        MockBlindoRepository.eliminarAsignacionEpp(detalle.asignacion.id)
                            .onSuccess {
                                refrescarTabInventario()
                                Snackbar.make(binding.root, "Asignación eliminada de servidor", Snackbar.LENGTH_SHORT).show()
                            }
                            .onFailure { err -> manejarErrorApi(err) }
                    }
                }
                .show()
        }

        dialog.showExpanded()
    }

    private fun confirmarRenovarEpp(detalle: AsignacionEppDetalle) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Renovar equipo EPP")
            .setMessage("¿Confirmas la entrega de un reemplazo nuevo de '${detalle.epp.nombre}' a ${detalle.personal.nombre}? Su vigencia reiniciará desde hoy.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                lifecycleScope.launch {
                    MockBlindoRepository.renovarEquipo(detalle.asignacion)
                        .onSuccess {
                            refrescarTabInventario()
                            Snackbar.make(binding.root, "Renovado en servidor: ${detalle.epp.nombre}", Snackbar.LENGTH_SHORT).show()
                        }
                        .onFailure { err -> manejarErrorApi(err) }
                }
            }
            .show()
    }
}
