package com.example.blindo.ui.personal

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blindo.R
import com.example.blindo.data.model.Personal
import com.example.blindo.data.remote.ApiException
import com.example.blindo.data.repository.MockBlindoRepository
import com.example.blindo.databinding.ActivityRegistroPersonalBinding
import com.example.blindo.databinding.DialogDetallePersonalBinding
import com.example.blindo.ui.showExpanded
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Pantalla para gestionar la plantilla de trabajadores (Operadores y Supervisores).
 * Permite crear (POST), listar (GET), editar (PUT) y eliminar (DELETE) directo en Laravel/MySQL.
 */
class RegistroPersonalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroPersonalBinding
    private lateinit var adapter: PersonalAdapter

    private var rolFiltroSeleccionado: String = "Todos"
    private var busquedaTexto: String = ""
    // Caché en memoria para filtrar al escribir sin saturar al servidor con peticiones por cada letra
    private var cachePersonal: List<Personal> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MockBlindoRepository.init(this)
        binding = ActivityRegistroPersonalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        setupFiltrosYBusqueda()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = PersonalAdapter(emptyList()) { personal ->
            mostrarFichaPersonalModal(personal)
        }
        binding.rvPersonal.layoutManager = LinearLayoutManager(this)
        binding.rvPersonal.adapter = adapter

        actualizarLista()
    }

    // Configura la barra de búsqueda y los filtros de rol (filtran en memoria al instante)
    private fun setupFiltrosYBusqueda() {
        binding.etBuscarPersonal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                busquedaTexto = s?.toString()?.trim()?.lowercase() ?: ""
                aplicarFiltroLocal()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupRolFiltro.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chipId = checkedIds.first()
                rolFiltroSeleccionado = when (chipId) {
                    binding.chipOperadores.id -> "OPERADOR"
                    binding.chipSupervisores.id -> "SUPERVISOR"
                    else -> "Todos"
                }
                aplicarFiltroLocal()
            }
        }
    }

    // Descarga la lista actualizada de personal desde Laravel (GET /api/personal)
    private fun actualizarLista() {
        lifecycleScope.launch {
            MockBlindoRepository.getPersonalResult()
                .onSuccess { todos ->
                    cachePersonal = todos
                    aplicarFiltroLocal()
                }
                .onFailure { err ->
                    Snackbar.make(binding.root, err.message ?: "Error al cargar personal", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    // Filtra la lista ya descargada según el rol seleccionado y el texto de búsqueda
    private fun aplicarFiltroLocal() {
        val filtrados = cachePersonal.filter { p ->
            val coincideRol = when (rolFiltroSeleccionado) {
                "OPERADOR" -> p.rol == "OPERADOR"
                "SUPERVISOR" -> p.rol == "SUPERVISOR"
                else -> true
            }
            val coincideBusqueda = busquedaTexto.isEmpty() ||
                p.nombre.lowercase().contains(busquedaTexto) ||
                p.numeroEmpleado.lowercase().contains(busquedaTexto)
            coincideRol && coincideBusqueda
        }
        adapter.updateData(filtrados)
    }

    private fun setupListeners() {
        binding.btnGuardarPersonal.setOnClickListener {
            val nombre = binding.etNombrePersonal.text.toString().trim()
            val numEmpleado = binding.etNumEmpleado.text.toString().trim()
            binding.etNombrePersonal.error = null
            binding.etNumEmpleado.error = null

            val rolSeleccionado = if (binding.toggleGroupRol.checkedButtonId == binding.btnRolSupervisor.id) {
                "SUPERVISOR"
            } else {
                "OPERADOR"
            }

            binding.btnGuardarPersonal.isEnabled = false

            lifecycleScope.launch {
                val resultado = MockBlindoRepository.agregarPersonal(nombre, rolSeleccionado, numEmpleado)
                binding.btnGuardarPersonal.isEnabled = true

                resultado.onSuccess { nuevo ->
                    binding.etNombrePersonal.text?.clear()
                    binding.etNumEmpleado.text?.clear()
                    actualizarLista()
                    Snackbar.make(binding.root, "Registrado en servidor: ${nuevo.nombre}", Snackbar.LENGTH_SHORT).show()
                }.onFailure { err ->
                    val apiErr = err as? ApiException
                    if (apiErr != null && apiErr.statusCode == 422) {
                        // Validación 422 directa de Laravel
                        apiErr.fieldErrors["nombre"]?.let { binding.etNombrePersonal.error = it }
                        apiErr.fieldErrors["numero_empleado"]?.let { binding.etNumEmpleado.error = it }
                    }
                    Snackbar.make(binding.root, err.message ?: "No se pudo registrar", Snackbar.LENGTH_LONG).show()
                }
            }
        }
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

        dialogBinding.btnDialogAsignarEppDirecto.visibility = View.GONE
        dialogBinding.btnDialogEppCaducidad.visibility = View.GONE

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
                    actualizarLista()
                    Snackbar.make(binding.root, "Datos actualizados: $nuevoNombre", Snackbar.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.onFailure { err ->
                    val apiErr = err as? ApiException
                    if (apiErr != null && apiErr.statusCode == 422) {
                        apiErr.fieldErrors["nombre"]?.let { etNombre.error = it }
                        apiErr.fieldErrors["numero_empleado"]?.let { etNum.error = it }
                    }
                    Toast.makeText(this@RegistroPersonalActivity, err.message ?: "Error al actualizar", Toast.LENGTH_LONG).show()
                }
            }
        }

        dialog.showExpanded()
    }

    private fun confirmarEliminarPersonal(personal: Personal) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar trabajador")
            .setMessage("¿Eliminar a ${personal.nombre} (${personal.numeroEmpleado}) de la base de datos? También se eliminarán sus asignaciones de EPP.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    MockBlindoRepository.eliminarPersonal(personal.id)
                        .onSuccess {
                            actualizarLista()
                            Snackbar.make(binding.root, "${personal.nombre} eliminado", Snackbar.LENGTH_SHORT).show()
                        }
                        .onFailure { err ->
                            Snackbar.make(binding.root, err.message ?: "Error al eliminar", Snackbar.LENGTH_LONG).show()
                        }
                }
            }
            .show()
    }
}
