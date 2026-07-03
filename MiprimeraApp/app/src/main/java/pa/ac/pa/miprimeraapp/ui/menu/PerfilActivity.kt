package pa.ac.pa.miprimeraapp.ui.menu

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.data.SaludAppRepository
import pa.ac.pa.miprimeraapp.sharedpreferences.SharedPreferencesManager
import java.io.File
import java.io.FileOutputStream
import androidx.appcompat.widget.SwitchCompat
import android.widget.Button
import android.widget.ScrollView

/**
 * Actividad que gestiona la pantalla de perfil del usuario.
 * Muestra los datos de registro y permite cambiar la foto mediante la cámara o la galería,
 * guardándola localmente de manera persistente.
 */
class PerfilActivity : AppCompatActivity() {

    private lateinit var repository: SaludAppRepository
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var tvHeaderName: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileLastName: TextView
    private lateinit var tvProfileAge: TextView
    private lateinit var tvProfileEmail: TextView

    // Activity Result Launcher para la Cámara
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                val path = saveBitmapToInternalStorage(bitmap)
                if (path != null) {
                    repository.saveProfileImagePath(path)
                    ivProfilePhoto.setImageBitmap(bitmap)
                    Toast.makeText(this, "Foto actualizada correctamente", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Activity Result Launcher para la Galería
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val path = saveUriToInternalStorage(uri)
                if (path != null) {
                    repository.saveProfileImagePath(path)
                    // Cargar imagen del archivo local guardado
                    val bitmap = BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        ivProfilePhoto.setImageBitmap(bitmap)
                        Toast.makeText(this, "Foto actualizada correctamente", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)

        repository = SharedPreferencesManager(this)

        // Configurar Edge-to-Edge window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar vistas
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnChangePhoto = findViewById<View>(R.id.btnChangePhoto)
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)
        tvHeaderName = findViewById(R.id.tvHeaderName)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileLastName = findViewById(R.id.tvProfileLastName)
        tvProfileAge = findViewById(R.id.tvProfileAge)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)

        // Volver atrás
        btnBack.setOnClickListener {
            finish()
        }

        // Cambiar foto
        btnChangePhoto.setOnClickListener {
            mostrarOpcionesImagen()
        }

        // Cargar datos en pantalla
        cargarDatosUsuario()

        // Configurar Opciones de Seguridad y Privacidad
        setupSeguridadYPrivacidad()

        // Configurar Toggles de Módulos (Oposición)
        setupTogglesModulos()
    }

