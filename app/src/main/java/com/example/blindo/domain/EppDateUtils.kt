package com.example.blindo.domain

import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class EstadoEpp {
    PERMANENTE,
    VIGENTE,
    ALERTA,
    CADUCADO
}

/**
 * Utilidad única de cálculo real de vigencias y caducidades de EPP usando Calendar.
 */
object EppDateUtils {

    const val VIDA_PERMANENTE_MESES = 1200

    fun esPermanente(vidaUtilMeses: Int): Boolean = vidaUtilMeses >= VIDA_PERMANENTE_MESES

    fun calcularFechaCaducidadMillis(fechaEntregaMillis: Long, vidaUtilMeses: Int): Long {
        if (esPermanente(vidaUtilMeses)) return Long.MAX_VALUE
        val cal = Calendar.getInstance().apply {
            timeInMillis = fechaEntregaMillis
            add(Calendar.MONTH, vidaUtilMeses)
        }
        return cal.timeInMillis
    }

    fun calcularDiasRestantes(fechaEntregaMillis: Long, vidaUtilMeses: Int): Int {
        if (esPermanente(vidaUtilMeses)) return Int.MAX_VALUE
        val caducidadMillis = calcularFechaCaducidadMillis(fechaEntregaMillis, vidaUtilMeses)
        val diffMillis = caducidadMillis - System.currentTimeMillis()
        return TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
    }

    fun evaluarEstado(fechaEntregaMillis: Long, vidaUtilMeses: Int): EstadoEpp {
        if (esPermanente(vidaUtilMeses)) return EstadoEpp.PERMANENTE
        val dias = calcularDiasRestantes(fechaEntregaMillis, vidaUtilMeses)
        return when {
            dias <= 0 -> EstadoEpp.CADUCADO
            dias <= 30 -> EstadoEpp.ALERTA
            else -> EstadoEpp.VIGENTE
        }
    }

    fun formatearVidaUtil(vidaUtilMeses: Int): String {
        return when {
            esPermanente(vidaUtilMeses) -> "Permanente"
            vidaUtilMeses >= 12 && vidaUtilMeses % 12 == 0 -> {
                val anios = vidaUtilMeses / 12
                if (anios == 1) "1 año" else "$anios años"
            }
            else -> "$vidaUtilMeses meses"
        }
    }
}
