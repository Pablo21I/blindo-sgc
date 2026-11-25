package com.example.blindo.ui

import android.app.Activity
import android.app.Dialog
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import com.example.blindo.R
import com.google.android.material.button.MaterialButton

/**
 * Visor a pantalla completa con zoom (+ / - / pellizco / doble toque)
 * reutilizado para Permisos STPS en PDF y fotos de evidencia en Bitácora.
 */
object PdfPreviewDialogHelper {

    fun mostrar(
        activity: Activity,
        titulo: String,
        subtitulo: String,
        bitmap: Bitmap,
        textoBotonPrincipal: String,
        onBotonPrincipalClick: (Dialog) -> Unit
    ) {
        val dialog = Dialog(activity, R.style.Theme_Blindo)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_preview_pdf, null)
        dialog.setContentView(view)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        view.findViewById<TextView>(R.id.tvPdfDialogTitulo).text = titulo
        view.findViewById<TextView>(R.id.tvPdfFolioSub).text = subtitulo

        val zoomView = view.findViewById<ZoomableImageView>(R.id.ivPdfPreview)
        val tvZoomPorcentaje = view.findViewById<TextView>(R.id.tvZoomPorcentaje)
        val btnZoomIn = view.findViewById<MaterialButton>(R.id.btnZoomIn)
        val btnZoomOut = view.findViewById<MaterialButton>(R.id.btnZoomOut)
        val btnPrincipal = view.findViewById<MaterialButton>(R.id.btnCompartirPdf)
        val btnCerrar = view.findViewById<MaterialButton>(R.id.btnCerrarPdfPreview)

        zoomView.onZoomChanged = { pct ->
            tvZoomPorcentaje.text = "$pct%"
        }
        zoomView.setImageBitmap(bitmap)

        btnZoomIn.setOnClickListener { zoomView.zoomIn() }
        btnZoomOut.setOnClickListener { zoomView.zoomOut() }
        tvZoomPorcentaje.setOnClickListener { zoomView.resetZoom() }

        btnPrincipal.text = textoBotonPrincipal
        btnPrincipal.setOnClickListener { onBotonPrincipalClick(dialog) }
        btnCerrar.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
