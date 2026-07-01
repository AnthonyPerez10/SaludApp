package pa.ac.pa.miprimeraapp.ui.hydration

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.data.SaludAppRepository
import pa.ac.pa.miprimeraapp.data.SaludAppRepositoryImpl
import pa.ac.pa.miprimeraapp.ui.custom.WaterWaveView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad para registrar el consumo diario de agua y calcular metas personalizadas.
 * Los datos se persisten de manera limpia a través del repositorio central.
 */
class HidratacionActivity : AppCompatActivity() {

    // Repositorio central de datos
    private lateinit var repository: SaludAppRepository

    // Vistas principales
    private lateinit var tvWaterCounter: TextView
    private lateinit var waterWaveView: WaterWaveView
    private lateinit var tvProgressPercentage: TextView
    private lateinit var tvSmartTip: TextView
    
    // KPIs del panel de estadísticas
    private lateinit var tvWaterStreakKPI: TextView
    private lateinit var tvWaterTodayKPI: TextView

    // Registro Rápido
    private lateinit var btnLog250: View
    private lateinit var btnLog500: View
    private lateinit var btnLog750: View
    private lateinit var btnLogCustom: View

    // Panel Custom SeekBar
    private lateinit var layoutCustomLog: View
    private lateinit var tvCustomAmount: TextView
    private lateinit var sbCustomAmount: SeekBar
    private lateinit var btnConfirmCustomLog: Button

    // Estadísticas de Hidratación Semanal (Gráfico de Barras)
    private lateinit var barWaterL: View
    private lateinit var barWaterM: View
    private lateinit var barWaterMi: View
    private lateinit var barWaterJ: View
    private lateinit var barWaterV: View
    private lateinit var barWaterS: View
    private lateinit var barWaterD: View

    // Formulario Meta
    private lateinit var etUserWeight: EditText
    private lateinit var rgUserGender: RadioGroup
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var spinnerUserActivity: Spinner
    private lateinit var btnCalculateGoal: Button

    // Estado local
    private var waterToday = 0
    private var waterGoal = 2000 // Predeterminado de 2 Litros

