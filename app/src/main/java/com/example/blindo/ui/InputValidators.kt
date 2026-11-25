package com.example.blindo.ui

import android.text.InputFilter
import android.widget.EditText

/**
 * Filtros y validaciones reutilizables para todos los formularios de Blindo.
 * Bloquean caracteres inválidos al escribir (ej. números en un nombre) y validan antes de enviar.
 */
object InputValidators {

    private val REGEX_NOMBRE = Regex("^[\\p{L}][\\p{L}\\s.]*$")
    private val REGEX_EMPLEADO = Regex("^[A-Za-z0-9_-]{3,20}$")
    private val REGEX_CONTIENE_LETRA = Regex(".*\\p{L}.*")

    // Aplica filtro de solo letras (incluye acentos, ñ, espacios y punto para títulos como Ing.)
    fun aplicarFiltroSoloNombre(editText: EditText, maxLength: Int = 80) {
        val filtroLetras = InputFilter { source, start, end, _, _, _ ->
            val sb = StringBuilder()
            for (i in start until end) {
                val c = source[i]
                if (Character.isLetter(c) || c == ' ' || c == '.') {
                    sb.append(c)
                }
            }
            if (sb.length == end - start) null else sb.toString()
        }
        editText.filters = arrayOf(filtroLetras, InputFilter.LengthFilter(maxLength))
    }

    // Aplica filtro de código de empleado (solo letras mayúsculas, números, guion y guion bajo, sin espacios)
    fun aplicarFiltroCodigoEmpleado(editText: EditText, maxLength: Int = 20) {
        val filtroCodigo = InputFilter { source, start, end, _, _, _ ->
            val sb = StringBuilder()
            for (i in start until end) {
                val c = source[i]
                if (c.isLetterOrDigit() || c == '-' || c == '_') {
                    sb.append(c.uppercaseChar())
                }
            }
            sb.toString()
        }
        editText.filters = arrayOf(filtroCodigo, InputFilter.LengthFilter(maxLength))
    }

    // Valida un nombre completo de persona (mín. 3 caracteres, solo letras y espacios)
    fun validarNombrePersona(nombre: String): String? {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return "Ingresa el nombre completo."
        if (limpio.length < 3) return "El nombre debe tener al menos 3 letras."
        if (!REGEX_NOMBRE.matches(limpio)) return "El nombre solo debe contener letras y espacios."
        return null
    }

    // Valida un número/código de empleado (mín. 3 caracteres, letras/números/guiones)
    fun validarNumeroEmpleado(codigo: String): String? {
        val limpio = codigo.trim()
        if (limpio.isEmpty()) return "Ingresa el número de empleado."
        if (!REGEX_EMPLEADO.matches(limpio)) return "Usa entre 3 y 20 caracteres (ej. OP-201 o SUP-001)."
        return null
    }

    // Valida nombres de zonas o tareas (mín. 3 caracteres y debe contener al menos una letra)
    fun validarTextoDescriptivo(texto: String, etiqueta: String): String? {
        val limpio = texto.trim()
        if (limpio.length < 3) return "Ingresa un nombre válido para $etiqueta (mín. 3 caracteres)."
        if (!REGEX_CONTIENE_LETRA.matches(limpio)) return "$etiqueta debe incluir letras, no solo números o símbolos."
        return null
    }
}
