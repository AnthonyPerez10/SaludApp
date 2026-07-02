package pa.ac.pa.miprimeraapp.data

import android.content.Context
import net.zetetic.database.sqlcipher.SQLiteDatabase

object SecureDatabaseManager {
    private var database: SQLiteDatabase? = null

    @Synchronized
    fun getDatabase(context: Context, passphrase: String): SQLiteDatabase {
        var db = database
        if (db == null || !db.isOpen) {
            // Cargar la librería nativa de SQLCipher
            System.loadLibrary("sqlcipher")
            val dbHelper = SaludAppDbHelper.getInstance(context, passphrase.toByteArray())
            db = dbHelper.writableDatabase
            database = db
        }
        return db!!
    }

    @Synchronized
    fun closeDatabase() {
        database?.close()
        database = null
    }

    @Synchronized
    fun destroyDatabase(context: Context) {
        closeDatabase()
        SaludAppDbHelper.clearInstance()
        context.deleteDatabase(SaludAppDbHelper.DATABASE_NAME)
    }
}