    private val activityLevels = arrayOf("Sedentario", "Moderado", "Muy Activo")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_hidratacion)

        repository = SaludAppRepositoryImpl(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inicializarVistas()
        setupSpinner()
        cargarDatos()
        setupListeners()
        actualizarUI()

        // Fecha dinámica en el encabezado
        val tvDateTimeInfo = findViewById<TextView>(R.id.tvDateTimeInfo)
        val hoyStr = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("es-ES")).format(Date())
        tvDateTimeInfo.text = "REGISTRO: Hoy - ${hoyStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
    }

    private fun inicializarVistas() {
        tvWaterCounter = findViewById(R.id.tvWaterCounter)
        waterWaveView = findViewById(R.id.waterWaveView)
        tvProgressPercentage = findViewById(R.id.tvProgressPercentage)
        tvSmartTip = findViewById(R.id.tvSmartTip)

        // KPIs de estadísticas
        tvWaterStreakKPI = findViewById(R.id.tvWaterStreakKPI)
        tvWaterTodayKPI = findViewById(R.id.tvWaterTodayKPI)

        btnLog250 = findViewById(R.id.btnLog250)
        btnLog500 = findViewById(R.id.btnLog500)
        btnLog750 = findViewById(R.id.btnLog750)
        btnLogCustom = findViewById(R.id.btnLogCustom)

        layoutCustomLog = findViewById(R.id.layoutCustomLog)
        tvCustomAmount = findViewById(R.id.tvCustomAmount)
        sbCustomAmount = findViewById(R.id.sbCustomAmount)
        btnConfirmCustomLog = findViewById(R.id.btnConfirmCustomLog)

        barWaterL = findViewById(R.id.barWaterL)
        barWaterM = findViewById(R.id.barWaterM)
        barWaterMi = findViewById(R.id.barWaterMi)
        barWaterJ = findViewById(R.id.barWaterJ)
        barWaterV = findViewById(R.id.barWaterV)
        barWaterS = findViewById(R.id.barWaterS)
        barWaterD = findViewById(R.id.barWaterD)

        etUserWeight = findViewById(R.id.etUserWeight)
        rgUserGender = findViewById(R.id.rgUserGender)
        rbMale = findViewById(R.id.rbMale)
        rbFemale = findViewById(R.id.rbFemale)
        spinnerUserActivity = findViewById(R.id.spinnerUserActivity)
        btnCalculateGoal = findViewById(R.id.btnCalculateGoal)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, activityLevels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUserActivity.adapter = adapter
    }

    private fun setupListeners() {
        // Botones rápidos
        btnLog250.setOnClickListener { registrarAgua(250) }
        btnLog500.setOnClickListener { registrarAgua(500) }
        btnLog750.setOnClickListener { registrarAgua(750) }

        // Botón Personalizado (Alternar panel)
        btnLogCustom.setOnClickListener {
            layoutCustomLog.visibility = if (layoutCustomLog.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // SeekBar
        sbCustomAmount.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val roundedProgress = (progress / 10) * 10
                tvCustomAmount.text = "$roundedProgress ml"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Confirmar custom log
        btnConfirmCustomLog.setOnClickListener {
            val amount = (sbCustomAmount.progress / 10) * 10
            if (amount > 0) {
                registrarAgua(amount)
                layoutCustomLog.visibility = View.GONE
            }
        }

        // Calcular meta ideal
        btnCalculateGoal.setOnClickListener {
            calcularMetaPersonalizada()
        }
    }

    /**
     * Registra una cantidad específica de consumo de agua, actualizando el estado,
     * persistiendo en SharedPreferences y refrescando los KPIs de UI.
     */
    private fun registrarAgua(cantidad: Int) {
        waterToday += cantidad
        if (waterToday > 10000) waterToday = 10000 // Límite de seguridad fisiológica

        guardarDatos()
        actualizarUI()
        Toast.makeText(this, "+$cantidad ml de agua registrados", Toast.LENGTH_SHORT).show()
    }

    /**
     * Calcula la meta diaria recomendada por la OMS adaptada a la actividad y género.
     */
    private fun calcularMetaPersonalizada() {
        val pesoStr = etUserWeight.text.toString().trim()
        if (pesoStr.isEmpty()) {
            etUserWeight.error = "Ingresa tu peso en kg"
            etUserWeight.requestFocus()
            return
        }

        val peso = pesoStr.toFloatOrNull()
        if (peso == null || peso < 30f || peso > 250f) {
            etUserWeight.error = "Ingresa un peso válido (30 a 250 kg)"
            etUserWeight.requestFocus()
            return
        }

        val esMasculino = rbMale.isChecked
        val actividad = spinnerUserActivity.selectedItem.toString()

        // Fórmula inteligente: Peso * 35 ml (base recomendada por la OMS)
        var metaRecomendada = (peso * 35).toInt()

        // Ajustes por actividad y género
        metaRecomendada += when (actividad) {
            "Moderado" -> 350
            "Muy Activo" -> 700
            else -> 0
        }
        metaRecomendada += if (esMasculino) 250 else 0

        // Límites razonables
        if (metaRecomendada < 1000) metaRecomendada = 1000
        if (metaRecomendada > 6000) metaRecomendada = 6000

        waterGoal = metaRecomendada
        guardarDatos()

        // Guardar parámetros del cálculo
        repository.saveHydrationWeight(peso)
        repository.saveHydrationIsMale(esMasculino)
        repository.saveHydrationActivityPos(spinnerUserActivity.selectedItemPosition)

        actualizarUI()
        Toast.makeText(this, "Meta recomendada calculada: $waterGoal ml", Toast.LENGTH_LONG).show()
    }

    private fun cargarDatos() {
        // Verificar si cambió de día
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val diaGuardado = repository.getHydrationCurrentDay()

        if (diaGuardado != hoyStr) {
            // Guardar meta de ayer antes de reiniciar
            guardarMetaAyer(diaGuardado)
            waterToday = 0
            repository.saveWaterToday(0)
            repository.saveHydrationCurrentDay(hoyStr)
        } else {
            waterToday = repository.getWaterToday()
        }

        waterGoal = repository.getWaterGoal()

        // Rellenar formulario si ya existían datos calculados previamente
        val peso = repository.getHydrationWeight()
        if (peso > 0) {
            etUserWeight.setText(peso.toString())
            val esMasculino = repository.getHydrationIsMale()
            if (esMasculino) rbMale.isChecked = true else rbFemale.isChecked = true
            spinnerUserActivity.setSelection(repository.getHydrationActivityPos())
        }
    }

    private fun guardarDatos() {
        repository.saveWaterToday(waterToday)
        repository.saveWaterGoal(waterGoal)

        // Registrar cumplimiento del día de la semana actual
        val calendar = Calendar.getInstance()
        val dayIndex = when(calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        repository.saveWaterHistoryDay(dayIndex, waterToday >= waterGoal)
    }

    private fun guardarMetaAyer(diaAyer: String?) {
        if (diaAyer.isNullOrEmpty()) return
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        try {
            val dateAyer = sdf.parse(diaAyer)
            if (dateAyer != null) {
                cal.time = dateAyer
                val dayIndex = when(cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }
                repository.saveWaterHistoryDay(dayIndex, waterToday >= waterGoal)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun actualizarUI() {
        tvWaterCounter.text = "$waterToday / $waterGoal ml"

        val ratio = if (waterGoal > 0) waterToday.toFloat() / waterGoal.toFloat() else 0f
        waterWaveView.progress = ratio

        val pct = (ratio * 100).toInt()
        tvProgressPercentage.text = "$pct% Completado"

        // KPIs de panel de estadísticas
        tvWaterTodayKPI.text = "$waterToday ml"
        
        // Calcular racha de días cumplidos de esta semana (0 a 7)
        var streakCount = 0
        for (i in 0..6) {
            if (repository.getWaterHistoryDay(i)) {
                streakCount++
            }
        }
        tvWaterStreakKPI.text = "$streakCount / 7 días"

        // Mensaje y consejo inteligente basado en el progreso
        val calendar = Calendar.getInstance()
        val hora = calendar.get(Calendar.HOUR_OF_DAY)
        val tip = when {
            pct >= 100 -> "🏆 ¡Felicidades! Has completado la meta del día. Tu cuerpo está óptimamente hidratado."
            hora >= 18 && pct < 40 -> "⚠️ Ya anochece y llevas solo $pct%. Intenta beber 1 o 2 vasos más antes de dormir."
            hora >= 12 && pct < 25 -> "🔔 Mediodía y llevas solo $pct%. ¡Toma un vaso de agua ahora mismo!"
            pct < 10 -> "💧 ¡Empieza a sumar! Mantén un vaso cerca para recordarte beber constantemente."
            pct in 50..85 -> "👍 ¡Buen progreso! Ya falta poco para alcanzar tu meta de hoy."
            else -> "✅ ¡Buen trabajo! Sigue hidratándote a pequeños sorbos a lo largo del día."
        }
        tvSmartTip.text = tip

        // Actualizar barras de historial semanal
        val bars = arrayOf(barWaterL, barWaterM, barWaterMi, barWaterJ, barWaterV, barWaterS, barWaterD)
        val density = resources.displayMetrics.density
        for (i in 0..6) {
            val cumple = repository.getWaterHistoryDay(i)
            val bar = bars[i]
            bar.setBackgroundColor(android.graphics.Color.parseColor(if (cumple) "#0288D1" else "#B0BEC5"))
            val heightDp = if (cumple) 90L else 30L
            val params = bar.layoutParams
            params.height = (heightDp * density).toInt()
            bar.layoutParams = params
        }
    }
}
