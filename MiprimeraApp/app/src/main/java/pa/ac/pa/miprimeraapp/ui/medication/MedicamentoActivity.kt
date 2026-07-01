package pa.ac.pa.miprimeraapp.ui.medication

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.data.Medication
import pa.ac.pa.miprimeraapp.data.SaludAppRepository
import pa.ac.pa.miprimeraapp.data.SaludAppRepositoryImpl
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad para la gestión de tratamientos médicos, control de tomas diarias y farmacia de inventario.
 * Utiliza SaludAppRepository para centralizar la persistencia temporal.
 */
class MedicamentoActivity : AppCompatActivity() {

    // Repositorio modular de datos
    private lateinit var repository: SaludAppRepository

    // Modelo de interfaz local para agrupar medicamento con horario
    data class MedicationSlot(
        val med: Medication,
        val timeLabel: String,
        val sortHour: Int
    )

    // Formulario de Entrada
    private lateinit var etMedName: EditText
    private lateinit var etMedDoseQty: EditText
    private lateinit var spinnerDoseType: Spinner
    private lateinit var spinnerFrequency: Spinner
    private lateinit var etDurationDays: EditText
    private lateinit var etInventoryInitial: EditText
    private lateinit var btnSaveMed: Button

    // KPIs del panel de estadísticas
    private lateinit var tvMedRegisteredKPI: TextView
    private lateinit var tvMedLowStockKPI: TextView

    // Contenedores dinámicos
    private lateinit var layoutTimeline: LinearLayout
    private lateinit var tvNoMedsMsg: TextView
    private lateinit var cardInventoryAlerts: View
    private lateinit var layoutInventoryAlertList: LinearLayout

    // Calendario Semanal
    private lateinit var dotMon: View
    private lateinit var dotTue: View
    private lateinit var dotWed: View
    private lateinit var dotThu: View
    private lateinit var dotFri: View
    private lateinit var dotSat: View
    private lateinit var dotSun: View

    private lateinit var tvMonDay: TextView
    private lateinit var tvTueDay: TextView
    private lateinit var tvWedDay: TextView
    private lateinit var tvThuDay: TextView
    private lateinit var tvFriDay: TextView
    private lateinit var tvSatDay: TextView
    private lateinit var tvSunDay: TextView

    // Listas locales
    private var medications = mutableListOf<Medication>()
    private var takenSlotsToday = mutableSetOf<String>() // Estructura: "medId_timeLabel"

    // Opciones spinners
    private val doseTypes = arrayOf("Cápsula", "Pastilla redonda", "Jarabe / Gotas")
    private val frequencies = arrayOf(
        "Cada 8 horas (3 veces)", 
        "Una vez al día (Mañana)", 
        "Antes de dormir (Noche)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicamento)

        repository = SaludAppRepositoryImpl(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inicializarVistas()
        setupSpinners()
        cargarDatos()
        setupListeners()
        resaltarDiaActual()
        actualizarUI()

        // Fecha dinámica en el encabezado
        val tvDateTimeInfo = findViewById<TextView>(R.id.tvDateTimeInfo)
        val hoyStr = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("es-ES")).format(Date())
        tvDateTimeInfo.text = "REGISTRO: Hoy - ${hoyStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
    }

    private fun inicializarVistas() {
        etMedName = findViewById(R.id.etMedName)
        etMedDoseQty = findViewById(R.id.etMedDoseQty)
        spinnerDoseType = findViewById(R.id.spinnerDoseType)
        spinnerFrequency = findViewById(R.id.spinnerFrequency)
        etDurationDays = findViewById(R.id.etDurationDays)
        etInventoryInitial = findViewById(R.id.etInventoryInitial)
        btnSaveMed = findViewById(R.id.btnSaveMed)

        // KPIs de estadísticas
        tvMedRegisteredKPI = findViewById(R.id.tvMedRegisteredKPI)
        tvMedLowStockKPI = findViewById(R.id.tvMedLowStockKPI)

        layoutTimeline = findViewById(R.id.layoutTimeline)
        tvNoMedsMsg = findViewById(R.id.tvNoMedsMsg)
        cardInventoryAlerts = findViewById(R.id.cardInventoryAlerts)
        layoutInventoryAlertList = findViewById(R.id.layoutInventoryAlertList)

        dotMon = findViewById(R.id.dotMon)
        dotTue = findViewById(R.id.dotTue)
        dotWed = findViewById(R.id.dotWed)
        dotThu = findViewById(R.id.dotThu)
        dotFri = findViewById(R.id.dotFri)
        dotSat = findViewById(R.id.dotSat)
        dotSun = findViewById(R.id.dotSun)

        tvMonDay = findViewById(R.id.tvMonDay)
        tvTueDay = findViewById(R.id.tvTueDay)
        tvWedDay = findViewById(R.id.tvWedDay)
        tvThuDay = findViewById(R.id.tvThuDay)
        tvFriDay = findViewById(R.id.tvFriDay)
        tvSatDay = findViewById(R.id.tvSatDay)
        tvSunDay = findViewById(R.id.tvSunDay)
    }

