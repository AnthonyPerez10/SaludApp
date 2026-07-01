package pa.ac.pa.miprimeraapp.ui.medication

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONObject
import pa.ac.pa.miprimeraapp.R
import java.text.SimpleDateFormat
import java.util.*

class MedicamentoActivity : AppCompatActivity() {

    // Modelos de datos
    data class Medication(
        val id: String,
        val name: String,
        val doseQty: Int,
        val doseType: String, // "Cápsula", "Pastilla", "Jarabe"
        val frequency: String, // "Cada 8 horas", "Una vez al día", "Antes de dormir"
        val durationDays: Int, // 0 = crónico
        val initialBoxSize: Int,
        var inventory: Int,
        val dateRegistered: String // "yyyy-MM-dd"
    )

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
    private lateinit var btnBack: View

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
    private var takenSlotsToday = mutableSetOf<String>() // Set de strings formato: "medId_timeLabel"

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

        // Set dynamic date in header
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
        btnBack = findViewById(R.id.btnBack)

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
        btnBack.setOnClickListener { finish() }

        btnSaveMed.setOnClickListener {
            registrarNuevoMedicamento()
        }
    }

    private fun registrarNuevoMedicamento() {
        val nombre = etMedName.text.toString().trim()
        val dosisCantStr = etMedDoseQty.text.toString().trim()
        val duracionStr = etDurationDays.text.toString().trim()
        val inventarioStr = etInventoryInitial.text.toString().trim()

        // Validaciones
        if (nombre.isEmpty()) {
            etMedName.error = "Ingresa el nombre del medicamento"
            etMedName.requestFocus()
            return
        }

        if (dosisCantStr.isEmpty()) {
            etMedDoseQty.error = "Ingresa la dosis"
            etMedDoseQty.requestFocus()
            return
        }
        val dosisCant = dosisCantStr.toIntOrNull() ?: 0
        if (dosisCant <= 0) {
            etMedDoseQty.error = "Dosis debe ser mayor a 0"
            etMedDoseQty.requestFocus()
            return
        }

        if (duracionStr.isEmpty()) {
            etDurationDays.error = "Ingresa los días de tratamiento"
            etDurationDays.requestFocus()
            return
        }
        val duracion = duracionStr.toIntOrNull() ?: -1
        if (duracion < 0) {
            etDurationDays.error = "Ingresa 0 o más días"
            etDurationDays.requestFocus()
            return
        }

        if (inventarioStr.isEmpty()) {
            etInventoryInitial.error = "Ingresa la cantidad inicial"
            etInventoryInitial.requestFocus()
            return
        }
        val inventario = inventarioStr.toIntOrNull() ?: 0
        if (inventario <= 0) {
            etInventoryInitial.error = "Debe tener al menos una dosis"
            etInventoryInitial.requestFocus()
            return
        }

        val presentacion = spinnerDoseType.selectedItem.toString()
        val frecuencia = spinnerFrequency.selectedItem.toString()
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val nuevoMed = Medication(
            id = UUID.randomUUID().toString(),
            name = nombre,
            doseQty = dosisCant,
            doseType = presentacion,
            frequency = frecuencia,
            durationDays = duracion,
            initialBoxSize = inventario,
            inventory = inventario,
            dateRegistered = hoyStr
        )

        medications.add(nuevoMed)
        guardarMedicamentos()
        actualizarUI()

        // Limpiar campos
        etMedName.text.clear()
        etMedDoseQty.text.clear()
        etDurationDays.text.clear()
        etInventoryInitial.text.clear()

        Toast.makeText(this, "Medicamento '$nombre' registrado con éxito", Toast.LENGTH_LONG).show()
    }

    private fun resaltarDiaActual() {
        val calendar = Calendar.getInstance()
        val dayViews = arrayOf(tvMonDay, tvTueDay, tvWedDay, tvThuDay, tvFriDay, tvSatDay, tvSunDay)
        
        // Domingo en Android es 1, Lunes es 2... Sábado es 7
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

        // Resaltar día de hoy con círculo
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

    private fun cargarDatos() {
        val prefs = getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)

        // Cargar lista de medicamentos
        medications.clear()
        val jsonStr = prefs.getString("med_list_json", null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    medications.add(
                        Medication(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            doseQty = obj.getInt("doseQty"),
                            doseType = obj.getString("doseType"),
                            frequency = obj.getString("frequency"),
                            durationDays = obj.getInt("durationDays"),
                            initialBoxSize = obj.getInt("initialBoxSize"),
                            inventory = obj.getInt("inventory"),
                            dateRegistered = obj.getString("dateRegistered")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Cargar tomas de hoy (y resetear si cambió el día)
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val diaGuardado = prefs.getString("med_dia_actual", "")

        takenSlotsToday.clear()
        if (diaGuardado != hoyStr) {
            prefs.edit()
                .putString("med_dia_actual", hoyStr)
                .putStringSet("med_tomas_hoy", emptySet())
                .apply()
        } else {
            val savedSet = prefs.getStringSet("med_tomas_hoy", emptySet())
            if (savedSet != null) {
                takenSlotsToday.addAll(savedSet)
            }
        }
    }

    private fun guardarMedicamentos() {
        val prefs = getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)
        val array = JSONArray()
        for (m in medications) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("name", m.name)
            obj.put("doseQty", m.doseQty)
            obj.put("doseType", m.doseType)
            obj.put("frequency", m.frequency)
            obj.put("durationDays", m.durationDays)
            obj.put("initialBoxSize", m.initialBoxSize)
            obj.put("inventory", m.inventory)
            obj.put("dateRegistered", m.dateRegistered)
            array.put(obj)
        }
        prefs.edit().putString("med_list_json", array.toString()).apply()
    }

    private fun guardarTomasHoy() {
        val prefs = getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("med_tomas_hoy", takenSlotsToday).apply()
    }

    private fun actualizarUI() {
        actualizarTimeline()
        actualizarAlertasInventario()
        actualizarPuntosCalendario()
    }

    private fun actualizarTimeline() {
        layoutTimeline.removeAllViews()

        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val activeSlots = mutableListOf<MedicationSlot>()

        for (med in medications) {
            // Verificar si el tratamiento ya finalizó
            if (med.durationDays > 0) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                try {
                    val dateReg = sdf.parse(med.dateRegistered)
                    if (dateReg != null) {
                        val cal = Calendar.getInstance()
                        cal.time = dateReg
                        cal.add(Calendar.DAY_OF_YEAR, med.durationDays)
                        
                        val dateExp = cal.time
                        val today = sdf.parse(hoyStr)

                        if (today != null && today.after(dateExp)) {
                            // El tratamiento ya venció, ignorar
                            continue
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Generar franjas horarias
            when {
                med.frequency.contains("8 horas") -> {
                    activeSlots.add(MedicationSlot(med, "08:00 AM", 8))
                    activeSlots.add(MedicationSlot(med, "04:00 PM", 16))
                    activeSlots.add(MedicationSlot(med, "12:00 AM", 0))
                }
                med.frequency.contains("Una vez") -> {
                    activeSlots.add(MedicationSlot(med, "08:00 AM", 8))
                }
                med.frequency.contains("Antes de dormir") -> {
                    activeSlots.add(MedicationSlot(med, "10:00 PM", 22))
                }
            }
        }

        // Ordenar cronológicamente
        activeSlots.sortBy { it.sortHour }

        if (activeSlots.isEmpty()) {
            tvNoMedsMsg.visibility = View.VISIBLE
            layoutTimeline.addView(tvNoMedsMsg)
            return
        }

        tvNoMedsMsg.visibility = View.GONE
        val inflater = LayoutInflater.from(this)

        for (slot in activeSlots) {
            val itemView = inflater.inflate(R.layout.item_medication_slot, layoutTimeline, false)
            
            val ivMedIcon = itemView.findViewById<ImageView>(R.id.ivMedIcon)
            val tvMedNameDose = itemView.findViewById<TextView>(R.id.tvMedNameDose)
            val tvMedTime = itemView.findViewById<TextView>(R.id.tvMedTime)
            val tvMedDuration = itemView.findViewById<TextView>(R.id.tvMedDuration)
            val btnTakeAction = itemView.findViewById<Button>(R.id.btnTakeAction)

            val med = slot.med
            
            // Icono según tipo
            val iconRes = when {
                med.doseType.contains("Cápsula") -> R.drawable.ic_pill
                med.doseType.contains("Pastilla") -> R.drawable.ic_tablet
                else -> R.drawable.ic_syrup
            }
            ivMedIcon.setImageResource(iconRes)

            // Info
            tvMedNameDose.text = "${med.name} - ${med.doseQty} ${med.doseType}"
            tvMedTime.text = slot.timeLabel
            tvMedDuration.text = if (med.durationDays == 0) "Tratamiento: Permanente" else "Tratamiento: Por ${med.durationDays} días"

            // Acción Tomar / Tomado
            val slotId = "${med.id}_${slot.timeLabel}"
            val isTaken = takenSlotsToday.contains(slotId)

            if (isTaken) {
                btnTakeAction.text = "✓ Tomado"
                btnTakeAction.setBackgroundColor(Color.parseColor("#2E7D32"))
                btnTakeAction.isEnabled = false
            } else {
                btnTakeAction.text = "Tomar"
                btnTakeAction.setBackgroundColor(Color.parseColor("#1A4373"))
                btnTakeAction.isEnabled = true
                
                btnTakeAction.setOnClickListener {
                    marcarComoTomado(med, slot.timeLabel)
                }
            }

            layoutTimeline.addView(itemView)
        }
    }

    private fun marcarComoTomado(med: Medication, timeLabel: String) {
        val slotId = "${med.id}_${timeLabel}"
        
        // Agregar a tomas de hoy
        takenSlotsToday.add(slotId)
        guardarTomasHoy()

        // Restar inventario
        med.inventory = (med.inventory - med.doseQty).coerceAtLeast(0)
        guardarMedicamentos()

        // Verificar cumplimiento total para guardar meta
        evaluarCumplimientoDia()

        actualizarUI()
        
        Toast.makeText(this, "Registrado: ${med.name} tomado a las $timeLabel", Toast.LENGTH_SHORT).show()
    }

    private fun evaluarCumplimientoDia() {
        // Generar todos los slots necesarios hoy
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val slotsHoy = mutableListOf<String>()

        for (med in medications) {
            if (med.durationDays > 0) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                try {
                    val dateReg = sdf.parse(med.dateRegistered)
                    if (dateReg != null) {
                        val cal = Calendar.getInstance()
                        cal.time = dateReg
                        cal.add(Calendar.DAY_OF_YEAR, med.durationDays)
                        val today = sdf.parse(hoyStr)
                        if (today != null && today.after(cal.time)) continue
                    }
                } catch (e: Exception) {
                    continue
                }
            }

            when {
                med.frequency.contains("8 horas") -> {
                    slotsHoy.add("${med.id}_08:00 AM")
                    slotsHoy.add("${med.id}_04:00 PM")
                    slotsHoy.add("${med.id}_12:00 AM")
                }
                med.frequency.contains("Una vez") -> {
                    slotsHoy.add("${med.id}_08:00 AM")
                }
                med.frequency.contains("Antes de dormir") -> {
                    slotsHoy.add("${med.id}_10:00 PM")
                }
            }
        }

        // Si hay slots y todos están en takenSlotsToday, marcar cumplimiento de hoy
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

        val compliance = slotsHoy.isNotEmpty() && takenSlotsToday.containsAll(slotsHoy)
        val prefs = getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("med_cumple_dia_$dayIndex", compliance).apply()
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
        val prefs = getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)
        val dots = arrayOf(dotMon, dotTue, dotWed, dotThu, dotFri, dotSat, dotSun)

        for (i in 0..6) {
            val cumple = prefs.getBoolean("med_cumple_dia_$i", false)
            if (cumple) {
                dots[i].setBackgroundColor(Color.parseColor("#2E7D32")) // Punto verde
            } else {
                dots[i].setBackgroundColor(Color.parseColor("#B0BEC5")) // Punto gris
            }
        }
    }
}
