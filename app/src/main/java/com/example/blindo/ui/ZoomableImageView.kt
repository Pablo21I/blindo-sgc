package com.example.blindo.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

/**
 * Visor de imágenes y páginas PDF con soporte nativo para:
 * - Zoom con pellizco (pinch-to-zoom)
 * - Doble toque para acercar/restaurar
 * - Arrastre fluido cuando está ampliado
 * - Botones de zoom (+, -, 100%) ideales para emulador y proyector
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrixValues = FloatArray(9)
    private val imageMatrixLocal = Matrix()

    private var minScale = 1f
    private var maxScale = 4f
    private var currentZoomFactor = 1f

    private var baseScale = 1f
    private var imgWidth = 0f
    private var imgHeight = 0f

    private val lastTouch = PointF()
    private var isDragging = false

    var onZoomChanged: ((Int) -> Unit)? = null

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                applyScale(detector.scaleFactor, detector.focusX, detector.focusY)
                return true
            }
        }
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (currentZoomFactor > 1.2f) {
                    resetZoom()
                } else {
                    val target = 2.2f
                    applyScale(target / currentZoomFactor, e.x, e.y)
                }
                return true
            }
        }
    )

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        if (bm != null) {
            imgWidth = bm.width.toFloat()
            imgHeight = bm.height.toFloat()
            post { fitToScreen() }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (imgWidth > 0 && imgHeight > 0) {
            fitToScreen()
        }
    }

    // Ajusta la imagen para que quepa completa y centrada al abrir el visor
    private fun fitToScreen() {
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        if (viewW <= 0f || viewH <= 0f || imgWidth <= 0f || imgHeight <= 0f) return

        val scaleX = viewW / imgWidth
        val scaleY = viewH / imgHeight
        baseScale = minOf(scaleX, scaleY)

        val redundantX = viewW - imgWidth * baseScale
        val redundantY = viewH - imgHeight * baseScale

        imageMatrixLocal.reset()
        imageMatrixLocal.postScale(baseScale, baseScale)
        imageMatrixLocal.postTranslate(redundantX / 2f, redundantY / 2f)
        imageMatrix = imageMatrixLocal

        currentZoomFactor = 1f
        notifyZoom()
    }

    private fun applyScale(factor: Float, focusX: Float, focusY: Float) {
        val newZoom = (currentZoomFactor * factor).coerceIn(minScale, maxScale)
        val delta = newZoom / currentZoomFactor
        currentZoomFactor = newZoom

        imageMatrixLocal.postScale(delta, delta, focusX, focusY)
        fixTranslation()
        imageMatrix = imageMatrixLocal
        notifyZoom()
    }

    // Evita que la imagen se salga de los bordes visibles al arrastrar o alejar
    private fun fixTranslation() {
        imageMatrixLocal.getValues(matrixValues)
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]
        val scaledW = imgWidth * baseScale * currentZoomFactor
        val scaledH = imgHeight * baseScale * currentZoomFactor

        val fixX = computeFix(transX, width.toFloat(), scaledW)
        val fixY = computeFix(transY, height.toFloat(), scaledH)
        if (fixX != 0f || fixY != 0f) {
            imageMatrixLocal.postTranslate(fixX, fixY)
        }
    }

    private fun computeFix(trans: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) {
            (viewSize - contentSize) / 2f - trans
        } else {
            val minTrans = viewSize - contentSize
            when {
                trans < minTrans -> minTrans - trans
                trans > 0f -> -trans
                else -> 0f
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouch.set(event.x, event.y)
                isDragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && !scaleDetector.isInProgress && currentZoomFactor > 1.01f) {
                    val dx = event.x - lastTouch.x
                    val dy = event.y - lastTouch.y
                    imageMatrixLocal.postTranslate(dx, dy)
                    fixTranslation()
                    imageMatrix = imageMatrixLocal
                    lastTouch.set(event.x, event.y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return true
    }

    fun zoomIn() {
        applyScale(1.4f, width / 2f, height / 2f)
    }

    fun zoomOut() {
        applyScale(1f / 1.4f, width / 2f, height / 2f)
    }

    fun resetZoom() {
        fitToScreen()
    }

    private fun notifyZoom() {
        onZoomChanged?.invoke((currentZoomFactor * 100).toInt())
    }
}
