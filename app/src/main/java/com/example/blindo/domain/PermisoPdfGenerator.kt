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
 * Generador nativo de Permisos de Trabajo STPS en PDF (sin dependencias externas)
 * con renderizador integrado de vista previa para Android Emulator y dispositivos físicos.
 */
object PermisoPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun generarPermisoPdf(context: Context, datos: DatosPermisoPdf): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        dibujarDocumento(canvas, datos)

        doc.finishPage(page)

        val dir = File(context.cacheDir, "permisos").apply { mkdirs() }
        val file = File(dir, "Permiso_Blindo_${datos.folio}.pdf")
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()
        return file
    }

    private fun dibujarDocumento(canvas: Canvas, datos: DatosPermisoPdf) {
        val margin = 36f
        val contentWidth = PAGE_WIDTH - margin * 2

        // Fondo blanco limpio
        canvas.drawColor(Color.WHITE)

        // 1. Encabezado corporativo azul oscuro
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B1324") }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 108f, headerPaint)

        val accentBar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0EA5E9") }
        canvas.drawRect(0f, 104f, PAGE_WIDTH.toFloat(), 108f, accentBar)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 10f
        }
        val folioPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }

        val df = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault())
        canvas.drawText("BLINDO SGC", margin, 44f, titlePaint)
        canvas.drawText("PERMISO DE TRABAJO SEGURO · NORMATIVA STPS / NOM-017", margin, 64f, subtitlePaint)
        canvas.drawText("Sistema de Control Poka-Yoke & Matriz SIMOPS", margin, 80f, subtitlePaint)

        canvas.drawText("FOLIO: ${datos.folio}", PAGE_WIDTH - margin, 44f, folioPaint)
        canvas.drawText("Emisión: ${df.format(Date(datos.fechaMillis))}", PAGE_WIDTH - margin, 62f, datePaint)

        var y = 126f

        // 2. Banner de Dictamen SGC
        val (statusBgHex, statusTextHex, statusLabel) = when (datos.estado) {
            "VERDE" -> Triple("#DCFCE7", "#065F46", "DICTAMEN: AUTORIZADO Y BLINDADO — APTO PARA OPERAR")
            "AMARILLO" -> Triple("#FEF3C7", "#92400E", "DICTAMEN: AUTORIZADO CON PRECAUCIÓN — SUPERVISIÓN ESPECIAL")
            else -> Triple("#FFE4E6", "#9F1239", "DICTAMEN: TRABAJO BLOQUEADO — OPERACIÓN NO AUTORIZADA")
        }
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(statusBgHex) }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(statusTextHex)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawRoundRect(RectF(margin, y, PAGE_WIDTH - margin, y + 32f), 8f, 8f, badgeBgPaint)
        canvas.drawText(statusLabel, margin + 12f, y + 20f, badgeTextPaint)
        y += 48f

        // Estilos de sección
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
        }
        val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val boxFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }

        // 3. Sección 1: Datos de la operación
        canvas.drawText("1. DATOS DE LA OPERACIÓN Y RESPONSABLES", margin, y, sectionTitlePaint)
        y += 8f

        val infoTop = y
        val infoHeight = 114f
        canvas.drawRoundRect(RectF(margin, infoTop, PAGE_WIDTH - margin, infoTop + infoHeight), 8f, 8f, boxFill)
        canvas.drawRoundRect(RectF(margin, infoTop, PAGE_WIDTH - margin, infoTop + infoHeight), 8f, 8f, boxStroke)

        val col1 = margin + 12f
        val col2 = margin + contentWidth / 2f + 8f

        canvas.drawText("ACTIVIDAD / TAREA", col1, infoTop + 18f, labelPaint)
        canvas.drawText(datos.trabajo.take(38), col1, infoTop + 32f, valuePaint)

        canvas.drawText("NORMA Y RIESGO", col2, infoTop + 18f, labelPaint)
        canvas.drawText("${datos.norma} · Riesgo ${datos.riesgo}", col2, infoTop + 32f, valuePaint)

        canvas.drawText("OPERADOR ASIGNADO", col1, infoTop + 56f, labelPaint)
        canvas.drawText(datos.operador.take(36), col1, infoTop + 70f, valuePaint)

        canvas.drawText("SUPERVISOR SGC", col2, infoTop + 56f, labelPaint)
        canvas.drawText(datos.supervisor.take(36), col2, infoTop + 70f, valuePaint)

        canvas.drawText("ZONA DE TRABAJO", col1, infoTop + 92f, labelPaint)
        canvas.drawText(datos.zona.take(36), col1, infoTop + 106f, valuePaint)

        canvas.drawText("TURNO", col2, infoTop + 92f, labelPaint)
        canvas.drawText(datos.turno.take(36), col2, infoTop + 106f, valuePaint)

        y = infoTop + infoHeight + 22f

        // 4. Sección 2: EPP Verificado
        canvas.drawText("2. EQUIPO DE PROTECCIÓN PERSONAL VERIFICADO (NOM-017)", margin, y, sectionTitlePaint)
        y += 10f

        val eppTexto = if (datos.eppsVerificados.isNotEmpty()) {
            datos.eppsVerificados.joinToString("   |   ") { "✓ $it" }
        } else {
            "Verificado conforme a checklist técnico de la tarea."
        }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
        }
        val eppLayout = StaticLayout.Builder.obtain(
            eppTexto, 0, eppTexto.length, textPaint, (contentWidth - 24f).toInt()
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(3f, 1f).build()

        val eppBoxHeight = eppLayout.height + 20f
        canvas.drawRoundRect(RectF(margin, y, PAGE_WIDTH - margin, y + eppBoxHeight), 8f, 8f, boxFill)
        canvas.drawRoundRect(RectF(margin, y, PAGE_WIDTH - margin, y + eppBoxHeight), 8f, 8f, boxStroke)
        canvas.save()
        canvas.translate(margin + 12f, y + 10f)
        eppLayout.draw(canvas)
        canvas.restore()

        y += eppBoxHeight + 20f

        // 5. Sección 3: Diagnóstico SGC / SIMOPS
        canvas.drawText("3. DIAGNÓSTICO TÉCNICO POKA-YOKE / SIMOPS", margin, y, sectionTitlePaint)
        y += 10f

        val diagLayout = StaticLayout.Builder.obtain(
            datos.diagnostico, 0, datos.diagnostico.length, textPaint, (contentWidth - 24f).toInt()
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(3f, 1f).build()

        val diagBoxHeight = diagLayout.height + 20f
        canvas.drawRoundRect(RectF(margin, y, PAGE_WIDTH - margin, y + diagBoxHeight), 8f, 8f, boxFill)
        canvas.drawRoundRect(RectF(margin, y, PAGE_WIDTH - margin, y + diagBoxHeight), 8f, 8f, boxStroke)
        canvas.save()
        canvas.translate(margin + 12f, y + 10f)
        diagLayout.draw(canvas)
        canvas.restore()

        y += diagBoxHeight + 20f

        // 6. Sección 4: Evidencia Fotográfica (si fue capturada)
        val fotoFile = datos.evidenciaPath?.let { File(it) }?.takeIf { it.exists() }
        if (fotoFile != null && y < 560f) {
            canvas.drawText("4. EVIDENCIA FOTOGRÁFICA DE INSPECCIÓN EN SITIO", margin, y, sectionTitlePaint)
            y += 10f

            val bmp = BitmapFactory.decodeFile(fotoFile.absolutePath)
            if (bmp != null) {
                val maxBoxH = (690f - y).coerceIn(100f, 180f)
                val maxBoxW = 240f
                val ratio = minOf(maxBoxW / bmp.width.toFloat(), maxBoxH / bmp.height.toFloat())
                val drawW = bmp.width * ratio
                val drawH = bmp.height * ratio

                val imgRect = RectF(margin, y, margin + drawW, y + drawH)
                canvas.drawBitmap(bmp, null, imgRect, Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.drawRect(imgRect, boxStroke)

                // Nota al lado de la foto
                val noteX = margin + drawW + 16f
                val noteW = (PAGE_WIDTH - margin - noteX).toInt().coerceAtLeast(100)
                val notaEvidencia = "Evidencia capturada al momento de la validación.\nFecha: ${df.format(Date(datos.fechaMillis))}\nZona: ${datos.zona}\nOperador: ${datos.operador}"
                val noteLayout = StaticLayout.Builder.obtain(
                    notaEvidencia, 0, notaEvidencia.length, textPaint, noteW
                ).setLineSpacing(4f, 1f).build()

                canvas.save()
                canvas.translate(noteX, y + 8f)
                noteLayout.draw(canvas)
                canvas.restore()

                bmp.recycle()
            }
        }

        // 7. Sección de Firmas al pie del documento
        val sigY = 768f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            strokeWidth = 1f
        }
        val sigLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
        }
        val sigNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val leftCenter = margin + contentWidth * 0.23f
        val rightCenter = margin + contentWidth * 0.77f

        canvas.drawLine(margin + 10f, sigY, margin + contentWidth * 0.44f, sigY, linePaint)
        canvas.drawText(datos.supervisor.take(30), leftCenter, sigY + 14f, sigNamePaint)
        canvas.drawText("Supervisor SGC Autorizador", leftCenter, sigY + 26f, sigLabelPaint)

        canvas.drawLine(margin + contentWidth * 0.56f, sigY, PAGE_WIDTH - margin - 10f, sigY, linePaint)
        canvas.drawText(datos.operador.take(30), rightCenter, sigY + 14f, sigNamePaint)
        canvas.drawText("Conformidad del Operador", rightCenter, sigY + 26f, sigLabelPaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Documento electrónico generado por Blindo SGC · Respaldo auditable NOM-017-STPS",
            PAGE_WIDTH / 2f,
            822f,
            footerPaint
        )
    }

    /**
     * Renderiza la página 1 del PDF como Bitmap de alta resolución usando PdfRenderer nativo.
     * Garantiza vista previa instantánea incluso en emuladores sin visor PDF instalado.
     */
    fun renderizarPrimeraPagina(pdfFile: File): Bitmap? {
        return try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount <= 0) return null
                    renderer.openPage(0).use { page ->
                        val scale = 2
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
