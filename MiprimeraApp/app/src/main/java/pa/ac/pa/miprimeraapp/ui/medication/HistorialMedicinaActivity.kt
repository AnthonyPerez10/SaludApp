package pa.ac.pa.miprimeraapp.ui.medication

import android.content.Context
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
import pa.ac.pa.miprimeraapp.data.Medication
import pa.ac.pa.miprimeraapp.data.SaludAppRepository
import pa.ac.pa.miprimeraapp.data.SaludAppRepositoryImpl
import java.util.Locale

/**
 * Actividad para mostrar la lista completa de medicamentos registrados (Historial de Medicina).
 * Mantiene la consistencia de diseño y paleta de colores de SaludApp.
 * Permite buscar por nombre y eliminar registros físicamente con diálogo de confirmación.
 */
class HistorialMedicinaActivity : AppCompatActivity() {

    private lateinit var repository: SaludAppRepository
    private val listaRegistros = mutableListOf<Medication>()
    private val listaFiltrada = mutableListOf<Medication>()
    private lateinit var adapter: MedicinaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historial_de_medicina)

        repository = SaludAppRepositoryImpl(this)

        // Configuración Edge-to-Edge window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val lvHistorial = findViewById<ListView>(R.id.listViewHistorial)
        val etSearchHistory = findViewById<EditText>(R.id.etSearchHistory)

        cargarRegistros()

        adapter = MedicinaAdapter(this, listaFiltrada) { record ->
            confirmarEliminacion(record)
        }
        lvHistorial.adapter = adapter

        // Filtrado en tiempo real al escribir en la barra de búsqueda
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
        listaRegistros.addAll(repository.getMedications())

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
                if (item.name.lowercase(Locale.getDefault()).contains(lowercaseQuery)) {
                    listaFiltrada.add(item)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun confirmarEliminacion(record: Medication) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Medicamento")
            .setMessage("¿Estás seguro de que deseas eliminar el medicamento '${record.name}' del historial de medicina?")
            .setPositiveButton("Eliminar") { _, _ ->
                // Eliminar de base de datos física
                repository.deleteMedication(record.id)

                // Actualizar listas locales
                listaRegistros.remove(record)
                listaFiltrada.remove(record)
                adapter.notifyDataSetChanged()

                Toast.makeText(this, "Medicamento eliminado exitosamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Adaptador para poblar la lista de medicamentos en el historial.
     */
    class MedicinaAdapter(
        private val context: Context,
        private val data: List<Medication>,
        private val onDeleteClicked: (Medication) -> Unit
    ) : BaseAdapter() {

        override fun getCount(): Int = data.size
        override fun getItem(position: Int): Any = data[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.ly_historialmedicina, parent, false)

            val item = data[position]

            val tvMedName = view.findViewById<TextView>(R.id.tvMedName)
            val tvMedDoseFrequency = view.findViewById<TextView>(R.id.tvMedDoseFrequency)
            val tvMedDurationInventory = view.findViewById<TextView>(R.id.tvMedDurationInventory)
            val tvMedDateRegistered = view.findViewById<TextView>(R.id.tvMedDateRegistered)
            val btnDeleteRecord = view.findViewById<ImageView>(R.id.btnDeleteRecord)

            tvMedName.text = item.name
            tvMedDoseFrequency.text = "Dosis: ${item.doseQty} ${item.doseType}  •  ${item.frequency}"
            tvMedDurationInventory.text = "Duración: ${item.durationDays} días  •  Inventario: ${item.inventory} un."
            tvMedDateRegistered.text = "Registrado el: ${item.dateRegistered}"

            btnDeleteRecord.setOnClickListener {
                onDeleteClicked(item)
            }

            return view
        }
    }
}
