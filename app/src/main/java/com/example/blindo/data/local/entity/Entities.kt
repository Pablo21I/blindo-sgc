package com.example.blindo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val numeroEmpleado: String,
    val passwordHash: String,
    val rol: String // "ADMIN" o "SUPERVISOR"
)

@Entity(tableName = "personal")
data class PersonalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val rol: String, // "OPERADOR" o "SUPERVISOR"
    val numeroEmpleado: String,
    val activo: Boolean = true
)

@Entity(tableName = "trabajos")
data class TrabajoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val norma: String,
    val riesgo: String,
    val categoriaRiesgo: String = "GENERAL"
)

@Entity(tableName = "epp")
data class EppEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val vidaUtilMeses: Int = 12
)

@Entity(tableName = "reglas_trabajo_epp")
data class ReglaTrabajoEppEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trabajoId: Int,
    val eppId: Int,
    val esObligatorio: Boolean
)

@Entity(tableName = "ordenes_activas")
data class OrdenActivaEntity(
    @PrimaryKey val id: String,
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

@Entity(tableName = "bitacora_auditoria")
data class RegistroAuditoriaEntity(
    @PrimaryKey val id: String,
    val fechaHora: Long,
    val trabajador: String,
    val supervisor: String = "No especificado",
    val zona: String,
    val trabajo: String,
    val resultado: String,
    val motivo: String
)

@Entity(tableName = "asignaciones_epp")
data class AsignacionEppEntity(
    @PrimaryKey val id: String,
    val personalId: Int,
    val eppId: Int,
    val fechaEntregaMillis: Long,
    val vidaUtilMesesAsignada: Int
)

@Entity(tableName = "zonas")
data class ZonaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String
)
