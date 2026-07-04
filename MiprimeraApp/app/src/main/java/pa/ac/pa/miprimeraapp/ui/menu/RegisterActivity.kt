package pa.ac.pa.miprimeraapp.ui.menu

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.sharedpreferences.SharedPreferencesManager

class RegisterActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etEdad: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbTerms: CheckBox
    private lateinit var btnEnviar: Button
    private lateinit var tvGoToLogin: TextView

    private lateinit var prefsManager: SharedPreferencesManager
    private var fechaNacimientoSeleccionada: String? = null

    private fun mostrarDatePicker() {
        val calendario = java.util.Calendar.getInstance()
        val anio = calendario.get(java.util.Calendar.YEAR)
        val mes = calendario.get(java.util.Calendar.MONTH)
        val dia = calendario.get(java.util.Calendar.DAY_OF_MONTH)

        val datePickerDialog = android.app.DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->
            fechaNacimientoSeleccionada = String.format(java.util.Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, monthOfYear + 1, year)
            etEdad.setText(fechaNacimientoSeleccionada)
            etEdad.error = null
        }, anio - 20, mes, dia)

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun calcularEdad(fecha: String): Int {
        return try {
            val parts = fecha.split("/")
            val day = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val year = parts[2].toInt()

            val today = java.util.Calendar.getInstance()
            val birth = java.util.Calendar.getInstance().apply {
                set(year, month, day)
            }
            var age = today.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
            if (today.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) {
                age--
            }
            if (age < 0) 0 else age
        } catch (e: Exception) {
            25
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Configuración Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefsManager = SharedPreferencesManager(this)

        inicializarVistas()
        setupListeners()
    }

    private fun inicializarVistas() {
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etEdad = findViewById(R.id.etEdad)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cbTerms = findViewById(R.id.cbTerms)
        btnEnviar = findViewById(R.id.btnEnviar)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)
    }

    private fun setupListeners() {
        btnEnviar.setOnClickListener {
            ejecutarRegistro()
        }

        etEdad.setOnClickListener {
            mostrarDatePicker()
        }

        // Configurar toggle para ver la contraseña escrita
        etPassword.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableEnd = etPassword.compoundDrawables[2]
                if (drawableEnd != null && event.rawX >= (etPassword.right - drawableEnd.bounds.width() - etPassword.paddingEnd)) {
                    val isVisible = etPassword.transformationMethod == null
                    if (isVisible) {
                        etPassword.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_view, 0)
                    } else {
                        etPassword.transformationMethod = null
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_close_clear_cancel, 0)
                    }
                    etPassword.setSelection(etPassword.text.length)
                    v.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }

        tvGoToLogin.setOnClickListener {
            // Ir a Login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun ejecutarRegistro() {
        val nombre = etNombre.text.toString().trim()
        val apellido = etApellido.text.toString().trim()
        val edadStr = etEdad.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val termsAccepted = cbTerms.isChecked

        // Validaciones
        if (nombre.isEmpty()) {
            etNombre.error = "El nombre es obligatorio"
            etNombre.requestFocus()
            return
        }

        if (nombre.length > 50) {
            etNombre.error = "El nombre no puede exceder los 50 caracteres"
            etNombre.requestFocus()
            return
        }

        if (apellido.isEmpty()) {
            etApellido.error = "El apellido es obligatorio"
            etApellido.requestFocus()
            return
        }

        if (apellido.length > 50) {
            etApellido.error = "El apellido no puede exceder los 50 caracteres"
            etApellido.requestFocus()
            return
        }

        if (fechaNacimientoSeleccionada == null) {
            etEdad.error = "La fecha de nacimiento es obligatoria"
            etEdad.requestFocus()
            return
        }

        val edad = calcularEdad(fechaNacimientoSeleccionada!!)
        if (edad < 1 || edad > 120) {
            etEdad.error = "Edad inválida (1 a 120 años)"
            etEdad.requestFocus()
            return
        }

        if (email.isEmpty()) {
            etEmail.error = "El correo electrónico es obligatorio"
            etEmail.requestFocus()
            return
        }

        if (email.length > 100) {
            etEmail.error = "El correo electrónico no puede exceder los 100 caracteres"
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Ingresa un correo electrónico válido"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "La contraseña es obligatoria"
            etPassword.requestFocus()
            return
        }

        if (password.length > 32) {
            etPassword.error = "La contraseña no puede exceder los 32 caracteres"
            etPassword.requestFocus()
            return
        }

        if (!isPasswordValid(password)) {
            etPassword.error = "La contraseña debe tener al menos 8 caracteres y contener al menos un carácter especial"
            etPassword.requestFocus()
            return
        }

        if (!termsAccepted) {
            Toast.makeText(this, "Debes aceptar los términos para compartir tus datos", Toast.LENGTH_SHORT).show()
            return
        }

        // Registrar usuario
        prefsManager.registerUser(nombre, apellido, edad, email, password, true)
        prefsManager.saveFechaNacimiento(fechaNacimientoSeleccionada!!)
        
        Toast.makeText(this, "¡Registro completado con éxito!", Toast.LENGTH_LONG).show()

        // Redirigir al menú principal
        val intent = Intent(this, MenuActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun isPasswordValid(password: String): Boolean {
        if (password.length < 8) return false
        val specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?\\~`"
        return password.any { it in specialChars }
    }
}
