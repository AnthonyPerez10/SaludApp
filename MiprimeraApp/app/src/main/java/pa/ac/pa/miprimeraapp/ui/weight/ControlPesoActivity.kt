package pa.ac.pa.miprimeraapp.ui.weight

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.data.RegistroPeso
import pa.ac.pa.miprimeraapp.data.SaludAppRepository
import pa.ac.pa.miprimeraapp.data.SaludAppRepositoryImpl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Actividad para el control y cálculo de Peso e IMC del usuario.
 * Proporciona persistencia temporal real a través de SaludAppRepository.
 */
class ControlPesoActivity : AppCompatActivity() {

    // Repositorio modular de datos
    private lateinit var repository: SaludAppRepository

    // Variables globales para guardar último resultado y pasarlos al historial
    private var ultimoPeso = 0.0
    private var ultimoIMC = 0.0

    // Componentes de visualización de estadísticas
    private lateinit var tvStatWeightInitial: TextView
    private lateinit var tvStatWeightCurrent: TextView
    private lateinit var tvStatWeightDiff: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_control_peso)

        // Inicializamos el repositorio
        repository = SaludAppRepositoryImpl(this)

        // Fecha dinámica en el encabezado
        val tvDateTimeInfo = findViewById<TextView>(R.id.tvDateTimeInfo)
        val hoyStr = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("es-ES")).format(Date())
        tvDateTimeInfo.text = "REGISTRO: Hoy - ${hoyStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"

        // Conexión XML -> Kotlin
        val etEdad = findViewById<EditText>(R.id.txtNedad)
        val etPeso = findViewById<EditText>(R.id.etPeso)
        val etEstatura = findViewById<EditText>(R.id.etEstatura)
        val swPeso = findViewById<SwitchCompat>(R.id.swPesoUnit)
        val swEstatura = findViewById<SwitchCompat>(R.id.swEstaturaUnit)
        val btnCalcular = findViewById<Button>(R.id.btnCalcular)
        val btnHistorial = findViewById<Button>(R.id.btnHistorial)
        val tvIMC = findViewById<TextView>(R.id.tvIMC)
        val tvPesoIdeal = findViewById<TextView>(R.id.tvPesoIdeal)
        val tvGrasa = findViewById<TextView>(R.id.tvGrasa)
        val tvClasificacion = findViewById<TextView>(R.id.tvClasificacion)

        // Inicialización de vistas de estadísticas
        tvStatWeightInitial = findViewById(R.id.tvStatWeightInitial)
        tvStatWeightCurrent = findViewById(R.id.tvStatWeightCurrent)
        tvStatWeightDiff = findViewById(R.id.tvStatWeightDiff)

        // Cargamos las estadísticas iniciales desde el repositorio
        actualizarEstadisticas()

        // Habilitar historial si ya existen datos registrados previamente en el repositorio
        btnHistorial.isEnabled = repository.getWeightHistory().isNotEmpty()

        // Cambio dinámico de placeholders según la unidad de medida seleccionada
        swPeso.setOnCheckedChangeListener { _, isChecked ->
            etPeso.hint = if (isChecked) "Peso (Lb)" else "Peso (Kg)"
            etPeso.text.clear()
        }

        swEstatura.setOnCheckedChangeListener { _, isChecked ->
            etEstatura.hint = if (isChecked) "Estatura (in)" else "Estatura (cm)"
            etEstatura.text.clear()
        }

        // LÓGICA DE CÁLCULO Y GUARDADO
        btnCalcular.setOnClickListener {
            val sEdad = etEdad.text.toString()
            val sPeso = etPeso.text.toString()
            val sEstatura = etEstatura.text.toString()

            if (sEdad.isEmpty() || sPeso.isEmpty() || sEstatura.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val edad = sEdad.toInt()
            var peso = sPeso.toDouble()
            var estatura = sEstatura.toDouble()

            // Validaciones básicas de rangos lógicos de salud
            if (edad <= 0 || edad > 120) {
                Toast.makeText(this, "Edad inválida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (peso <= 0 || peso >= 500) {
                Toast.makeText(this, "Peso inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (estatura <= 0 || estatura >= 300) {
                Toast.makeText(this, "Estatura inválida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Conversión de unidades a sistema métrico estándar (Kg / Cm)
            if (swPeso.isChecked) {
                peso *= 0.453592
            }

            if (swEstatura.isChecked) {
                estatura *= 2.54
            }

            val estaturaMetros = estatura / 100
            val imc = peso / (estaturaMetros * estaturaMetros)

            ultimoPeso = peso
            ultimoIMC = imc

            val pesoIdeal = 22 * (estaturaMetros * estaturaMetros)
            val grasa = (1.20 * imc) + (0.23 * edad) - 16.2

            // Mostrar resultados en pantalla
            tvIMC.text = String.format(Locale.getDefault(), "%.1f", imc)
            tvPesoIdeal.text = String.format(Locale.getDefault(), "%.1f kg", pesoIdeal)
            tvGrasa.text = String.format(Locale.getDefault(), "%.1f%%", grasa)
            tvClasificacion.text = categorizarIMC(imc)

            // Persistencia del nuevo cálculo en la capa de datos modular
            val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val nuevoRegistro = RegistroPeso(fechaActual, peso, imc)
            repository.addWeightRecord(nuevoRegistro)

            // Actualizar la sección de estadísticas en tiempo real
            actualizarEstadisticas()
            btnHistorial.isEnabled = true

            Toast.makeText(this, "Cálculo realizado y guardado", Toast.LENGTH_SHORT).show()
        }

        // REDIRECCIÓN AL HISTORIAL
        btnHistorial.setOnClickListener {
            val intent = Intent(this, HistorialPesoActivity::class.java)
            // Mandamos los valores actuales calculados de manera rápida por intent extras
            intent.putExtra("peso", ultimoPeso)
            intent.putExtra("imc", ultimoIMC)
            startActivity(intent)
        }
    }

    /**
     * Calcula y actualiza las estadísticas de peso inicial, actual y la diferencia en Kg.
     */
    private fun actualizarEstadisticas() {
        val history = repository.getWeightHistory()
        if (history.isNotEmpty()) {
            val actual = history.first().peso
            val inicial = history.last().peso
            val diff = actual - inicial

            tvStatWeightInitial.text = String.format(Locale.getDefault(), "%.1f kg", inicial)
            tvStatWeightCurrent.text = String.format(Locale.getDefault(), "%.1f kg", actual)

            val diffText = if (diff > 0) "+${String.format(Locale.getDefault(), "%.1f", diff)}" else String.format(Locale.getDefault(), "%.1f", diff)
            tvStatWeightDiff.text = "$diffText kg"

            // Verde para pérdida/mantenimiento, rojo para aumento de peso
            if (diff <= 0) {
                tvStatWeightDiff.setTextColor(Color.parseColor("#2E7D32"))
            } else {
                tvStatWeightDiff.setTextColor(Color.parseColor("#D32F2F"))
            }
        } else {
            tvStatWeightInitial.text = "--"
            tvStatWeightCurrent.text = "--"
            tvStatWeightDiff.text = "--"
            tvStatWeightDiff.setTextColor(Color.parseColor("#607D8B"))
        }
    }

    /**
     * Clasifica el IMC del usuario según la Organización Mundial de la Salud.
     */
    private fun categorizarIMC(imc: Double): String {
        return when {
            imc < 18.5 -> "Bajo peso"
            imc < 25 -> "Normal"
            imc < 30 -> "Sobrepeso"
            imc < 35 -> "Obesidad I"
            imc < 40 -> "Obesidad II"
            else -> "Obesidad III"
        }
    }
}
