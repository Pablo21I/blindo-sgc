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
import com.example.blindo.ui.main.MainActivity
import com.example.blindo.ui.showExpanded
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

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
                Toast.makeText(this@LoginActivity, "Bienvenido, ${usuario.nombre}", Toast.LENGTH_SHORT).show()
                irADashboard()
            }.onFailure { err ->
                binding.btnLogin.text = "Entrar al sistema"
                binding.progressLogin.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                val apiErr = err as? ApiException
                if (apiErr != null && apiErr.statusCode == 422) {
                    // Muestra errores reales de validación 422 enviados por Laravel
                    apiErr.fieldErrors["numero_empleado"]?.let { binding.tilUsuario.error = it }
                    apiErr.fieldErrors["password"]?.let { binding.tilPassword.error = it }
                    Snackbar.make(binding.root, apiErr.message, Snackbar.LENGTH_LONG).show()
                } else {
                    // Maneja 401 (credenciales incorrectas) y servidor caído sin crashear
                    val mensaje = err.message ?: "Error al iniciar sesión"
                    Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG).show()
                    if (apiErr?.statusCode == 401) {
                        binding.etPassword.text?.clear()
                    }
                }
            }
        }
    }

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

        btnCrear.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val numEmp = etEmp.text.toString().trim()
            val pass = etPass.text.toString().trim()
            val passConfirm = etPassConfirm.text.toString().trim()

            etNombre.error = null
            etEmp.error = null
            etPass.error = null
            etPassConfirm.error = null

            if (pass.isNotEmpty() && pass != passConfirm) {
                etPassConfirm.error = "Las contraseñas no coinciden"
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
                    Toast.makeText(this@LoginActivity, "Cuenta creada. Bienvenido, ${usuario.nombre}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    irADashboard()
                }.onFailure { err ->
                    btnCrear.isEnabled = true
                    val apiErr = err as? ApiException
                    if (apiErr != null && apiErr.statusCode == 422) {
                        apiErr.fieldErrors["name"]?.let { etNombre.error = it }
                        apiErr.fieldErrors["numero_empleado"]?.let { etEmp.error = it }
                        apiErr.fieldErrors["password"]?.let { etPass.error = it }
                        Toast.makeText(this@LoginActivity, apiErr.message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@LoginActivity, err.message ?: "Error al registrar cuenta", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        dialog.showExpanded()
    }

    private fun irADashboard() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
