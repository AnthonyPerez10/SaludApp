package pa.ac.pa.miprimeraapp

import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat

class Presion_arterial : AppCompatActivity() {

    // Variables del XML Presión Arterial Locales
    private lateinit var btnFecha: Button
    private lateinit var txtFecha: TextView
    private lateinit var timePicker: TimePicker
    private lateinit var npSistolica: NumberPicker
    private lateinit var npDiastolica: NumberPicker
    private lateinit var npPulso: NumberPicker
    private lateinit var rgBrazo: RadioGroup
    private lateinit var txtResultado: TextView
    private lateinit var btnAnalizar: Button

    private var fechaSeleccionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_presion_arterial)
        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/

        // Aquí se inicializan los controles con findViewById
        setContentView(R.layout.activity_presion_arterial)

        btnFecha = findViewById(R.id.btnFecha)
        txtFecha = findViewById(R.id.txtFecha)
        timePicker = findViewById(R.id.timePicker)
        npSistolica = findViewById(R.id.npSistolica)
        npDiastolica = findViewById(R.id.npDiastolica)
        npPulso = findViewById(R.id.npPulso)
        rgBrazo = findViewById(R.id.rgBrazo)
        txtResultado = findViewById(R.id.txtResultado)
        btnAnalizar = findViewById(R.id.btnAnalizar)

        //Cotroles visuales y definicion de eventos de la interfaz
        timePicker.setIs24HourView(true)
        npSistolica.minValue = 80
        npSistolica.maxValue = 200
        npDiastolica.minValue = 40
        npDiastolica.maxValue = 130
        npPulso.minValue = 40
        npPulso.maxValue = 180

        // Evento de iteracion del usuario
        btnFecha.setOnClickListener {
            mostrarDatePicker()
        }

        btnAnalizar.setOnClickListener {
            mostrarMedicion()
        }
    }

    /* Metodo para mostar DatePicker que tiene como funcionalidad mostrar
    en el cuadro de dialgo para seleccionar una fecha y almacenarla*/
    private fun mostrarDatePicker() {
        val calendario = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                fechaSeleccionada = "$dayOfMonth/${month + 1}/$year"
                txtFecha.text = "Fecha: $fechaSeleccionada"
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    /*Este metodo se encarga de validar los datos ingresados por el usuario, analizarlos y
    mostrar un resumen de la medición.*/

    private fun mostrarMedicion() {
        // 1. Validaciones previas
        if (fechaSeleccionada == null) {
            Toast.makeText(this, "Debe seleccionar una fecha", Toast.LENGTH_SHORT).show()
            return
        }

        if (rgBrazo.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Debe seleccionar el brazo de medición", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Obtención de valores (Usando .value de los NumberPicker)
        val sistolica = npSistolica.value
        val diastolica = npDiastolica.value
        val pulso = npPulso.value

        // 3. Obtención de hora y brazo
        val hora = String.format("%02d:%02d", timePicker.hour, timePicker.minute)
        val seleccionadoId = rgBrazo.checkedRadioButtonId
        val brazo = findViewById<RadioButton>(seleccionadoId).text

        // 4. Lógica de clasificación
        val clasificacion = clasificarPresion(sistolica, diastolica)

        // 5. Mostrar resultado en el TextView
        // Nota: Se corrigió la interpolación de la variable 'clasificacion'
        txtResultado.text = """ 
        Resumen de la medición 
         
        Fecha: $fechaSeleccionada 
        Hora: $hora 
        Presión: $sistolica / $diastolica mmHg 
        Pulso: $pulso BPM 
        Brazo: $brazo
        Clasificación: $clasificacion 
    """.trimIndent()
    }

    /*Este metodo tiene como propósito clasificar el nivel de presión arterial a partir de los
    valores de presión sistólica y diastólica ingresados por el usuario. */
    private fun clasificarPresion(sistolica: Int, diastolica: Int): String {
        return when {
            sistolica < 90 || diastolica < 60 -> "Presión baja"
            sistolica in 90..119 && diastolica in 60..79 -> "Presión normal"
            sistolica in 120..129 && diastolica < 80 -> "Presión elevada"
            else -> "Hipertensión"
        }
    }
}