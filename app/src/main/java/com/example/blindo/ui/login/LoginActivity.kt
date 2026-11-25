package com.example.blindo.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.blindo.R
import com.example.blindo.data.remote.ApiException
import com.example.blindo.data.repository.MockBlindoRepository
import com.example.blindo.data.session.SessionManager
import com.example.blindo.databinding.ActivityLoginBinding
import com.example.blindo.ui.BlindoNotice
import com.example.blindo.ui.InputValidators
import com.example.blindo.ui.main.MainActivity
import com.example.blindo.ui.showExpanded
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Pantalla de inicio de sesión y registro de cuentas.
 * Se conecta a Laravel Sanctum, guarda el token en SharedPreferences y muestra errores 401/422 sin crashear.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        MockBlindoRepository.init(this)

        if (SessionManager.isLoggedIn(this)) {
            irADashboard()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        InputValidators.aplicarFiltroCodigoEmpleado(binding.etUsuario)

        intent.getStringExtra("AVISO_SESION")?.takeIf { it.isNotBlank() }?.let { aviso ->
            binding.root.post {
                BlindoNotice.alerta(this, "Sesión finalizada", aviso)
            }
        }

        lifecycleScope.launch {
            MockBlindoRepository.ensureCatalogosNormativos()
        }

        binding.btnLogin.setOnClickListener {
            intentarLogin()
        }

        binding.btnAbrirRegistroCuenta.setOnClickListener {
            mostrarDialogRegistroCuenta()
        }
    }

    // Envía número de empleado y contraseña a POST /api/login y guarda el token si es correcto
    private fun intentarLogin() {
        val noEmpleado = binding.etUsuario.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.tilUsuario.error = null
        binding.tilPassword.error = null

        binding.btnLogin.text = ""
        binding.progressLogin.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            val resultado = MockBlindoRepository.autenticarUsuario(noEmpleado, password)

            resultado.onSuccess { (usuario, sanctumToken) ->
                SessionManager.saveSession(
                    context = this@LoginActivity,
                    userId = usuario.id,
                    nombre = usuario.nombre,
                    numeroEmpleado = usuario.numeroEmpleado,
                    rol = usuario.rol,
                    token = sanctumToken
                )
                val saludo = usuario.nombre.trim().split(Regex("\\s+")).take(2).joinToString(" ")
                irADashboard(
                    bienvenidaTitulo = "¡Bienvenido, $saludo!",
                    bienvenidaSub = "Sesión iniciada · Piso listo para supervisar"
                )
            }.onFailure { err ->
                binding.btnLogin.text = "Entrar al sistema"
                binding.progressLogin.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                val apiErr = err as? ApiException
                when (apiErr?.statusCode) {
                    422 -> {
                        apiErr.fieldErrors["numero_empleado"]?.let { binding.tilUsuario.error = it }
                        apiErr.fieldErrors["password"]?.let { binding.tilPassword.error = it }
                        BlindoNotice.alerta(this@LoginActivity, "Completa tus datos", apiErr.message)
                    }
                    401 -> {
                        binding.etPassword.text?.clear()
                        BlindoNotice.error(this@LoginActivity, "Acceso denegado", apiErr.message)
                    }
                    0 -> {
                        BlindoNotice.error(this@LoginActivity, "Sin conexión", apiErr.message)
                    }
                    else -> {
                        BlindoNotice.error(
                            this@LoginActivity,
                            "No se pudo iniciar sesión",
                            err.message ?: "Intenta nuevamente en unos segundos."
                        )
                    }
                }
            }
        }
    }

    // Abre el modal para crear una cuenta nueva en MySQL (POST /api/register)
    private fun mostrarDialogRegistroCuenta() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_registro_usuario, null)
        dialog.setContentView(view)

        val etNombre = view.findViewById<TextInputEditText>(R.id.etRegNombre)
        val etEmp = view.findViewById<TextInputEditText>(R.id.etRegNumEmpleado)
        val toggleRol = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleRegRol)
        val etPass = view.findViewById<TextInputEditText>(R.id.etRegPassword)
        val etPassConfirm = view.findViewById<TextInputEditText>(R.id.etRegPasswordConfirm)
        val btnCrear = view.findViewById<MaterialButton>(R.id.btnCrearCuentaConfirm)

        // Bloquea números en el nombre y caracteres inválidos en el código de empleado al escribir
        InputValidators.aplicarFiltroSoloNombre(etNombre)
        InputValidators.aplicarFiltroCodigoEmpleado(etEmp)

        btnCrear.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val numEmp = etEmp.text.toString().trim()
            val pass = etPass.text.toString().trim()
            val passConfirm = etPassConfirm.text.toString().trim()

            etNombre.error = null
            etEmp.error = null
            etPass.error = null
            etPassConfirm.error = null

            InputValidators.validarNombrePersona(nombre)?.let { errMsg ->
                etNombre.error = errMsg
                BlindoNotice.alerta(this, "Nombre inválido", errMsg, window = dialog.window)
                return@setOnClickListener
            }
            InputValidators.validarNumeroEmpleado(numEmp)?.let { errMsg ->
                etEmp.error = errMsg
                BlindoNotice.alerta(this, "Número de empleado inválido", errMsg, window = dialog.window)
                return@setOnClickListener
            }
            if (pass.length < 4) {
                etPass.error = "La contraseña debe tener al menos 4 caracteres."
                BlindoNotice.alerta(this, "Contraseña muy corta", "Usa al menos 4 caracteres o dígitos.", window = dialog.window)
                return@setOnClickListener
            }
            if (pass != passConfirm) {
                etPassConfirm.error = "Las contraseñas no coinciden"
                BlindoNotice.alerta(this, "Revisa tu contraseña", "Las contraseñas no coinciden.", window = dialog.window)
                return@setOnClickListener
            }

            val rol = if (toggleRol.checkedButtonId == R.id.btnRegRolAdmin) "ADMIN" else "SUPERVISOR"
            btnCrear.isEnabled = false

            lifecycleScope.launch {
                val resultado = MockBlindoRepository.registrarUsuario(nombre, numEmp, pass, rol)
                resultado.onSuccess { (usuario, sanctumToken) ->
                    SessionManager.saveSession(
                        context = this@LoginActivity,
                        userId = usuario.id,
                        nombre = usuario.nombre,
                        numeroEmpleado = usuario.numeroEmpleado,
                        rol = usuario.rol,
                        token = sanctumToken
                    )
                    val saludo = usuario.nombre.trim().split(Regex("\\s+")).take(2).joinToString(" ")
                    dialog.dismiss()
                    irADashboard(
                        bienvenidaTitulo = "¡Cuenta creada, $saludo!",
                        bienvenidaSub = "Ya puedes gestionar personal, EPP y permisos en piso"
                    )
                }.onFailure { err ->
                    btnCrear.isEnabled = true
                    val apiErr = err as? ApiException
                    if (apiErr != null && apiErr.statusCode == 422) {
                        apiErr.fieldErrors["name"]?.let { etNombre.error = it }
                        apiErr.fieldErrors["numero_empleado"]?.let { etEmp.error = it }
                        apiErr.fieldErrors["password"]?.let { etPass.error = it }
                        BlindoNotice.alerta(this@LoginActivity, "Revisa tus datos", apiErr.message, window = dialog.window)
                    } else {
                        BlindoNotice.error(
                            this@LoginActivity,
                            "No se pudo crear la cuenta",
                            err.message ?: "Verifica tu conexión e intenta de nuevo.",
                            window = dialog.window
                        )
                    }
                }
            }
        }

        dialog.showExpanded()
    }

    private fun irADashboard(bienvenidaTitulo: String? = null, bienvenidaSub: String? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            if (!bienvenidaTitulo.isNullOrBlank()) putExtra("BIENVENIDA_TITULO", bienvenidaTitulo)
            if (!bienvenidaSub.isNullOrBlank()) putExtra("BIENVENIDA_SUB", bienvenidaSub)
        }
        startActivity(intent)
        finish()
    }
}
