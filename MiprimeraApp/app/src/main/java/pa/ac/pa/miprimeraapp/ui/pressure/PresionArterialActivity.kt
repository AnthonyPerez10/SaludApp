package pa.ac.pa.miprimeraapp.ui.pressure

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.data.RegistroPresion
import pa.ac.pa.miprimeraapp.data.SaludAppRepository
import pa.ac.pa.miprimeraapp.data.SaludAppRepositoryImpl
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad para registrar, analizar y persistir mediciones de presión arterial y pulso.
 * Utiliza SaludAppRepository para centralizar el almacenamiento.
 */
class PresionArterialActivity : AppCompatActivity() {

    // Repositorio modular de datos
    private lateinit var repository: SaludAppRepository

    // Variables del XML
    private lateinit var btnFecha: Button
    private lateinit var btnHora: LinearLayout
    private lateinit var txtHora: TextView
    private lateinit var npSistolica: NumberPicker
    private lateinit var npDiastolica: NumberPicker
    private lateinit var npPulso: NumberPicker
    private lateinit var rgBrazo: RadioGroup
    private lateinit var btnAnalizar: Button
    private lateinit var btnHistorial: Button



    // KPIs del panel de estadísticas
    private lateinit var tvStatSysAvg: TextView
    private lateinit var tvStatDiaAvg: TextView
    private lateinit var tvStatPulseAvg: TextView

