package pa.ac.pa.miprimeraapp.sharedpreferences

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("SaludApp_UserPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_REGISTERED = "is_registered"
        private const val KEY_NOMBRE = "user_nombre"
        private const val KEY_APELLIDO = "user_apellido"
        private const val KEY_EDAD = "user_edad"
        private const val KEY_PASSWORD = "user_password"
        private const val KEY_SHARE_DATA = "user_share_data"
    }

    fun registerUser(nombre: String, apellido: String, edad: Int, contrasena: String, shareData: Boolean) {
        sharedPreferences.edit().apply {
            putString(KEY_NOMBRE, nombre)
            putString(KEY_APELLIDO, apellido)
            putInt(KEY_EDAD, edad)
            putString(KEY_PASSWORD, contrasena)
            putBoolean(KEY_SHARE_DATA, shareData)
            putBoolean(KEY_IS_REGISTERED, true)
            putBoolean(KEY_IS_LOGGED_IN, true) // Auto login on register
            apply()
        }
    }

    fun loginUser(contrasena: String): Boolean {
        val savedPassword = sharedPreferences.getString(KEY_PASSWORD, null)
        return if (savedPassword == contrasena) {
            sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
            true
        } else {
            false
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun isRegistered(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_REGISTERED, false)
    }

    fun getNombre(): String {
        return sharedPreferences.getString(KEY_NOMBRE, "") ?: ""
    }

    fun getApellido(): String {
        return sharedPreferences.getString(KEY_APELLIDO, "") ?: ""
    }

    fun getEdad(): Int {
        return sharedPreferences.getInt(KEY_EDAD, 0)
    }

    fun logout() {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
    }
}