    private fun cargarDatosUsuario() {
        val nombre = repository.getNombre()
        val apellido = repository.getApellido()
        val edad = repository.getEdad()
        val correo = repository.getCorreo()

        tvHeaderName.text = "$nombre $apellido"
        tvProfileName.text = nombre
        tvProfileLastName.text = apellido
        tvProfileAge.text = "$edad años"
        tvProfileEmail.text = if (correo.isEmpty()) "No registrado" else correo

        // Cargar foto si ya fue configurada anteriormente
        val path = repository.getProfileImagePath()
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    ivProfilePhoto.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun mostrarOpcionesImagen() {
        val opciones = arrayOf("Tomar foto (Cámara)", "Seleccionar del dispositivo (Galería)", "Cancelar")
        AlertDialog.Builder(this)
            .setTitle("Cambiar foto de perfil")
            .setItems(opciones) { dialog, which ->
                when (which) {
                    0 -> abrirCamara()
                    1 -> abrirGaleria()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap): String? {
        return try {
            val file = File(filesDir, "profile_picture.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveUriToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(filesDir, "profile_picture.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun setupSeguridadYPrivacidad() {
        val swBiometricEnabled = findViewById<SwitchCompat>(R.id.swBiometricEnabled)
        val btnViewPrivacy = findViewById<Button>(R.id.btnViewPrivacy)
        val btnDeleteAllData = findViewById<Button>(R.id.btnDeleteAllData)

        // Vincular switch biometrico
        swBiometricEnabled.isChecked = repository.isBiometricEnabled()
        swBiometricEnabled.setOnCheckedChangeListener { _, isChecked ->
            repository.saveBiometricEnabled(isChecked)
            val msg = if (isChecked) "Acceso biométrico activado" else "Acceso biométrico desactivado"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        // Vincular boton aviso de privacidad
        btnViewPrivacy.setOnClickListener {
            mostrarAvisoPrivacidadDialog()
        }

        // Vincular boton borrar datos
        btnDeleteAllData.setOnClickListener {
            confirmarEliminarTodo()
        }
    }

    private fun setupTogglesModulos() {
        val swModulePeso = findViewById<SwitchCompat>(R.id.swModulePeso)
        val swModulePresion = findViewById<SwitchCompat>(R.id.swModulePresion)
        val swModuleGlucosa = findViewById<SwitchCompat>(R.id.swModuleGlucosa)
        val swModuleActividad = findViewById<SwitchCompat>(R.id.swModuleActividad)
        val swModuleHidratacion = findViewById<SwitchCompat>(R.id.swModuleHidratacion)
        val swModuleMedicina = findViewById<SwitchCompat>(R.id.swModuleMedicina)

        // Configurar modulos con llaves correspondientes
        configurarToggleModulo(swModulePeso, "peso", "Control de Peso")
        configurarToggleModulo(swModulePresion, "presion", "Presión Arterial")
        configurarToggleModulo(swModuleGlucosa, "glucosa", "Control de Glucosa")
        configurarToggleModulo(swModuleActividad, "actividad", "Actividad Física")
        configurarToggleModulo(swModuleHidratacion, "hidratacion", "Hidratación")
        configurarToggleModulo(swModuleMedicina, "medicina", "Medicamentos y Recordatorios")
    }

    private fun configurarToggleModulo(switch: SwitchCompat, key: String, nombreModulo: String) {
        switch.isChecked = repository.isModuleEnabled(key)
        switch.setOnCheckedChangeListener { _, isChecked ->
            repository.saveModuleEnabled(key, isChecked)
            val estado = if (isChecked) "habilitado" else "deshabilitado"
            Toast.makeText(this, "Módulo de $nombreModulo $estado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarAvisoPrivacidadDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_privacy, null)
        // Ocultar checkbox y botones del layout ya que solo es visualización
        dialogView.findViewById<View>(R.id.cbAcceptConsent).visibility = View.GONE
        dialogView.findViewById<View>(R.id.btnReject).visibility = View.GONE
        dialogView.findViewById<View>(R.id.btnAccept).visibility = View.GONE

        // Ajustar el tamaño del scroll del texto para que use todo el espacio disponible
        val tvPrivacyPolicyText = dialogView.findViewById<TextView>(R.id.tvPrivacyPolicyText)
        tvPrivacyPolicyText.text = obtenerTextoAvisoPrivacidad()

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun confirmarEliminarTodo() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ ¡ATENCIÓN!")
            .setMessage("¿Estás completamente seguro de eliminar TODA tu información? Esta acción es irreversible, borrará tu perfil y destruirá la base de datos de salud encriptada local por completo.")
            .setPositiveButton("Eliminar de forma permanente") { _, _ ->
                repository.destroyAllData()
                Toast.makeText(this, "Toda tu información ha sido eliminada permanentemente.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun obtenerTextoAvisoPrivacidad(): String {
        return """
AVISO DE PRIVACIDAD Y CONSENTIMIENTO INFORMADO
(Cumplimiento Ley N° 81 del 26 de marzo de 2019 de la República de Panamá)

SaludApp está comprometida con la protección de tus datos personales, especialmente tus datos sensibles de salud. A continuación se detalla cómo procesamos tu información:

1. RESPONSABLE DEL TRATAMIENTO
El responsable del tratamiento es Anthony Pérez (Desarrollador de SaludApp), con dirección de contacto local y correo electrónico registrado en esta aplicación.

2. FINALIDAD Y PROPORCIONALIDAD (Art. 10 y 11)
Los datos que recolecta la aplicación (peso, edad, estatura, niveles de glucosa en sangre, presión arterial, frecuencia cardíaca, tomas de agua y medicamentos registrados) son tratados con la única finalidad de monitoreo personal y cálculo de tus métricas de salud (como el IMC, meta de hidratación y control diario). No se recopilarán datos que no sean necesarios para este fin específico (como ubicación exacta o número de cédula).

3. SEGURIDAD Y CONFIDENCIALIDAD (Art. 14)
Todos tus datos personales y de salud son almacenados localmente en tu dispositivo móvil. Los datos sensibles están protegidos mediante cifrado local utilizando SQLCipher con una clave única generada por la aplicación, lo que garantiza que tu historial médico no sea legible si el dispositivo es comprometido. Adicionalmente, puedes activar el bloqueo por biometría (huella dactilar o rostro) desde la configuración para proteger físicamente el acceso.

4. CUSTODIA Y TRANSFERENCIA (Art. 12)
SaludApp funciona de manera offline (100% local). El usuario es el único custodio de su información. No compartimos tus datos de salud con terceros ni los transferimos a servidores externos (nube). En caso de integraciones futuras, se solicitará autorización explícita previa y se garantizará una jurisdicción con niveles de protección iguales o superiores a Panamá.

5. DERECHOS ARCO (Art. 15)
Como titular de los datos, puedes ejercer tus derechos en cualquier momento directamente en la app:
- ACCESO: Ver tus historiales y registros médicos desde cada módulo.
- RECTIFICACIÓN: Corregir tu información personal en "Mi Perfil".
- CANCELACIÓN (Eliminación): Borrar permanentemente tu base de datos y restablecer la app mediante el botón "Eliminar toda mi información" en la pantalla de Perfil (Configuración).
- OPOSICIÓN: Habilitar o deshabilitar módulos (como medicamentos/recordatorios) desde la configuración de "Mi Perfil" sin afectar el funcionamiento del resto de la aplicación.

Al presionar "Aceptar y Continuar", otorgas tu consentimiento libre, previo, expreso, informado e inequívoco para que SaludApp trate tus datos de salud bajo los términos expuestos.
""".trimIndent()
    }
}
