package com.example.blindo.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.blindo.R
import com.google.android.material.card.MaterialCardView

/**
 * Notificaciones flotantes propias de Blindo (con identidad visual propia, icono y borde semáforo).
 * Aparecen arriba con animación suave para no ser tapadas por el teclado ni la barra inferior.
 */
object BlindoNotice {

    private const val TAG_NOTICE = "BLINDO_FLOATING_NOTICE"

    enum class Tipo {
        EXITO,
        ALERTA,
        ERROR,
        INFO
    }

    fun exito(activity: Activity, titulo: String, mensaje: String, duracionMs: Long = 2900L, window: Window? = null) =
        mostrar(activity, Tipo.EXITO, titulo, mensaje, duracionMs, window)

    fun alerta(activity: Activity, titulo: String, mensaje: String, duracionMs: Long = 3800L, window: Window? = null) =
        mostrar(activity, Tipo.ALERTA, titulo, mensaje, duracionMs, window)

    fun error(activity: Activity, titulo: String, mensaje: String, duracionMs: Long = 4200L, window: Window? = null) =
        mostrar(activity, Tipo.ERROR, titulo, mensaje, duracionMs, window)

    fun info(activity: Activity, titulo: String, mensaje: String, duracionMs: Long = 2800L, window: Window? = null) =
        mostrar(activity, Tipo.INFO, titulo, mensaje, duracionMs, window)

    fun mostrar(
        activity: Activity,
        tipo: Tipo,
        titulo: String,
        mensaje: String,
        duracionMs: Long = 3200L,
        targetWindow: Window? = null
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        val root = (targetWindow?.decorView?.findViewById<ViewGroup>(android.R.id.content))
            ?: activity.findViewById<ViewGroup>(android.R.id.content)
            ?: return

        // Si ya había un aviso activo, lo quitamos al instante para no encimar tarjetas
        root.findViewWithTag<View>(TAG_NOTICE)?.let { prev ->
            prev.removeCallbacks(null)
            root.removeView(prev)
        }

        val banner = LayoutInflater.from(activity).inflate(R.layout.view_blindo_notice, root, false)
        banner.tag = TAG_NOTICE

        val card = banner.findViewById<MaterialCardView>(R.id.cardBlindoNotice)
        val stripe = banner.findViewById<View>(R.id.viewNoticeStripe)
        val iconFrame = banner.findViewById<FrameLayout>(R.id.frameNoticeIcon)
        val icon = banner.findViewById<ImageView>(R.id.ivNoticeIcon)
        val tvTitulo = banner.findViewById<TextView>(R.id.tvNoticeTitulo)
        val tvMensaje = banner.findViewById<TextView>(R.id.tvNoticeMensaje)

        val (colorRes, bgRes, iconRes) = when (tipo) {
            Tipo.EXITO -> Triple(R.color.status_green, R.drawable.bg_pill_green, R.drawable.ic_check_circle)
            Tipo.ALERTA -> Triple(R.color.status_yellow, R.drawable.bg_pill_yellow, R.drawable.ic_warning_triangle)
            Tipo.ERROR -> Triple(R.color.status_red, R.drawable.bg_pill_red, R.drawable.ic_block)
            Tipo.INFO -> Triple(R.color.industrial_accent, R.drawable.bg_pill_accent, R.drawable.ic_shield_check)
        }

        val color = ContextCompat.getColor(activity, colorRes)
        card.strokeColor = color
        stripe.setBackgroundColor(color)
        iconFrame.setBackgroundResource(bgRes)
        icon.setImageResource(iconRes)
        icon.imageTintList = ColorStateList.valueOf(color)

        tvTitulo.text = titulo
        tvMensaje.text = mensaje

        val statusBarHeight = obtenerAlturaStatusBar(activity)
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        ).apply {
            topMargin = statusBarHeight
        }

        root.addView(banner, lp)

        // Animación de entrada suave desde arriba
        banner.alpha = 0f
        banner.translationY = -70f
        banner.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(240L)
            .start()

        val ocultarRunnable = Runnable {
            if (banner.parent == null) return@Runnable
            banner.animate()
                .alpha(0f)
                .translationY(-70f)
                .setDuration(200L)
                .withEndAction {
                    (banner.parent as? ViewGroup)?.removeView(banner)
                }
                .start()
        }

        // Permite cerrar el aviso tocándolo
        card.setOnClickListener {
            banner.removeCallbacks(ocultarRunnable)
            ocultarRunnable.run()
        }

        banner.postDelayed(ocultarRunnable, duracionMs)
    }

    private fun obtenerAlturaStatusBar(activity: Activity): Int {
        val resId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) activity.resources.getDimensionPixelSize(resId) else 0
    }
}
