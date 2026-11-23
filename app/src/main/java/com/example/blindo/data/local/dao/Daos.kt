package com.example.blindo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.blindo.data.local.entity.AsignacionEppEntity
import com.example.blindo.data.local.entity.EppEntity
import com.example.blindo.data.local.entity.OrdenActivaEntity
import com.example.blindo.data.local.entity.PersonalEntity
import com.example.blindo.data.local.entity.ReglaTrabajoEppEntity
import com.example.blindo.data.local.entity.RegistroAuditoriaEntity
import com.example.blindo.data.local.entity.TrabajoEntity
import com.example.blindo.data.local.entity.UsuarioEntity
import com.example.blindo.data.local.entity.ZonaEntity

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE LOWER(numeroEmpleado) = LOWER(:numeroEmpleado) LIMIT 1")
    fun findByNumeroEmpleado(numeroEmpleado: String): UsuarioEntity?

    @Query("SELECT COUNT(*) FROM usuarios")
    fun countUsuarios(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertUsuario(usuario: UsuarioEntity): Long
}

@Dao
interface PersonalDao {
    @Query("SELECT * FROM personal WHERE activo = 1 ORDER BY id DESC")
    fun getAllPersonal(): List<PersonalEntity>

    @Query("SELECT * FROM personal WHERE id = :id LIMIT 1")
    fun getPersonalById(id: Int): PersonalEntity?

    @Query("SELECT * FROM personal WHERE LOWER(numeroEmpleado) = LOWER(:numeroEmpleado) AND activo = 1 LIMIT 1")
    fun findByNumeroEmpleado(numeroEmpleado: String): PersonalEntity?

    @Query("SELECT * FROM personal WHERE rol = 'OPERADOR' AND activo = 1 ORDER BY id DESC")
    fun getOperadores(): List<PersonalEntity>

    @Query("SELECT * FROM personal WHERE rol = 'SUPERVISOR' AND activo = 1 ORDER BY id DESC")
    fun getSupervisores(): List<PersonalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPersonal(personal: PersonalEntity): Long

    @Update
    fun updatePersonal(personal: PersonalEntity)

    @Query("DELETE FROM personal WHERE id = :personalId")
    fun deletePersonal(personalId: Int)
}

@Dao
interface TrabajoDao {
    @Query("SELECT * FROM trabajos ORDER BY id DESC")
    fun getAllTrabajos(): List<TrabajoEntity>

    @Query("SELECT * FROM trabajos WHERE id = :id LIMIT 1")
    fun getTrabajoById(id: Int): TrabajoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrabajo(trabajo: TrabajoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<TrabajoEntity>)
}

@Dao
interface EppDao {
    @Query("SELECT * FROM epp ORDER BY nombre ASC")
    fun getAllEpps(): List<EppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<EppEntity>)
}

@Dao
interface ReglaTrabajoEppDao {
    @Query("SELECT * FROM reglas_trabajo_epp WHERE trabajoId = :trabajoId")
    fun getReglasPorTrabajo(trabajoId: Int): List<ReglaTrabajoEppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRegla(regla: ReglaTrabajoEppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<ReglaTrabajoEppEntity>)
}

@Dao
interface OrdenActivaDao {
    @Query("SELECT * FROM ordenes_activas ORDER BY id DESC")
    fun getAllOrdenes(): List<OrdenActivaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrden(orden: OrdenActivaEntity)

    @Update
    fun updateOrden(orden: OrdenActivaEntity)

    @Query("DELETE FROM ordenes_activas WHERE id = :ordenId")
    fun deleteOrden(ordenId: String)
}

@Dao
interface RegistroAuditoriaDao {
    @Query("SELECT * FROM bitacora_auditoria ORDER BY fechaHora DESC")
    fun getAllAuditorias(): List<RegistroAuditoriaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAuditoria(auditoria: RegistroAuditoriaEntity)
}

@Dao
interface AsignacionEppDao {
    @Query("SELECT * FROM asignaciones_epp ORDER BY fechaEntregaMillis ASC")
    fun getAllAsignaciones(): List<AsignacionEppEntity>

    @Query("SELECT * FROM asignaciones_epp WHERE personalId = :personalId")
    fun getAsignacionesPorPersonal(personalId: Int): List<AsignacionEppEntity>

    @Query("SELECT * FROM asignaciones_epp WHERE personalId = :personalId AND eppId = :eppId LIMIT 1")
    fun findByPersonalAndEpp(personalId: Int, eppId: Int): AsignacionEppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAsignacion(asignacion: AsignacionEppEntity)

    @Update
    fun updateAsignacion(asignacion: AsignacionEppEntity)

    @Query("DELETE FROM asignaciones_epp WHERE id = :asignacionId")
    fun deleteAsignacionById(asignacionId: String)

    @Query("DELETE FROM asignaciones_epp WHERE personalId = :personalId")
    fun deleteAsignacionesPorPersonal(personalId: Int)
}

@Dao
interface ZonaDao {
    @Query("SELECT * FROM zonas ORDER BY id ASC")
    fun getAllZonas(): List<ZonaEntity>

    @Query("SELECT * FROM zonas WHERE LOWER(nombre) = LOWER(:nombre) LIMIT 1")
    fun findByNombre(nombre: String): ZonaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertZona(zona: ZonaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<ZonaEntity>)
}
