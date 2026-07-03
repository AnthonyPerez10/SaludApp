package pa.ac.pa.miprimeraapp.ui.physical

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.data.SaludAppRepository
import pa.ac.pa.miprimeraapp.data.SaludAppRepositoryImpl
import pa.ac.pa.miprimeraapp.services.PedometerService
import pa.ac.pa.miprimeraapp.ui.custom.CircularProgressView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad que gestiona los pasos diarios, calorías quemadas y racha de ejercicio.
 * Utiliza un servicio en segundo plano con acelerómetro para funcionar como podómetro automático.
 */
class ActividadFisicaActivity : AppCompatActivity() {

    // Capa de datos modular
    private lateinit var repository: SaludAppRepository

    // Vistas principales
    private lateinit var circularProgress: CircularProgressView
    private lateinit var tvStreakValue: TextView
    private lateinit var tvStepsLogged: TextView
    private lateinit var btnAddSteps: Button
    private lateinit var switchNotifications: androidx.appcompat.widget.SwitchCompat

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

    // Variables de Estado Local
    private var stepsToday = 0f
    private var caloriesToday = 0f
    private var streakDays = 0

    private val REQUEST_PERMISSIONS_CODE = 1002

    // Receptor para obtener pasos del servicio en tiempo real
    private val stepUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "pa.ac.pa.miprimeraapp.STEP_UPDATE") {
                stepsToday = intent.getFloatExtra("steps", 0f)
                // Estimar calorías quemadas basadas en los pasos reales (0.04 kcal por paso)
                caloriesToday = stepsToday * 0.04f
                repository.savePhysicalCaloriesToday(caloriesToday)
                
                actualizarUI()
            }
        }
    }

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

        // Solicitar permisos e iniciar el podómetro en segundo plano
        checkAndRequestPermissions()

        // Fecha dinámica en el encabezado
        val tvDateTimeInfo = findViewById<TextView>(R.id.tvDateTimeInfo)
        val hoyStr = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("es-ES")).format(Date())
        tvDateTimeInfo.text = "REGISTRO: Hoy - ${hoyStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
    }

    override fun onStart() {
        super.onStart()
        // Registrar receptor de broadcast para recibir pasos del PedometerService
        val filter = IntentFilter("pa.ac.pa.miprimeraapp.STEP_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stepUpdateReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(stepUpdateReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        // Desregistrar receptor
        unregisterReceiver(stepUpdateReceiver)
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
        switchNotifications = findViewById(R.id.switchNotifications)
    }

    private fun setupListeners() {
        // Agregar Pasos rápidos (+1000) enviando comando al servicio en segundo plano
        btnAddSteps.setOnClickListener {
            val intent = Intent(this, PedometerService::class.java).apply {
                action = "pa.ac.pa.miprimeraapp.ADD_STEPS"
                putExtra("amount", 1000)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "+1000 Pasos manuales solicitados", Toast.LENGTH_SHORT).show()
        }

        val sharedPrefs = getSharedPreferences("SaludAppPrefs", Context.MODE_PRIVATE)
        switchNotifications.isChecked = sharedPrefs.getBoolean("pref_notifications_activity", true)
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("pref_notifications_activity", isChecked).apply()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_PERMISSIONS_CODE)
        } else {
            iniciarServicioPodometro()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            // Iniciamos el servicio independientemente de los permisos concedidos
            iniciarServicioPodometro()
        }
    }

    private fun iniciarServicioPodometro() {
        val intent = Intent(this, PedometerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
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
