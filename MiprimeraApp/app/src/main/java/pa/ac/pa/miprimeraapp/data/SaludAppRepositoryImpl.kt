package pa.ac.pa.miprimeraapp.data

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * Implementación de SaludAppRepository basada en SQLite encriptado (SQLCipher) para históricos
 * y EncryptedSharedPreferences para configuraciones y consultas rápidas de estado.
 */
class SaludAppRepositoryImpl(private val context: Context) : SaludAppRepository {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Almacenamiento Cifrado Seguro para credenciales, contraseñas y claves criptográficas
    private val secureSharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "SaludApp_SecurePrefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // SharedPreferences clásica (para migración)
    private val legacySharedPreferences: SharedPreferences = context.getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)

    init {
        // Ejecutar migración de secureSharedPreferences clásicas a secureSharedPreferences encriptadas si aplica
        migrateLegacySharedPreferencesToSecure()
        // Cargar o generar la contraseña única de la base de datos cifrada
        getOrCreatePassphrase()
        // Migrar datos de secureSharedPreferences a SQLite encriptada en primer inicio
        migrateExistingDataFromSharedPreferences()
    }

    private fun migrateLegacySharedPreferencesToSecure() {
        if (legacySharedPreferences.all.isNotEmpty()) {
            val editor = secureSharedPreferences.edit()
            for ((key, value) in legacySharedPreferences.all) {
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Long -> editor.putLong(key, value)
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        editor.putStringSet(key, value as Set<String>)
                    }
                }
            }
            editor.apply()
            // Limpiar preferencias inseguras heredadas
            legacySharedPreferences.edit().clear().apply()
        }
    }

    private fun getOrCreatePassphrase(): String {
        var key = secureSharedPreferences.getString("db_passphrase", null)
        if (key == null) {
            key = java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID().toString()
            secureSharedPreferences.edit().putString("db_passphrase", key).apply()
        }
        return key
    }

    private fun getDb(): SQLiteDatabase {
        return SecureDatabaseManager.getDatabase(context, getOrCreatePassphrase())
    }

    private fun migrateExistingDataFromSharedPreferences() {
        try {
            val db = getDb()

            // 1. Migrar Peso
            val weightJson = secureSharedPreferences.getString(KEY_WEIGHT_HISTORY, null)
            if (!weightJson.isNullOrEmpty()) {
                try {
                    val array = JSONArray(weightJson)
                    db.beginTransaction()
                    try {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val values = ContentValues().apply {
                                put("fecha", obj.getString("fecha"))
                                put("peso", obj.getDouble("peso"))
                                put("imc", obj.getDouble("imc"))
                            }
                            db.insert("weight_records", null, values)
                        }
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                    secureSharedPreferences.edit().remove(KEY_WEIGHT_HISTORY).apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Migrar Glucosa
            val glucoseJson = secureSharedPreferences.getString(KEY_GLUCOSE_HISTORY, null)
            if (!glucoseJson.isNullOrEmpty()) {
                try {
                    val array = JSONArray(glucoseJson)
                    db.beginTransaction()
                    try {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val values = ContentValues().apply {
                                put("valor", obj.getDouble("valor"))
                                put("tipo", obj.getString("tipo"))
                                put("notas", obj.optString("notas", "")) // compatibilidad de notas
                                put("hora", obj.getString("hora"))
                                put("fecha", obj.getString("fecha"))
                            }
                            db.insert("glucose_records", null, values)
                        }
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                    secureSharedPreferences.edit().remove(KEY_GLUCOSE_HISTORY).apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 3. Migrar Presión
            val pressureJson = secureSharedPreferences.getString(KEY_PRESSURE_HISTORY, null)
            if (!pressureJson.isNullOrEmpty()) {
                try {
                    val array = JSONArray(pressureJson)
                    db.beginTransaction()
                    try {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val values = ContentValues().apply {
                                put("fecha", obj.getString("fecha"))
                                put("hora", obj.getString("hora"))
                                put("sistolica", obj.getInt("sistolica"))
                                put("diastolica", obj.getInt("diastolica"))
                                put("pulso", obj.getInt("pulso"))
                                put("brazo", obj.getString("brazo"))
                                put("clasificacion", obj.getString("clasificacion"))
                            }
                            db.insert("pressure_records", null, values)
                        }
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                    secureSharedPreferences.edit().remove(KEY_PRESSURE_HISTORY).apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 4. Migrar Medicamentos
            val medJson = secureSharedPreferences.getString(KEY_MED_LIST, null)
            if (!medJson.isNullOrEmpty()) {
                try {
                    val array = JSONArray(medJson)
                    db.beginTransaction()
                    try {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val values = ContentValues().apply {
                                put("id", obj.getString("id"))
                                put("name", obj.getString("name"))
                                put("doseQty", obj.getInt("doseQty"))
                                put("doseType", obj.getString("doseType"))
                                put("frequency", obj.getString("frequency"))
                                put("durationDays", obj.getInt("durationDays"))
                                put("initialBoxSize", obj.getInt("initialBoxSize"))
                                put("inventory", obj.getInt("inventory"))
                                put("dateRegistered", obj.getString("dateRegistered"))
                            }
                            db.insert("medications", null, values)
                        }
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                    secureSharedPreferences.edit().remove(KEY_MED_LIST).apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        // Clases de Claves de Preferencias
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_REGISTERED = "is_registered"
        private const val KEY_NOMBRE = "user_nombre"
        private const val KEY_APELLIDO = "user_apellido"
        private const val KEY_EDAD = "user_edad"
        private const val KEY_PASSWORD = "user_password"
        private const val KEY_CORREO = "user_correo"
        private const val KEY_SHARE_DATA = "user_share_data"

        // Claves Históricos en secureSharedPreferences (para migración)
        private const val KEY_WEIGHT_HISTORY = "weight_history_json"
        private const val KEY_GLUCOSE_HISTORY = "glucose_history_json"
        private const val KEY_PRESSURE_HISTORY = "pressure_history_json"
        private const val KEY_MED_LIST = "med_list_json"

        // Claves Hidratación
        private const val KEY_WATER_TODAY = "hid_agua_hoy"
        private const val KEY_WATER_GOAL = "hid_meta_agua"
        private const val KEY_WATER_WEIGHT = "hid_user_weight"
        private const val KEY_WATER_IS_MALE = "hid_user_is_male"
        private const val KEY_WATER_ACTIVITY_POS = "hid_user_activity_pos"
        private const val KEY_WATER_CURRENT_DAY = "hid_dia_actual"

        // Claves Actividad Física
        private const val KEY_PHYS_STEPS = "actividad_pasos_hoy"
        private const val KEY_PHYS_CALORIES = "actividad_calorias_hoy"
        private const val KEY_PHYS_STREAK = "actividad_racha"
        private const val KEY_PHYS_LAST_DATE = "actividad_ultima_fecha"
        private const val KEY_PHYS_CURRENT_DAY = "actividad_dia_actual"

        // Claves Medicamentos (Configuración rápida)
        private const val KEY_MED_CURRENT_DAY = "med_dia_actual"
        private const val KEY_MED_TAKEN_TODAY = "med_tomas_hoy"

        // Claves de Consentimiento, Biometría y Módulos (Ley N° 81)
        private const val KEY_PRIVACY_ACCEPTED = "user_privacy_accepted"
        private const val KEY_BIOMETRIC_ENABLED = "user_biometric_enabled"
        private const val KEY_MODULE_PREFIX = "module_enabled_"
    }

    // --- Perfil de Usuario y Autenticación ---

    override fun registerUser(nombre: String, apellido: String, edad: Int, correo: String, contrasena: String, shareData: Boolean) {
        secureSharedPreferences.edit().apply {
            putString(KEY_NOMBRE, nombre)
            putString(KEY_APELLIDO, apellido)
            putInt(KEY_EDAD, edad)
            putString(KEY_CORREO, correo)
            putString(KEY_PASSWORD, contrasena)
            putBoolean(KEY_SHARE_DATA, shareData)
            putBoolean(KEY_IS_REGISTERED, true)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
        // Asegurar la contraseña de la base de datos cifrada
        getOrCreatePassphrase()
    }

    override fun loginUser(contrasena: String): Boolean {
        val savedPassword = secureSharedPreferences.getString(KEY_PASSWORD, null)
        return if (savedPassword == contrasena) {
            secureSharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
            true
        } else {
            false
        }
    }

    override fun isLoggedIn(): Boolean = secureSharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)

    override fun setLoggedIn(loggedIn: Boolean) {
        secureSharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
    }

    override fun isRegistered(): Boolean = secureSharedPreferences.getBoolean(KEY_IS_REGISTERED, false)

    override fun getNombre(): String = secureSharedPreferences.getString(KEY_NOMBRE, "") ?: ""

    override fun getApellido(): String = secureSharedPreferences.getString(KEY_APELLIDO, "") ?: ""

    override fun getEdad(): Int = secureSharedPreferences.getInt(KEY_EDAD, 0)

    override fun getCorreo(): String = secureSharedPreferences.getString(KEY_CORREO, "") ?: ""

    override fun verifyPassword(password: String): Boolean {
        val saved = secureSharedPreferences.getString(KEY_PASSWORD, null)
        return saved == password
    }

    override fun updatePassword(newPassword: String) {
        secureSharedPreferences.edit().putString(KEY_PASSWORD, newPassword).apply()
    }

    override fun destroyAllData() {
        SecureDatabaseManager.destroyDatabase(context)
        secureSharedPreferences.edit().clear().apply()
    }

    override fun logout() {
        secureSharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
    }

    override fun getProfileImagePath(): String? = secureSharedPreferences.getString("profile_image_path", null)

    override fun saveProfileImagePath(path: String?) {
        secureSharedPreferences.edit().putString("profile_image_path", path).apply()
    }

    override fun getFechaNacimiento(): String = secureSharedPreferences.getString("user_fecha_nacimiento", "") ?: ""

    override fun saveFechaNacimiento(fecha: String) {
        secureSharedPreferences.edit().putString("user_fecha_nacimiento", fecha).apply()
    }

    // --- Control de Peso e IMC ---

    override fun getWeightHistory(): List<RegistroPeso> {
        val list = mutableListOf<RegistroPeso>()
        try {
            val db = getDb()
            val cursor = db.rawQuery("SELECT fecha, peso, imc FROM weight_records ORDER BY id DESC", null)
            if (cursor.moveToFirst()) {
                do {
                    list.add(
                        RegistroPeso(
                            fecha = cursor.getString(0),
                            peso = cursor.getDouble(1),
                            imc = cursor.getDouble(2)
                        )
                    )
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun addWeightRecord(record: RegistroPeso) {
        try {
            val db = getDb()
            val values = ContentValues().apply {
                put("fecha", record.fecha)
                put("peso", record.peso)
                put("imc", record.imc)
            }
            db.insert("weight_records", null, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun deleteWeightRecord(record: RegistroPeso) {
        try {
            val db = getDb()
            db.delete(
                "weight_records",
                "fecha = ? AND peso = ? AND imc = ?",
                arrayOf(record.fecha, record.peso.toString(), record.imc.toString())
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Hidratación ---

    override fun getWaterToday(): Int = secureSharedPreferences.getInt(KEY_WATER_TODAY, 0)

    override fun saveWaterToday(amount: Int) {
        secureSharedPreferences.edit().putInt(KEY_WATER_TODAY, amount).apply()
    }

    override fun getWaterGoal(): Int = secureSharedPreferences.getInt(KEY_WATER_GOAL, 2000)

    override fun saveWaterGoal(goal: Int) {
        secureSharedPreferences.edit().putInt(KEY_WATER_GOAL, goal).apply()
    }

    override fun getWaterHistoryDay(dayIndex: Int): Boolean {
        return secureSharedPreferences.getBoolean("hid_cumple_dia_$dayIndex", false)
    }

    override fun saveWaterHistoryDay(dayIndex: Int, completed: Boolean) {
        secureSharedPreferences.edit().putBoolean("hid_cumple_dia_$dayIndex", completed).apply()
    }

    override fun getHydrationWeight(): Float = secureSharedPreferences.getFloat(KEY_WATER_WEIGHT, 70f)

    override fun saveHydrationWeight(weight: Float) {
        secureSharedPreferences.edit().putFloat(KEY_WATER_WEIGHT, weight).apply()
    }

    override fun getHydrationIsMale(): Boolean = secureSharedPreferences.getBoolean(KEY_WATER_IS_MALE, true)

    override fun saveHydrationIsMale(isMale: Boolean) {
        secureSharedPreferences.edit().putBoolean(KEY_WATER_IS_MALE, isMale).apply()
    }

    override fun getHydrationActivityPos(): Int = secureSharedPreferences.getInt(KEY_WATER_ACTIVITY_POS, 0)

    override fun saveHydrationActivityPos(pos: Int) {
        secureSharedPreferences.edit().putInt(KEY_WATER_ACTIVITY_POS, pos).apply()
    }

    override fun getHydrationCurrentDay(): String = secureSharedPreferences.getString(KEY_WATER_CURRENT_DAY, "") ?: ""

    override fun saveHydrationCurrentDay(day: String) {
        secureSharedPreferences.edit().putString(KEY_WATER_CURRENT_DAY, day).apply()
    }

    // --- Actividad Física ---

    override fun getPhysicalStepsToday(): Float = secureSharedPreferences.getFloat(KEY_PHYS_STEPS, 0f)

    override fun savePhysicalStepsToday(steps: Float) {
        secureSharedPreferences.edit().putFloat(KEY_PHYS_STEPS, steps).apply()
    }

    override fun getPhysicalCaloriesToday(): Float = secureSharedPreferences.getFloat(KEY_PHYS_CALORIES, 0f)

    override fun savePhysicalCaloriesToday(calories: Float) {
        secureSharedPreferences.edit().putFloat(KEY_PHYS_CALORIES, calories).apply()
    }

    override fun getPhysicalStreak(): Int = secureSharedPreferences.getInt(KEY_PHYS_STREAK, 0)

    override fun savePhysicalStreak(streak: Int) {
        secureSharedPreferences.edit().putInt(KEY_PHYS_STREAK, streak).apply()
    }

    override fun getPhysicalLastDate(): String = secureSharedPreferences.getString(KEY_PHYS_LAST_DATE, "") ?: ""

    override fun savePhysicalLastDate(date: String) {
        secureSharedPreferences.edit().putString(KEY_PHYS_LAST_DATE, date).apply()
    }

    override fun getPhysicalCurrentDay(): String = secureSharedPreferences.getString(KEY_PHYS_CURRENT_DAY, "") ?: ""

    override fun savePhysicalCurrentDay(day: String) {
        secureSharedPreferences.edit().putString(KEY_PHYS_CURRENT_DAY, day).apply()
    }

    override fun getPhysicalHistoryDay(dayIndex: Int): Boolean {
        return secureSharedPreferences.getBoolean("actividad_cumple_dia_$dayIndex", false)
    }

    override fun savePhysicalHistoryDay(dayIndex: Int, completed: Boolean) {
        secureSharedPreferences.edit().putBoolean("actividad_cumple_dia_$dayIndex", completed).apply()
    }

    override fun getPhysicalStepsYesterday(): Float = secureSharedPreferences.getFloat("actividad_pasos_ayer", 0f)

    override fun savePhysicalStepsYesterday(steps: Float) {
        secureSharedPreferences.edit().putFloat("actividad_pasos_ayer", steps).apply()
    }

    override fun getStreakNotificationSentToday(): Boolean = secureSharedPreferences.getBoolean("actividad_notif_racha_enviada", false)

    override fun saveStreakNotificationSentToday(sent: Boolean) {
        secureSharedPreferences.edit().putBoolean("actividad_notif_racha_enviada", sent).apply()
    }

    // --- Medicamentos ---

    override fun getMedications(): List<Medication> {
        val list = mutableListOf<Medication>()
        try {
            val db = getDb()
            val cursor = db.rawQuery("SELECT id, name, doseQty, doseType, frequency, durationDays, initialBoxSize, inventory, dateRegistered FROM medications", null)
            if (cursor.moveToFirst()) {
                do {
                    list.add(
                        Medication(
                            id = cursor.getString(0),
                            name = cursor.getString(1),
                            doseQty = cursor.getInt(2),
                            doseType = cursor.getString(3),
                            frequency = cursor.getString(4),
                            durationDays = cursor.getInt(5),
                            initialBoxSize = cursor.getInt(6),
                            inventory = cursor.getInt(7),
                            dateRegistered = cursor.getString(8)
                        )
                    )
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun saveMedications(meds: List<Medication>) {
        try {
            val db = getDb()
            db.beginTransaction()
            try {
                db.delete("medications", null, null)
                for (med in meds) {
                    val values = ContentValues().apply {
                        put("id", med.id)
                        put("name", med.name)
                        put("doseQty", med.doseQty)
                        put("doseType", med.doseType)
                        put("frequency", med.frequency)
                        put("durationDays", med.durationDays)
                        put("initialBoxSize", med.initialBoxSize)
                        put("inventory", med.inventory)
                        put("dateRegistered", med.dateRegistered)
                    }
                    db.insert("medications", null, values)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun deleteMedication(id: String) {
        try {
            val db = getDb()
            db.delete("medications", "id = ?", arrayOf(id))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getMedicationCurrentDay(): String = secureSharedPreferences.getString(KEY_MED_CURRENT_DAY, "") ?: ""

    override fun saveMedicationCurrentDay(day: String) {
        secureSharedPreferences.edit().putString(KEY_MED_CURRENT_DAY, day).apply()
    }

    override fun getTakenSlotsToday(): Set<String> {
        return secureSharedPreferences.getStringSet(KEY_MED_TAKEN_TODAY, emptySet()) ?: emptySet()
    }

    override fun saveTakenSlotsToday(slots: Set<String>) {
        secureSharedPreferences.edit().putStringSet(KEY_MED_TAKEN_TODAY, slots).apply()
    }

    override fun getMedicationHistoryDay(dayIndex: Int): Boolean {
        return secureSharedPreferences.getBoolean("med_cumple_dia_$dayIndex", false)
    }

    override fun saveMedicationHistoryDay(dayIndex: Int, completed: Boolean) {
        secureSharedPreferences.edit().putBoolean("med_cumple_dia_$dayIndex", completed).apply()
    }

    // --- Control de Glucosa ---

    override fun getGlucoseRecords(): List<RegistroGlucosa> {
        val list = mutableListOf<RegistroGlucosa>()
        try {
            val db = getDb()
            val cursor = db.rawQuery("SELECT valor, tipo, notas, hora, fecha FROM glucose_records ORDER BY id DESC", null)
            if (cursor.moveToFirst()) {
                do {
                    list.add(
                        RegistroGlucosa(
                            valor = cursor.getDouble(0),
                            tipo = cursor.getString(1),
                            notas = cursor.getString(2),
                            hora = cursor.getString(3),
                            fecha = cursor.getString(4)
                        )
                    )
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun addGlucoseRecord(record: RegistroGlucosa) {
        try {
            val db = getDb()
            val values = ContentValues().apply {
                put("valor", record.valor)
                put("tipo", record.tipo)
                put("notas", record.notas)
                put("hora", record.hora)
                put("fecha", record.fecha)
            }
            db.insert("glucose_records", null, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Presión Arterial ---

    override fun getPressureRecords(): List<RegistroPresion> {
        val list = mutableListOf<RegistroPresion>()
        try {
            val db = getDb()
            val cursor = db.rawQuery("SELECT fecha, hora, sistolica, diastolica, pulso, brazo, clasificacion FROM pressure_records ORDER BY id DESC", null)
            if (cursor.moveToFirst()) {
                do {
                    list.add(
                        RegistroPresion(
                            fecha = cursor.getString(0),
                            hora = cursor.getString(1),
                            sistolica = cursor.getInt(2),
                            diastolica = cursor.getInt(3),
                            pulso = cursor.getInt(4),
                            brazo = cursor.getString(5),
                            clasificacion = cursor.getString(6)
                        )
                    )
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun addPressureRecord(record: RegistroPresion) {
        try {
            val db = getDb()
            val values = ContentValues().apply {
                put("fecha", record.fecha)
                put("hora", record.hora)
                put("sistolica", record.sistolica)
                put("diastolica", record.diastolica)
                put("pulso", record.pulso)
                put("brazo", record.brazo)
                put("clasificacion", record.clasificacion)
            }
            db.insert("pressure_records", null, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun deletePressureRecord(record: RegistroPresion) {
        try {
            val db = getDb()
            db.delete(
                "pressure_records",
                "fecha = ? AND hora = ? AND sistolica = ? AND diastolica = ? AND pulso = ?",
                arrayOf(
                    record.fecha,
                    record.hora,
                    record.sistolica.toString(),
                    record.diastolica.toString(),
                    record.pulso.toString()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun isPrivacyAccepted(): Boolean = secureSharedPreferences.getBoolean(KEY_PRIVACY_ACCEPTED, false)

    override fun savePrivacyAccepted(accepted: Boolean) {
        secureSharedPreferences.edit().putBoolean(KEY_PRIVACY_ACCEPTED, accepted).apply()
    }

    override fun isBiometricEnabled(): Boolean = secureSharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    override fun saveBiometricEnabled(enabled: Boolean) {
        secureSharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    override fun isModuleEnabled(moduleKey: String): Boolean = secureSharedPreferences.getBoolean(KEY_MODULE_PREFIX + moduleKey, true)

    override fun saveModuleEnabled(moduleKey: String, enabled: Boolean) {
        secureSharedPreferences.edit().putBoolean(KEY_MODULE_PREFIX + moduleKey, enabled).apply()
    }
}


