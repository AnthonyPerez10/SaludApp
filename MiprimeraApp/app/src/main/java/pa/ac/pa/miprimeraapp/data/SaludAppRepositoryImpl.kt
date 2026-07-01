package pa.ac.pa.miprimeraapp.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Implementación de SaludAppRepository basada en SharedPreferences.
 * Simula una base de datos local serializando listas complejas en cadenas JSON.
 */
class SaludAppRepositoryImpl(context: Context) : SaludAppRepository {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("SaludApp_Prefs", Context.MODE_PRIVATE)

    companion object {
        // Clases de Claves de Preferencias
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_REGISTERED = "is_registered"
        private const val KEY_NOMBRE = "user_nombre"
        private const val KEY_APELLIDO = "user_apellido"
        private const val KEY_EDAD = "user_edad"
        private const val KEY_PASSWORD = "user_password"
        private const val KEY_SHARE_DATA = "user_share_data"

        // Claves Peso
        private const val KEY_WEIGHT_HISTORY = "weight_history_json"

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

        // Claves Medicamentos
        private const val KEY_MED_LIST = "med_list_json"
        private const val KEY_MED_CURRENT_DAY = "med_dia_actual"
        private const val KEY_MED_TAKEN_TODAY = "med_tomas_hoy"

        // Claves Glucosa
        private const val KEY_GLUCOSE_HISTORY = "glucose_history_json"

        // Claves Presión Arterial
        private const val KEY_PRESSURE_HISTORY = "pressure_history_json"
    }

    // --- Perfil de Usuario y Autenticación ---

