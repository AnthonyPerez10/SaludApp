package pa.ac.pa.miprimeraapp.ui.weight

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import pa.ac.pa.miprimeraapp.R

import android.view.View
import android.widget.ImageView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class ControlPesoActivity : AppCompatActivity() {

    // Variables globales para guardar último resultado
    private var ultimoPeso = 0.0
    private var ultimoIMC = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_control_peso)

        // Botón de regreso
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // Fecha dinámica en el encabezado
        val tvDateTimeInfo = findViewById<TextView>(R.id.tvDateTimeInfo)
        val hoyStr = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("es-ES")).format(Date())
        tvDateTimeInfo.text = "REGISTRO: Hoy - ${hoyStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"

        // Conexion XML -> Kotlin
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

        // Evita abrir el historial sin haber calculado antes.
        btnHistorial.isEnabled = false

        // Cambio dinámico de hints
        swPeso.setOnCheckedChangeListener { _, isChecked ->
            etPeso.hint = if (isChecked) "Peso (Lb)" else "Peso (Kg)"
            etPeso.text.clear()
        }

        swEstatura.setOnCheckedChangeListener { _, isChecked ->
            etEstatura.hint = if (isChecked) "Estatura (in)" else "Estatura (cm)"
            etEstatura.text.clear()
        }

        // BOTÓN CALCULAR
        btnCalcular.setOnClickListener {

            val sEdad = etEdad.text.toString()
            val sPeso = etPeso.text.toString()
            val sEstatura = etEstatura.text.toString()

            // Validar campos vacíos
            if (sEdad.isEmpty() || sPeso.isEmpty() || sEstatura.isEmpty()) {
                Toast.makeText(
                    this,
                    "Completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Convertir datos
            val edad = sEdad.toInt()
            var peso = sPeso.toDouble()
            var estatura = sEstatura.toDouble()

            // Validaciones
            if (edad <= 0 || edad > 120) {

                Toast.makeText(
                    this,
                    "Edad inválida",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (peso <= 0 || peso >= 500) {
                Toast.makeText(
                    this,
                    "Peso inválido",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (estatura <= 0 || estatura >= 300) {
                Toast.makeText(
                    this,
                    "Estatura inválida",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Conversión peso
            if (swPeso.isChecked) {
                peso *= 0.453592
            }

            // Conversión estatura
            if (swEstatura.isChecked) {
                estatura *= 2.54
            }

            // Convertir cm -> metros
            val estaturaMetros = estatura / 100

            // Calcular IMC
            val imc = peso / (estaturaMetros * estaturaMetros)

            // Guardar resultados globales
            ultimoPeso = peso
            ultimoIMC = imc

            // Peso ideal
            val pesoIdeal = 22 * (estaturaMetros * estaturaMetros)

            // Porcentaje grasa
            val grasa = (1.20 * imc) + (0.23 * edad) - 16.2

            // Mostrar resultados
            tvIMC.text = String.format("%.1f", imc)
            tvPesoIdeal.text = String.format("%.1f kg", pesoIdeal)
            tvGrasa.text = String.format("%.1f%%", grasa)
            tvClasificacion.text = categorizarIMC(imc)

            // Habilita historial una vez hay resultados calculados.
            btnHistorial.isEnabled = true

            Toast.makeText(
                this,
                "Cálculo realizado",
                Toast.LENGTH_SHORT
            ).show()
        }

        // BOTÓN HISTORIAL
        btnHistorial.setOnClickListener {
            if (ultimoIMC == 0.0 || ultimoPeso == 0.0) {
                Toast.makeText(
                    this,
                    "Primero calcula tu IMC para ver el historial",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val intent = Intent(
                this,
                HistorialPesoActivity::class.java
            )

            // Enviar resultados
            intent.putExtra("peso", ultimoPeso)
            intent.putExtra("imc", ultimoIMC)
            startActivity(intent)
        }
    }

    // Clasificación IMC
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
