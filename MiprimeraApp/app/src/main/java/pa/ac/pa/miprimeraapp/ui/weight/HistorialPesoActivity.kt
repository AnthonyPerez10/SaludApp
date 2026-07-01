package pa.ac.pa.miprimeraapp.ui.weight

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.data.RegistroPeso
import pa.ac.pa.miprimeraapp.data.SaludAppRepository
import pa.ac.pa.miprimeraapp.data.SaludAppRepositoryImpl
import java.util.Locale

/**
 * Actividad para mostrar el historial persistido de peso e IMC del usuario.
 * Utiliza SaludAppRepository para cargar el historial real guardado.
 */
class HistorialPesoActivity : AppCompatActivity() {

    private lateinit var repository: SaludAppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historial_de_peso)

        repository = SaludAppRepositoryImpl(this)

        val lvHistorial = findViewById<ListView>(R.id.listViewHistorial)

        // Cargamos la lista real de registros guardados en SharedPreferences a través de la capa modular
        val listaRegistros = repository.getWeightHistory().toMutableList()

        // Si la lista está vacía, podemos añadir registros de ejemplo para poblar inicialmente la vista de forma agradable
        if (listaRegistros.isEmpty()) {
            poblarRegistrosPorDefecto()
            listaRegistros.addAll(repository.getWeightHistory())
        }

        val adapter = PesoAdapter(this, listaRegistros)
        lvHistorial.adapter = adapter
    }

    /**
     * Llena el historial inicial con datos simulados si es la primera vez que se abre la app
     * para asegurar una experiencia visual inicial pulida y realista.
     */
    private fun poblarRegistrosPorDefecto() {
        val mockData = listOf(
            RegistroPeso("29/06/2024", 70.5, 23.9),
            RegistroPeso("14/06/2024", 72.0, 24.4),
            RegistroPeso("30/05/2024", 74.5, 25.2),
            RegistroPeso("15/05/2024", 77.0, 26.1),
            RegistroPeso("30/04/2024", 79.5, 26.9),
            RegistroPeso("15/04/2024", 82.0, 27.8),
            RegistroPeso("31/03/2024", 84.5, 28.6),
            RegistroPeso("16/03/2024", 86.2, 29.2),
            RegistroPeso("01/03/2024", 88.0, 29.8),
            RegistroPeso("15/02/2024", 90.5, 30.7),
            RegistroPeso("31/01/2024", 92.0, 31.2),
            RegistroPeso("16/01/2024", 93.8, 31.8),
            RegistroPeso("01/01/2024", 95.5, 32.4)
        )
        for (item in mockData.reversed()) {
            repository.addWeightRecord(item)
        }
    }

    /**
     * Adaptador personalizado para renderizar la lista de registros de peso en el ListView.
     */
    class PesoAdapter(
        private val context: Context,
        private val data: List<RegistroPeso>
    ) : BaseAdapter() {

        override fun getCount(): Int = data.size

        override fun getItem(position: Int): Any = data[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup?
        ): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.ly_historialpeso, parent, false)

            val item = data[position]

            val ivIcono = view.findViewById<ImageView>(R.id.ivIconoIMC)
            val tvFecha = view.findViewById<TextView>(R.id.tvFecha)
            val tvPesoIMC = view.findViewById<TextView>(R.id.tvPesoIMC)

            // Textos descriptivos del registro
            tvFecha.text = "Fecha: ${item.fecha}"
            tvPesoIMC.text = String.format(Locale.getDefault(), "Peso: %.1f kg | IMC: %.1f", item.peso, item.imc)

            // Asignar el icono visual del IMC según su categoría de salud
            val imc = item.imc
            when {
                imc < 18.5 -> {
                    ivIcono.setImageResource(R.drawable.bajopeso)
                }
                imc < 25.0 -> {
                    ivIcono.setImageResource(R.drawable.normal)
                }
                imc < 30.0 -> {
                    ivIcono.setImageResource(R.drawable.sobrepeso)
                }
                imc < 35.0 -> {
                    ivIcono.setImageResource(R.drawable.obesidad1)
                }
                imc < 40.0 -> {
                    ivIcono.setImageResource(R.drawable.obesidad2)
                }
                else -> {
                    ivIcono.setImageResource(R.drawable.obesidad3)
                }
            }

            return view
        }
    }
}