    private fun setupSpinners() {
        val doseAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, doseTypes)
        doseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDoseType.adapter = doseAdapter

        val freqAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, frequencies)
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrequency.adapter = freqAdapter
    }

    private fun setupListeners() {
        btnSaveMed.setOnClickListener {
            registrarNuevoMedicamento()
        }
    }

    /**
     * Valida la entrada del usuario y registra un nuevo medicamento, guardándolo
     * a través del repositorio y actualizando los KPIs e interfaces en tiempo real.
     */
    private fun registrarNuevoMedicamento() {
        val nombre = etMedName.text.toString().trim()
        val dosisCantStr = etMedDoseQty.text.toString().trim()
        val duracionStr = etDurationDays.text.toString().trim()
        val inventarioStr = etInventoryInitial.text.toString().trim()

        if (nombre.isEmpty() || dosisCantStr.isEmpty() || duracionStr.isEmpty() || inventarioStr.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos del formulario", Toast.LENGTH_SHORT).show()
            return
        }

        val dosisCant = dosisCantStr.toIntOrNull() ?: 1
        val duracion = duracionStr.toIntOrNull() ?: 0
        val inventario = inventarioStr.toIntOrNull() ?: 0

        val idUnique = UUID.randomUUID().toString()
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val nuevoMed = Medication(
            id = idUnique,
            name = nombre,
            doseQty = dosisCant,
            doseType = spinnerDoseType.selectedItem.toString(),
            frequency = spinnerFrequency.selectedItem.toString(),
            durationDays = duracion,
            initialBoxSize = inventario,
            inventory = inventario,
            dateRegistered = hoyStr
        )

        medications.add(nuevoMed)
        repository.saveMedications(medications)

        // Limpiar formulario
        etMedName.text.clear()
        etMedDoseQty.text.clear()
        etDurationDays.text.clear()
        etInventoryInitial.text.clear()
        spinnerDoseType.setSelection(0)
        spinnerFrequency.setSelection(0)

        actualizarUI()
        Toast.makeText(this, "Medicamento registrado con éxito", Toast.LENGTH_SHORT).show()
    }

    private fun cargarDatos() {
        medications.clear()
        medications.addAll(repository.getMedications())

        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val diaGuardado = repository.getMedicationCurrentDay()

        takenSlotsToday.clear()
        if (diaGuardado != hoyStr) {
            repository.saveMedicationCurrentDay(hoyStr)
            repository.saveTakenSlotsToday(emptySet())
        } else {
            takenSlotsToday.addAll(repository.getTakenSlotsToday())
        }
    }

    private fun resaltarDiaActual() {
        val calendar = Calendar.getInstance()
        val currentDayIndex = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        val dayViews = arrayOf(tvMonDay, tvTueDay, tvWedDay, tvThuDay, tvFriDay, tvSatDay, tvSunDay)
        for (i in dayViews.indices) {
            if (i == currentDayIndex) {
                dayViews[i].setBackgroundResource(R.drawable.circle_highlight)
                dayViews[i].setTextColor(Color.WHITE)
            } else {
                dayViews[i].background = null
                dayViews[i].setTextColor(Color.parseColor("#112E52"))
            }
        }
    }

    private fun actualizarUI() {
        actualizarTimeline()
        actualizarAlertasInventario()
        actualizarPuntosCalendario()
        actualizarKPIs()
    }

    /**
     * Actualiza los KPIs visuales superiores del panel de estadísticas.
     */
    private fun actualizarKPIs() {
        tvMedRegisteredKPI.text = medications.size.toString()
        val lowStockCount = medications.count { it.inventory < 5 }
        tvMedLowStockKPI.text = lowStockCount.toString()

        // Cambiar color de alerta si hay bajo stock
        if (lowStockCount > 0) {
            tvMedLowStockKPI.setTextColor(Color.parseColor("#D32F2F"))
        } else {
            tvMedLowStockKPI.setTextColor(Color.parseColor("#2E7D32"))
        }
    }

    /**
     * Construye y renderiza de forma ordenada los horarios del tratamiento
     * del día actual basándose en la frecuencia configurada.
     */
    private fun actualizarTimeline() {
        layoutTimeline.removeAllViews()

        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val activeSlots = mutableListOf<MedicationSlot>()

        for (med in medications) {
            if (med.durationDays > 0) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                try {
                    val dateReg = sdf.parse(med.dateRegistered)
                    if (dateReg != null) {
                        val cal = Calendar.getInstance()
                        cal.time = dateReg
                        cal.add(Calendar.DAY_OF_YEAR, med.durationDays)
                        val dateLimit = cal.time
                        if (Date().after(dateLimit)) {
                            continue // Tratamiento terminado
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Generar slots de tomas según frecuencia
            when (med.frequency) {
                "Cada 8 horas (3 veces)" -> {
                    activeSlots.add(MedicationSlot(med, "08:00 AM", 8))
                    activeSlots.add(MedicationSlot(med, "04:00 PM", 16))
                    activeSlots.add(MedicationSlot(med, "12:00 AM", 24))
                }
                "Una vez al día (Mañana)" -> {
                    activeSlots.add(MedicationSlot(med, "09:00 AM", 9))
                }
                "Antes de dormir (Noche)" -> {
                    activeSlots.add(MedicationSlot(med, "10:00 PM", 22))
                }
            }
        }

        // Ordenar tomas por hora cronológica
        activeSlots.sortBy { it.sortHour }

        if (activeSlots.isEmpty()) {
            tvNoMedsMsg.visibility = View.VISIBLE
            layoutTimeline.addView(tvNoMedsMsg)
        } else {
            tvNoMedsMsg.visibility = View.GONE
            val inflater = LayoutInflater.from(this)

            for (slot in activeSlots) {
                val slotView = inflater.inflate(R.layout.item_medication_slot, layoutTimeline, false)
                val tvMedNameDose = slotView.findViewById<TextView>(R.id.tvMedNameDose)
                val tvMedTime = slotView.findViewById<TextView>(R.id.tvMedTime)
                val tvMedDuration = slotView.findViewById<TextView>(R.id.tvMedDuration)
                val btnTakeAction = slotView.findViewById<Button>(R.id.btnTakeAction)
                val ivMedIcon = slotView.findViewById<ImageView>(R.id.ivMedIcon)

                tvMedNameDose.text = "${slot.med.name} - ${slot.med.doseQty} dosis"
                tvMedTime.text = slot.timeLabel
                tvMedDuration.text = if (slot.med.durationDays > 0) "Tratamiento: ${slot.med.durationDays} días" else "Tratamiento: Crónico"

                // Cambiar icono según tipo
                when (slot.med.doseType) {
                    "Cápsula" -> ivMedIcon.setImageResource(R.drawable.ic_medicina)
                    "Pastilla redonda" -> ivMedIcon.setImageResource(R.drawable.ic_medicina)
                    else -> ivMedIcon.setImageResource(R.drawable.ic_medicina)
                }

                val slotKey = "${slot.med.id}_${slot.timeLabel}"
                val yaTomado = takenSlotsToday.contains(slotKey)

                if (yaTomado) {
                    btnTakeAction.text = "Tomado"
                    btnTakeAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")))
                    btnTakeAction.isEnabled = false
                } else {
                    btnTakeAction.text = "Tomar"
                    btnTakeAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A4373")))
                    btnTakeAction.isEnabled = true
                    
                    btnTakeAction.setOnClickListener {
                        if (slot.med.inventory >= slot.med.doseQty) {
                            slot.med.inventory -= slot.med.doseQty
                            repository.saveMedications(medications)
                            
                            takenSlotsToday.add(slotKey)
                            repository.saveTakenSlotsToday(takenSlotsToday)
                            
                            evaluarCumplimientoDiario(activeSlots)
                            actualizarUI()
                            Toast.makeText(this@MedicamentoActivity, "Dosis de ${slot.med.name} registrada", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MedicamentoActivity, "Sin inventario de ${slot.med.name}. Reabastece la farmacia.", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                layoutTimeline.addView(slotView)
            }
        }
    }

    /**
     * Evalúa si todas las tomas configuradas para hoy fueron realizadas.
     */
    private fun evaluarCumplimientoDiario(slotsHoy: List<MedicationSlot>) {
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

        val slotKeysHoy = slotsHoy.map { "${it.med.id}_${it.timeLabel}" }
        val compliance = slotKeysHoy.isNotEmpty() && takenSlotsToday.containsAll(slotKeysHoy)
        repository.saveMedicationHistoryDay(dayIndex, compliance)
    }

    private fun actualizarAlertasInventario() {
        layoutInventoryAlertList.removeAllViews()
        var hasAlerts = false
        val inflater = LayoutInflater.from(this)

        for (med in medications) {
            if (med.inventory <= 5) {
                hasAlerts = true
                val alertView = inflater.inflate(R.layout.item_inventory_alert, layoutInventoryAlertList, false)
                
                val tvAlertMedName = alertView.findViewById<TextView>(R.id.tvAlertMedName)
                val tvAlertStockText = alertView.findViewById<TextView>(R.id.tvAlertStockText)
                val pbInventoryProgress = alertView.findViewById<ProgressBar>(R.id.pbInventoryProgress)

                tvAlertMedName.text = med.name
                tvAlertStockText.text = "Quedan ${med.inventory} de ${med.initialBoxSize} dosis"
                
                pbInventoryProgress.max = med.initialBoxSize
                pbInventoryProgress.progress = med.inventory

                layoutInventoryAlertList.addView(alertView)
            }
        }

        cardInventoryAlerts.visibility = if (hasAlerts) View.VISIBLE else View.GONE
    }

    private fun actualizarPuntosCalendario() {
        val dots = arrayOf(dotMon, dotTue, dotWed, dotThu, dotFri, dotSat, dotSun)
        for (i in 0..6) {
            val cumple = repository.getMedicationHistoryDay(i)
            dots[i].setBackgroundColor(Color.parseColor(if (cumple) "#2E7D32" else "#B0BEC5"))
        }
    }
}
