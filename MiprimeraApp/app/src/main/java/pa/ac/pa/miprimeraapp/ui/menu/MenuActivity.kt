package pa.ac.pa.miprimeraapp.ui.menu

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.sharedpreferences.SharedPreferencesManager
import pa.ac.pa.miprimeraapp.ui.weight.ControlPesoActivity
import pa.ac.pa.miprimeraapp.ui.pressure.PresionArterialActivity
import pa.ac.pa.miprimeraapp.ui.glucose.ControlGlucosaActivity
import pa.ac.pa.miprimeraapp.ui.physical.ActividadFisicaActivity
import pa.ac.pa.miprimeraapp.ui.hydration.HidratacionActivity
import pa.ac.pa.miprimeraapp.ui.medication.MedicamentoActivity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad del Menú Principal.
 * Actúa como dashboard de salud que consolida y muestra indicadores rápidos de cada módulo en tiempo real.
 */
class MenuActivity : AppCompatActivity() {

    private lateinit var prefsManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // Forzar tema claro para consistencia visual

        supportActionBar?.hide()

        prefsManager = SharedPreferencesManager(this)

        // Configurar Saludo Personalizado
        val tvUserGreeting = findViewById<TextView>(R.id.tvUserGreeting)
        if (prefsManager.isRegistered()) {
            val nombre = prefsManager.getNombre()
            tvUserGreeting.text = "¡Hola, $nombre!"
        } else {
            tvUserGreeting.text = "¡Hola, Usuario!"
        }

        // Configurar Botón de Cierre de Sesión (Logout)
        val btnLogout = findViewById<ImageButton>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            prefsManager.logout()
            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Obtener referencias de tarjetas de navegación
        val cardPeso = findViewById<CardView>(R.id.CardV_Peso)
        val cardArterial = findViewById<CardView>(R.id.CarV_Presion_Arterial)
        val cardGlucosa = findViewById<CardView>(R.id.CardV_Glucosa)
        val cardFisico = findViewById<CardView>(R.id.CardV_Control_Fisico)
        val cardHidratacion = findViewById<CardView>(R.id.CardV_Hidratacion)
        val cardMedicamentos = findViewById<CardView>(R.id.CardV_medicamentos)

        // Configurar navegación entre pantallas
        cardPeso.setOnClickListener {
            startActivity(Intent(this, ControlPesoActivity::class.java))
        }

        cardArterial.setOnClickListener {
            startActivity(Intent(this, PresionArterialActivity::class.java))
        }

        cardGlucosa.setOnClickListener {
            startActivity(Intent(this, ControlGlucosaActivity::class.java))
        }

        cardFisico.setOnClickListener {
            startActivity(Intent(this, ActividadFisicaActivity::class.java))
        }

        cardHidratacion.setOnClickListener {
            startActivity(Intent(this, HidratacionActivity::class.java))
        }

        cardMedicamentos.setOnClickListener {
            startActivity(Intent(this, MedicamentoActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Refrescar indicadores del dashboard cada vez que se vuelve a esta pantalla
        actualizarDashboard()
    }

    /**
     * Consulta el repositorio central para actualizar las estadísticas rápidas
     * (Pasos, Agua y Medicación) en el panel del menú.
     */
    private fun actualizarDashboard() {
        val tvMenuStepsKPI = findViewById<TextView>(R.id.tvMenuStepsKPI)
        val tvMenuWaterKPI = findViewById<TextView>(R.id.tvMenuWaterKPI)
        val tvMenuMedsKPI = findViewById<TextView>(R.id.tvMenuMedsKPI)

        // 1. KPI de Pasos
        val steps = prefsManager.getPhysicalStepsToday().toInt()
        tvMenuStepsKPI.text = "$steps"

        // 2. KPI de Agua
        val water = prefsManager.getWaterToday()
        tvMenuWaterKPI.text = "$water ml"

        // 3. KPI de Medicamentos tomados vs programados para hoy
        val meds = prefsManager.getMedications()
        var totalSlots = 0
        for (med in meds) {
            // Filtrar tratamientos que ya expiraron
            if (med.durationDays > 0) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                try {
                    val dateReg = sdf.parse(med.dateRegistered)
                    if (dateReg != null) {
                        val cal = Calendar.getInstance()
                        cal.time = dateReg
                        cal.add(Calendar.DAY_OF_YEAR, med.durationDays)
                        if (Date().after(cal.time)) {
                            continue // Ignorar medicamento expirado
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Aportación de tomas según frecuencia
            totalSlots += when (med.frequency) {
                "Cada 8 horas (3 veces)" -> 3
                "Una vez al día (Mañana)" -> 1
                "Antes de dormir (Noche)" -> 1
                else -> 0
            }
        }

        val taken = prefsManager.getTakenSlotsToday().size
        tvMenuMedsKPI.text = "$taken / $totalSlots"
    }
}
