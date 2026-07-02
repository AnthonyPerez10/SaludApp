package pa.ac.pa.miprimeraapp.ui.medication

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
 * Utiliza un diálogo modal emergente para registrar nuevos medicamentos para mantener la interfaz limpia.
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

    // Botón para abrir el modal
    private lateinit var btnOpenAddMedModal: Button

    // KPIs del panel de estadísticas
    private lateinit var tvMedRegisteredKPI: TextView
    private lateinit var tvMedLowStockKPI: TextView

    // Contenedores dinámicos
    private lateinit var layoutTimeline: LinearLayout
    private lateinit var tvNoMedsMsg: TextView
    private lateinit var cardInventoryAlerts: View
    private lateinit var layoutInventoryAlertList: LinearLayout

    // Calendario Semanal (Puntos indicadores de toma completa)
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

    // Listas locales de medicamentos y tomas realizadas hoy
    private var medications = mutableListOf<Medication>()
    private var takenSlotsToday = mutableSetOf<String>() // Estructura: "medId_timeLabel"

    // Opciones spinners del formulario
    private val doseTypes = arrayOf("Pastillas", "ml")
    private val frequencies = arrayOf(
        "Cada 4 horas",
        "Cada 6 horas",
        "Cada 8 horas",
        "Cada 12 horas",
        "Una vez al día (Mañana)",
        "Antes de dormir (Noche)",
        "Un día sí, un día no"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicamento)

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
        resaltarDiaActual()
        actualizarUI()

        // Fecha dinámica en el encabezado
        val tvDateTimeInfo = findViewById<TextView>(R.id.tvDateTimeInfo)
        val hoyStr = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("es-ES")).format(Date())
        tvDateTimeInfo.text = "REGISTRO: Hoy - ${hoyStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
    }

    private fun inicializarVistas() {
        btnOpenAddMedModal = findViewById(R.id.btnOpenAddMedModal)

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

    private fun setupListeners() {
        // Al presionar el botón de añadir se despliega el formulario modal
        btnOpenAddMedModal.setOnClickListener {
            mostrarDialogoAgregarMedicamento()
        }
    }

    /**
     * Muestra un diálogo emergente (AlertDialog) con el formulario para registrar un medicamento,
     * manteniendo la interfaz limpia y fácil de navegar.
     */
    private fun mostrarDialogoAgregarMedicamento() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_medication, null)

        val etMedName = dialogView.findViewById<EditText>(R.id.etMedName)
        val etMedDoseQty = dialogView.findViewById<EditText>(R.id.etMedDoseQty)
        val spinnerDoseType = dialogView.findViewById<Spinner>(R.id.spinnerDoseType)
        val spinnerFrequency = dialogView.findViewById<Spinner>(R.id.spinnerFrequency)
        val etDurationDays = dialogView.findViewById<EditText>(R.id.etDurationDays)
        val etInventoryInitial = dialogView.findViewById<EditText>(R.id.etInventoryInitial)

        // Configurar los adaptadores de los spinners del modal
        val doseAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, doseTypes)
        doseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDoseType.adapter = doseAdapter

        val freqAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, frequencies)
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrequency.adapter = freqAdapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Registrar", null) // Se anula temporalmente para invalidar cierre en errores
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()

        // Sobrescribir clic del botón de registro para validar campos sin cerrar el diálogo en fallas
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = etMedName.text.toString().trim()
            val dosisCantStr = etMedDoseQty.text.toString().trim()
            val duracionStr = etDurationDays.text.toString().trim()
            val inventarioStr = etInventoryInitial.text.toString().trim()

            // Validar campos vacíos
            if (nombre.isEmpty() || dosisCantStr.isEmpty() || duracionStr.isEmpty() || inventarioStr.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos del formulario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar longitud del nombre
            if (nombre.length > 50) {
                etMedName.error = "El nombre no puede exceder los 50 caracteres"
                etMedName.requestFocus()
                return@setOnClickListener
            }

            // Validar cantidad de dosis por toma
            if (dosisCantStr.length > 5) {
                etMedDoseQty.error = "Dosis demasiado larga"
                etMedDoseQty.requestFocus()
                return@setOnClickListener
            }
            val dosisCant = dosisCantStr.toIntOrNull()
            if (dosisCant == null || dosisCant <= 0) {
                etMedDoseQty.error = "La cantidad debe ser mayor a 0"
                etMedDoseQty.requestFocus()
                return@setOnClickListener
            }

            // Validar duración
            if (duracionStr.length > 4) {
                etDurationDays.error = "Duración demasiado larga"
                etDurationDays.requestFocus()
                return@setOnClickListener
            }
            val duracion = duracionStr.toIntOrNull()
            if (duracion == null || duracion < 0) {
                etDurationDays.error = "Duración debe ser mayor o igual a 0"
                etDurationDays.requestFocus()
                return@setOnClickListener
            }

            // Validar inventario inicial
            if (inventarioStr.length > 5) {
                etInventoryInitial.error = "Inventario inicial demasiado largo"
                etInventoryInitial.requestFocus()
                return@setOnClickListener
            }
            val inventario = inventarioStr.toIntOrNull()
            if (inventario == null || inventario < 0) {
                etInventoryInitial.error = "Inventario debe ser mayor o igual a 0"
                etInventoryInitial.requestFocus()
                return@setOnClickListener
            }

            val idUnique = UUID.randomUUID().toString()
            val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Crear y guardar el medicamento
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

            actualizarUI()
            Toast.makeText(this, "Medicamento registrado con éxito", Toast.LENGTH_SHORT).show()
            dialog.dismiss() // Cerrar el modal solo en caso exitoso
        }
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

    private fun actualizarUI() {
        actualizarKPIs()
        actualizarTimeline()
        actualizarAlertasDeInventario()
    }

    private fun actualizarKPIs() {
        tvMedRegisteredKPI.text = medications.size.toString()
        val lowStockCount = medications.count { it.inventory < 5 }
        tvMedLowStockKPI.text = lowStockCount.toString()
    }

    /**
     * Reconstruye dinámicamente la línea de tiempo cronológica de tomas programadas
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
                "Cada 4 horas" -> {
                    activeSlots.add(MedicationSlot(med, "08:00 AM", 8))
                    activeSlots.add(MedicationSlot(med, "12:00 PM", 12))
                    activeSlots.add(MedicationSlot(med, "04:00 PM", 16))
                    activeSlots.add(MedicationSlot(med, "08:00 PM", 20))
                    activeSlots.add(MedicationSlot(med, "12:00 AM", 24))
                    activeSlots.add(MedicationSlot(med, "04:00 AM", 4))
                }
                "Cada 6 horas" -> {
                    activeSlots.add(MedicationSlot(med, "06:00 AM", 6))
                    activeSlots.add(MedicationSlot(med, "12:00 PM", 12))
                    activeSlots.add(MedicationSlot(med, "06:00 PM", 18))
                    activeSlots.add(MedicationSlot(med, "12:00 AM", 24))
                }
                "Cada 8 horas", "Cada 8 horas (3 veces)" -> {
                    activeSlots.add(MedicationSlot(med, "08:00 AM", 8))
                    activeSlots.add(MedicationSlot(med, "04:00 PM", 16))
                    activeSlots.add(MedicationSlot(med, "12:00 AM", 24))
                }
                "Cada 12 horas" -> {
                    activeSlots.add(MedicationSlot(med, "08:00 AM", 8))
                    activeSlots.add(MedicationSlot(med, "08:00 PM", 20))
                }
                "Una vez al día (Mañana)" -> {
                    activeSlots.add(MedicationSlot(med, "09:00 AM", 9))
                }
                "Antes de dormir (Noche)" -> {
                    activeSlots.add(MedicationSlot(med, "10:00 PM", 22))
                }
                "Un día sí, un día no" -> {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    try {
                        val dateReg = sdf.parse(med.dateRegistered)
                        if (dateReg != null) {
                            val diffTime = Date().time - dateReg.time
                            val diffDays = (diffTime / (1000 * 60 * 60 * 24)).toInt()
                            if (diffDays % 2 == 0) {
                                activeSlots.add(MedicationSlot(med, "09:00 AM", 9))
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
                val tvHour = slotView.findViewById<TextView>(R.id.tvMedTime)
                val tvName = slotView.findViewById<TextView>(R.id.tvMedNameDose)
                val tvDuration = slotView.findViewById<TextView>(R.id.tvMedDuration)
                val btnTakeAction = slotView.findViewById<Button>(R.id.btnTakeAction)

                tvHour.text = slot.timeLabel
                tvName.text = "${slot.med.name} - ${slot.med.doseQty} ${slot.med.doseType}"
                tvDuration.text = if (slot.med.durationDays > 0) "Tratamiento: Por ${slot.med.durationDays} días" else "Tratamiento: Crónico"

                val slotKey = "${slot.med.id}_${slot.timeLabel}"
                val isTaken = takenSlotsToday.contains(slotKey)

                if (isTaken) {
                    btnTakeAction.text = "Tomado"
                    btnTakeAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E7D32"))
                } else {
                    btnTakeAction.text = "Tomar"
                    btnTakeAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#112E52"))
                }

                // Listener para registrar tomas y descontar del inventario
                btnTakeAction.setOnClickListener {
                    val isNowTaken = !takenSlotsToday.contains(slotKey)
                    if (isNowTaken) {
                        takenSlotsToday.add(slotKey)
                        // Descontar dosis del inventario
                        if (slot.med.inventory >= slot.med.doseQty) {
                            slot.med.inventory -= slot.med.doseQty
                        } else {
                            slot.med.inventory = 0
                            Toast.makeText(this, "¡Inventario agotado para ${slot.med.name}!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        takenSlotsToday.remove(slotKey)
                        // Devolver dosis al inventario
                        slot.med.inventory += slot.med.doseQty
                    }
                    repository.saveTakenSlotsToday(takenSlotsToday)
                    repository.saveMedications(medications)
                    actualizarUI()
                }

                layoutTimeline.addView(slotView)
            }
        }
        evaluarMetaCompletaSemana()
    }

    /**
     * Mapea alertas visuales de color rojo para fármacos con menos de 5 unidades.
     */
    private fun actualizarAlertasDeInventario() {
        layoutInventoryAlertList.removeAllViews()
        val lowStockMeds = medications.filter { it.inventory < 5 }

        if (lowStockMeds.isEmpty()) {
            cardInventoryAlerts.visibility = View.GONE
        } else {
            cardInventoryAlerts.visibility = View.VISIBLE
            val inflater = LayoutInflater.from(this)
            for (med in lowStockMeds) {
                val alertView = inflater.inflate(R.layout.item_inventory_alert, layoutInventoryAlertList, false)
                val tvAlertMedName = alertView.findViewById<TextView>(R.id.tvAlertMedName)
                val tvAlertStockText = alertView.findViewById<TextView>(R.id.tvAlertStockText)
                val pbInventoryProgress = alertView.findViewById<ProgressBar>(R.id.pbInventoryProgress)

                tvAlertMedName.text = "¡Alerta: ${med.name}!"
                tvAlertStockText.text = "Quedan solo ${med.inventory} ${med.doseType}"
                pbInventoryProgress.max = med.initialBoxSize
                pbInventoryProgress.progress = med.inventory

                layoutInventoryAlertList.addView(alertView)
            }
        }
    }

    private fun resaltarDiaActual() {
        val calendar = Calendar.getInstance()
        val dayIndex = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        val days = arrayOf(tvMonDay, tvTueDay, tvWedDay, tvThuDay, tvFriDay, tvSatDay, tvSunDay)
        for (i in days.indices) {
            if (i == dayIndex) {
                days[i].setTextColor(Color.parseColor("#112E52"))
                days[i].paintFlags = days[i].paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                days[i].textSize = 14f
            }
        }
    }

    private fun evaluarMetaCompletaSemana() {
        val calendar = Calendar.getInstance()
        val dayIndex = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        // Evaluar si se han completado todas las tomas de hoy
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val activeSlotsCount = contarTomasDeHoy(hoyStr)

        val tomasHechasHoy = countHechasHoy()
        val completado = activeSlotsCount > 0 && tomasHechasHoy >= activeSlotsCount

        repository.saveMedicationHistoryDay(dayIndex, completado)

        val dots = arrayOf(dotMon, dotTue, dotWed, dotThu, dotFri, dotSat, dotSun)
        for (i in dots.indices) {
            val cumplio = repository.getMedicationHistoryDay(i)
            dots[i].backgroundTintList = ColorStateList.valueOf(
                Color.parseColor(if (cumplio) "#2E7D32" else "#B0BEC5")
            )
        }
    }

    private fun contarTomasDeHoy(hoyStr: String): Int {
        var count = 0
        for (med in medications) {
            if (med.durationDays > 0) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                try {
                    val dateReg = sdf.parse(med.dateRegistered)
                    if (dateReg != null) {
                        val cal = Calendar.getInstance()
                        cal.time = dateReg
                        cal.add(Calendar.DAY_OF_YEAR, med.durationDays)
                        if (Date().after(cal.time)) {
                            continue
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            count += when (med.frequency) {
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
        return count
    }

    private fun countHechasHoy(): Int {
        var count = 0
        for (slotKey in takenSlotsToday) {
            // Verificar si el slotKey corresponde a un medicamento registrado activo
            val parts = slotKey.split("_")
            if (parts.isNotEmpty()) {
                val id = parts[0]
                if (medications.any { it.id == id }) {
                    count++
                }
            }
        }
        return count
    }
}
