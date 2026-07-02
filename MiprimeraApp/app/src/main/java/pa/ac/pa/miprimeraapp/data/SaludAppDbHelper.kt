package pa.ac.pa.miprimeraapp.data

import android.content.Context
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper

class SaludAppDbHelper private constructor(context: Context, password: ByteArray) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    password,
    null,
    DATABASE_VERSION,
    0,
    null,
    null,
    false
) {

    override fun onCreate(db: SQLiteDatabase) {
        // Tabla de peso e IMC
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS weight_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha TEXT,
                peso REAL,
                imc REAL
            )
            """.trimIndent()
        )

        // Tabla de glucosa
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS glucose_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                valor REAL,
                tipo TEXT,
                notas TEXT,
                hora TEXT,
                fecha TEXT
            )
            """.trimIndent()
        )

        // Tabla de presión arterial
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pressure_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha TEXT,
                hora TEXT,
                sistolica INTEGER,
                diastolica INTEGER,
                pulso INTEGER,
                brazo TEXT,
                clasificacion TEXT
            )
            """.trimIndent()
        )

        // Tabla de medicamentos
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS medications (
                id TEXT PRIMARY KEY,
                name TEXT,
                doseQty INTEGER,
                doseType TEXT,
                frequency TEXT,
                durationDays INTEGER,
                initialBoxSize INTEGER,
                inventory INTEGER,
                dateRegistered TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS weight_records")
        db.execSQL("DROP TABLE IF EXISTS glucose_records")
        db.execSQL("DROP TABLE IF EXISTS pressure_records")
        db.execSQL("DROP TABLE IF EXISTS medications")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "saludapp_secure.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var instance: SaludAppDbHelper? = null

        fun getInstance(context: Context, password: ByteArray): SaludAppDbHelper {
            return instance ?: synchronized(this) {
                instance ?: SaludAppDbHelper(context.applicationContext, password).also { instance = it }
            }
        }

        fun clearInstance() {
            instance = null
        }
    }
}
