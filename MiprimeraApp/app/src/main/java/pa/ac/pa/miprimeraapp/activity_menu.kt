package pa.ac.pa.miprimeraapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.view.WindowCompat.enableEdgeToEdge
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class activity_menu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) //Control de tema del sistema
        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/

        supportActionBar?.hide()

        // Accion del boton CardV_Peso
        val cardPeso = findViewById<CardView>(R.id.CardV_Peso)
        // Accion del boton CarV_Presion_Arterial
        val cardArterial = findViewById<CardView>(R.id.CarV_Presion_Arterial)
        // Accion del boton CardV_Glucosa
        val cardGlucosa = findViewById<CardView>(R.id.CardV_Glucosa)
        // Accion del boton CardV_Actividad_fisica
        val cardFisico = findViewById<CardView>(R.id.CardV_Control_Fisico)
        // Accion del boton CardV_Hidratacion
        val cardHidratacion = findViewById<CardView>(R.id.CardV_Hidratacion)
        // Accion del boton CardV_Medicamentos
        val cardMedicamentos = findViewById<CardView>(R.id.CardV_medicamentos)


        // Accion de botones
        cardPeso.setOnClickListener { // Control del peso
            val intent = Intent(this, Control_peso::class.java)
            startActivity(intent)
        }

        cardArterial.setOnClickListener { // Control de presion Arterial
            val intent = Intent(this, Presion_arterial::class.java)
            startActivity(intent)
        }

        cardGlucosa.setOnClickListener { // Control de Glucosa
            val intent = Intent(this, Control_Glucosa::class.java)
            startActivity(intent)
        }

        cardFisico.setOnClickListener { // Control de Actividad Fisica
            val intent = Intent(this, Actividad_Fisica::class.java)
            startActivity(intent)
        }

        cardHidratacion.setOnClickListener { // Control de Hidratacion
            val intent = Intent(this, Hidratacion_Activity::class.java)
            startActivity(intent)
        }

        cardMedicamentos.setOnClickListener { // Control de Medicamentos
            val intent = Intent(this, Medicamento_Activity::class.java)
            startActivity(intent)
        }
    }
}