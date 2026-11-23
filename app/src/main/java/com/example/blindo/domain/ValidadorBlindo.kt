package com.example.blindo.domain

import com.example.blindo.data.model.Trabajo
import com.example.blindo.data.repository.MockBlindoRepository

sealed class ResultadoValidacion {
    data class VerdeAutorizado(val mensaje: String) : ResultadoValidacion()
    data class AmarilloPrecaucion(val mensaje: String) : ResultadoValidacion()
    data class RojoBloqueado(val mensaje: String) : ResultadoValidacion()
}

/**
 * Motor lógico Poka-Yoke + SIMOPS dinámico.
 * Cruza inspección física, EPP obligatorio, vigencias del inventario real del trabajador
 * y matriz de incompatibilidades por categoría de riesgo en la misma zona.
 */
object ValidadorBlindo {

    suspend fun evaluarSeguridadTrabajo(
        personalId: Int,
        zona: String,
        trabajo: Trabajo,
        eppsSeleccionadosIds: List<Int>,
        eppBuenEstado: Boolean
    ): ResultadoValidacion {

        // REGLA 1: Poka-Yoke Físico (Inspección visual NOM-017)
        if (!eppBuenEstado) {
            return ResultadoValidacion.RojoBloqueado(
                "Poka-Yoke NOM-017: No se confirmó el buen estado físico del EPP. Inspecciona daños o desgaste antes de autorizar."
            )
        }

        // REGLA 2: EPP Obligatorio Completo
        val eppsRequeridos = MockBlindoRepository.getEppsParaTrabajo(trabajo.id)
        val faltantesObligatorios = eppsRequeridos.filter { it.second && !eppsSeleccionadosIds.contains(it.first.id) }
        if (faltantesObligatorios.isNotEmpty()) {
            val nombres = faltantesObligatorios.joinToString(", ") { it.first.nombre }
            return ResultadoValidacion.RojoBloqueado(
                "Falta equipo obligatorio: $nombres. (NOM-017-STPS)"
            )
        }

        // REGLA 3: Cruce real con Inventario y Caducidades de EPP del Operador
        val asignacionesOperador = MockBlindoRepository.getAsignacionesPorPersonal(personalId)
            .associateBy { it.eppId }

        val advertenciasCaducidad = mutableListOf<String>()
        for ((epp, esObligatorio) in eppsRequeridos) {
            if (!esObligatorio) continue
            val asignacion = asignacionesOperador[epp.id] ?: continue
            val vidaMeses = if (asignacion.vidaUtilMesesAsignada > 0) asignacion.vidaUtilMesesAsignada else epp.vidaUtilMeses
            val estado = EppDateUtils.evaluarEstado(asignacion.fechaEntregaMillis, vidaMeses)
            val dias = EppDateUtils.calcularDiasRestantes(asignacion.fechaEntregaMillis, vidaMeses)

            if (estado == EstadoEpp.CADUCADO) {
                val diasVencido = -dias
                return ResultadoValidacion.RojoBloqueado(
                    "Poka-Yoke Inventario EPP: El equipo obligatorio '${epp.nombre}' asignado al operador está CADUCADO hace $diasVencido días. Renuévalo en la pestaña EPP antes de autorizar."
                )
            } else if (estado == EstadoEpp.ALERTA) {
                advertenciasCaducidad.add("'${epp.nombre}' vence en $dias días")
            }
        }

        // REGLA 4: Matriz SIMOPS Dinámica por Categoría de Riesgo (aplica también a trabajos nuevos)
        val ordenesEnZona = MockBlindoRepository.getOrdenesActivas().filter { it.zona == zona }
        val trabajosMap = MockBlindoRepository.getTrabajos().associateBy { it.id }

        val advertenciasSimops = mutableListOf<String>()
        for (orden in ordenesEnZona) {
            val trabajoActivo = trabajosMap[orden.trabajoId]
            val catNueva = trabajo.categoriaRiesgo
            val catActiva = trabajoActivo?.categoriaRiesgo ?: "GENERAL"

            val choque = evaluarChoqueCategorias(catNueva, catActiva)
            if (choque == "ROJO") {
                return ResultadoValidacion.RojoBloqueado(
                    "Operación Incompatible (SIMOPS): '${trabajo.nombre}' choca críticamente con '${orden.nombreTrabajo}' (${orden.trabajador}) en $zona. Riesgo crítico de explosión o incendio por fuente de ignición en atmósfera inflamable."
                )
            } else if (choque == "AMARILLO") {
                advertenciasSimops.add(
                    "Simultaneidad SIMOPS con '${orden.nombreTrabajo}' (${orden.trabajador}): ${justificacionAmarillo(catNueva, catActiva)}"
                )
            }
        }

        // REGLA 5: Precauciones menores (SIMOPS amarillo, EPP próximo a vencer o EPP opcional faltante)
        val advertencias = mutableListOf<String>()
        advertencias.addAll(advertenciasSimops)

        if (advertenciasCaducidad.isNotEmpty()) {
            advertencias.add("EPP próximo a caducar: ${advertenciasCaducidad.joinToString(", ")}.")
        }

        val faltantesOpcionales = eppsRequeridos.filter { !it.second && !eppsSeleccionadosIds.contains(it.first.id) }
        if (faltantesOpcionales.isNotEmpty()) {
            advertencias.add(
                "Falta equipo recomendado: ${faltantesOpcionales.joinToString(", ") { it.first.nombre }}."
            )
        }

        if (advertencias.isNotEmpty()) {
            return ResultadoValidacion.AmarilloPrecaucion(advertencias.joinToString("\n• ", prefix = "• "))
        }

        return ResultadoValidacion.VerdeAutorizado(
            "Todas las validaciones Poka-Yoke, vigencias de EPP y matriz SIMOPS aprobadas. Seguro para operar."
        )
    }

    private fun evaluarChoqueCategorias(cat1: String, cat2: String): String? {
        val par = setOf(cat1, cat2)
        if (par.containsAll(listOf("FUEGO_CHISPAS", "INFLAMABLE"))) return "ROJO"
        if (par.containsAll(listOf("FUEGO_CHISPAS", "ALTURAS"))) return "AMARILLO"
        if (par.containsAll(listOf("INFLAMABLE", "ALTURAS"))) return "AMARILLO"
        return null
    }

    private fun justificacionAmarillo(cat1: String, cat2: String): String {
        val par = setOf(cat1, cat2)
        return when {
            par.containsAll(listOf("FUEGO_CHISPAS", "ALTURAS")) ->
                "Riesgo de caída de chispas u objetos sobre personal. Requiere acordonamiento y lona ignífuga."
            par.containsAll(listOf("INFLAMABLE", "ALTURAS")) ->
                "Exposición a vapores inflamables en trabajo en altura. Requiere monitoreo de atmósfera y ventilación."
            else ->
                "Trabajos simultáneos requieren supervisión especial."
        }
    }
}
