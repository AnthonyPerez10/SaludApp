package pa.ac.pa.miprimeraapp.ui.menu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.sharedpreferences.SharedPreferencesManager

class PrivacyActivity : AppCompatActivity() {

    private lateinit var prefsManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_privacy)

        // Configuración Edge-to-Edge window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefsManager = SharedPreferencesManager(this)

        val tvPrivacyPolicyText = findViewById<TextView>(R.id.tvPrivacyPolicyText)
        val cbAcceptConsent = findViewById<CheckBox>(R.id.cbAcceptConsent)
        val btnReject = findViewById<Button>(R.id.btnReject)
        val btnAccept = findViewById<Button>(R.id.btnAccept)

        // Asignar el texto del aviso de privacidad
        tvPrivacyPolicyText.text = obtenerTextoAvisoPrivacidad()

        // Escuchar cambios en el checkbox para habilitar el botón de aceptación
        cbAcceptConsent.setOnCheckedChangeListener { _, isChecked ->
            btnAccept.isEnabled = isChecked
        }

        // Rechazar e informar que es obligatorio
        btnReject.setOnClickListener {
            Toast.makeText(this, "Para usar la aplicación debes aceptar el aviso de privacidad.", Toast.LENGTH_LONG).show()
            finishAffinity() // Cerrar todas las actividades de la app
        }

        // Aceptar, guardar estado y enrutar
        btnAccept.setOnClickListener {
            prefsManager.savePrivacyAccepted(true)
            Toast.makeText(this, "Consentimiento registrado exitosamente", Toast.LENGTH_SHORT).show()
            
            val intent = when {
                prefsManager.isLoggedIn() -> Intent(this, MenuActivity::class.java)
                prefsManager.isRegistered() -> Intent(this, LoginActivity::class.java)
                else -> Intent(this, RegisterActivity::class.java)
            }
            startActivity(intent)
            finish()
        }
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
