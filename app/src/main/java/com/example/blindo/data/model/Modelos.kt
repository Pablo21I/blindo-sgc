package com.example.blindo.data.model

import com.example.blindo.domain.EstadoEpp

/**
 * Cuenta de acceso al sistema Blindo SGC (alineada con tabla users de Laravel).
 */
data class Usuario(
    val id: Int,
    val nombre: String,
    val numeroEmpleado: String,
    val rol: String // "ADMIN" o "SUPERVISOR"
)

/**
 * Representa al personal registrado en el taller (Operadores, Supervisores).
 */
data class Personal(
    val id: Int,
    val nombre: String,
    val rol: String, // "OPERADOR" o "SUPERVISOR"
    val numeroEmpleado: String,
    val activo: Boolean = true
)

/**
 * Representa los trabajos disponibles en el catálogo STPS.
 */
data class Trabajo(
    val id: Int,
    val nombre: String,
    val norma: String,
    val riesgo: String,
    val categoriaRiesgo: String = "GENERAL" // "FUEGO_CHISPAS", "INFLAMABLE", "ALTURAS", "GENERAL"
)

/**
 * Representa el catálogo global de Equipos de Protección Personal (EPP).
 */
data class Epp(
    val id: Int,
    val nombre: String,
    val vidaUtilMeses: Int
)

/**
 * Representa un EPP asignado a un trabajador.
 */
data class AsignacionEpp(
    val id: String,
    val personalId: Int,
    val eppId: Int,
    val fechaEntregaMillis: Long,
    val vidaUtilMesesAsignada: Int
)

/**
 * Proyección lista para la UI sin consultas N+1.
 */
data class AsignacionEppDetalle(
    val asignacion: AsignacionEpp,
    val personal: Personal,
    val epp: Epp,
    val diasRestantes: Int,
    val estado: EstadoEpp
)

/**
 * Tabla intermedia que relaciona Trabajo <-> EPP e indica si es obligatorio.
 */
data class ReglaTrabajoEpp(
    val trabajoId: Int,
    val eppId: Int,
    val esObligatorio: Boolean
)

/**
 * Representa una orden de trabajo activa en el piso.
 */
data class OrdenActiva(
    val id: String,
    val personalId: Int = 0,
    val trabajador: String,
    val supervisor: String = "No especificado",
    val turno: String = "Matutino",
    val zona: String,
    val trabajoId: Int,
    val nombreTrabajo: String,
    val estado: String,
    val notas: String = ""
)

/**
 * Para la bitácora de auditoría SGC con firma histórica inmutable.
 */
data class RegistroAuditoria(
    val id: String,
    val fechaHora: Long,
    val trabajador: String,
    val supervisor: String,
    val zona: String,
    val trabajo: String,
    val resultado: String,
    val motivo: String,
    val turno: String = "",
    val trabajoId: Int = -1
)
