package com.example.blindo.data.remote

import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiException(
    val statusCode: Int,
    override val message: String,
    val fieldErrors: Map<String, String> = emptyMap()
) : Exception(message)

/**
 * Traduce códigos HTTP de Laravel (401, 422, 500) y caídas de red en mensajes amigables
 * para que la app nunca crashee si el servidor se apaga o rechaza un formulario.
 */
object ApiErrorParser {

    fun parseHttpError(response: Response<*>): ApiException {
        val code = response.code()
        val rawBody = try {
            response.errorBody()?.string().orEmpty()
        } catch (_: Exception) {
            ""
        }

        val fieldErrors = mutableMapOf<String, String>()
        var serverMessage: String? = null

        if (rawBody.isNotBlank()) {
            try {
                val json = JSONObject(rawBody)
                if (json.has("message") && !json.isNull("message")) {
                    serverMessage = json.getString("message")
                }
                if (json.has("errors") && !json.isNull("errors")) {
                    val errorsObj = json.getJSONObject("errors")
                    val keys = errorsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val arr = errorsObj.optJSONArray(key)
                        if (arr != null && arr.length() > 0) {
                            fieldErrors[key] = arr.getString(0)
                        }
                    }
                }
            } catch (_: Exception) {
                // Respuesta no JSON
            }
        }

        val friendlyMessage = when (code) {
            401 -> serverMessage ?: "Credenciales incorrectas o sesión expirada (401)."
            403 -> serverMessage ?: "No tienes permisos para realizar esta acción (403)."
            404 -> "Registro no encontrado en el servidor (404)."
            422 -> {
                if (fieldErrors.isNotEmpty()) {
                    fieldErrors.values.joinToString("\n")
                } else {
                    serverMessage ?: "Faltan datos obligatorios o tienen formato inválido (422)."
                }
            }
            in 500..599 -> "El servidor Laravel presentó un error interno ($code). Intenta de nuevo."
            else -> serverMessage ?: "Error inesperado del servidor (HTTP $code)."
        }

        return ApiException(code, friendlyMessage, fieldErrors)
    }

    fun parseException(e: Throwable): ApiException {
        if (e is ApiException) return e
        val msg = when (e) {
            is ConnectException, is UnknownHostException ->
                "No se pudo conectar con el servidor Laravel. Verifica que 'php artisan serve' esté encendido."
            is SocketTimeoutException ->
                "El servidor tardó demasiado en responder. Revisa tu conexión."
            is IOException ->
                "Error de red: el servidor no está disponible en este momento."
            else ->
                e.message ?: "Error inesperado de comunicación."
        }
        return ApiException(statusCode = 0, message = msg)
    }
}
