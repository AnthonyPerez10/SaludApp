package pa.ac.pa.miprimeraapp.ui.menu

import android.app.DatePickerDialog
import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.util.Calendar
import java.util.Locale
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.services.BiometricHelper
import pa.ac.pa.miprimeraapp.sharedpreferences.SharedPreferencesManager

class LoginActivity : AppCompatActivity() {

    private lateinit var tvWelcomeTitle: TextView
    private lateinit var tvWelcomeSubtitle: TextView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnEnviar: Button
    private lateinit var tvGoToRegister: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnFingerprint: ImageButton

    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Configuración Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefsManager = SharedPreferencesManager(this)
        biometricHelper = BiometricHelper(this)

        inicializarVistas()
        setupUI()
        setupListeners()
    }

    private fun inicializarVistas() {
        tvWelcomeTitle = findViewById(R.id.tvWelcomeTitle)
        tvWelcomeSubtitle = findViewById(R.id.tvWelcomeSubtitle)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnEnviar = findViewById(R.id.btnEnviar)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        btnFingerprint = findViewById(R.id.btnFingerprint)
    }

    private fun setupUI() {
        if (prefsManager.isRegistered()) {
            val nombre = prefsManager.getNombre()
            tvWelcomeTitle.text = "¡Hola, $nombre!"
            tvWelcomeSubtitle.text = "Ingresa tu contraseña para continuar"
            
            // Pre-rellenar con el correo registrado y mantenerlo habilitado/editable
            etEmail.setText(prefsManager.getCorreo())
            etEmail.isEnabled = true

            // Validar si el dispositivo soporta biometría
            if (biometricHelper.canAuthenticate()) {
                btnFingerprint.visibility = android.view.View.VISIBLE
                // Disparar prompt automáticamente solo si el usuario lo tiene habilitado
                if (prefsManager.isBiometricEnabled()) {
                    iniciarAutenticacionBiometrica()
                }
            }
        } else {
            tvWelcomeTitle.text = "¡Te damos la bienvenida!"
            tvWelcomeSubtitle.text = "Por favor, regístrate para crear tu cuenta"
            
            // Si no está registrado, redirigir automáticamente a RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupListeners() {
        btnEnviar.setOnClickListener {
            ejecutarLogin()
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

        tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        tvForgotPassword.setOnClickListener {
            mostrarDialogoRestablecerContrasena()
        }

        btnFingerprint.setOnClickListener {
            iniciarAutenticacionBiometrica()
        }
    }

    private fun ejecutarLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "El correo electrónico es obligatorio"
            etEmail.requestFocus()
            return
        }

        if (email.length > 100) {
            etEmail.error = "El correo no puede exceder los 100 caracteres"
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

        if (prefsManager.loginUser(password)) {
            Toast.makeText(this, "¡Bienvenido de nuevo!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            etPassword.error = "Contraseña incorrecta"
            etPassword.requestFocus()
        }
    }

    private fun mostrarDialogoRestablecerContrasena() {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        var fechaSeleccionadaDlg: String? = null

        val btnFechaDlg = Button(context).apply {
            text = "Seleccionar Fecha de Nacimiento"
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1A4373"))
            setTextColor(Color.WHITE)
        }

        val tvFechaDlg = TextView(context).apply {
            text = "Ninguna fecha seleccionada"
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(10, 10, 10, 20)
        }

        btnFechaDlg.setOnClickListener {
            val cal = Calendar.getInstance()
            val anio = cal.get(Calendar.YEAR)
            val mes = cal.get(Calendar.MONTH)
            val dia = cal.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(context, { _, year, month, dayOfMonth ->
                fechaSeleccionadaDlg = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year)
                tvFechaDlg.text = "Fecha seleccionada: $fechaSeleccionadaDlg"
            }, anio - 20, mes, dia).apply {
                datePicker.maxDate = System.currentTimeMillis()
                show()
            }
        }

        val etNewPass = EditText(context).apply {
            hint = "Nueva Contraseña (mín. 4 caracteres)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val etConfirmPass = EditText(context).apply {
            hint = "Confirmar Nueva Contraseña"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
        }

        layout.addView(btnFechaDlg)
        layout.addView(tvFechaDlg)
        layout.addView(etNewPass)
        layout.addView(etConfirmPass)

        AlertDialog.Builder(context)
            .setTitle("Restablecer Contraseña")
            .setView(layout)
            .setPositiveButton("Restablecer") { _, _ ->
                val birthdate = fechaSeleccionadaDlg
                val newPass = etNewPass.text.toString().trim()
                val confirm = etConfirmPass.text.toString().trim()

                if (birthdate == null || newPass.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val savedBirthdate = prefsManager.getFechaNacimiento()
                if (savedBirthdate.isEmpty()) {
                    Toast.makeText(context, "No hay fecha de nacimiento registrada para esta cuenta.", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (savedBirthdate != birthdate) {
                    Toast.makeText(context, "La fecha de nacimiento no coincide", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (newPass.length < 4) {
                    Toast.makeText(context, "La contraseña debe tener al menos 4 caracteres", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (newPass.length > 32 || confirm.length > 32) {
                    Toast.makeText(context, "La contraseña no puede exceder los 32 caracteres", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (newPass != confirm) {
                    Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                prefsManager.updatePassword(newPass)
                Toast.makeText(context, "¡Contraseña restablecida con éxito!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun iniciarAutenticacionBiometrica() {
        biometricHelper.showBiometricPrompt(
            title = "Inicio de Sesión Rápido",
            subtitle = "Autentícate con tu huella digital",
            description = "Coloca tu dedo en el sensor para ingresar",
            onSuccess = {
                Toast.makeText(this, "¡Autenticación exitosa!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MenuActivity::class.java)
                startActivity(intent)
                finish()
            },
            onFailure = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
