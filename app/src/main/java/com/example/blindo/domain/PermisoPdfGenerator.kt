package com.example.blindo.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DatosPermisoPdf(
    val folio: String,
    val estado: String, // "VERDE", "AMARILLO", "ROJO"
    val trabajo: String,
    val norma: String,
    val riesgo: String,
    val operador: String,
    val supervisor: String,
    val zona: String,
    val turno: String,
    val diagnostico: String,
    val eppsVerificados: List<String> = emptyList(),
    val evidenciaPath: String? = null,
    val fechaMillis: Long = System.currentTimeMillis()
)

/**
 * Generador nativo de Permiso de Trabajo Seguro (formato tabular clásico STPS / ISO-9001).
 * Utiliza cuadrícula formal en blanco y negro con acentos sobrios de dictamen y ajuste multilínea
 * para que ningún nombre ni dato se recorte.
 */
object PermisoPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun generarPermisoPdf(context: Context, datos: DatosPermisoPdf): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        dibujarFormatoOficial(page.canvas, datos)
        doc.finishPage(page)

        val dir = File(context.cacheDir, "permisos").apply { mkdirs() }
        val file = File(dir, "Permiso_Blindo_${datos.folio}.pdf")
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()
        return file
    }

    private fun dibujarFormatoOficial(canvas: Canvas, datos: DatosPermisoPdf) {
        val m = 34f
        val contentW = PAGE_WIDTH - m * 2
        val df = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())

        // Hoja blanca limpia
        canvas.drawColor(Color.WHITE)

        // Tintas y bordes formales de formato industrial (estilo ISO-9001 / STPS)
        val borderHeavy = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F2937")
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
        }
        val borderThin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7280")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val bandFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5E7EB")
            style = Paint.Style.FILL
        }
        val subtleCellFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F9FAFB")
            style = Paint.Style.FILL
        }

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val brandSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val docTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val docSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#374151")
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }
        val metaLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val fieldLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 9.5f
        }
        val boldBodyTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Marco exterior del formato (estilo documento técnico impreso)
        canvas.drawRect(m - 6f, 24f, PAGE_WIDTH - m + 6f, PAGE_HEIGHT - 26f, borderThin)

        var y = 34f

        // =====================================================================
        // ENCABEZADO TABULAR ISO-9001 / STPS (3 COLUMNAS)
        // =====================================================================
        val headerH = 66f
        val leftColW = 115f
        val rightColW = 152f
        val midColX = m + leftColW
        val rightColX = m + contentW - rightColW

        canvas.drawRect(m, y, m + contentW, y + headerH, borderHeavy)
        canvas.drawLine(midColX, y, midColX, y + headerH, borderHeavy)
        canvas.drawLine(rightColX, y, rightColX, y + headerH, borderHeavy)

        // Columna izquierda: Identidad
        canvas.drawText("BLINDO", m + leftColW / 2f, y + 32f, brandPaint)
        canvas.drawText("SEGURIDAD INDUSTRIAL", m + leftColW / 2f, y + 46f, brandSubPaint)
        canvas.drawText("CONTROL EN SITIO", m + leftColW / 2f, y + 56f, brandSubPaint)

        // Columna central: Nombre oficial del formato
        val midCenter = midColX + (rightColX - midColX) / 2f
        canvas.drawText("PERMISO DE TRABAJO SEGURO", midCenter, y + 29f, docTitlePaint)
        canvas.drawText("Registro de Validación de EPP y Condiciones de Riesgo", midCenter, y + 44f, docSubPaint)
        canvas.drawText("Referencia normativa: ${datos.norma} / NOM-017-STPS", midCenter, y + 56f, docSubPaint)

        // Columna derecha: 3 sub-filas (Folio, Fecha, Riesgo)
        val rowH = headerH / 3f
        canvas.drawLine(rightColX, y + rowH, m + contentW, y + rowH, borderThin)
        canvas.drawLine(rightColX, y + rowH * 2f, m + contentW, y + rowH * 2f, borderThin)

        canvas.drawText("FOLIO:", rightColX + 6f, y + 14f, metaLabelPaint)
        canvas.drawText(datos.folio, rightColX + 42f, y + 14f, metaValuePaint)

        canvas.drawText("FECHA:", rightColX + 6f, y + rowH + 14f, metaLabelPaint)
        canvas.drawText(df.format(Date(datos.fechaMillis)), rightColX + 42f, y + rowH + 14f, metaValuePaint)

        canvas.drawText("RIESGO:", rightColX + 6f, y + rowH * 2f + 14f, metaLabelPaint)
        canvas.drawText(datos.riesgo.uppercase(Locale.getDefault()), rightColX + 46f, y + rowH * 2f + 14f, metaValuePaint)

        y += headerH + 12f

        // =====================================================================
        // SELLO DE DICTAMEN (ESTILO SELLO INDUSTRIAL RECTANGULAR, SIN PILL SAAS)
        // =====================================================================
        val (stampColorHex, stampBgHex, stampTitle, stampDesc) = when (datos.estado) {
            "VERDE" -> Quadruple(
                "#166534", "#F0FDF4",
                "DICTAMEN: AUTORIZADO",
                "Condiciones de seguridad y EPP conformes para iniciar operación."
            )
            "AMARILLO" -> Quadruple(
                "#92400E", "#FFFBEB",
                "DICTAMEN: AUTORIZADO CON PRECAUCIÓN",
                "Requiere supervisión continua y medidas adicionales indicadas abajo."
            )
            else -> Quadruple(
                "#991B1B", "#FEF2F2",
                "DICTAMEN: TRABAJO BLOQUEADO",
                "Operación detenida por incumplimiento de seguridad o riesgo incompatible."
            )
        }
        val stampFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(stampBgHex)
            style = Paint.Style.FILL
        }
        val stampBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(stampColorHex)
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
        }
        val stampText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(stampColorHex)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val stampSubText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F2937")
            textSize = 8.8f
        }

        val stampH = 36f
        canvas.drawRect(m, y, m + contentW, y + stampH, stampFill)
        canvas.drawRect(m, y, m + contentW, y + stampH, stampBorder)
        canvas.drawRect(m, y, m + 5f, y + stampH, Paint().apply { color = Color.parseColor(stampColorHex) })
        canvas.drawText(stampTitle, m + 13f, y + 15f, stampText)
        canvas.drawText(stampDesc, m + 13f, y + 28f, stampSubText)

        y += stampH + 12f

        // =====================================================================
        // SECCIÓN I: DATOS GENERALES DE LA OPERACIÓN (TABLA CON AJUSTE MULTILÍNEA)
        // =====================================================================
        y = dibujarBandaSeccion(canvas, m, y, contentW, "I. DATOS GENERALES DE LA OPERACIÓN", bandFill, borderHeavy, sectionHeaderPaint)

        val halfW = contentW / 2f
        val cellPad = 7f
        val textMaxW = (halfW - cellPad * 2).toInt()

        val filasDatos = listOf(
            Pair("ACTIVIDAD / TAREA" to datos.trabajo, "NORMA APLICABLE" to "${datos.norma} · Riesgo ${datos.riesgo}"),
            Pair("OPERADOR ASIGNADO" to datos.operador, "SUPERVISOR AUTORIZADOR" to datos.supervisor),
            Pair("ZONA / ÁREA DE TRABAJO" to datos.zona, "TURNO" to datos.turno)
        )

        for ((izq, der) in filasDatos) {
            val leftLayout = buildStaticLayout(izq.second, bodyTextPaint, textMaxW)
            val rightLayout = buildStaticLayout(der.second, bodyTextPaint, textMaxW)
            val cellH = maxOf(leftLayout.height, rightLayout.height) + 22f

            canvas.drawRect(m, y, m + halfW, y + cellH, subtleCellFill)
            canvas.drawRect(m + halfW, y, m + contentW, y + cellH, subtleCellFill)
            canvas.drawRect(m, y, m + halfW, y + cellH, borderThin)
            canvas.drawRect(m + halfW, y, m + contentW, y + cellH, borderThin)

            canvas.drawText(izq.first, m + cellPad, y + 11f, fieldLabelPaint)
            drawStaticLayout(canvas, leftLayout, m + cellPad, y + 15f)

            canvas.drawText(der.first, m + halfW + cellPad, y + 11f, fieldLabelPaint)
            drawStaticLayout(canvas, rightLayout, m + halfW + cellPad, y + 15f)

            y += cellH
        }

        y += 12f

        // =====================================================================
        // SECCIÓN II: EQUIPO DE PROTECCIÓN PERSONAL VERIFICADO (LISTA [X])
        // =====================================================================
        y = dibujarBandaSeccion(canvas, m, y, contentW, "II. EQUIPO DE PROTECCIÓN PERSONAL VERIFICADO (NOM-017)", bandFill, borderHeavy, sectionHeaderPaint)

        val eppTexto = if (datos.eppsVerificados.isNotEmpty()) {
            datos.eppsVerificados.joinToString("    ") { "[X] $it" }
        } else {
            "[ ] Sin equipos marcados en el listado de verificación."
        }
        val eppLayout = buildStaticLayout(eppTexto, bodyTextPaint, (contentW - 16f).toInt())
        val eppBoxH = eppLayout.height + 16f
        canvas.drawRect(m, y, m + contentW, y + eppBoxH, borderThin)
        drawStaticLayout(canvas, eppLayout, m + 8f, y + 8f)

        y += eppBoxH + 12f

        // =====================================================================
        // SECCIÓN III: OBSERVACIONES Y DIAGNÓSTICO DE SEGURIDAD
        // =====================================================================
        y = dibujarBandaSeccion(canvas, m, y, contentW, "III. DIAGNÓSTICO Y OBSERVACIONES DE SEGURIDAD", bandFill, borderHeavy, sectionHeaderPaint)

        val diagLimpio = datos.diagnostico
            .removePrefix("Orden iniciada en piso: ")
            .trim()
            .ifEmpty { "Verificación completada conforme a procedimiento interno." }
        val diagLayout = buildStaticLayout(diagLimpio, bodyTextPaint, (contentW - 16f).toInt())
        val diagBoxH = diagLayout.height + 16f
        canvas.drawRect(m, y, m + contentW, y + diagBoxH, borderThin)
        drawStaticLayout(canvas, diagLayout, m + 8f, y + 8f)

        y += diagBoxH + 12f

        // =====================================================================
        // SECCIÓN IV: EVIDENCIA FOTOGRÁFICA EN SITIO (SI EXISTE)
        // =====================================================================
        val fotoFile = datos.evidenciaPath?.let { File(it) }?.takeIf { it.exists() }
        if (fotoFile != null && y < 565f) {
            y = dibujarBandaSeccion(canvas, m, y, contentW, "IV. EVIDENCIA FOTOGRÁFICA EN SITIO", bandFill, borderHeavy, sectionHeaderPaint)

            val bmp = BitmapFactory.decodeFile(fotoFile.absolutePath)
            if (bmp != null) {
                val maxBoxH = (675f - y).coerceIn(105f, 168f)
                val maxBoxW = 235f
                val ratio = minOf(maxBoxW / bmp.width.toFloat(), maxBoxH / bmp.height.toFloat())
                val drawW = bmp.width * ratio
                val drawH = bmp.height * ratio
                val boxH = drawH + 16f

                canvas.drawRect(m, y, m + contentW, y + boxH, borderThin)

                val imgRect = RectF(m + 8f, y + 8f, m + 8f + drawW, y + 8f + drawH)
                canvas.drawBitmap(bmp, null, imgRect, Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.drawRect(imgRect, borderThin)

                val noteX = m + 16f + drawW
                val noteW = (m + contentW - 8f - noteX).toInt().coerceAtLeast(110)
                val notaEvidencia =
                    "Registro visual tomado al momento de la inspección.\n\n" +
                        "Fecha y hora: ${df.format(Date(datos.fechaMillis))}\n" +
                        "Área: ${datos.zona}\n" +
                        "Operador: ${datos.operador}"
                val noteLayout = buildStaticLayout(notaEvidencia, bodyTextPaint, noteW)
                drawStaticLayout(canvas, noteLayout, noteX, y + 10f)

                bmp.recycle()
            }
        }

        // =====================================================================
        // SECCIÓN DE FIRMAS (CON AJUSTE MULTILÍNEA — NUNCA RECORTA NOMBRES LARGOS)
        // =====================================================================
        val sigBoxTop = 716f
        val sigBoxH = 82f
        val sigColW = contentW / 2f

        canvas.drawRect(m, sigBoxTop, m + sigColW, sigBoxTop + sigBoxH, borderThin)
        canvas.drawRect(m + sigColW, sigBoxTop, m + contentW, sigBoxTop + sigBoxH, borderThin)

        val lineY = sigBoxTop + 34f
        canvas.drawLine(m + 24f, lineY, m + sigColW - 24f, lineY, borderHeavy)
        canvas.drawLine(m + sigColW + 24f, lineY, m + contentW - 24f, lineY, borderHeavy)

        val sigTextW = (sigColW - 20f).toInt()
        val supLayout = buildStaticLayout(datos.supervisor, boldBodyTextPaint, sigTextW, Layout.Alignment.ALIGN_CENTER)
        val opLayout = buildStaticLayout(datos.operador, boldBodyTextPaint, sigTextW, Layout.Alignment.ALIGN_CENTER)

        drawStaticLayout(canvas, supLayout, m + 10f, lineY + 5f)
        drawStaticLayout(canvas, opLayout, m + sigColW + 10f, lineY + 5f)

        val rolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Firma del Supervisor Autorizador", m + sigColW / 2f, sigBoxTop + sigBoxH - 8f, rolePaint)
        canvas.drawText("Firma de Conformidad del Operador", m + sigColW + sigColW / 2f, sigBoxTop + sigBoxH - 8f, rolePaint)

        // Pie de página sobrio
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7280")
            textSize = 7.8f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Formato de Control Interno · Blindo Seguridad Industrial · Folio ${datos.folio}",
            PAGE_WIDTH / 2f,
            810f,
            footerPaint
        )
    }

    private fun dibujarBandaSeccion(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        titulo: String,
        bgPaint: Paint,
        borderPaint: Paint,
        textPaint: Paint
    ): Float {
        val h = 19f
        canvas.drawRect(x, y, x + w, y + h, bgPaint)
        canvas.drawRect(x, y, x + w, y + h, borderPaint)
        canvas.drawText(titulo, x + 7f, y + 13f, textPaint)
        return y + h
    }

    private fun buildStaticLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(40))
            .setAlignment(alignment)
            .setLineSpacing(2.5f, 1f)
            .build()
    }

    private fun drawStaticLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    /**
     * Renderiza la página 1 del PDF como Bitmap en alta resolución (3x) usando PdfRenderer nativo.
     */
    fun renderizarPrimeraPagina(pdfFile: File): Bitmap? {
        return try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount <= 0) return null
                    renderer.openPage(0).use { page ->
                        val scale = 3
                        val bmp = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, Rect(0, 0, bmp.width, bmp.height), null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun compartirPdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Permiso de Trabajo STPS - ${pdfFile.nameWithoutExtension}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Exportar o compartir Permiso STPS (PDF)"))
    }
}
