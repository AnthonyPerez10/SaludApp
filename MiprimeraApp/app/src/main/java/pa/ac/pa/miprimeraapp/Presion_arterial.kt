package pa.ac.pa.miprimeraapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class Presion_arterial : AppCompatActivity() {

    // Variables del XML Presión Arterial Locales
    private lateinit var btnFecha: Button
    private lateinit var btnHora: LinearLayout
    private lateinit var txtHora: TextView
    private lateinit var npSistolica: NumberPicker
    private lateinit var npDiastolica: NumberPicker
    private lateinit var npPulso: NumberPicker
    private lateinit var rgBrazo: RadioGroup
    private lateinit var btnAnalizar: Button

    private lateinit var cardResumen: CardView
    private lateinit var txtResFecha: TextView
    private lateinit var txtResHora: TextView
    private lateinit var txtResSistolica: TextView
    private lateinit var txtResDiastolica: TextView
    private lateinit var txtResPulso: TextView
    private lateinit var txtResBrazo: TextView
    private lateinit var txtResClasificacion: TextView

    private var fechaSeleccionada: String? = null
    private var horaSeleccionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_presion_arterial)

        btnFecha = findViewById(R.id.btnFecha)
        btnHora = findViewById(R.id.btnHora)
        txtHora = findViewById(R.id.txtHora)

        npSistolica = findViewById(R.id.npSistolica)
        npDiastolica = findViewById(R.id.npDiastolica)
        npPulso = findViewById(R.id.npPulso)
        rgBrazo = findViewById(R.id.rgBrazo)
        btnAnalizar = findViewById(R.id.btnAnalizar)

        cardResumen = findViewById(R.id.cardResumen)
        txtResFecha = findViewById(R.id.txtResFecha)
        txtResHora = findViewById(R.id.txtResHora)
        txtResSistolica = findViewById(R.id.txtResSistolica)
        txtResDiastolica = findViewById(R.id.txtResDiastolica)
        txtResPulso = findViewById(R.id.txtResPulso)
        txtResBrazo = findViewById(R.id.txtResBrazo)
        txtResClasificacion = findViewById(R.id.txtResClasificacion)

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

        btnHora.setOnClickListener {
            mostrarTimePicker()
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
                fechaSeleccionada = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                btnFecha.text = "Seleccionar Fecha:\n$fechaSeleccionada"
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    /*Este metodo tiene como propósito clasificar el nivel de presión arterial a partir de los
     valores de presión sistólica y diastólica ingresados por el usuario. */

    private fun mostrarTimePicker() {
        val calendario = Calendar.getInstance()
        val timePicker = TimePickerDialog(
            this,
            android.R.style.Theme_Holo_Light_Dialog,
            { _, hourOfDay, minute ->
                val formato = if (hourOfDay >= 12) "PM" else "AM"
                val hora12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12

                horaSeleccionada = String.format("%02d:%02d %s", hora12, minute, formato)
                txtHora.text = horaSeleccionada
            },
            calendario.get(Calendar.HOUR_OF_DAY),
            calendario.get(Calendar.MINUTE),
            false
        )
        timePicker.window?.setBackgroundDrawableResource(android.R.color.transparent)
        timePicker.show()
    }

    // 1. Validaciones previas
    private fun mostrarMedicion() {
        if (fechaSeleccionada == null) {
            Toast.makeText(this, "Debe seleccionar una fecha", Toast.LENGTH_SHORT).show()
            return
        }

        if (horaSeleccionada == null) {
            Toast.makeText(this, "Debe seleccionar una hora", Toast.LENGTH_SHORT).show()
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

        val seleccionadoId = rgBrazo.checkedRadioButtonId
        val brazo = findViewById<RadioButton>(seleccionadoId).text

        val clasificacion = clasificarPresion(sistolica, diastolica)

        txtResFecha.text = fechaSeleccionada
        txtResHora.text = horaSeleccionada
        txtResSistolica.text = "$sistolica mmHg"
        txtResDiastolica.text = "$diastolica mmHg"
        txtResPulso.text = "$pulso BPM"
        txtResBrazo.text = brazo

        txtResClasificacion.text = clasificacion.uppercase()
        when (clasificacion) {
            "Presión normal" -> txtResClasificacion.setBackgroundColor(Color.parseColor("#4CAF50"))
            "Presión baja" -> txtResClasificacion.setBackgroundColor(Color.parseColor("#2196F3"))
            "Presión elevada" -> txtResClasificacion.setBackgroundColor(Color.parseColor("#FF9800"))
            else -> txtResClasificacion.setBackgroundColor(Color.parseColor("#F44336"))
        }

        cardResumen.visibility = View.VISIBLE
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