package pa.ac.pa.miprimeraapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Control_Glucosa : AppCompatActivity() {

    // Declaración de las variables para las vistas
    private lateinit var etGlucoseValue: EditText
    private lateinit var etOptionalNotes: EditText
    private lateinit var rgRecordType: RadioGroup
    private lateinit var btnSaveRecord: Button

    // Lista global para manejar los RadioButtons anidados manualmente
    private lateinit var listaRadioButtons: List<RadioButton>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_control_glucosa)

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

        // Inicializamos los RadioButtons individuales que están ocultos dentro de los LinearLayouts
        listaRadioButtons = listOf(
            findViewById(R.id.rbAyunas),
            findViewById(R.id.rbAntesAlmuerzo),
            findViewById(R.id.rbDespuesAlmuerzo),
            findViewById(R.id.rbCena)
        )
    }

    private fun configurarListeners() {
        // Simular el comportamiento del RadioGroup manualmente para cada botón
        listaRadioButtons.forEach { rb ->
            rb.setOnClickListener { vistaClickeada ->
                listaRadioButtons.forEach { boton ->
                    // Solo se queda marcado el botón que el usuario acaba de presionar
                    boton.isChecked = (boton.id == vistaClickeada.id)
                }
            }
        }

        btnSaveRecord.setOnClickListener {
            ejecutarRegistro()
        }
    }

    private fun ejecutarRegistro() {
        // Obtener textos y limpiar espacios en blanco innecesarios
        val glucosaTexto = etGlucoseValue.text.toString().trim()
        val notasOpcionales = etOptionalNotes.text.toString().trim()

        // Como el RadioGroup está "ciego", buscamos manualmente cuál de nuestros botones está activo
        val selectedRadioId = listaRadioButtons.find { it.isChecked }?.id ?: -1
        val tipoRegistro = obtenerTipoRegistroTexto(selectedRadioId)

        // Ejecutar las validaciones de seguridad
        if (!validarGlucosa(glucosaTexto)) {
            return // Detiene la ejecución si hay errores en los datos
        }

        // Conversión 100% segura tras pasar la validación
        val valorGlucosa = glucosaTexto.toDouble()

        // Procesar la acción final si todo es correcto
        guardarRegistro(valorGlucosa, tipoRegistro, notasOpcionales)
    }

    /**
     * Aplica filtros estrictos para evitar datos inválidos, vacíos o lógicamente erróneos.
     */
    private fun validarGlucosa(texto: String): Boolean {
        // Validación 1: Campo completamente vacío
        if (texto.isEmpty()) {
            etGlucoseValue.error = "El valor de glucosa es obligatorio"
            etGlucoseValue.requestFocus()
            return false
        }

        // Validación 2: Transformación segura (evita caracteres corruptos inesperados)
        val valor = texto.toDoubleOrNull()
        if (valor == null) {
            etGlucoseValue.error = "Ingresa un formato numérico válido"
            etGlucoseValue.requestFocus()
            return false
        }

        // Validación 3: Límites médicos estandarizados (mg/dL)
        if (valor < 20.0 || valor > 600.0) {
            etGlucoseValue.error = "El rango real debe estar entre 20 y 600 mg/dL"
            etGlucoseValue.requestFocus()
            return false
        }

        return true
    }

    /**
     * Convierte el ID seleccionado a un texto comprensible.
     */
    private fun obtenerTipoRegistroTexto(radioId: Int): String {
        return when (radioId) {
            R.id.rbAyunas -> "Ayunas"
            R.id.rbAntesAlmuerzo -> "Antes de Almuerzo"
            R.id.rbDespuesAlmuerzo -> "Después de Almuerzo"
            R.id.rbCena -> "Cena"
            else -> "No definido"
        }
    }

    /**
     * Muestra la confirmación de guardado y resetea los campos.
     */
    private fun guardarRegistro(glucosa: Double, tipo: String, notas: String) {
        val resumen = "Glucosa: $glucosa mg/dL\nTipo: $tipo\nNotas: ${notas.ifEmpty { "Ninguna" }}"

        // Alerta de éxito al usuario
        Toast.makeText(this, "Registro Exitoso:\n$resumen", Toast.LENGTH_LONG).show()

        // Limpiar el formulario para un próximo uso limpio
        etGlucoseValue.text.clear()
        etOptionalNotes.text.clear()

        // Reseteo manual de los botones: marcamos únicamente 'Ayunas' y apagamos el resto
        listaRadioButtons.forEach { boton ->
            boton.isChecked = (boton.id == R.id.rbAyunas)
        }
    }
}