package pa.ac.pa.miprimeraapp.ui.physical

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.ui.custom.CircularProgressView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.Calendar

class ActividadFisicaActivity : AppCompatActivity() {

    // Vistas principales
    private lateinit var circularProgress: CircularProgressView
    private lateinit var tvStreakValue: TextView
    private lateinit var tvStepsLogged: TextView
    private lateinit var btnAddSteps: Button
    private lateinit var btnBack: View

    // Historial Semanal (Barras)
    private lateinit var barL: View
    private lateinit var barM: View
    private lateinit var barMi: View
    private lateinit var barJ: View
    private lateinit var barV: View
    private lateinit var barS: View
    private lateinit var barD: View

    // Cronómetro Overlay
    private lateinit var layoutChronometer: View
    private lateinit var tvChronoTitle: TextView
    private lateinit var tvChronoTime: Chronometer
    private lateinit var btnChronoPlay: View
    private lateinit var ivChronoPlayIcon: ImageView
    private lateinit var btnChronoStop: View

    // Botones Rápidos
    private lateinit var btnRun: View
    private lateinit var btnStrength: View
    private lateinit var btnYoga: View
    private lateinit var btnBike: View

    // Variables de Estado
    private var stepsToday = 0f
    private var caloriesToday = 0f
    private var streakDays = 0
    private var isChronoRunning = false
    private var timeWhenPaused: Long = 0
    private var selectedChronoType = "Correr"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_actividad_fisica)

        // Configuración Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inicializarVistas()
        cargarDatos()
        setupListeners()
        actualizarUI()

        // Set dynamic date in header
        val tvDateTimeInfo = findViewById<TextView>(R.id.tvDateTimeInfo)
        val hoyStr = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("es-ES")).format(Date())
        tvDateTimeInfo.text = "REGISTRO: Hoy - ${hoyStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
    }

    private fun inicializarVistas() {
        circularProgress = findViewById(R.id.circularProgress)
        tvStreakValue = findViewById(R.id.tvStreakValue)
        tvStepsLogged = findViewById(R.id.tvStepsLogged)
        btnAddSteps = findViewById(R.id.btnAddSteps)
        btnBack = findViewById(R.id.btnBack)

        barL = findViewById(R.id.barL)
        barM = findViewById(R.id.barM)
        barMi = findViewById(R.id.barMi)
        barJ = findViewById(R.id.barJ)
        barV = findViewById(R.id.barV)
        barS = findViewById(R.id.barS)
        barD = findViewById(R.id.barD)

        layoutChronometer = findViewById(R.id.layoutChronometer)
        tvChronoTitle = findViewById(R.id.tvChronoTitle)
        tvChronoTime = findViewById(R.id.tvChronoTime)
        btnChronoPlay = findViewById(R.id.btnChronoPlay)
        ivChronoPlayIcon = findViewById(R.id.ivChronoPlayIcon)
        btnChronoStop = findViewById(R.id.btnChronoStop)

        btnRun = findViewById(R.id.btnRun)
        btnStrength = findViewById(R.id.btnStrength)
        btnYoga = findViewById(R.id.btnYoga)
        btnBike = findViewById(R.id.btnBike)
    }

    private fun setupListeners() {
        // Regresar
        btnBack.setOnClickListener { finish() }

        // Agregar Pasos rápidos (+1000)
        btnAddSteps.setOnClickListener {
            stepsToday += 1000f
            if (stepsToday > 25000f) stepsToday = 25000f // Límite razonable
            guardarProgresoDiario()
            actualizarUI()
            Toast.makeText(this, "+1000 Pasos agregados", Toast.LENGTH_SHORT).show()
        }

        // Quick Actions
        btnRun.setOnClickListener { mostrarCronometro("Correr") }
        btnStrength.setOnClickListener { mostrarCronometro("Fuerza") }
        btnYoga.setOnClickListener { mostrarCronometro("Yoga") }
        btnBike.setOnClickListener { mostrarCronometro("Ciclismo") }

        // Cronómetro Controles
        btnChronoPlay.setOnClickListener {
            if (isChronoRunning) {
                // Pausar
                timeWhenPaused = tvChronoTime.base - SystemClock.elapsedRealtime()
                tvChronoTime.stop()
                isChronoRunning = false
                ivChronoPlayIcon.setImageResource(android.R.drawable.ic_media_play)
            } else {
                // Iniciar/Reanudar
                tvChronoTime.base = SystemClock.elapsedRealtime() + timeWhenPaused
                tvChronoTime.start()
                isChronoRunning = true
                ivChronoPlayIcon.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        btnChronoStop.setOnClickListener {
            detenerYGuardarCronometro()
        }
    }

    private fun mostrarCronometro(tipo: String) {
        selectedChronoType = tipo
        tvChronoTitle.text = "Registrando: $tipo"
        layoutChronometer.visibility = View.VISIBLE
        
        // Resetear cronómetro
        tvChronoTime.base = SystemClock.elapsedRealtime()
        timeWhenPaused = 0
        tvChronoTime.start()
        isChronoRunning = true
        ivChronoPlayIcon.setImageResource(android.R.drawable.ic_media_pause)
    }

    private fun detenerYGuardarCronometro() {
        tvChronoTime.stop()
        val elapsedMillis = SystemClock.elapsedRealtime() - tvChronoTime.base
        val elapsedMinutes = (elapsedMillis / 1000 / 60).toInt().coerceAtLeast(1)

        layoutChronometer.visibility = View.GONE
        isChronoRunning = false

        // Calcular calorías quemadas según tipo
        val factorCalorias = when (selectedChronoType) {
            "Correr" -> 8.5f
            "Fuerza" -> 5.5f
            "Yoga" -> 4f
            "Ciclismo" -> 7f
            else -> 5f
        }

        val caloriasQuemadas = elapsedMinutes * factorCalorias
        caloriesToday += caloriasQuemadas
        if (caloriesToday > 5000f) caloriesToday = 5000f

        // Registrar pasos ficticios si es cardio
        if (selectedChronoType == "Correr" || selectedChronoType == "Ciclismo") {
            stepsToday += (elapsedMinutes * 120).toFloat() // Aprox 120 pasos por minuto
            if (stepsToday > 25000f) stepsToday = 25000f
        }

        // Evaluar Racha de ejercicio
        evaluarRacha()

        // Guardar en SharedPreferences
        guardarProgresoDiario()
        actualizarUI()

        Toast.makeText(this, "Sesión de $selectedChronoType guardada: +${caloriasQuemadas.toInt()} kcal ($elapsedMinutes min)", Toast.LENGTH_LONG).show()
    }

    private fun evaluarRacha() {
        val prefs = getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val ultimaFechaStr = prefs.getString("actividad_ultima_fecha", "") ?: ""

        if (ultimaFechaStr != hoyStr) {
            if (ultimaFechaStr.isNotEmpty()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val hoy = sdf.parse(hoyStr)
                val ultima = sdf.parse(ultimaFechaStr)
                if (hoy != null && ultima != null) {
                    val diffTime = hoy.time - ultima.time
                    val diffDays = diffTime / (1000 * 60 * 60 * 24)

                    if (diffDays == 1L) {
                        streakDays += 1
                    } else if (diffDays > 1L) {
                        streakDays = 1 // Se rompió la racha, reinicia
                    }
                }
            } else {
                streakDays = 1 // Primer registro absoluto
            }

            prefs.edit().putString("actividad_ultima_fecha", hoyStr).apply()
        }
    }

    private fun cargarDatos() {
        val prefs = getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)
        
        // Comprobar si cambió de día para reiniciar contador diario
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val diaGuardado = prefs.getString("actividad_dia_actual", "")

        if (diaGuardado != hoyStr) {
            // Guardar meta cumplida de ayer en el historial de la semana antes de reiniciar
            guardarMetaAyer(prefs, diaGuardado)
            
            stepsToday = 0f
            caloriesToday = 0f
            prefs.edit()
                .putFloat("actividad_pasos_hoy", 0f)
                .putFloat("actividad_calorias_hoy", 0f)
                .putString("actividad_dia_actual", hoyStr)
                .apply()
        } else {
            stepsToday = prefs.getFloat("actividad_pasos_hoy", 0f)
            caloriesToday = prefs.getFloat("actividad_calorias_hoy", 0f)
        }

        streakDays = prefs.getInt("actividad_racha", 0)
    }

    private fun guardarProgresoDiario() {
        val prefs = getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putFloat("actividad_pasos_hoy", stepsToday)
        editor.putFloat("actividad_calorias_hoy", caloriesToday)
        editor.putInt("actividad_racha", streakDays)
        
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
        
        val cumpleMeta = (stepsToday >= 10000f || caloriesToday >= 600f)
        editor.putBoolean("actividad_cumple_dia_$dayIndex", cumpleMeta)
        
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
                val pasosAyer = prefs.getFloat("actividad_pasos_hoy", 0f)
                val caloriasAyer = prefs.getFloat("actividad_calorias_hoy", 0f)
                val cumpleAyer = (pasosAyer >= 10000f || caloriasAyer >= 600f)
                prefs.edit().putBoolean("actividad_cumple_dia_$dayIndex", cumpleAyer).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun actualizarUI() {
        circularProgress.stepsCurrent = stepsToday
        circularProgress.caloriesCurrent = caloriesToday
        tvStepsLogged.text = "Total Hoy: ${stepsToday.toInt()}"
        tvStreakValue.text = "Racha: $streakDays Días"

        // Actualizar barras de historial semanal
        val prefs = getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)
        val bars = arrayOf(barL, barM, barMi, barJ, barV, barS, barD)
        
        for (i in 0..6) {
            val cumple = prefs.getBoolean("actividad_cumple_dia_$i", false)
            val bar = bars[i]
            
            if (cumple) {
                bar.setBackgroundColor(android.graphics.Color.parseColor("#2E7D32")) // Verde meta
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
