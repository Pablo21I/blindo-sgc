package com.example.blindo.data.repository

import android.content.Context
import com.example.blindo.data.local.BlindoDatabase
import com.example.blindo.data.local.entity.EppEntity
import com.example.blindo.data.local.entity.ReglaTrabajoEppEntity
import com.example.blindo.data.local.entity.TrabajoEntity
import com.example.blindo.data.local.entity.ZonaEntity
import com.example.blindo.data.model.AsignacionEpp
import com.example.blindo.data.model.AsignacionEppDetalle
import com.example.blindo.data.model.Epp
import com.example.blindo.data.model.OrdenActiva
import com.example.blindo.data.model.Personal
import com.example.blindo.data.model.RegistroAuditoria
import com.example.blindo.data.model.Trabajo
import com.example.blindo.data.model.Usuario
import com.example.blindo.data.remote.ApiErrorParser
import com.example.blindo.data.remote.ApiException
import com.example.blindo.data.remote.AsignacionEppDto
import com.example.blindo.data.remote.AsignacionEppRequest
import com.example.blindo.data.remote.AsignacionEppUpdateRequest
import com.example.blindo.data.remote.LoginRequest
import com.example.blindo.data.remote.PersonalDto
import com.example.blindo.data.remote.PersonalRequest
import com.example.blindo.data.remote.RegisterRequest
import com.example.blindo.data.remote.RetrofitClient
import com.example.blindo.data.remote.ValidacionCreateRequest
import com.example.blindo.data.remote.ValidacionDto
import com.example.blindo.data.remote.ValidacionUpdateRequest
import com.example.blindo.domain.EppDateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Repositorio conectado en tiempo real al backend REST Laravel + MySQL + Sanctum (vía Retrofit)
 * para Auth, Personal, Asignaciones EPP, Órdenes Activas y Bitácora.
 * Room se conserva únicamente para los catálogos normativos estáticos STPS (trabajos, EPP base, reglas y zonas).
 */
object MockBlindoRepository {

    private var db: BlindoDatabase? = null
    private val catalogMutex = Mutex()
    @Volatile
    private var catalogsSeeded = false

    fun init(context: Context) {
        val appContext = context.applicationContext
        if (db == null) {
            db = BlindoDatabase.getInstance(appContext)
        }
        RetrofitClient.init(appContext)
    }

    private fun getDb(): BlindoDatabase {
        return db ?: throw IllegalStateException("MockBlindoRepository no ha sido inicializado con Context")
    }

