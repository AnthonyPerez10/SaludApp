package pa.ac.pa.miprimeraapp.ui.pressure

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.data.RegistroPresion
import pa.ac.pa.miprimeraapp.data.SaludAppRepository
import pa.ac.pa.miprimeraapp.data.SaludAppRepositoryImpl
import java.util.Locale

/**
 * Actividad para mostrar el historial persistido de presión arterial y pulso del usuario.
 * Ofrece buscador dinámico por fecha y opción de eliminar registros permanentemente.
 */
class HistorialPresionActivity : AppCompatActivity() {

    private lateinit var repository: SaludAppRepository
    private val listaRegistros = mutableListOf<RegistroPresion>()
    private val listaFiltrada = mutableListOf<RegistroPresion>()
    private lateinit var adapter: PresionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historial_de_presion)

        repository = SaludAppRepositoryImpl(this)

        // Configurar Edge-to-Edge window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val lvHistorial = findViewById<ListView>(R.id.listViewHistorial)
        val etSearchHistory = findViewById<EditText>(R.id.etSearchHistory)

        cargarRegistros()

        adapter = PresionAdapter(this, listaFiltrada) { record ->
            confirmarEliminacion(record)
        }
        lvHistorial.adapter = adapter

        etSearchHistory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarHistorial(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun cargarRegistros() {
        listaRegistros.clear()
        listaRegistros.addAll(repository.getPressureRecords())

        // Si la lista está vacía, poblar registros de prueba por defecto
        if (listaRegistros.isEmpty()) {
            poblarRegistrosPorDefecto()
            listaRegistros.addAll(repository.getPressureRecords())
        }

        listaFiltrada.clear()
        listaFiltrada.addAll(listaRegistros)
    }

    private fun filtrarHistorial(query: String) {
        listaFiltrada.clear()
        if (query.isEmpty()) {
            listaFiltrada.addAll(listaRegistros)
        } else {
            val lowercaseQuery = query.lowercase(Locale.getDefault())
            for (item in listaRegistros) {
                if (item.fecha.lowercase(Locale.getDefault()).contains(lowercaseQuery)) {
                    listaFiltrada.add(item)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun confirmarEliminacion(record: RegistroPresion) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Registro")
            .setMessage("¿Estás seguro de que deseas eliminar este registro de presión del historial?")
            .setPositiveButton("Eliminar") { _, _ ->
                // Eliminar de base de datos
                repository.deletePressureRecord(record)

                // Eliminar de las listas locales
                listaRegistros.remove(record)
                listaFiltrada.remove(record)
                adapter.notifyDataSetChanged()

                Toast.makeText(this, "Registro eliminado exitosamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun poblarRegistrosPorDefecto() {
        val mockData = listOf(
            RegistroPresion("29/06/2026", "09:30 AM", 118, 78, 72, "Izquierdo", "Normal"),
            RegistroPresion("14/06/2026", "10:15 AM", 125, 83, 74, "Izquierdo", "Elevada"),
            RegistroPresion("30/05/2026", "08:00 AM", 135, 87, 68, "Derecho", "Hipertensión Grado 1"),
            RegistroPresion("15/05/2026", "09:45 AM", 142, 92, 80, "Izquierdo", "Hipertensión Grado 2")
        )
        for (item in mockData.reversed()) {
            repository.addPressureRecord(item)
        }
    }

    /**
     * Adaptador personalizado para renderizar la lista de registros de presión.
     */
    class PresionAdapter(
        private val context: Context,
        private val data: List<RegistroPresion>,
        private val onDeleteClicked: (RegistroPresion) -> Unit
    ) : BaseAdapter() {

        override fun getCount(): Int = data.size
        override fun getItem(position: Int): Any = data[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.ly_historialpresion, parent, false)

            val item = data[position]

            val tvFechaHora = view.findViewById<TextView>(R.id.tvFechaHora)
            val tvPresionValores = view.findViewById<TextView>(R.id.tvPresionValores)
            val tvBrazo = view.findViewById<TextView>(R.id.tvBrazo)
            val tvClasificacion = view.findViewById<TextView>(R.id.tvClasificacion)
            val btnDeleteRecord = view.findViewById<ImageView>(R.id.btnDeleteRecord)

            tvFechaHora.text = "${item.fecha} ${item.hora}"
            tvPresionValores.text = "${item.sistolica}/${item.diastolica} mmHg  •  Pulso: ${item.pulso} BPM"
            tvBrazo.text = "Brazo ${item.brazo}"
            tvClasificacion.text = item.clasificacion.uppercase(Locale.getDefault())

            // Colorear el badge de clasificación según su rango
            when (item.clasificacion) {
                "Normal" -> {
                    tvClasificacion.setBackgroundColor(Color.parseColor("#2E7D32")) // Verde
                }
                "Elevada" -> {
                    tvClasificacion.setBackgroundColor(Color.parseColor("#F57C00")) // Naranja
                }
                else -> {
                    tvClasificacion.setBackgroundColor(Color.parseColor("#D32F2F")) // Rojo
                }
            }

            btnDeleteRecord.setOnClickListener {
                onDeleteClicked(item)
            }

            return view
        }
    }
}
