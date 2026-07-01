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
import pa.ac.pa.miprimeraapp.data.SaludAppRepository
import pa.ac.pa.miprimeraapp.data.SaludAppRepositoryImpl
import pa.ac.pa.miprimeraapp.ui.custom.CircularProgressView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad que gestiona los pasos diarios, calorías quemadas y racha de ejercicio.
 * Utiliza un cronómetro para registrar actividades físicas y las persiste mediante el repositorio.
 */
class ActividadFisicaActivity : AppCompatActivity() {

    // Capa de datos modular
    private lateinit var repository: SaludAppRepository

    // Vistas principales
    private lateinit var circularProgress: CircularProgressView
    private lateinit var tvStreakValue: TextView
    private lateinit var tvStepsLogged: TextView
    private lateinit var btnAddSteps: Button

    // KPIs del panel de estadísticas
    private lateinit var tvPhysStreakKPI: TextView
    private lateinit var tvPhysCaloriesKPI: TextView

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

    // Botones Rápidos de Actividades
    private lateinit var btnRun: View
    private lateinit var btnStrength: View
    private lateinit var btnYoga: View
    private lateinit var btnBike: View

    // Variables de Estado Local
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

        repository = SaludAppRepositoryImpl(this)

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

        // Fecha dinámica en el encabezado
        val tvDateTimeInfo = findViewById<TextView>(R.id.tvDateTimeInfo)
        val hoyStr = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("es-ES")).format(Date())
        tvDateTimeInfo.text = "REGISTRO: Hoy - ${hoyStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
    }

    private fun inicializarVistas() {
        circularProgress = findViewById(R.id.circularProgress)
        tvStreakValue = findViewById(R.id.tvStreakValue)
        tvStepsLogged = findViewById(R.id.tvStepsLogged)
        btnAddSteps = findViewById(R.id.btnAddSteps)

        // KPIs del panel de estadísticas
        tvPhysStreakKPI = findViewById(R.id.tvPhysStreakKPI)
        tvPhysCaloriesKPI = findViewById(R.id.tvPhysCaloriesKPI)

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
        // Agregar Pasos rápidos (+1000)
        btnAddSteps.setOnClickListener {
            stepsToday += 1000f
            if (stepsToday > 25000f) stepsToday = 25000f
            
            guardarProgresoDiario()
            actualizarUI()
            Toast.makeText(this, "+1000 Pasos agregados", Toast.LENGTH_SHORT).show()
        }

        // Configuración de botones de tipos de ejercicio
        btnRun.setOnClickListener { mostrarCronometro("Correr") }
        btnStrength.setOnClickListener { mostrarCronometro("Fuerza") }
        btnYoga.setOnClickListener { mostrarCronometro("Yoga") }
        btnBike.setOnClickListener { mostrarCronometro("Ciclismo") }

        // Controladores de estado del cronómetro
        btnChronoPlay.setOnClickListener {
            if (isChronoRunning) {
                // Pausar cronómetro
                timeWhenPaused = tvChronoTime.base - SystemClock.elapsedRealtime()
                tvChronoTime.stop()
                isChronoRunning = false
                ivChronoPlayIcon.setImageResource(android.R.drawable.ic_media_play)
            } else {
                // Reanudar cronómetro
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
        
        tvChronoTime.base = SystemClock.elapsedRealtime()
        timeWhenPaused = 0
        tvChronoTime.start()
        isChronoRunning = true
        ivChronoPlayIcon.setImageResource(android.R.drawable.ic_media_pause)
    }

    /**
     * Finaliza la sesión de cronómetro, calcula la quema de calorías según el tipo
     * de ejercicio y su duración, y simula pasos si corresponde.
     */
    private fun detenerYGuardarCronometro() {
        tvChronoTime.stop()
        val elapsedMillis = SystemClock.elapsedRealtime() - tvChronoTime.base
        val elapsedMinutes = (elapsedMillis / 1000 / 60).toInt().coerceAtLeast(1)

        layoutChronometer.visibility = View.GONE
        isChronoRunning = false

        // Factores MET simplificados por tipo de ejercicio
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

        // Aporte de pasos estimado para ejercicios cardiovasculares
        if (selectedChronoType == "Correr" || selectedChronoType == "Ciclismo") {
            stepsToday += (elapsedMinutes * 120).toFloat()
            if (stepsToday > 25000f) stepsToday = 25000f
        }

        evaluarRacha()
        guardarProgresoDiario()
        actualizarUI()

        Toast.makeText(this, "Sesión de $selectedChronoType guardada: +${caloriasQuemadas.toInt()} kcal", Toast.LENGTH_LONG).show()
    }

    /**
     * Evalúa si el usuario mantiene su racha diaria de actividad física.
     */
    private fun evaluarRacha() {
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val ultimaFechaStr = repository.getPhysicalLastDate()

        if (ultimaFechaStr != hoyStr) {
            if (ultimaFechaStr.isNotEmpty()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                try {
                    val hoy = sdf.parse(hoyStr)
                    val ultima = sdf.parse(ultimaFechaStr)
                    if (hoy != null && ultima != null) {
                        val diffTime = hoy.time - ultima.time
                        val diffDays = diffTime / (1000 * 60 * 60 * 24)

                        if (diffDays == 1L) {
                            streakDays += 1
                        } else if (diffDays > 1L) {
                            streakDays = 1 // Racha rota
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                streakDays = 1
            }
            repository.savePhysicalLastDate(hoyStr)
        }
    }

    private fun cargarDatos() {
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val diaGuardado = repository.getPhysicalCurrentDay()

        if (diaGuardado != hoyStr) {
            guardarMetaAyer(diaGuardado)
            stepsToday = 0f
            caloriesToday = 0f
            repository.savePhysicalStepsToday(0f)
            repository.savePhysicalCaloriesToday(0f)
            repository.savePhysicalCurrentDay(hoyStr)
        } else {
            stepsToday = repository.getPhysicalStepsToday()
            caloriesToday = repository.getPhysicalCaloriesToday()
        }

        streakDays = repository.getPhysicalStreak()
    }

    private fun guardarProgresoDiario() {
        repository.savePhysicalStepsToday(stepsToday)
        repository.savePhysicalCaloriesToday(caloriesToday)
        repository.savePhysicalStreak(streakDays)
        
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
        
        // La meta es cumplir 10,000 pasos o quemar 600 kcal
        val cumpleMeta = (stepsToday >= 10000f || caloriesToday >= 600f)
        repository.savePhysicalHistoryDay(dayIndex, cumpleMeta)
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
                repository.savePhysicalHistoryDay(dayIndex, stepsToday >= 10000f || caloriesToday >= 600f)
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

        // Actualizar KPIs de estadísticas
        tvPhysCaloriesKPI.text = "${caloriesToday.toInt()} kcal"
        
        var activeDays = 0
        for (i in 0..6) {
            if (repository.getPhysicalHistoryDay(i)) activeDays++
        }
        tvPhysStreakKPI.text = "$activeDays / 7 días"

        // Actualizar gráfico de barras semanal
        val bars = arrayOf(barL, barM, barMi, barJ, barV, barS, barD)
        val density = resources.displayMetrics.density
        for (i in 0..6) {
            val cumple = repository.getPhysicalHistoryDay(i)
            val bar = bars[i]
            bar.setBackgroundColor(android.graphics.Color.parseColor(if (cumple) "#2E7D32" else "#B0BEC5"))
            val heightDp = if (cumple) 90L else 30L
            val params = bar.layoutParams
            params.height = (heightDp * density).toInt()
            bar.layoutParams = params
        }
    }
}
