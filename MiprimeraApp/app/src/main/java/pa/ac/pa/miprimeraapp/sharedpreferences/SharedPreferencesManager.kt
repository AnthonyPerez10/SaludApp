package pa.ac.pa.miprimeraapp.sharedpreferences

import android.content.Context
import pa.ac.pa.miprimeraapp.data.*

/**
 * Clase SharedPreferencesManager refactorizada.
 * Ahora delega todas sus operaciones en SaludAppRepositoryImpl para mantener
 * la arquitectura modular y desacoplada del motor de almacenamiento.
 */
class SharedPreferencesManager(context: Context) : SaludAppRepository by SaludAppRepositoryImpl(context)
