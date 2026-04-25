package pa.ac.pa.miprimeraapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Control_peso : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_control_peso)
        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/

        //Conexion entre XML y Kotlin
        val etEdad = findViewById<EditText>(R.id.txtNedad)
        val etPeso = findViewById<EditText>(R.id.etPeso)
        val etEstatura = findViewById<EditText>(R.id.etEstatura)
        val swPeso = findViewById<SwitchCompat>(R.id.swPesoUnit)
        val swEstatura = findViewById<SwitchCompat>(R.id.swEstaturaUnit)
        val btnCalcular = findViewById<Button>(R.id.btnCalcular)
        val tvIMC = findViewById<TextView>(R.id.tvIMC)
        val tvPesoIdeal = findViewById<TextView>(R.id.tvPesoIdeal)
        val tvGrasa = findViewById<TextView>(R.id.tvGrasa)
        val tvClasificacion = findViewById<TextView>(R.id.tvClasificacion)

        // Listeners para cambiar los Hints dinámicamente
        swPeso.setOnCheckedChangeListener { _, isChecked ->
            etPeso.hint = if (isChecked) "Peso (Lb)" else "Peso (Kg)"
            etPeso.text.clear()
        }
        swEstatura.setOnCheckedChangeListener { _, isChecked ->
            etEstatura.hint = if (isChecked) "Estatura (in)" else "Estatura (cm)"
            etEstatura.text.clear()
        }

        val isChecked = false
        etEstatura.hint = if (isChecked) "Estatura (in)" else "Estatura (cm)"

        // accion principal: boton calcular
        btnCalcular.setOnClickListener {

            // obtener los valores de los campos de texto
            val sEdad = etEdad.text.toString()
            val sPeso = etPeso.text.toString()
            val sEstatura = etEstatura.text.toString()

            // validacion: verificar que ningun campo este vacio
            if (sEdad.isEmpty() || sPeso.isEmpty() || sEstatura.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // conversion de texto a valores numericos
            val edad = sEdad.toInt()
            var peso = sPeso.toDouble()
            var estatura = sEstatura.toDouble()

            // Añadiendo validaciones reales
            if (edad <=0 || edad > 120){ //Validacion de la edad
                Toast.makeText(this, "Edad inválida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(peso <=0 || peso >= 500){ //Validacion del peso
                Toast.makeText(this, "Peso no valido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(estatura <= 0 || estatura >= 300){
                Toast.makeText(this, "Estatura no valida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // normalizacion de unidades de peso si se requiere
            // convertir de libras a kilogramos usando el factor de conversion
            if (swPeso.isChecked) {
                peso *= 0.453592
            }

            // normalizacion de unidades de estatura si se requiere
            // convertir de pulgadas a centimetros multiplicando por el factor estandar
            if (swEstatura.isChecked) {
                estatura *= 2.54
            }

            // calculos de indicadores de salud
            // convertir estatura a metros para la formula del imc
            val estaturaMetros = estatura / 100

            // formula del indice de masa corporal peso entre estatura al cuadrado
            val imc = peso / (estaturaMetros * estaturaMetros)

            // calculo del peso ideal estimado basado en un imc de 22
            val pesoIdeal = 22 * (estaturaMetros * estaturaMetros)

            // calculo aproximado del porcentaje de grasa corporal
            val grasa = (1.20 * imc) + (0.23 * edad) - 16.2

            // actualizacion de la interfaz de usuario con formato de un decimal
            tvIMC.text = String.format("%.1f", imc)
            tvPesoIdeal.text = String.format("%.1f kg", pesoIdeal)
            tvGrasa.text = String.format("%.1f%%", grasa)

            // mostrar la clasificacion del imc llamando a la funcion auxiliar
            tvClasificacion.text = categorizarIMC(imc)
        }

    }
    // funcion para clasificar el resultado del imc en rangos especificos
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