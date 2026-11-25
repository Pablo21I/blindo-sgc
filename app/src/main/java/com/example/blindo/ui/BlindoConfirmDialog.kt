package com.example.blindo.ui

import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.example.blindo.R
import com.google.android.material.button.MaterialButton

/**
 * Diálogo de confirmación flotante centrado propio de Blindo (reemplaza el AlertDialog genérico).
 * Incluye spinner integrado en el botón mientras se ejecuta la acción (ej. cerrar sesión, finalizar o eliminar).
 */
object BlindoConfirmDialog {

    enum class Estilo {
        PELIGRO,
        EXITO,
        ACENTO
    }

    fun mostrar(
        activity: Activity,
        titulo: String,
        mensaje: String,
        textoConfirmar: String,
        textoCancelar: String = "Cancelar",
        estilo: Estilo = Estilo.PELIGRO,
        @DrawableRes iconoRes: Int? = null,
        onConfirmar: (finalizar: () -> Unit) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity, R.style.Theme_Blindo_Dialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_blindo_confirm, null)
        dialog.setContentView(view)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            val widthPx = (activity.resources.displayMetrics.widthPixels * 0.92f).toInt()
            setLayout(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val frameIcon = view.findViewById<FrameLayout>(R.id.frameConfirmIcon)
        val ivIcon = view.findViewById<ImageView>(R.id.ivConfirmIcon)
        val tvTitulo = view.findViewById<TextView>(R.id.tvConfirmTitulo)
        val tvMensaje = view.findViewById<TextView>(R.id.tvConfirmMensaje)
        val btnCancelar = view.findViewById<MaterialButton>(R.id.btnConfirmCancelar)
        val btnAceptar = view.findViewById<MaterialButton>(R.id.btnConfirmAceptar)
        val progress = view.findViewById<ProgressBar>(R.id.progressConfirmAceptar)

        val (colorRes, pillBgRes, defaultIcon) = when (estilo) {
            Estilo.PELIGRO -> Triple(R.color.status_red, R.drawable.bg_pill_red, R.drawable.ic_warning_triangle)
            Estilo.EXITO -> Triple(R.color.status_green, R.drawable.bg_pill_green, R.drawable.ic_check_circle)
            Estilo.ACENTO -> Triple(R.color.industrial_accent, R.drawable.bg_pill_accent, R.drawable.ic_shield_check)
        }

        val accentColor = ContextCompat.getColor(activity, colorRes)
        val btnTextColor = ContextCompat.getColor(
            activity,
            if (estilo == Estilo.EXITO) R.color.industrial_bg else R.color.white
        )

        frameIcon.setBackgroundResource(pillBgRes)
        ivIcon.setImageResource(iconoRes ?: defaultIcon)
        ivIcon.imageTintList = ColorStateList.valueOf(accentColor)

        tvTitulo.text = titulo
        tvMensaje.text = mensaje
        btnCancelar.text = textoCancelar
        btnAceptar.text = textoConfirmar
        btnAceptar.backgroundTintList = ColorStateList.valueOf(accentColor)
        btnAceptar.setTextColor(btnTextColor)
        progress.indeterminateTintList = ColorStateList.valueOf(btnTextColor)

        btnCancelar.setOnClickListener { dialog.dismiss() }
        btnAceptar.setOnClickListener {
            // Activamos spinner y bloqueamos doble toque mientras responde el servicio
            dialog.setCancelable(false)
            btnCancelar.isEnabled = false
            btnCancelar.alpha = 0.45f
            btnAceptar.isEnabled = false
            btnAceptar.text = ""
            progress.visibility = View.VISIBLE

            onConfirmar {
                if (dialog.isShowing && !activity.isFinishing && !activity.isDestroyed) {
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }
}
