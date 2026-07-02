package pa.ac.pa.miprimeraapp.ui.menu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.sharedpreferences.SharedPreferencesManager

class LoginActivity : AppCompatActivity() {

    private lateinit var tvWelcomeTitle: TextView
    private lateinit var tvWelcomeSubtitle: TextView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnEnviar: Button
    private lateinit var tvGoToRegister: TextView

    private lateinit var prefsManager: SharedPreferencesManager

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
    }

    private fun setupUI() {
        if (prefsManager.isRegistered()) {
            val nombre = prefsManager.getNombre()
            tvWelcomeTitle.text = "¡Hola, $nombre!"
            tvWelcomeSubtitle.text = "Ingresa tu contraseña para continuar"
            
            // Pre-rellenar con el correo registrado y mantenerlo habilitado/editable
            etEmail.setText(prefsManager.getCorreo())
            etEmail.isEnabled = true
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
}
