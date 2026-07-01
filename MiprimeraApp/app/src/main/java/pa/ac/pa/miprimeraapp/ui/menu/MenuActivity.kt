package pa.ac.pa.miprimeraapp.ui.menu

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.ui.weight.ControlPesoActivity
import pa.ac.pa.miprimeraapp.ui.pressure.PresionArterialActivity
import pa.ac.pa.miprimeraapp.ui.glucose.ControlGlucosaActivity
import pa.ac.pa.miprimeraapp.ui.physical.ActividadFisicaActivity
import pa.ac.pa.miprimeraapp.ui.hydration.HidratacionActivity
import pa.ac.pa.miprimeraapp.ui.medication.MedicamentoActivity

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) //Control de tema del sistema

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
            val intent = Intent(this, ControlPesoActivity::class.java)
            startActivity(intent)
        }

        cardArterial.setOnClickListener { // Control de presion Arterial
            val intent = Intent(this, PresionArterialActivity::class.java)
            startActivity(intent)
        }

        cardGlucosa.setOnClickListener { // Control de Glucosa
            val intent = Intent(this, ControlGlucosaActivity::class.java)
            startActivity(intent)
        }

        cardFisico.setOnClickListener { // Control de Actividad Fisica
            val intent = Intent(this, ActividadFisicaActivity::class.java)
            startActivity(intent)
        }

        cardHidratacion.setOnClickListener { // Control de Hidratacion
            val intent = Intent(this, HidratacionActivity::class.java)
            startActivity(intent)
        }

        cardMedicamentos.setOnClickListener { // Control de Medicamentos
            val intent = Intent(this, MedicamentoActivity::class.java)
            startActivity(intent)
        }
    }
}
