package pa.ac.pa.miprimeraapp.ui.glucose

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.data.RegistroGlucosa
import pa.ac.pa.miprimeraapp.data.SaludAppRepository
import pa.ac.pa.miprimeraapp.data.SaludAppRepositoryImpl
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad para el control, registro y análisis del nivel de glucosa en sangre del usuario.
 * Utiliza SaludAppRepository para persistir las lecturas de forma modular.
 */
class ControlGlucosaActivity : AppCompatActivity() {

    // Repositorio modular de datos
    private lateinit var repository: SaludAppRepository

    // Componentes del formulario
    private lateinit var etGlucoseValue: EditText
    private lateinit var etOptionalNotes: EditText
    private lateinit var rgRecordType: RadioGroup
    private lateinit var btnSaveRecord: Button
    private lateinit var tvPreviousSummary: TextView

    // KPIs de estadísticas de glucosa
    private lateinit var tvStatGlucoseAvg: TextView
    private lateinit var tvStatGlucoseRange: TextView
    private lateinit var tvStatGlucoseCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_control_glucosa)

        repository = SaludAppRepositoryImpl(this)

        // Configuración de márgenes para el diseño Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inicializarVistas()
        actualizarUI()

        // Fecha dinámica en el encabezado
        val tvDateTimeInfo = findViewById<TextView>(R.id.tvDateTimeInfo)
        val hoyStr = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("es-ES")).format(Date())
        tvDateTimeInfo.text = "REGISTRO: Hoy - ${hoyStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"

        btnSaveRecord.setOnClickListener {
            ejecutarRegistro()
        }
    }

    private fun inicializarVistas() {
        etGlucoseValue = findViewById(R.id.etGlucoseValue)
        etOptionalNotes = findViewById(R.id.etOptionalNotes)
        rgRecordType = findViewById(R.id.rgRecordType)
        btnSaveRecord = findViewById(R.id.btnSaveRecord)
        tvPreviousSummary = findViewById(R.id.tvPreviousSummary)

        // Vistas de estadísticas
        tvStatGlucoseAvg = findViewById(R.id.tvStatGlucoseAvg)
        tvStatGlucoseRange = findViewById(R.id.tvStatGlucoseRange)
        tvStatGlucoseCount = findViewById(R.id.tvStatGlucoseCount)
    }

    /**
     * Valida e inicia el proceso de guardado del nuevo registro de glucosa.
     */
    private fun ejecutarRegistro() {
        val glucosaTexto = etGlucoseValue.text.toString().trim()
        val notasOpcionales = etOptionalNotes.text.toString().trim()

        val selectedRadioId = rgRecordType.checkedRadioButtonId
        val tipoRegistro = obtenerTipoRegistroTexto(selectedRadioId)

        if (!validarGlucosa(glucosaTexto)) {
            return
        }

        val valorGlucosa = glucosaTexto.toDouble()
        guardarRegistro(valorGlucosa, tipoRegistro, notasOpcionales)
    }

    /**
     * Valida que el valor ingresado sea numérico y se encuentre en un rango de salud fisiológico real (20 a 600 mg/dL).
     */
    private fun validarGlucosa(texto: String): Boolean {
        if (texto.isEmpty()) {
            etGlucoseValue.error = "El valor de glucosa es obligatorio"
            etGlucoseValue.requestFocus()
            return false
        }

        val valor = texto.toDoubleOrNull()
        if (valor == null) {
            etGlucoseValue.error = "Ingresa un formato numérico válido"
            etGlucoseValue.requestFocus()
            return false
        }

        if (valor < 20.0 || valor > 600.0) {
            etGlucoseValue.error = "El rango real debe estar entre 20 y 600 mg/dL"
            etGlucoseValue.requestFocus()
            return false
        }

        return true
    }

    private fun obtenerTipoRegistroTexto(radioId: Int): String {
        return when (radioId) {
            R.id.rbAyunas -> "Ayunas"
            R.id.rbAntesAlmuerzo -> "Antes de almuerzo"
            R.id.rbDespuesAlmuerzo -> "Después de almuerzo"
            R.id.rbCena -> "Cena"
            else -> "No definido"
        }
    }

    /**
     * Guarda la lectura en la persistencia local modular a través del repositorio,
     * limpia el formulario y actualiza la sección de KPIs.
     */
    private fun guardarRegistro(glucosa: Double, tipo: String, notas: String) {
        val horaActual = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Calendar.getInstance().time)
        val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val nuevoRegistro = RegistroGlucosa(
            valor = glucosa,
            tipo = tipo,
            notas = notas,
            hora = horaActual,
            fecha = fechaActual
        )

        // Agregar al repositorio
        repository.addGlucoseRecord(nuevoRegistro)

        val resumen = "Glucosa: $glucosa mg/dL\nTipo: $tipo\nNotas: ${notas.ifEmpty { "Ninguna" }}"
        Toast.makeText(this, "Registro Exitoso:\n$resumen", Toast.LENGTH_LONG).show()

        // Limpiar el formulario
        etGlucoseValue.text.clear()
        etOptionalNotes.text.clear()
        rgRecordType.check(R.id.rbAyunas)

        actualizarUI()
    }

    /**
     * Refresca las estadísticas generales y el último resumen del historial.
     */
    private fun actualizarUI() {
        val history = repository.getGlucoseRecords()

        if (history.isNotEmpty()) {
            val ultimo = history.first()
            val glucosaEntero = ultimo.valor.toInt()
            tvPreviousSummary.text = "Última lectura: $glucosaEntero mg/dL (${ultimo.tipo}; ${ultimo.hora})"

            // Calcular KPIs
            val count = history.size
            val sum = history.sumOf { it.valor }
            val avg = sum / count

            // Rango objetivo estándar saludable: 70 a 140 mg/dL
            val inRangeCount = history.count { it.valor in 70.0..140.0 }
            val pctRange = (inRangeCount.toFloat() / count.toFloat() * 100).toInt()

            tvStatGlucoseAvg.text = String.format(Locale.getDefault(), "%.0f mg/dL", avg)
            tvStatGlucoseRange.text = "$pctRange%"
            tvStatGlucoseCount.text = count.toString()
        } else {
            tvPreviousSummary.text = "No hay lecturas registradas"
            tvStatGlucoseAvg.text = "--"
            tvStatGlucoseRange.text = "--"
            tvStatGlucoseCount.text = "0"
        }
    }
}
