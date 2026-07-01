package pa.ac.pa.miprimeraapp.ui.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

class WaterWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Progreso entre 0.0 y 1.0
    var progress: Float = 0.0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    // Animación de la onda
    private var waveShift = 0f
    private val waveAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 2 * Math.PI.toFloat()).apply {
        duration = 1800
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animator ->
            waveShift = animator.animatedValue as Float
            postInvalidateOnAnimation()
        }
    }

    private val bottlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A4373") // Contorno azul primario oscuro
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val bottleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#151A4373") // Transparente muy suave
        style = Paint.Style.FILL
    }

    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bottlePath = Path()
    private val waterPath = Path()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        waveAnimator.start()
    }

    override fun onDetachedFromWindow() {
        waveAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildBottlePath(w.toFloat(), h.toFloat())
    }

    // Crea un contorno estilizado de botella
    private fun rebuildBottlePath(width: Float, height: Float) {
        bottlePath.reset()
        
        val left = width * 0.25f
        val right = width * 0.75f
        val top = height * 0.1f
        val bottom = height * 0.9f
        val neckWidth = width * 0.15f
        val neckLeft = width * 0.425f
        val neckRight = width * 0.575f
        val shoulderY = height * 0.25f
        
        // Empezar arriba a la izquierda del cuello
        bottlePath.moveTo(neckLeft, top)
        // Ir a la derecha del cuello
        bottlePath.lineTo(neckRight, top)
        // Ir abajo en el cuello
        bottlePath.lineTo(neckRight, shoulderY * 0.8f)
        // Curva del hombro derecho
        bottlePath.cubicTo(
            neckRight, shoulderY,
            right, shoulderY,
            right, shoulderY * 1.3f
        )
        // Cuerpo derecho
        bottlePath.lineTo(right, bottom - 40f)
        // Esquina inferior derecha redonda
        bottlePath.quadTo(right, bottom, right - 40f, bottom)
        // Base inferior
        bottlePath.lineTo(left + 40f, bottom)
        // Esquina inferior izquierda redonda
        bottlePath.quadTo(left, bottom, left, bottom - 40f)
        // Cuerpo izquierdo
        bottlePath.lineTo(left, shoulderY * 1.3f)
        // Curva del hombro izquierdo
        bottlePath.cubicTo(
            left, shoulderY,
            neckLeft, shoulderY,
            neckLeft, shoulderY * 0.8f
        )
        // Cerrar con el cuello izquierdo
        bottlePath.close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w == 0f || h == 0f) return

        // Dibujar el fondo translúcido del interior de la botella
        canvas.drawPath(bottlePath, bottleBgPaint)

        // Dibujar el agua con ondas si hay progreso
        if (progress > 0f) {
            canvas.save()
            // Limitar el dibujo del agua a los bordes de la botella
            canvas.clipPath(bottlePath)

            waterPath.reset()

            // Calcular altura de agua
            val topLimit = h * 0.1f
            val bottomLimit = h * 0.9f
            val maxWaterHeight = bottomLimit - topLimit
            val waterY = bottomLimit - (maxWaterHeight * progress)

            val waveAmplitude = 15f // Altura de las crestas
            val waveFrequency = 0.03f // Ancho de las crestas

            waterPath.moveTo(0f, waterY)
            for (x in 0..w.toInt()) {
                val xF = x.toFloat()
                // Ecuación de onda
                val y = waterY + waveAmplitude * sin(waveFrequency * xF + waveShift)
                waterPath.lineTo(xF, y)
            }
            waterPath.lineTo(w, h)
            waterPath.lineTo(0f, h)
            waterPath.close()

            // Shader de degradado azul moderno
            val waterGradient = LinearGradient(
                w / 2, waterY, w / 2, h,
                Color.parseColor("#0288D1"), // Azul claro
                Color.parseColor("#1565C0"), // Azul oscuro
                Shader.TileMode.CLAMP
            )
            waterPaint.shader = waterGradient

            canvas.drawPath(waterPath, waterPaint)
            canvas.restore()
        }

        // Dibujar el contorno físico de la botella por encima del agua
        canvas.drawPath(bottlePath, bottlePaint)

        // Dibujar una marca decorativa de tapa (tapón) en la botella
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#112E52")
            style = Paint.Style.FILL
        }
        val top = h * 0.1f
        val neckLeft = w * 0.425f
        val neckRight = w * 0.575f
        val capHeight = 25f
        canvas.drawRoundRect(
            neckLeft - 5f, top - capHeight,
            neckRight + 5f, top,
            8f, 8f, capPaint
        )
    }
}
