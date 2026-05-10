package pa.ac.pa.miprimeraapp

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
import java.util.Locale

class historial_de_peso : AppCompatActivity() {

    // Modelo de datos
    data class RegistroPeso(
        val fecha: String,
        val peso: Double,
        val imc: Double
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_historial_de_peso)

        // Referencia ListView
        val lvHistorial = findViewById<ListView>(R.id.listViewHistorial)

        // Datos estáticos
        val listaRegistros = listOf(
            RegistroPeso("01/01/2024", 95.5, 42.4),
            RegistroPeso("16/01/2024", 93.8, 31.8),
            RegistroPeso("31/01/2024", 92.0, 31.2),
            RegistroPeso("15/02/2024", 90.5, 30.7),
            RegistroPeso("01/03/2024", 88.0, 29.8),
            RegistroPeso("16/03/2024", 86.2, 29.2),
            RegistroPeso("31/03/2024", 84.5, 28.6),
            RegistroPeso("15/04/2024", 82.0, 27.8),
            RegistroPeso("30/04/2024", 79.5, 26.9),
            RegistroPeso("15/05/2024", 77.0, 26.1),
            RegistroPeso("30/05/2024", 74.5, 25.2),
            RegistroPeso("14/06/2024", 72.0, 24.4),
            RegistroPeso("29/06/2024", 70.5, 23.9),
            RegistroPeso("14/07/2024", 69.0, 23.4),
            RegistroPeso("29/07/2024", 67.5, 22.9)
        )

        // Adapter
        val adapter = PesoAdapter(this, listaRegistros)
        lvHistorial.adapter = adapter
    }

    // Adapter personalizado
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

            // Textos
            tvFecha.text = "Fecha: ${item.fecha}"

            tvPesoIMC.text = String.format(
                Locale.getDefault(),
                "Peso: %.1f kg | IMC: %.1f",
                item.peso,
                item.imc
            )

            // Selección de icono según IMC
            val nombreIcono = when {
                item.imc < 18.5 -> "pesobajo"
                item.imc < 25.0 -> "normal"
                item.imc < 30.0 -> "sobrepeso"
                item.imc < 35.0 -> "obesidad1"
                item.imc < 40.0 -> "obesidad2"
                else -> "obesidad3"
            }

            // Obtener drawable dinámicamente
            val resId = context.resources.getIdentifier(
                nombreIcono,
                "drawable",
                context.packageName
            )

            // Asignar icono
            if (resId != 0) {
                ivIcono.setImageResource(resId)
            } else {
                // Fallback
                ivIcono.setImageResource(android.R.drawable.ic_menu_info_details)
            }

            return view
        }
    }
}