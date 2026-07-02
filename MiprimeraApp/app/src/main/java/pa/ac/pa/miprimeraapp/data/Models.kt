package pa.ac.pa.miprimeraapp.data

/**
 * Representa un registro de peso e IMC en el historial.
 */
data class RegistroPeso(
    val fecha: String,
    val peso: Double,
    val imc: Double
)

/**
 * Representa una lectura de glucosa tomada por el usuario.
 */
data class RegistroGlucosa(
    val valor: Double,
    val tipo: String, // Ayunas, Antes de almuerzo, Después de almuerzo, Cena
    val notas: String,
    val hora: String,
    val fecha: String
)

/**
 * Representa un registro de presión arterial y pulso.
 */
data class RegistroPresion(
    val fecha: String,
    val hora: String,
    val sistolica: Int,
    val diastolica: Int,
    val pulso: Int,
    val brazo: String,
    val clasificacion: String
)

/**
 * Representa un medicamento registrado y su inventario disponible.
 */
data class Medication(
    val id: String,
    val name: String,
    val doseQty: Int,
    val doseType: String,
    val frequency: String,
    val durationDays: Int,
    val initialBoxSize: Int,
    var inventory: Int,
    val dateRegistered: String
)
