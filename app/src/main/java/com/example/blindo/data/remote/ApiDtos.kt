package com.example.blindo.data.remote

import com.google.gson.annotations.SerializedName

// --- AUTH (Laravel Sanctum) ---

data class LoginRequest(
    @SerializedName("numero_empleado") val numeroEmpleado: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("numero_empleado") val numeroEmpleado: String,
    @SerializedName("password") val password: String,
    @SerializedName("rol") val rol: String
)

data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("numero_empleado") val numeroEmpleado: String,
    @SerializedName("rol") val rol: String? = "SUPERVISOR"
)

data class AuthResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto
)

// --- PERSONAL ---

data class PersonalDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("numero_empleado") val numeroEmpleado: String,
    @SerializedName("activo") val activo: Boolean = true
)

data class PersonalRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("numero_empleado") val numeroEmpleado: String
)

// --- ASIGNACIONES EPP ---

data class AsignacionEppDto(
    @SerializedName("id") val id: Int,
    @SerializedName("personal_id") val personalId: Int,
    @SerializedName("epp_id") val eppId: Int,
    @SerializedName("nombre_epp") val nombreEpp: String,
    @SerializedName("fecha_entrega_millis") val fechaEntregaMillis: Long,
    @SerializedName("vida_util_meses_asignada") val vidaUtilMesesAsignada: Int,
    @SerializedName("personal") val personal: PersonalDto? = null
)

data class AsignacionEppRequest(
    @SerializedName("personal_id") val personalId: Int,
    @SerializedName("epp_id") val eppId: Int,
    @SerializedName("nombre_epp") val nombreEpp: String,
    @SerializedName("vida_util_meses_asignada") val vidaUtilMesesAsignada: Int
)

data class AsignacionEppStoreResponse(
    @SerializedName("actualizado") val actualizado: Boolean = false,
    @SerializedName("data") val data: AsignacionEppDto
)

data class AsignacionEppUpdateRequest(
    @SerializedName("vida_util_meses_asignada") val vidaUtilMesesAsignada: Int? = null,
    @SerializedName("renovar") val renovar: Boolean? = null
)

// --- VALIDACIONES (Órdenes Activas + Bitácora) ---

data class ValidacionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("personal_id") val personalId: Int = 0,
    @SerializedName("trabajador") val trabajador: String,
    @SerializedName("supervisor") val supervisor: String,
    @SerializedName("turno") val turno: String,
    @SerializedName("zona") val zona: String,
    @SerializedName("trabajo_id") val trabajoId: Int,
    @SerializedName("nombre_trabajo") val nombreTrabajo: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("activa") val activa: Boolean,
    @SerializedName("notas") val notas: String? = "",
    @SerializedName("fecha_hora_millis") val fechaHoraMillis: Long
)

data class ValidacionCreateRequest(
    @SerializedName("personal_id") val personalId: Int?,
    @SerializedName("trabajador") val trabajador: String,
    @SerializedName("supervisor") val supervisor: String,
    @SerializedName("turno") val turno: String,
    @SerializedName("zona") val zona: String,
    @SerializedName("trabajo_id") val trabajoId: Int,
    @SerializedName("nombre_trabajo") val nombreTrabajo: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("activa") val activa: Boolean,
    @SerializedName("notas") val notas: String
)

data class ValidacionUpdateRequest(
    @SerializedName("notas") val notas: String? = null,
    @SerializedName("activa") val activa: Boolean? = null
)

data class MessageResponse(
    @SerializedName("message") val message: String? = null
)