    override fun registerUser(nombre: String, apellido: String, edad: Int, contrasena: String, shareData: Boolean) {
        sharedPreferences.edit().apply {
            putString(KEY_NOMBRE, nombre)
            putString(KEY_APELLIDO, apellido)
            putInt(KEY_EDAD, edad)
            putString(KEY_PASSWORD, contrasena)
            putBoolean(KEY_SHARE_DATA, shareData)
            putBoolean(KEY_IS_REGISTERED, true)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    override fun loginUser(contrasena: String): Boolean {
        val savedPassword = sharedPreferences.getString(KEY_PASSWORD, null)
        return if (savedPassword == contrasena) {
            sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
            true
        } else {
            false
        }
    }

    override fun isLoggedIn(): Boolean = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)

    override fun isRegistered(): Boolean = sharedPreferences.getBoolean(KEY_IS_REGISTERED, false)

    override fun getNombre(): String = sharedPreferences.getString(KEY_NOMBRE, "") ?: ""

    override fun getApellido(): String = sharedPreferences.getString(KEY_APELLIDO, "") ?: ""

    override fun getEdad(): Int = sharedPreferences.getInt(KEY_EDAD, 0)

    override fun logout() {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
    }

    // --- Control de Peso e IMC ---

    override fun getWeightHistory(): List<RegistroPeso> {
        val list = mutableListOf<RegistroPeso>()
        val jsonStr = sharedPreferences.getString(KEY_WEIGHT_HISTORY, null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        RegistroPeso(
                            fecha = obj.getString("fecha"),
                            peso = obj.getDouble("peso"),
                            imc = obj.getDouble("imc")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    override fun addWeightRecord(record: RegistroPeso) {
        val currentHistory = getWeightHistory().toMutableList()
        currentHistory.add(0, record) // Añadimos al inicio (más reciente primero)
        
        try {
            val array = JSONArray()
            for (item in currentHistory) {
                val obj = JSONObject().apply {
                    put("fecha", item.fecha)
                    put("peso", item.peso)
                    put("imc", item.imc)
                }
                array.put(obj)
            }
            sharedPreferences.edit().putString(KEY_WEIGHT_HISTORY, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Hidratación ---

    override fun getWaterToday(): Int = sharedPreferences.getInt(KEY_WATER_TODAY, 0)

    override fun saveWaterToday(amount: Int) {
        sharedPreferences.edit().putInt(KEY_WATER_TODAY, amount).apply()
    }

    override fun getWaterGoal(): Int = sharedPreferences.getInt(KEY_WATER_GOAL, 2000)

    override fun saveWaterGoal(goal: Int) {
        sharedPreferences.edit().putInt(KEY_WATER_GOAL, goal).apply()
    }

    override fun getWaterHistoryDay(dayIndex: Int): Boolean {
        return sharedPreferences.getBoolean("hid_cumple_dia_$dayIndex", false)
    }

    override fun saveWaterHistoryDay(dayIndex: Int, completed: Boolean) {
        sharedPreferences.edit().putBoolean("hid_cumple_dia_$dayIndex", completed).apply()
    }

    override fun getHydrationWeight(): Float = sharedPreferences.getFloat(KEY_WATER_WEIGHT, 70f)

    override fun saveHydrationWeight(weight: Float) {
        sharedPreferences.edit().putFloat(KEY_WATER_WEIGHT, weight).apply()
    }

    override fun getHydrationIsMale(): Boolean = sharedPreferences.getBoolean(KEY_WATER_IS_MALE, true)

    override fun saveHydrationIsMale(isMale: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_WATER_IS_MALE, isMale).apply()
    }

    override fun getHydrationActivityPos(): Int = sharedPreferences.getInt(KEY_WATER_ACTIVITY_POS, 0)

    override fun saveHydrationActivityPos(pos: Int) {
        sharedPreferences.edit().putInt(KEY_WATER_ACTIVITY_POS, pos).apply()
    }

    override fun getHydrationCurrentDay(): String = sharedPreferences.getString(KEY_WATER_CURRENT_DAY, "") ?: ""

    override fun saveHydrationCurrentDay(day: String) {
        sharedPreferences.edit().putString(KEY_WATER_CURRENT_DAY, day).apply()
    }

    // --- Actividad Física ---

    override fun getPhysicalStepsToday(): Float = sharedPreferences.getFloat(KEY_PHYS_STEPS, 0f)

    override fun savePhysicalStepsToday(steps: Float) {
        sharedPreferences.edit().putFloat(KEY_PHYS_STEPS, steps).apply()
    }

    override fun getPhysicalCaloriesToday(): Float = sharedPreferences.getFloat(KEY_PHYS_CALORIES, 0f)

    override fun savePhysicalCaloriesToday(calories: Float) {
        sharedPreferences.edit().putFloat(KEY_PHYS_CALORIES, calories).apply()
    }

    override fun getPhysicalStreak(): Int = sharedPreferences.getInt(KEY_PHYS_STREAK, 0)

    override fun savePhysicalStreak(streak: Int) {
        sharedPreferences.edit().putInt(KEY_PHYS_STREAK, streak).apply()
    }

    override fun getPhysicalLastDate(): String = sharedPreferences.getString(KEY_PHYS_LAST_DATE, "") ?: ""

    override fun savePhysicalLastDate(date: String) {
        sharedPreferences.edit().putString(KEY_PHYS_LAST_DATE, date).apply()
    }

    override fun getPhysicalCurrentDay(): String = sharedPreferences.getString(KEY_PHYS_CURRENT_DAY, "") ?: ""

    override fun savePhysicalCurrentDay(day: String) {
        sharedPreferences.edit().putString(KEY_PHYS_CURRENT_DAY, day).apply()
    }

    override fun getPhysicalHistoryDay(dayIndex: Int): Boolean {
        return sharedPreferences.getBoolean("actividad_cumple_dia_$dayIndex", false)
    }

    override fun savePhysicalHistoryDay(dayIndex: Int, completed: Boolean) {
        sharedPreferences.edit().putBoolean("actividad_cumple_dia_$dayIndex", completed).apply()
    }

    // --- Medicamentos ---

    override fun getMedications(): List<Medication> {
        val list = mutableListOf<Medication>()
        val jsonStr = sharedPreferences.getString(KEY_MED_LIST, null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Medication(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            doseQty = obj.getInt("doseQty"),
                            doseType = obj.getString("doseType"),
                            frequency = obj.getString("frequency"),
                            durationDays = obj.getInt("durationDays"),
                            initialBoxSize = obj.getInt("initialBoxSize"),
                            inventory = obj.getInt("inventory"),
                            dateRegistered = obj.getString("dateRegistered")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    override fun saveMedications(meds: List<Medication>) {
        try {
            val array = JSONArray()
            for (med in meds) {
                val obj = JSONObject().apply {
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
                array.put(obj)
            }
            sharedPreferences.edit().putString(KEY_MED_LIST, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getMedicationCurrentDay(): String = sharedPreferences.getString(KEY_MED_CURRENT_DAY, "") ?: ""

    override fun saveMedicationCurrentDay(day: String) {
        sharedPreferences.edit().putString(KEY_MED_CURRENT_DAY, day).apply()
    }

    override fun getTakenSlotsToday(): Set<String> {
        return sharedPreferences.getStringSet(KEY_MED_TAKEN_TODAY, emptySet()) ?: emptySet()
    }

    override fun saveTakenSlotsToday(slots: Set<String>) {
        sharedPreferences.edit().putStringSet(KEY_MED_TAKEN_TODAY, slots).apply()
    }

    override fun getMedicationHistoryDay(dayIndex: Int): Boolean {
        return sharedPreferences.getBoolean("med_cumple_dia_$dayIndex", false)
    }

    override fun saveMedicationHistoryDay(dayIndex: Int, completed: Boolean) {
        sharedPreferences.edit().putBoolean("med_cumple_dia_$dayIndex", completed).apply()
    }

    // --- Control de Glucosa ---

    override fun getGlucoseRecords(): List<RegistroGlucosa> {
        val list = mutableListOf<RegistroGlucosa>()
        val jsonStr = sharedPreferences.getString(KEY_GLUCOSE_HISTORY, null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        RegistroGlucosa(
                            valor = obj.getDouble("valor"),
                            tipo = obj.getString("tipo"),
                            notas = obj.getString("notas"),
                            hora = obj.getString("hora"),
                            fecha = obj.getString("fecha")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    override fun addGlucoseRecord(record: RegistroGlucosa) {
        val currentHistory = getGlucoseRecords().toMutableList()
        currentHistory.add(0, record)
        
        try {
            val array = JSONArray()
            for (item in currentHistory) {
                val obj = JSONObject().apply {
                    put("valor", item.valor)
                    put("tipo", item.tipo)
                    put("notas", item.notas)
                    put("hora", item.hora)
                    put("fecha", item.fecha)
                }
                array.put(obj)
            }
            sharedPreferences.edit().putString(KEY_GLUCOSE_HISTORY, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Presión Arterial ---

    override fun getPressureRecords(): List<RegistroPresion> {
        val list = mutableListOf<RegistroPresion>()
        val jsonStr = sharedPreferences.getString(KEY_PRESSURE_HISTORY, null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        RegistroPresion(
                            fecha = obj.getString("fecha"),
                            hora = obj.getString("hora"),
                            sistolica = obj.getInt("sistolica"),
                            diastolica = obj.getInt("diastolica"),
                            pulso = obj.getInt("pulso"),
                            brazo = obj.getString("brazo"),
                            clasificacion = obj.getString("clasificacion")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    override fun addPressureRecord(record: RegistroPresion) {
        val currentHistory = getPressureRecords().toMutableList()
        currentHistory.add(0, record)
        
        try {
            val array = JSONArray()
            for (item in currentHistory) {
                val obj = JSONObject().apply {
                    put("fecha", item.fecha)
                    put("hora", item.hora)
                    put("sistolica", item.sistolica)
                    put("diastolica", item.diastolica)
                    put("pulso", item.pulso)
                    put("brazo", item.brazo)
                    put("clasificacion", item.clasificacion)
                }
                array.put(obj)
            }
            sharedPreferences.edit().putString(KEY_PRESSURE_HISTORY, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
