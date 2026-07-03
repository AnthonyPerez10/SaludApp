package pa.ac.pa.miprimeraapp.ui.menu

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
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
 * Actúa como dashboard de salud y ahora incluye un panel de navegación lateral (Hamburger Menu)
 * para administrar el perfil de usuario, cambiar contraseñas y destruir datos de salud.
 */
class MenuActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private lateinit var cardPeso: CardView
    private lateinit var cardArterial: CardView
    private lateinit var cardGlucosa: CardView
    private lateinit var cardFisico: CardView
    private lateinit var cardHidratacion: CardView
    private lateinit var cardMedicamentos: CardView
    private lateinit var gridLayout: androidx.gridlayout.widget.GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // Forzar tema claro para consistencia visual

        supportActionBar?.hide()

        prefsManager = SharedPreferencesManager(this)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        navView.setNavigationItemSelectedListener(this)

        // Configurar Botón de Hamburguesa para deslizar el panel lateral
        val btnHamburger = findViewById<ImageButton>(R.id.btnHamburger)
        btnHamburger.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Configurar Saludo Personalizado en la pantalla principal
        val tvUserGreeting = findViewById<TextView>(R.id.tvUserGreeting)
        if (prefsManager.isRegistered()) {
            val nombre = prefsManager.getNombre()
            tvUserGreeting.text = "¡Hola, $nombre!"
        } else {
            tvUserGreeting.text = "¡Hola, Usuario!"
        }

        // Rellenar dinámicamente los datos de perfil en la cabecera del panel lateral
        configurarHeaderDrawer()

        gridLayout = findViewById(R.id.MenuNavegacion)

        // Obtener referencias de tarjetas de navegación principal
        cardPeso = findViewById<CardView>(R.id.CardV_Peso)
        cardArterial = findViewById<CardView>(R.id.CarV_Presion_Arterial)
        cardGlucosa = findViewById<CardView>(R.id.CardV_Glucosa)
        cardFisico = findViewById<CardView>(R.id.CardV_Control_Fisico)
        cardHidratacion = findViewById<CardView>(R.id.CardV_Hidratacion)
        cardMedicamentos = findViewById<CardView>(R.id.CardV_medicamentos)

        // Asignar listeners de navegación
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

    private fun configurarHeaderDrawer() {
        val headerView = navView.getHeaderView(0)
        val tvNavHeaderName = headerView.findViewById<TextView>(R.id.tvNavHeaderName)
        val tvNavHeaderEmail = headerView.findViewById<TextView>(R.id.tvNavHeaderEmail)
        val ivNavHeaderProfilePhoto = headerView.findViewById<ImageView>(R.id.ivNavHeaderProfilePhoto)

        if (prefsManager.isRegistered()) {
            tvNavHeaderName.text = "¡Hola, ${prefsManager.getNombre()}!"
            tvNavHeaderEmail.text = prefsManager.getCorreo().ifEmpty { "correo@ejemplo.com" }
        } else {
            tvNavHeaderName.text = "¡Hola, Usuario!"
            tvNavHeaderEmail.text = "correo@ejemplo.com"
        }

        // Cargar foto de perfil en el drawer
        val path = prefsManager.getProfileImagePath()
        if (path != null) {
            val file = java.io.File(path)
            if (file.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    ivNavHeaderProfilePhoto.setImageBitmap(bitmap)
                } else {
                    ivNavHeaderProfilePhoto.setImageResource(R.drawable.icono_perfil)
                }
            } else {
                ivNavHeaderProfilePhoto.setImageResource(R.drawable.icono_perfil)
            }
        } else {
            ivNavHeaderProfilePhoto.setImageResource(R.drawable.icono_perfil)
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarDashboard()
        configurarHeaderDrawer()
        actualizarVisibilidadModulos()
    }

    private fun actualizarVisibilidadModulos() {
        gridLayout.removeAllViews()

        val activeCards = mutableListOf<CardView>()

        if (prefsManager.isModuleEnabled("peso")) {
            activeCards.add(cardPeso)
            cardPeso.visibility = View.VISIBLE
        } else {
            cardPeso.visibility = View.GONE
        }

        if (prefsManager.isModuleEnabled("presion")) {
            activeCards.add(cardArterial)
            cardArterial.visibility = View.VISIBLE
        } else {
            cardArterial.visibility = View.GONE
        }

        if (prefsManager.isModuleEnabled("glucosa")) {
            activeCards.add(cardGlucosa)
            cardGlucosa.visibility = View.VISIBLE
        } else {
            cardGlucosa.visibility = View.GONE
        }

        if (prefsManager.isModuleEnabled("actividad")) {
            activeCards.add(cardFisico)
            cardFisico.visibility = View.VISIBLE
        } else {
            cardFisico.visibility = View.GONE
        }

        if (prefsManager.isModuleEnabled("hidratacion")) {
            activeCards.add(cardHidratacion)
            cardHidratacion.visibility = View.VISIBLE
        } else {
            cardHidratacion.visibility = View.GONE
        }

        if (prefsManager.isModuleEnabled("medicina")) {
            activeCards.add(cardMedicamentos)
            cardMedicamentos.visibility = View.VISIBLE
        } else {
            cardMedicamentos.visibility = View.GONE
        }

        val visibleCount = activeCards.size
        gridLayout.columnCount = if (visibleCount <= 1) 1 else 2
        gridLayout.rowCount = if (visibleCount <= 2) 1 else if (visibleCount <= 4) 2 else 3

        for (card in activeCards) {
            val params = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(androidx.gridlayout.widget.GridLayout.UNDEFINED, 1f)
                rowSpec = androidx.gridlayout.widget.GridLayout.spec(androidx.gridlayout.widget.GridLayout.UNDEFINED, 1f)
                val margin = (8 * resources.displayMetrics.density).toInt()
                setMargins(margin, margin, margin, margin)
            }
            card.layoutParams = params
            gridLayout.addView(card)
        }
    }

    /**
     * Procesa los clicks de los elementos del panel de navegación lateral.
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                val intent = Intent(this, PerfilActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_change_password -> {
                mostrarDialogoCambiarContrasena()
            }
            R.id.nav_delete_data -> {
                confirmarDestruirDatos()
            }
            R.id.nav_logout -> {
                ejecutarLogout()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Muestra una ventana de diálogo personalizada con los datos de registro del usuario.
     */
    private fun mostrarDialogoPerfil() {
        val nombre = prefsManager.getNombre()
        val apellido = prefsManager.getApellido()
        val edad = prefsManager.getEdad()
        val correo = prefsManager.getCorreo()

        val info = "Nombre Completo:\n$nombre $apellido\n\nEdad:\n$edad años\n\nCorreo Electrónico:\n${correo.ifEmpty { "No registrado" }}"

        AlertDialog.Builder(this)
            .setTitle("Mi Perfil de Usuario")
            .setMessage(info)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    /**
     * Despliega un formulario emergente interactivo para cambiar la clave de acceso.
     */
    private fun mostrarDialogoCambiarContrasena() {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        val etNew = EditText(context).apply {
            hint = "Nueva Contraseña (mín. 4 caracteres)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etConfirm = EditText(context).apply {
            hint = "Confirmar Nueva Contraseña"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
        }

        layout.addView(etNew)
        layout.addView(etConfirm)

        AlertDialog.Builder(context)
            .setTitle("Cambiar Contraseña")
            .setView(layout)
            .setPositiveButton("Guardar") { dialog, _ ->
                val newPass = etNew.text.toString().trim()
                val confirm = etConfirm.text.toString().trim()

                if (newPass.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPass.length > 32 || confirm.length > 32) {
                    Toast.makeText(context, "Las contraseñas no pueden exceder los 32 caracteres", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (newPass.length < 4) {
                    Toast.makeText(context, "La nueva contraseña debe tener al menos 4 caracteres", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (newPass != confirm) {
                    Toast.makeText(context, "Las contraseñas nuevas no coinciden", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                prefsManager.updatePassword(newPass)
                Toast.makeText(context, "¡Contraseña actualizada con éxito!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Solicita una confirmación irreversible antes de borrar todos los datos.
     */
    private fun confirmarDestruirDatos() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Destruir Datos de la Cuenta")
            .setMessage("¿Estás completamente seguro? Esta acción borrará irreversiblemente tu perfil, contraseña, e historial de peso, agua, medicamentos y registros médicos. La aplicación se reiniciará.")
            .setPositiveButton("Destruir permanentemente") { _, _ ->
                prefsManager.destroyAllData()
                Toast.makeText(this, "Todos los datos de la cuenta fueron destruidos", Toast.LENGTH_LONG).show()
                val intent = Intent(this, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun ejecutarLogout() {
        prefsManager.logout()
        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
                "Cada 4 horas" -> 6
                "Cada 6 horas" -> 4
                "Cada 8 horas", "Cada 8 horas (3 veces)" -> 3
                "Cada 12 horas" -> 2
                "Una vez al día (Mañana)" -> 1
                "Antes de dormir (Noche)" -> 1
                "Un día sí, un día no" -> {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    try {
                        val dateReg = sdf.parse(med.dateRegistered)
                        if (dateReg != null) {
                            val diffTime = Date().time - dateReg.time
                            val diffDays = (diffTime / (1000 * 60 * 60 * 24)).toInt()
                            if (diffDays % 2 == 0) 1 else 0
                        } else 0
                    } catch (e: Exception) {
                        0
                    }
                }
                else -> 0
            }
        }

        val taken = prefsManager.getTakenSlotsToday().size
        tvMenuMedsKPI.text = "$taken / $totalSlots"
    }
}
