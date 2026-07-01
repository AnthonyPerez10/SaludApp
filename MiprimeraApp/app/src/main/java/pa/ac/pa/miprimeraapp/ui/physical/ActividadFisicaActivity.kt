package pa.ac.pa.miprimeraapp.ui.physical

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.ui.custom.CircularProgressView
import java.text.SimpleDateFormat
import java.util.*

class ActividadFisicaActivity : AppCompatActivity() {

    // Vistas principales
    private lateinit var circularProgress: CircularProgressView
    private lateinit var tvStreakValue: TextView
    private lateinit var tvStepsLogged: TextView
    private lateinit var btnAddSteps: Button
    
    // Formulario
    private lateinit var spinnerActivityType: Spinner
    private lateinit var spinnerIntensity: Spinner
    private lateinit var etDuration: EditText
    private lateinit var tvRecommendation: TextView
    private lateinit var btnSaveActivity: Button
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
    private var chronoBaseTime: Long = 0
    private var timeWhenPaused: Long = 0
    private var selectedChronoType = "Correr"

    // Opciones para Spinners
    private val activityTypes = arrayOf(
        "Cardio (Correr)", "Cardio (Nadar)", "Cardio (Ciclismo)",
        "Fuerza (Gimnasio)", "Fuerza (Pesas)",
        "Deportes (Fútbol)", "Deportes (Tenis)", "Yoga / Estiramientos"
    )

    private val intensities = arrayOf("Baja", "Media", "Alta")

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
        setupSpinners()
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
        
        spinnerActivityType = findViewById(R.id.spinnerActivityType)
        spinnerIntensity = findViewById(R.id.spinnerIntensity)
        etDuration = findViewById(R.id.etDuration)
        tvRecommendation = findViewById(R.id.tvRecommendation)
        btnSaveActivity = findViewById(R.id.btnSaveActivity)
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

    private fun setupSpinners() {
        val adapterType = ArrayAdapter(this, android.R.layout.simple_spinner_item, activityTypes)
        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerActivityType.adapter = adapterType

        val adapterInt = ArrayAdapter(this, android.R.layout.simple_spinner_item, intensities)
        adapterInt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerIntensity.adapter = adapterInt
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

        // Listener para recomendación dinámica en base a duración e intensidad
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calcularRecomendacionDinamica()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etDuration.addTextChangedListener(textWatcher)

        spinnerIntensity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calcularRecomendacionDinamica()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Guardar actividad manual
        btnSaveActivity.setOnClickListener {
            registrarActividadManual()
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

    private fun calcularRecomendacionDinamica() {
        val duracionStr = etDuration.text.toString().trim()
        if (duracionStr.isEmpty()) {
            tvRecommendation.text = "Ingresa la duración para calcular..."
            return
        }

        val duracion = duracionStr.toIntOrNull() ?: 0
        val intensidad = spinnerIntensity.selectedItem.toString()

        if (duracion <= 0) {
            tvRecommendation.text = "Ingresa una duración válida."
            return
        }

        // Reglas de negocio para sugerir calidad
        val calidad: String
        val consejo: String

        if (duracion < 20) {
            calidad = "Baja (Sesión corta)"
            consejo = "Buen esfuerzo inicial. Intenta llegar a los 30 minutos para activar tu sistema aeróbico."
        } else if (duracion in 20..45) {
            if (intensidad == "Alta") {
                calidad = "Alta (Alta Intensidad)"
                consejo = "Excelente estímulo cardiovascular y metabólico. Asegúrate de estirar después."
            } else {
                calidad = "Media"
                consejo = "Rango ideal para mantenimiento de salud y quema de calorías básica."
            }
        } else {
            // > 45 minutos
            if (intensidad == "Baja") {
                calidad = "Media (Resistencia Aeróbica)"
                consejo = "Buen trabajo de volumen. Ideal para recuperar energía e hidratar articulaciones."
            } else {
                calidad = "Alta (Alto Rendimiento)"
                consejo = "¡Impresionante! Gran volumen e intensidad. Excelente para tu capacidad física global."
            }
        }

        tvRecommendation.text = "Calidad: $calidad\n$consejo"
    }

    private fun registrarActividadManual() {
        val duracionTexto = etDuration.text.toString().trim()
        if (duracionTexto.isEmpty()) {
            etDuration.error = "Ingresa la duración en minutos"
            etDuration.requestFocus()
            return
        }

        val duracion = duracionTexto.toIntOrNull()
        if (duracion == null || duracion <= 0 || duracion > 300) {
            etDuration.error = "Ingresa un tiempo realista (1 a 300 minutos)"
            etDuration.requestFocus()
            return
        }

        val tipo = spinnerActivityType.selectedItem.toString()
        val intensidad = spinnerIntensity.selectedItem.toString()

        // Calcular calorías quemadas
        val factorCalorias = when {
            tipo.contains("Cardio") -> if (intensidad == "Alta") 10f else if (intensidad == "Media") 7.5f else 5f
            tipo.contains("Fuerza") -> if (intensidad == "Alta") 8f else if (intensidad == "Media") 5.5f else 3.5f
            tipo.contains("Deportes") -> if (intensidad == "Alta") 9f else if (intensidad == "Media") 6.5f else 4.5f
            else -> 4f // Yoga/Otros
        }

        val caloriasQuemadas = duracion * factorCalorias
        caloriesToday += caloriasQuemadas
        if (caloriesToday > 5000f) caloriesToday = 5000f

        // Registrar pasos ficticios si es cardio
        if (tipo.contains("Cardio") || tipo.contains("Deportes")) {
            stepsToday += (duracion * 120).toFloat() // Aprox 120 pasos por minuto
            if (stepsToday > 25000f) stepsToday = 25000f
        }

        // Evaluar Racha de ejercicio
        evaluarRacha()

        // Guardar en SharedPreferences
        guardarProgresoDiario()
        actualizarUI()

        // Limpiar
        etDuration.text.clear()
        Toast.makeText(this, "Actividad registrada: +${caloriasQuemadas.toInt()} kcal", Toast.LENGTH_LONG).show()
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

        // Poner en el formulario
        etDuration.setText(elapsedMinutes.toString())
        
        // Pre-seleccionar tipo en el Spinner
        val spinnerPosition = when (selectedChronoType) {
            "Correr" -> 0
            "Fuerza" -> 3
            "Yoga" -> 7
            "Ciclismo" -> 2
            else -> 0
        }
        spinnerActivityType.setSelection(spinnerPosition)
        
        // Pre-seleccionar intensidad Media
        spinnerIntensity.setSelection(1)

        Toast.makeText(this, "Cronómetro detenido. Duración cargada: $elapsedMinutes min", Toast.LENGTH_SHORT).show()
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
        // Day of week: Sunday = 1, Monday = 2... Saturday = 7
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
        
        // Se cumple meta si pasos >= 10,000 o calorías >= 2,000 (o combinaciones moderadas)
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
            
            // Establecer color verde si se cumplió la meta, o gris/azul si no.
            if (cumple) {
                bar.setBackgroundColor(android.graphics.Color.parseColor("#2E7D32")) // Verde meta
            } else {
                bar.setBackgroundColor(android.graphics.Color.parseColor("#B0BEC5")) // Gris claro
            }

            // Cambiar altura programáticamente para reflejar progreso de ese día
            // (Para simplicidad, asignamos alturas preestablecidas si cumple/no cumple, simulando el gráfico)
            val heightDp = if (cumple) 90L else 30L
            val density = resources.displayMetrics.density
            val params = bar.layoutParams
            params.height = (heightDp * density).toInt()
            bar.layoutParams = params
        }
    }
}
