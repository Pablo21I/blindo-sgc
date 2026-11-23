package com.example.blindo.data.session

import android.content.Context
import java.io.File

data class EvidenciaGuardada(
    val folio: String,
    val evidenciaPath: String?,
    val eppsVerificados: List<String>
)

/**
 * Asocia localmente la fotografía de evidencia, el folio STPS y los EPP verificados
 * al ID de cada validación registrada en MySQL para poder reabrir su PDF desde Taller o Bitácora.
 */
object EvidenciaStore {

    private const val PREFS_NAME = "BlindoEvidenciasPrefs"
    private const val SEP = "|||"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun guardar(
        context: Context,
        validacionId: String,
        folio: String,
        evidenciaPath: String?,
        eppsVerificados: List<String>
    ) {
        prefs(context).edit()
            .putString("folio_$validacionId", folio)
            .putString("foto_$validacionId", evidenciaPath ?: "")
            .putString("epps_$validacionId", eppsVerificados.joinToString(SEP))
            .apply()
    }

    fun obtener(context: Context, validacionId: String): EvidenciaGuardada? {
        val p = prefs(context)
        val folio = p.getString("folio_$validacionId", null)
        val fotoRaw = p.getString("foto_$validacionId", "")?.takeIf { it.isNotBlank() }
        val fotoValida = fotoRaw?.takeIf { File(it).exists() }
        val eppsRaw = p.getString("epps_$validacionId", "").orEmpty()
        val epps = if (eppsRaw.isBlank()) emptyList() else eppsRaw.split(SEP).filter { it.isNotBlank() }

        if (folio == null && fotoValida == null && epps.isEmpty()) return null
        return EvidenciaGuardada(
            folio = folio ?: "PTW-${validacionId.padStart(4, '0')}",
            evidenciaPath = fotoValida,
            eppsVerificados = epps
        )
    }

    fun tieneFoto(context: Context, validacionId: String): Boolean {
        val path = prefs(context).getString("foto_$validacionId", "").orEmpty()
        return path.isNotBlank() && File(path).exists()
    }
}
