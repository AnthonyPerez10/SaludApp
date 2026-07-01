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
import pa.ac.pa.miprimeraapp.ui.custom.WaterWaveView
import java.text.SimpleDateFormat
import java.util.*

class HidratacionActivity : AppCompatActivity() {

    // Vistas principales
    private lateinit var tvWaterCounter: TextView
    private lateinit var waterWaveView: WaterWaveView
    private lateinit var tvProgressPercentage: TextView
    private lateinit var tvSmartTip: TextView
    private lateinit var btnBack: View

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

    // Estadísticas de Hidratación Semanal
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

    // Estado
    private var waterToday = 0
    private var waterGoal = 2000 // default 2 Litros

    private val activityLevels = arrayOf("Sedentario", "Moderado", "Muy Activo")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_hidratacion)

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

        // Set dynamic date in header
        val tvDateTimeInfo = findViewById<TextView>(R.id.tvDateTimeInfo)
        val hoyStr = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("es-ES")).format(Date())
        tvDateTimeInfo.text = "REGISTRO: Hoy - ${hoyStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
    }

    private fun inicializarVistas() {
        tvWaterCounter = findViewById(R.id.tvWaterCounter)
        waterWaveView = findViewById(R.id.waterWaveView)
        tvProgressPercentage = findViewById(R.id.tvProgressPercentage)
        tvSmartTip = findViewById(R.id.tvSmartTip)
        btnBack = findViewById(R.id.btnBack)

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
        btnBack.setOnClickListener { finish() }

        // Botones rápidos
        btnLog250.setOnClickListener { registrarAgua(250) }
        btnLog500.setOnClickListener { registrarAgua(500) }
        btnLog750.setOnClickListener { registrarAgua(750) }

        // Botón Personalizado
        btnLogCustom.setOnClickListener {
            if (layoutCustomLog.visibility == View.VISIBLE) {
                layoutCustomLog.visibility = View.GONE
            } else {
                layoutCustomLog.visibility = View.VISIBLE
            }
        }

        // SeekBar
        sbCustomAmount.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val roundedProgress = (progress / 10) * 10 // Redondear a múltiplos de 10
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

    private fun registrarAgua(cantidad: Int) {
        waterToday += cantidad
        if (waterToday > 10000) waterToday = 10000 // Limite de seguridad

        guardarDatos()
        actualizarUI()
        
        Toast.makeText(this, "+$cantidad ml de agua registrados", Toast.LENGTH_SHORT).show()
    }

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

        // Fórmula inteligente:
        // Peso * 35 ml (base recomendada por la OMS)
        var metaRecomendada = (peso * 35).toInt()

        // Nivel de actividad física
        metaRecomendada += when (actividad) {
            "Moderado" -> 350
            "Muy Activo" -> 700
            else -> 0
        }

        // Género
        metaRecomendada += if (esMasculino) 250 else 0

        // Límite de seguridad
        if (metaRecomendada < 1000) metaRecomendada = 1000
        if (metaRecomendada > 6000) metaRecomendada = 6000

        waterGoal = metaRecomendada
        guardarDatos()
        actualizarUI()

        // Guardar valores del formulario
        val prefs = getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("hid_user_weight", peso)
            putBoolean("hid_user_is_male", esMasculino)
            putInt("hid_user_activity_pos", spinnerUserActivity.selectedItemPosition)
            apply()
        }

        Toast.makeText(this, "Nueva Meta Calculada: $waterGoal ml", Toast.LENGTH_LONG).show()
    }

    private fun cargarDatos() {
        val prefs = getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)

        // Verificar cambio de día
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val diaGuardado = prefs.getString("hid_dia_actual", "")

        if (diaGuardado != hoyStr) {
            // Guardar meta cumplida de ayer en el historial de la semana antes de reiniciar
            guardarMetaAyer(prefs, diaGuardado)
            
            waterToday = 0
            prefs.edit()
                .putInt("hid_agua_hoy", 0)
                .putString("hid_dia_actual", hoyStr)
                .apply()
        } else {
            waterToday = prefs.getInt("hid_agua_hoy", 0)
        }

        waterGoal = prefs.getInt("hid_meta_agua", 2000)

        // Rellenar formulario si ya existían datos
        if (prefs.contains("hid_user_weight")) {
            val peso = prefs.getFloat("hid_user_weight", 70f)
            etUserWeight.setText(peso.toString())
            
            val esMasculino = prefs.getBoolean("hid_user_is_male", true)
            if (esMasculino) {
                rbMale.isChecked = true
            } else {
                rbFemale.isChecked = true
            }

            val actPos = prefs.getInt("hid_user_activity_pos", 0)
            spinnerUserActivity.setSelection(actPos)
        }
    }

    private fun guardarDatos() {
        val prefs = getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt("hid_agua_hoy", waterToday)
        editor.putInt("hid_meta_agua", waterGoal)

        // Registrar cumplimiento de hoy
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
        editor.putBoolean("hid_cumple_dia_$dayIndex", waterToday >= waterGoal)
        editor.apply()
    }

    private fun guardarMetaAyer(prefs: android.content.SharedPreferences, diaAyer: String?) {
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
                val aguaAyer = prefs.getInt("hid_agua_hoy", 0)
                val metaAyer = prefs.getInt("hid_meta_agua", 2000)
                prefs.edit().putBoolean("hid_cumple_dia_$dayIndex", aguaAyer >= metaAyer).apply()
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

        // Recordatorios Inteligentes Basados en Progreso y Hora
        val calendar = Calendar.getInstance()
        val hora = calendar.get(Calendar.HOUR_OF_DAY)

        val tip: String
        when {
            pct >= 100 -> {
                tip = "🏆 ¡Felicidades! Has completado el 100% de tu meta diaria. Tu cuerpo te lo agradece."
            }
            hora >= 18 && pct < 40 -> {
                tip = "⚠️ Ya está anocheciendo y solo llevas el $pct% de tu meta. Intenta tomar 1 o 2 vasos más antes de dormir."
            }
            hora >= 12 && pct < 25 -> {
                tip = "🔔 Ya es mediodía y solo llevas el $pct% de tu meta. ¡Tómate un vaso de agua ahora mismo!"
            }
            pct < 10 -> {
                tip = "💧 ¡Empieza a sumar! Mantener un vaso de agua en tu escritorio te recordará beber constantemente."
            }
            pct in 50..85 -> {
                tip = "👍 ¡Buen progreso! Estás cerca de la meta. Un vaso más después de tu almuerzo o ejercicio ayudará."
            }
            else -> {
                tip = "✅ ¡Buen trabajo! Continúa bebiendo agua en pequeños sorbos a lo largo de la tarde."
            }
        }

        tvSmartTip.text = tip

        // Actualizar barras de historial semanal de hidratación
        val prefs = getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)
        val bars = arrayOf(barWaterL, barWaterM, barWaterMi, barWaterJ, barWaterV, barWaterS, barWaterD)
        
        for (i in 0..6) {
            val cumple = prefs.getBoolean("hid_cumple_dia_$i", false)
            val bar = bars[i]
            
            if (cumple) {
                bar.setBackgroundColor(android.graphics.Color.parseColor("#0288D1")) // Azul agua meta
            } else {
                bar.setBackgroundColor(android.graphics.Color.parseColor("#B0BEC5")) // Gris claro
            }

            val heightDp = if (cumple) 90L else 30L
            val density = resources.displayMetrics.density
            val params = bar.layoutParams
            params.height = (heightDp * density).toInt()
            bar.layoutParams = params
        }
    }
}
