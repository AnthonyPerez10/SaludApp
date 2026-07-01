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
            
            // Opcional: Si el usuario ya está registrado, podemos ocultar o deshabilitar el input de Email
            // o rellenarlo por conveniencia, pero para cumplir con la presencia del input etEmail lo mantenemos activo.
            etEmail.setText("correo@registrado.com") // Relleno por defecto para agilizar
            etEmail.isEnabled = false // Mantener fijo o deshabilitado si ya está registrado
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

        tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun ejecutarLogin() {
        val password = etPassword.text.toString().trim()

        if (password.isEmpty()) {
            etPassword.error = "La contraseña es obligatoria"
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
