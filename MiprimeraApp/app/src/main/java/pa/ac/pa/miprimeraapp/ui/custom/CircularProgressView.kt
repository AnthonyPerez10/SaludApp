package pa.ac.pa.miprimeraapp.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Valores de progreso y meta
    var stepsCurrent: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var stepsGoal: Float = 10000f
        set(value) {
            field = value
            invalidate()
        }

    var caloriesCurrent: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var caloriesGoal: Float = 2000f
        set(value) {
            field = value
            invalidate()
        }

    // Configuración de pinceles (Paints)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E6ED")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val stepsProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A4373") // Azul primario
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val caloriesProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF7043") // Naranja quemadores
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPrimaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#112E52")
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        isFakeBoldText = true
    }

    private val textSecondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#607D8B")
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
    }

    private val rectSteps = RectF()
    private val rectCalories = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val size = min(width, height)

        val strokeWidthSteps = size * 0.08f
        val strokeWidthCalories = size * 0.08f

        backgroundPaint.strokeWidth = strokeWidthSteps
        stepsProgressPaint.strokeWidth = strokeWidthSteps

        caloriesProgressPaint.strokeWidth = strokeWidthCalories
        // Hacemos el pincel de fondo de calorías un poco más delgado
        val bgCaloriesPaint = Paint(backgroundPaint).apply {
            strokeWidth = strokeWidthCalories
        }

        // Espaciado entre círculos
        val paddingOuter = strokeWidthSteps / 2f + size * 0.04f
        val stepsRadius = (size / 2f) - paddingOuter

        rectSteps.set(
            width / 2f - stepsRadius,
            height / 2f - stepsRadius,
            width / 2f + stepsRadius,
            height / 2f + stepsRadius
        )

        // Dibujar círculo base de pasos
        canvas.drawArc(rectSteps, 0f, 360f, false, backgroundPaint)

        // Dibujar arco de pasos
        val stepsPercentage = if (stepsGoal > 0) min(stepsCurrent / stepsGoal, 1f) else 0f
        val stepsAngle = stepsPercentage * 360f
        canvas.drawArc(rectSteps, -90f, stepsAngle, false, stepsProgressPaint)

        // Radio para calorías (círculo interno)
        val caloriesRadius = stepsRadius - strokeWidthSteps - size * 0.03f
        rectCalories.set(
            width / 2f - caloriesRadius,
            height / 2f - caloriesRadius,
            width / 2f + caloriesRadius,
            height / 2f + caloriesRadius
        )

        // Dibujar círculo base de calorías
        canvas.drawArc(rectCalories, 0f, 360f, false, bgCaloriesPaint)

        // Dibujar arco de calorías
        val caloriesPercentage = if (caloriesGoal > 0) min(caloriesCurrent / caloriesGoal, 1f) else 0f
        val caloriesAngle = caloriesPercentage * 360f
        canvas.drawArc(rectCalories, -90f, caloriesAngle, false, caloriesProgressPaint)

        // Dibujar textos en el centro
        textPrimaryPaint.textSize = size * 0.11f
        textSecondaryPaint.textSize = size * 0.05f

        // Centrado vertical manual simple
        val stepsText = "${stepsCurrent.toInt()} Pasos"
        val caloriesText = "${caloriesCurrent.toInt()} / ${caloriesGoal.toInt()} kcal"

        canvas.drawText(stepsText, width / 2f, height / 2f - (size * 0.02f), textPrimaryPaint)
        canvas.drawText(caloriesText, width / 2f, height / 2f + (size * 0.07f), textSecondaryPaint)
    }
}
