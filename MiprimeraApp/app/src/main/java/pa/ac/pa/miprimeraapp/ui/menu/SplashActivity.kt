package pa.ac.pa.miprimeraapp.ui.menu

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.sharedpreferences.SharedPreferencesManager

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        supportActionBar?.hide()

        val prefsManager = SharedPreferencesManager(this)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = when {
                !prefsManager.isPrivacyAccepted() -> Intent(this, PrivacyActivity::class.java)
                prefsManager.isLoggedIn() -> Intent(this, MenuActivity::class.java)
                prefsManager.isRegistered() -> Intent(this, LoginActivity::class.java)
                else -> Intent(this, RegisterActivity::class.java)
            }
            startActivity(intent)
            finish()   // Evita que el usuario regrese al Splash
        }, 3000) // Cambiado a 3 segundos para una mejor respuesta de UX
    }
}
