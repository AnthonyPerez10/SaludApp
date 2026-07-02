package pa.ac.pa.miprimeraapp.data

/**
 * Interfaz que define el repositorio de datos para SaludApp.
 * Al abstraer el acceso a datos en esta interfaz, facilitamos el reemplazo
 * futuro de SharedPreferences por una base de datos local como Room o SQLite.
 */
interface SaludAppRepository {

    // --- Perfil de Usuario y Autenticación ---
    fun registerUser(nombre: String, apellido: String, edad: Int, correo: String, contrasena: String, shareData: Boolean)
    fun loginUser(contrasena: String): Boolean
    fun isLoggedIn(): Boolean
    fun isRegistered(): Boolean
    fun getNombre(): String
    fun getApellido(): String
    fun getEdad(): Int
    fun getCorreo(): String
    fun verifyPassword(password: String): Boolean
    fun updatePassword(newPassword: String)
    fun destroyAllData()
    fun logout()
    fun getProfileImagePath(): String?
    fun saveProfileImagePath(path: String?)
    fun getFechaNacimiento(): String
    fun saveFechaNacimiento(fecha: String)

    // --- Control de Peso e IMC ---
    fun getWeightHistory(): List<RegistroPeso>
    fun addWeightRecord(record: RegistroPeso)
    fun deleteWeightRecord(record: RegistroPeso)

    // --- Hidratación ---
    fun getWaterToday(): Int
    fun saveWaterToday(amount: Int)
    fun getWaterGoal(): Int
    fun saveWaterGoal(goal: Int)
    fun getWaterHistoryDay(dayIndex: Int): Boolean
    fun saveWaterHistoryDay(dayIndex: Int, completed: Boolean)
    fun getHydrationWeight(): Float
    fun saveHydrationWeight(weight: Float)
    fun getHydrationIsMale(): Boolean
    fun saveHydrationIsMale(isMale: Boolean)
    fun getHydrationActivityPos(): Int
    fun saveHydrationActivityPos(pos: Int)
    fun getHydrationCurrentDay(): String
    fun saveHydrationCurrentDay(day: String)

    // --- Actividad Física ---
    fun getPhysicalStepsToday(): Float
    fun savePhysicalStepsToday(steps: Float)
    fun getPhysicalCaloriesToday(): Float
    fun savePhysicalCaloriesToday(calories: Float)
    fun getPhysicalStreak(): Int
    fun savePhysicalStreak(streak: Int)
    fun getPhysicalLastDate(): String
    fun savePhysicalLastDate(date: String)
    fun getPhysicalCurrentDay(): String
    fun savePhysicalCurrentDay(day: String)
    fun getPhysicalHistoryDay(dayIndex: Int): Boolean
    fun savePhysicalHistoryDay(dayIndex: Int, completed: Boolean)
    fun getPhysicalStepsYesterday(): Float
    fun savePhysicalStepsYesterday(steps: Float)
    fun getStreakNotificationSentToday(): Boolean
    fun saveStreakNotificationSentToday(sent: Boolean)

    // --- Medicamentos ---
    fun getMedications(): List<Medication>
    fun saveMedications(meds: List<Medication>)
    fun getMedicationCurrentDay(): String
    fun saveMedicationCurrentDay(day: String)
    fun getTakenSlotsToday(): Set<String>
    fun saveTakenSlotsToday(slots: Set<String>)
    fun getMedicationHistoryDay(dayIndex: Int): Boolean
    fun saveMedicationHistoryDay(dayIndex: Int, completed: Boolean)

    // --- Control de Glucosa ---
    fun getGlucoseRecords(): List<RegistroGlucosa>
    fun addGlucoseRecord(record: RegistroGlucosa)

    // --- Presión Arterial ---
    fun getPressureRecords(): List<RegistroPresion>
    fun addPressureRecord(record: RegistroPresion)
    fun deletePressureRecord(record: RegistroPresion)
}
