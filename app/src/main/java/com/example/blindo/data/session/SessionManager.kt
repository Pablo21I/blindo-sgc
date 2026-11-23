package com.example.blindo.data.session

import android.content.Context
import java.security.MessageDigest

/**
 * Gestor centralizado de sesión del usuario.
 * Preparado para almacenar el Bearer Token emitido por Laravel Sanctum.
 */
object SessionManager {
    private const val PREFS_NAME = "BlindoPrefs"
    private const val KEY_USER_ID = "USER_ID"
    private const val KEY_USER_NAME = "USER_LOGGED"
    private const val KEY_USER_EMP_NUM = "USER_EMP_NUM"
    private const val KEY_USER_ROLE = "USER_ROLE"
    private const val KEY_AUTH_TOKEN = "AUTH_TOKEN"

    fun saveSession(
        context: Context,
        userId: Int,
        nombre: String,
        numeroEmpleado: String,
        rol: String,
        token: String
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, nombre)
            .putString(KEY_USER_EMP_NUM, numeroEmpleado)
            .putString(KEY_USER_ROLE, rol)
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_USER_NAME, null).isNullOrBlank() &&
            !prefs.getString(KEY_AUTH_TOKEN, null).isNullOrBlank()
    }

    fun getUserName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_NAME, "Supervisor SGC") ?: "Supervisor SGC"
    }

    fun getUserEmpNum(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_EMP_NUM, "") ?: ""
    }

    fun getUserRole(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ROLE, "SUPERVISOR") ?: "SUPERVISOR"
    }

    fun getAuthToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun hashPassword(rawPassword: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("blindo_sgc_salt::$rawPassword".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
