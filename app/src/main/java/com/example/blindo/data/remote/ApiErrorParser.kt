package com.example.blindo.data.remote

import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// Excepción propia para guardar el código HTTP (ej. 401, 422), el mensaje amigable y los errores por campo
class ApiException(
    val statusCode: Int,
    override val message: String,
    val fieldErrors: Map<String, String> = emptyMap()
) : Exception(message)

/**
 * Convierte errores del servidor (401, 422, 500) y fallos de red en mensajes fáciles de entender
 * para que la app nunca se cierre (crashee) si el servidor se cae o faltan datos.
 */
object ApiErrorParser {

    // Lee la respuesta de error que devolvió Laravel en JSON y extrae los mensajes por campo
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
            401 -> serverMessage ?: "Credenciales incorrectas o tu sesión expiró."
            403 -> serverMessage ?: "No tienes permisos para realizar esta acción."
            404 -> "No se encontró el registro solicitado."
            422 -> {
                if (fieldErrors.isNotEmpty()) {
                    fieldErrors.values.joinToString("\n")
                } else {
                    serverMessage ?: "Revisa los campos marcados; faltan datos obligatorios."
                }
            }
            in 500..599 -> "No pudimos completar tu solicitud en este momento. Intenta de nuevo."
            else -> serverMessage ?: "Ocurrió un problema inesperado. Intenta nuevamente."
        }

        return ApiException(code, friendlyMessage, fieldErrors)
    }

    // Traduce errores cuando no hay internet, se cayó el servidor o tardó demasiado en responder
    fun parseException(e: Throwable): ApiException {
        if (e is ApiException) return e
        val msg = when (e) {
            is ConnectException, is UnknownHostException ->
                "Sin conexión con el servicio. Verifica tu red e intenta de nuevo."
            is SocketTimeoutException ->
                "La conexión tardó demasiado en responder. Intenta de nuevo."
            is IOException ->
                "No hay conexión disponible en este momento."
            else ->
                e.message ?: "Ocurrió un problema de comunicación."
        }
        return ApiException(statusCode = 0, message = msg)
    }
}