    private suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> = withContext(Dispatchers.IO) {
        try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(ApiException(response.code(), "Respuesta vacía del servidor."))
                }
            } else {
                Result.failure(ApiErrorParser.parseHttpError(response))
            }
        } catch (e: Throwable) {
            Result.failure(ApiErrorParser.parseException(e))
        }
    }

    /**
     * Garantiza los catálogos normativos STPS locales (trabajos, EPP NOM-017, reglas y zonas).
     */
    suspend fun ensureCatalogosNormativos() = withContext(Dispatchers.IO) {
        if (catalogsSeeded) return@withContext
        catalogMutex.withLock {
            if (catalogsSeeded) return@withLock
            val database = getDb()

            if (database.trabajoDao().getAllTrabajos().isEmpty()) {
                database.trabajoDao().insertAll(
                    listOf(
                        TrabajoEntity(1, "Soldadura con arco eléctrico", "NOM-027-STPS", "Alto", "FUEGO_CHISPAS"),
                        TrabajoEntity(2, "Pintura con solventes inflamables", "NOM-005-STPS", "Alto", "INFLAMABLE"),
                        TrabajoEntity(3, "Corte con esmeril angular", "NOM-004-STPS", "Medio", "FUEGO_CHISPAS"),
                        TrabajoEntity(4, "Trabajo en alturas (>1.8m)", "NOM-009-STPS", "Alto", "ALTURAS")
                    )
                )
            }

            if (database.eppDao().getAllEpps().isEmpty()) {
                database.eppDao().insertAll(
                    listOf(
                        EppEntity(1, "Careta para soldar", 24),
                        EppEntity(2, "Guantes de carnaza (Soldador)", 3),
                        EppEntity(3, "Peto de cuero (Carnaza)", 12),
                        EppEntity(4, "Respirador media cara (Cartuchos químicos)", 6),
                        EppEntity(5, "Lentes herméticos (Goggles para químicos)", 12),
                        EppEntity(6, "Guantes de nitrilo o neopreno (Químicos)", 1),
                        EppEntity(7, "Careta facial transparente (Policarbonato)", 12),
                        EppEntity(8, "Lentes de seguridad (Claros/Oscuros)", 12),
                        EppEntity(9, "Guantes anticorte (Kevlar/Dyneema)", 6),
                        EppEntity(10, "Tapones auditivos desechables o silicón", 2),
                        EppEntity(11, "Arnés de cuerpo completo (Paracaidista)", 60),
                        EppEntity(12, "Línea de vida con amortiguador", 60),
                        EppEntity(13, "Casco de seguridad con barbiquejo", 36),
                        EppEntity(14, "Botas dieléctricas c/ casquillo", 12)
                    )
                )
            }

            if (database.reglaTrabajoEppDao().getReglasPorTrabajo(1).isEmpty()) {
                database.reglaTrabajoEppDao().insertAll(
                    listOf(
                        ReglaTrabajoEppEntity(1, 1, 1, true),
                        ReglaTrabajoEppEntity(2, 1, 2, true),
                        ReglaTrabajoEppEntity(3, 1, 3, false),
                        ReglaTrabajoEppEntity(4, 2, 4, true),
                        ReglaTrabajoEppEntity(5, 2, 5, true),
                        ReglaTrabajoEppEntity(6, 2, 6, false),
                        ReglaTrabajoEppEntity(7, 3, 7, true),
                        ReglaTrabajoEppEntity(8, 3, 8, true),
                        ReglaTrabajoEppEntity(9, 3, 9, true),
                        ReglaTrabajoEppEntity(10, 3, 10, false),
                        ReglaTrabajoEppEntity(11, 4, 11, true),
                        ReglaTrabajoEppEntity(12, 4, 12, true),
                        ReglaTrabajoEppEntity(13, 4, 13, true)
                    )
                )
            }

            if (database.zonaDao().getAllZonas().isEmpty()) {
                database.zonaDao().insertAll(
                    listOf(
                        ZonaEntity(1, "Zona A - Nave Principal"),
                        ZonaEntity(2, "Zona B - Cabina"),
                        ZonaEntity(3, "Zona C - Patio")
                    )
                )
            }

            catalogsSeeded = true
        }
    }

    // Mappers Locales
    private fun TrabajoEntity.toModel() = Trabajo(id, nombre, norma, riesgo, categoriaRiesgo)
    private fun EppEntity.toModel() = Epp(id, nombre, vidaUtilMeses)

    // Mappers Remotos (Laravel JSON -> Modelos Blindo)
    private fun PersonalDto.toModel() = Personal(id, nombre, rol, numeroEmpleado, activo)

    private fun AsignacionEppDto.toAsignacionModel() = AsignacionEpp(
        id = id.toString(),
        personalId = personalId,
        eppId = eppId,
        fechaEntregaMillis = fechaEntregaMillis,
        vidaUtilMesesAsignada = vidaUtilMesesAsignada
    )

    private fun AsignacionEppDto.toDetalleModel(): AsignacionEppDetalle? {
        val persona = personal?.toModel() ?: return null
        val epp = Epp(id = eppId, nombre = nombreEpp, vidaUtilMeses = vidaUtilMesesAsignada)
        val dias = EppDateUtils.calcularDiasRestantes(fechaEntregaMillis, vidaUtilMesesAsignada)
        val estado = EppDateUtils.evaluarEstado(fechaEntregaMillis, vidaUtilMesesAsignada)
        return AsignacionEppDetalle(
            asignacion = toAsignacionModel(),
            personal = persona,
            epp = epp,
            diasRestantes = dias,
            estado = estado
        )
    }

    private fun ValidacionDto.toOrdenModel() = OrdenActiva(
        id = id.toString(),
        personalId = personalId,
        trabajador = trabajador,
        supervisor = supervisor,
        turno = turno,
        zona = zona,
        trabajoId = trabajoId,
        nombreTrabajo = nombreTrabajo,
        estado = estado,
        notas = notas.orEmpty()
    )

    private fun ValidacionDto.toAuditoriaModel(): RegistroAuditoria {
        val resultadoTag = when (estado) {
            "ROJO" -> "ROJO_BLOQUEADO"
            "AMARILLO" -> "AMARILLO_PRECAUCION"
            "VERDE" -> "VERDE_AUTORIZADO"
            else -> estado
        }
        return RegistroAuditoria(
            id = id.toString(),
            fechaHora = fechaHoraMillis,
            trabajador = trabajador,
            supervisor = supervisor,
            zona = zona,
            trabajo = nombreTrabajo,
            resultado = resultadoTag,
            motivo = notas.orEmpty(),
            turno = turno,
            trabajoId = trabajoId
        )
    }

    // =========================================================================
    // 1. AUTENTICACIÓN LARAVEL SANCTUM (POST /login, POST /register, POST /logout)
    // =========================================================================

    suspend fun registrarUsuario(
        nombre: String,
        numeroEmpleado: String,
        password: String,
        rol: String
    ): Result<Pair<Usuario, String>> {
        return safeApiCall {
            RetrofitClient.api.register(
                RegisterRequest(
                    name = nombre.trim(),
                    numeroEmpleado = numeroEmpleado.trim(),
                    password = password,
                    rol = rol
                )
            )
        }.map { res ->
            val u = Usuario(
                id = res.user.id,
                nombre = res.user.name,
                numeroEmpleado = res.user.numeroEmpleado,
                rol = res.user.rol ?: "SUPERVISOR"
            )
            Pair(u, res.token)
        }
    }

    suspend fun autenticarUsuario(
        numeroEmpleado: String,
        password: String
    ): Result<Pair<Usuario, String>> {
        return safeApiCall {
            RetrofitClient.api.login(
                LoginRequest(
                    numeroEmpleado = numeroEmpleado.trim(),
                    password = password
                )
            )
        }.map { res ->
            val u = Usuario(
                id = res.user.id,
                nombre = res.user.name,
                numeroEmpleado = res.user.numeroEmpleado,
                rol = res.user.rol ?: "SUPERVISOR"
            )
            Pair(u, res.token)
        }
    }

    suspend fun cerrarSesionServidor() {
        // Mejor esfuerzo: si el servidor está caído, no bloquea el logout local
        safeApiCall { RetrofitClient.api.logout() }
    }

    // =========================================================================
    // 2. CRUD PERSONAL EN SERVIDOR LARAVEL (GET, POST, PUT, DELETE)
    // =========================================================================

    suspend fun getPersonalResult(): Result<List<Personal>> {
        return safeApiCall { RetrofitClient.api.getPersonal() }
            .map { list -> list.map { it.toModel() } }
    }

    suspend fun getPersonal(): List<Personal> = getPersonalResult().getOrDefault(emptyList())

    suspend fun getOperadores(): List<Personal> = getPersonal().filter { it.rol == "OPERADOR" }

    suspend fun getSupervisores(): List<Personal> = getPersonal().filter { it.rol == "SUPERVISOR" }

    suspend fun agregarPersonal(nombre: String, rol: String, numeroEmpleado: String): Result<Personal> {
        return safeApiCall {
            RetrofitClient.api.createPersonal(
                PersonalRequest(
                    nombre = nombre.trim(),
                    rol = rol,
                    numeroEmpleado = numeroEmpleado.trim()
                )
            )
        }.map { it.toModel() }
    }

    suspend fun editarPersonal(personal: Personal): Result<Personal> {
        return safeApiCall {
            RetrofitClient.api.updatePersonal(
                id = personal.id,
                request = PersonalRequest(
                    nombre = personal.nombre.trim(),
                    rol = personal.rol,
                    numeroEmpleado = personal.numeroEmpleado.trim()
                )
            )
        }.map { it.toModel() }
    }

    suspend fun eliminarPersonal(personalId: Int): Result<Unit> {
        return safeApiCall { RetrofitClient.api.deletePersonal(personalId) }.map { Unit }
    }

    // =========================================================================
    // 3. CRUD ASIGNACIONES EPP EN SERVIDOR LARAVEL (GET, POST, PUT, DELETE)
    // =========================================================================

    suspend fun getAsignacionesConDetalleResult(): Result<List<AsignacionEppDetalle>> {
        return safeApiCall { RetrofitClient.api.getAsignacionesEpp() }
            .map { list -> list.mapNotNull { it.toDetalleModel() }.sortedBy { it.diasRestantes } }
    }

    suspend fun getAsignacionesPorPersonal(personalId: Int): List<AsignacionEpp> {
        return safeApiCall { RetrofitClient.api.getAsignacionesEpp() }
            .getOrDefault(emptyList())
            .filter { it.personalId == personalId }
            .map { it.toAsignacionModel() }
    }

    suspend fun asignarOActualizarEpp(
        personalId: Int,
        eppId: Int,
        nombreEpp: String,
        vidaUtilMeses: Int
    ): Result<Boolean> {
        return safeApiCall {
            RetrofitClient.api.createAsignacionEpp(
                AsignacionEppRequest(
                    personalId = personalId,
                    eppId = eppId,
                    nombreEpp = nombreEpp,
                    vidaUtilMesesAsignada = vidaUtilMeses
                )
            )
        }.map { it.actualizado }
    }

    suspend fun editarVigenciaAsignacion(asignacion: AsignacionEpp, nuevaVidaUtilMeses: Int): Result<Unit> {
        val idInt = asignacion.id.toIntOrNull()
            ?: return Result.failure(ApiException(400, "ID de asignación inválido"))
        return safeApiCall {
            RetrofitClient.api.updateAsignacionEpp(
                id = idInt,
                request = AsignacionEppUpdateRequest(vidaUtilMesesAsignada = nuevaVidaUtilMeses)
            )
        }.map { Unit }
    }

    suspend fun renovarEquipo(asignacion: AsignacionEpp): Result<Unit> {
        val idInt = asignacion.id.toIntOrNull()
            ?: return Result.failure(ApiException(400, "ID de asignación inválido"))
        return safeApiCall {
            RetrofitClient.api.updateAsignacionEpp(
                id = idInt,
                request = AsignacionEppUpdateRequest(renovar = true)
            )
        }.map { Unit }
    }

    suspend fun eliminarAsignacionEpp(asignacionId: String): Result<Unit> {
        val idInt = asignacionId.toIntOrNull()
            ?: return Result.failure(ApiException(400, "ID de asignación inválido"))
        return safeApiCall { RetrofitClient.api.deleteAsignacionEpp(idInt) }.map { Unit }
    }

    // =========================================================================
    // 4. CRUD VALIDACIONES (ÓRDENES ACTIVAS + BITÁCORA) EN LARAVEL
    // =========================================================================

    suspend fun getValidacionesSeparadasResult(): Result<Pair<List<OrdenActiva>, List<RegistroAuditoria>>> {
        return safeApiCall { RetrofitClient.api.getValidaciones() }.map { list ->
            val activas = list.filter { it.activa }.map { it.toOrdenModel() }
            val auditorias = list.map { it.toAuditoriaModel() }
            Pair(activas, auditorias)
        }
    }

    suspend fun getOrdenesActivas(): List<OrdenActiva> {
        return safeApiCall { RetrofitClient.api.getValidaciones(activa = true) }
            .getOrDefault(emptyList())
            .map { it.toOrdenModel() }
    }

    suspend fun registrarValidacionRemota(
        personalId: Int,
        trabajador: String,
        supervisor: String,
        turno: String,
        zona: String,
        trabajoId: Int,
        nombreTrabajo: String,
        estado: String,
        activa: Boolean,
        notas: String
    ): Result<OrdenActiva> {
        return safeApiCall {
            RetrofitClient.api.createValidacion(
                ValidacionCreateRequest(
                    personalId = if (personalId > 0) personalId else null,
                    trabajador = trabajador,
                    supervisor = supervisor,
                    turno = turno,
                    zona = zona,
                    trabajoId = trabajoId,
                    nombreTrabajo = nombreTrabajo,
                    estado = estado,
                    activa = activa,
                    notas = notas
                )
            )
        }.map { it.toOrdenModel() }
    }

    suspend fun actualizarOrdenNotas(ordenId: String, nuevasNotas: String): Result<Unit> {
        val idInt = ordenId.toIntOrNull()
            ?: return Result.failure(ApiException(400, "ID de orden inválido"))
        return safeApiCall {
            RetrofitClient.api.updateValidacion(idInt, ValidacionUpdateRequest(notas = nuevasNotas))
        }.map { Unit }
    }

    suspend fun finalizarOrden(ordenId: String): Result<Unit> {
        val idInt = ordenId.toIntOrNull()
            ?: return Result.failure(ApiException(400, "ID de orden inválido"))
        return safeApiCall {
            RetrofitClient.api.updateValidacion(idInt, ValidacionUpdateRequest(activa = false))
        }.map { Unit }
    }

    suspend fun eliminarValidacion(ordenId: String): Result<Unit> {
        val idInt = ordenId.toIntOrNull()
            ?: return Result.failure(ApiException(400, "ID de validación inválido"))
        return safeApiCall { RetrofitClient.api.deleteValidacion(idInt) }.map { Unit }
    }

    // =========================================================================
    // 5. CATÁLOGOS NORMATIVOS LOCALES (STPS: Trabajos, EPP Base, Reglas, Zonas)
    // =========================================================================

    suspend fun getTrabajos(): List<Trabajo> = withContext(Dispatchers.IO) {
        ensureCatalogosNormativos()
        getDb().trabajoDao().getAllTrabajos().map { it.toModel() }
    }

    suspend fun getTrabajoPorId(trabajoId: Int): Trabajo? = withContext(Dispatchers.IO) {
        ensureCatalogosNormativos()
        getDb().trabajoDao().getTrabajoById(trabajoId)?.toModel()
    }

    suspend fun agregarNuevoTrabajo(
        nombre: String,
        norma: String,
        riesgo: String,
        categoriaRiesgo: String,
        eppsObligatoriosIds: List<Int>
    ) = withContext(Dispatchers.IO) {
        val nuevoId = getDb().trabajoDao().insertTrabajo(
            TrabajoEntity(
                nombre = nombre.trim(),
                norma = norma.trim(),
                riesgo = riesgo,
                categoriaRiesgo = categoriaRiesgo
            )
        ).toInt()
        eppsObligatoriosIds.forEach { eppId ->
            getDb().reglaTrabajoEppDao().insertRegla(
                ReglaTrabajoEppEntity(trabajoId = nuevoId, eppId = eppId, esObligatorio = true)
            )
        }
    }

    suspend fun getEppsGlobales(): List<Epp> = withContext(Dispatchers.IO) {
        ensureCatalogosNormativos()
        getDb().eppDao().getAllEpps().map { it.toModel() }
    }

    suspend fun getEppsParaTrabajo(trabajoId: Int): List<Pair<Epp, Boolean>> = withContext(Dispatchers.IO) {
        ensureCatalogosNormativos()
        val reglas = getDb().reglaTrabajoEppDao().getReglasPorTrabajo(trabajoId)
        val epps = getDb().eppDao().getAllEpps().associateBy { it.id }
        reglas.mapNotNull { regla ->
            val eppEntity = epps[regla.eppId]
            if (eppEntity != null) Pair(eppEntity.toModel(), regla.esObligatorio) else null
        }
    }

    suspend fun getZonas(): List<String> = withContext(Dispatchers.IO) {
        ensureCatalogosNormativos()
        getDb().zonaDao().getAllZonas().map { it.nombre }
    }

    suspend fun agregarZona(nombre: String): Result<Unit> = withContext(Dispatchers.IO) {
        val limpia = nombre.trim()
        if (getDb().zonaDao().findByNombre(limpia) != null) {
            return@withContext Result.failure(IllegalArgumentException("La zona '$limpia' ya existe"))
        }
        getDb().zonaDao().insertZona(ZonaEntity(nombre = limpia))
        Result.success(Unit)
    }
}