    private var fechaSeleccionada: String? = null
    private var horaSeleccionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_presion_arterial)

        repository = SaludAppRepositoryImpl(this)

        // Fecha dinámica en el encabezado
        val tvDateTimeInfo = findViewById<TextView>(R.id.tvDateTimeInfo)
        val hoyStr = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("es-ES")).format(Date())
        tvDateTimeInfo.text = "REGISTRO: Hoy - ${hoyStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"

        inicializarVistas()
        configurarNumberPickers()
        actualizarUI()

        // Eventos
        btnFecha.setOnClickListener { mostrarDatePicker() }
        btnHora.setOnClickListener { mostrarTimePicker() }
        btnAnalizar.setOnClickListener { registrarMedicion() }
        btnHistorial.setOnClickListener {
            val intent = android.content.Intent(this, HistorialPresionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun inicializarVistas() {
        btnFecha = findViewById(R.id.btnFecha)
        btnHora = findViewById(R.id.btnHora)
        txtHora = findViewById(R.id.txtHora)

        npSistolica = findViewById(R.id.npSistolica)
        npDiastolica = findViewById(R.id.npDiastolica)
        npPulso = findViewById(R.id.npPulso)
        rgBrazo = findViewById(R.id.rgBrazo)
        btnAnalizar = findViewById(R.id.btnAnalizar)
        btnHistorial = findViewById(R.id.btnHistorial)



        // KPIs de estadísticas
        tvStatSysAvg = findViewById(R.id.tvStatSysAvg)
        tvStatDiaAvg = findViewById(R.id.tvStatDiaAvg)
        tvStatPulseAvg = findViewById(R.id.tvStatPulseAvg)
    }

    private fun configurarNumberPickers() {
        npSistolica.minValue = 80
        npSistolica.maxValue = 200
        npSistolica.value = 120 // Valor inicial promedio normal

        npDiastolica.minValue = 40
        npDiastolica.maxValue = 130
        npDiastolica.value = 80  // Valor inicial promedio normal

        npPulso.minValue = 40
        npPulso.maxValue = 180
        npPulso.value = 70  // Valor inicial promedio normal
    }

    /**
     * Muestra un DatePickerDialog para seleccionar la fecha de la toma.
     */
    private fun mostrarDatePicker() {
        val calendario = Calendar.getInstance()
        val anio = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->
            fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, monthOfYear + 1, year)
            btnFecha.text = fechaSeleccionada
        }, anio, mes, dia)

        // Restricción: Desde 2015 hasta el día de hoy
        val calMin = Calendar.getInstance().apply { set(2015, Calendar.JANUARY, 1) }
        datePickerDialog.datePicker.minDate = calMin.timeInMillis
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        datePickerDialog.show()
    }

    /**
     * Muestra un TimePickerDialog de tipo Spinner para seleccionar la hora de la toma.
     */
    private fun mostrarTimePicker() {
        val calendario = Calendar.getInstance()
        val hora = calendario.get(Calendar.HOUR_OF_DAY)
        val minuto = calendario.get(Calendar.MINUTE)

        // Usamos el tema Holo Light Dialog para mostrar selectores de rueda (spinner) en lugar del reloj radial
        val timePickerDialog = TimePickerDialog(
            this,
            android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            { _, hourOfDay, minute ->
                val amPm = if (hourOfDay < 12) "AM" else "PM"
                val hora12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                horaSeleccionada = String.format(Locale.getDefault(), "%02d:%02d %s", hora12, minute, amPm)
                txtHora.text = horaSeleccionada
            },
            hora,
            minuto,
            false
        )

        // Limpiar el fondo clásico de Holo para una apariencia moderna
        timePickerDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        timePickerDialog.show()
    }

    /**
     * Valida y procesa la nueva medición, calculando la clasificación de salud
     * y guardándola mediante el repositorio.
     */
    private fun registrarMedicion() {
        if (fechaSeleccionada == null) {
            Toast.makeText(this, "Por favor, seleccione una fecha", Toast.LENGTH_SHORT).show()
            return
        }

        if (horaSeleccionada == null) {
            Toast.makeText(this, "Por favor, seleccione una hora", Toast.LENGTH_SHORT).show()
            return
        }

        val sis = npSistolica.value
        val dia = npDiastolica.value
        val pulso = npPulso.value

        val selectedRadioId = rgBrazo.checkedRadioButtonId
        val brazo = if (selectedRadioId == R.id.rbDerecho) "Derecho" else "Izquierdo"
        val clasificacion = clasificarPresion(sis, dia)

        val nuevoRegistro = RegistroPresion(
            fecha = fechaSeleccionada!!,
            hora = horaSeleccionada!!,
            sistolica = sis,
            diastolica = dia,
            pulso = pulso,
            brazo = brazo,
            clasificacion = clasificacion
        )

        // Guardar en repositorio
        repository.addPressureRecord(nuevoRegistro)

        Toast.makeText(this, "Medición guardada con éxito", Toast.LENGTH_SHORT).show()

        // Resetear botones del formulario
        fechaSeleccionada = null
        horaSeleccionada = null
        btnFecha.text = "Seleccionar Fecha"
        txtHora.text = "Seleccionar Hora"

        actualizarUI()
    }

    /**
     * Clasifica la presión arterial basándose en los parámetros de la American Heart Association.
     */
    private fun clasificarPresion(sistolica: Int, diastolica: Int): String {
        return when {
            sistolica < 120 && diastolica < 80 -> "Normal"
            sistolica in 120..129 && diastolica < 80 -> "Elevada"
            sistolica in 130..139 || diastolica in 80..89 -> "Presión alta (Estadio 1)"
            sistolica >= 140 || diastolica >= 90 -> "Presión alta (Estadio 2)"
            else -> "Crisis de Hipertensión (Consulte a su médico)"
        }
    }

    /**
     * Carga el historial desde el repositorio y actualiza las estadísticas y el último resumen.
     */
    private fun actualizarUI() {
        val history = repository.getPressureRecords()

        if (history.isNotEmpty()) {
            // Calcular KPIs promedio de todo el historial
            val count = history.size
            val avgSys = history.sumOf { it.sistolica } / count
            val avgDia = history.sumOf { it.diastolica } / count
            val avgPulse = history.sumOf { it.pulso } / count

            tvStatSysAvg.text = "$avgSys mmHg"
            tvStatDiaAvg.text = "$avgDia mmHg"
            tvStatPulseAvg.text = "$avgPulse lpm"
        } else {
            tvStatSysAvg.text = "--"
            tvStatDiaAvg.text = "--"
            tvStatPulseAvg.text = "--"
        }
    }
}
