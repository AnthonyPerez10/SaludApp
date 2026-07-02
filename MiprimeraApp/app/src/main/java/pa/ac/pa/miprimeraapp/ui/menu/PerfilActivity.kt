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
}
