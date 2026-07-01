package pa.ac.pa.miprimeraapp.ui.glucose

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView // <-- 1. IMPORTANTE: Importar TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.pa.miprimeraapp.R
import android.widget.ImageView
import java.text.SimpleDateFormat // <-- Para capturar la hora actual
import java.util.Locale
import java.util.Calendar
import java.util.Date

class ControlGlucosaActivity : AppCompatActivity() {

    // Declaración de las variables para las vistas
    private lateinit var etGlucoseValue: EditText
    private lateinit var etOptionalNotes: EditText
    private lateinit var rgRecordType: RadioGroup
    private lateinit var btnSaveRecord: Button

    // 2. Variable para el TextView del resumen previo
    private lateinit var tvPreviousSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_control_glucosa)

        // Botón de regreso
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // Fecha dinámica en el encabezado
        val tvDateTimeInfo = findViewById<TextView>(R.id.tvDateTimeInfo)
        val hoyStr = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("es-ES")).format(Date())
        tvDateTimeInfo.text = "REGISTRO: Hoy - ${hoyStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"

        // Configuración de márgenes para el diseño Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Inicializar las vistas mediante sus IDs del XML
        inicializarVistas()

        // 2. Configurar los eventos de clic
        configurarListeners()
    }

    private fun inicializarVistas() {
        etGlucoseValue = findViewById(R.id.etGlucoseValue)
        etOptionalNotes = findViewById(R.id.etOptionalNotes)
        rgRecordType = findViewById(R.id.rgRecordType)
        btnSaveRecord = findViewById(R.id.btnSaveRecord)

        // 3. Inicializar el componente del XML
        tvPreviousSummary = findViewById(R.id.tvPreviousSummary)
    }

    private fun configurarListeners() {
        // RadioGroup ya garantiza que solo un RadioButton esté seleccionado.
        btnSaveRecord.setOnClickListener {
            ejecutarRegistro()
        }
    }

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
     * Muestra la confirmación de guardado, ACTUALIZA EL RESUMEN y resetea los campos.
     */
    private fun guardarRegistro(glucosa: Double, tipo: String, notas: String) {
        val resumen = "Glucosa: $glucosa mg/dL\nTipo: $tipo\nNotas: ${notas.ifEmpty { "Ninguna" }}"

        // Alerta de éxito al usuario
        Toast.makeText(this, "Registro Exitoso:\n$resumen", Toast.LENGTH_LONG).show()

        // 4. Obtener la hora actual del sistema en formato hh:mm a (Ej: 12:48 PM)
        val formateadorHora = SimpleDateFormat("h:mm a", Locale.getDefault())
        val horaActual = formateadorHora.format(Calendar.getInstance().time)

        // 5. MODIFICAR EL TEXTVIEW CON LOS NUEVOS DATOS INGRESADOS
        // Quitamos los decimales innecesarios convirtiendo la glucosa a entero si es necesario (ej: 124 en vez de 124.0)
        val glucosaEntero = glucosa.toInt()
        tvPreviousSummary.text = "Última lectura: $glucosaEntero mg/dL ($tipo; $horaActual)"

        // Limpiar el formulario para un próximo uso limpio
        etGlucoseValue.text.clear()
        etOptionalNotes.text.clear()

        // Reseteo manual de los botones
        rgRecordType.check(R.id.rbAyunas)
    }
}
