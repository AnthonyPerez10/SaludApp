package pa.ac.pa.miprimeraapp.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import pa.ac.pa.miprimeraapp.R
import pa.ac.pa.miprimeraapp.data.SaludAppRepository
import pa.ac.pa.miprimeraapp.data.SaludAppRepositoryImpl
import java.text.SimpleDateFormat
import java.util.*

/**
 * Servicio en segundo plano que funciona como podómetro.
 * Utiliza el acelerómetro y algoritmos de filtrado por ventana de tiempo y buffer
 * para diferenciar pasos reales de movimientos aleatorios (como ir en vehículo).
 */
class PedometerService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var repository: SaludAppRepository

    // Canal y ID de notificación
    private val CHANNEL_ID = "PedometerServiceChannel"
    private val NOTIFICATION_ID = 1001
    private val STREAK_NOTIFICATION_ID = 1002

    // Variables del algoritmo de filtrado
    private var alpha = 0.9f
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f

    private var stepsToday = 0f
    private var stepsYesterday = 0f

    private val STEP_THRESHOLD = 2.2f // Aceleración mínima en m/s^2 sobre la gravedad
    private var wasAboveThreshold = false

    private var lastStepTime: Long = 0
    private var consecutiveStepsCount = 0
    private val MIN_STEP_DELAY_MS = 250L // Ritmo máximo: 4 pasos por segundo (correr rápido)
    private val MAX_STEP_DELAY_MS = 1500L // Ritmo mínimo: 1 paso cada 1.5 segundos (caminar lento)
    private val PAUSE_RESET_MS = 2000L // Tiempo máximo para considerar que se detuvo

    // Handler para tareas periódicas en segundo plano (Medicamentos)
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val medicationCheckRunnable = object : Runnable {
        override fun run() {
            verificarMedicamentosOmitidos()
            // Re-ejecutar cada 5 minutos
            handler.postDelayed(this, 300000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("PedometerService", "onCreate: Inicializando Servicio del Podómetro")

        repository = SaludAppRepositoryImpl(this)

        // Iniciar ciclo de comprobación periódica de medicamentos omitidos
        handler.post(medicationCheckRunnable)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        crearCanalDeNotificacion()
        inicializarPasos()

        // Iniciar como Foreground Service para asegurar ejecución en segundo plano
        iniciarForegroundService()

        // Registrar listener del acelerómetro
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Log.e("PedometerService", "El acelerómetro no está disponible en este dispositivo.")
        }
    }

    private fun inicializarPasos() {
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val diaGuardado = repository.getPhysicalCurrentDay()

        if (diaGuardado != hoyStr) {
            // Guardar pasos de hoy como ayer
            val pasosAyer = repository.getPhysicalStepsToday()
            repository.savePhysicalStepsYesterday(pasosAyer)
            stepsYesterday = pasosAyer

            // Reiniciar pasos de hoy
            repository.savePhysicalStepsToday(0f)
            stepsToday = 0f
            repository.savePhysicalCurrentDay(hoyStr)
            repository.saveStreakNotificationSentToday(false)
        } else {
            stepsToday = repository.getPhysicalStepsToday()
            stepsYesterday = repository.getPhysicalStepsYesterday()
        }
    }

    private fun iniciarForegroundService() {
        val notificationIntent = Intent() // Intent vacío o que apunte a la actividad
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Podómetro Activo")
            .setContentText("Pasos hoy: ${stepsToday.toInt()}")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun actualizarNotificacion() {
        val notificationIntent = Intent()
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Podómetro Activo")
            .setContentText("Pasos hoy: ${stepsToday.toInt()}")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == "pa.ac.pa.miprimeraapp.ADD_STEPS") {
            val amount = intent.getIntExtra("amount", 1000)
            registrarPasos(amount)
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // 1. Filtrar la gravedad mediante filtro exponencial (Paso Bajo)
        gravityX = alpha * gravityX + (1 - alpha) * event.values[0]
        gravityY = alpha * gravityY + (1 - alpha) * event.values[1]
        gravityZ = alpha * gravityZ + (1 - alpha) * event.values[2]

        // 2. Extraer aceleración lineal (remover componente de gravedad)
        val userX = event.values[0] - gravityX
        val userY = event.values[1] - gravityY
        val userZ = event.values[2] - gravityZ

        // Magnitud de la aceleración lineal del usuario
        val magnitude = Math.sqrt((userX * userX + userY * userY + userZ * userZ).toDouble()).toFloat()

        // 3. Algoritmo de Detección de Pasos con Filtrado de Vehículos
        val now = System.currentTimeMillis()

        if (magnitude > STEP_THRESHOLD) {
            if (!wasAboveThreshold) {
                wasAboveThreshold = true
                val delay = now - lastStepTime

                if (delay > PAUSE_RESET_MS) {
                    // Si ha pasado demasiado tiempo, el usuario se detuvo. Resetear buffer
                    consecutiveStepsCount = 0
                }

                // Verificar si la cadencia de tiempo coincide con caminar/correr real
                if (delay in MIN_STEP_DELAY_MS..MAX_STEP_DELAY_MS) {
                    consecutiveStepsCount++
                    
                    // Buffer consecutivo: require 5 pasos continuos rítmicos para validar el caminar
                    if (consecutiveStepsCount < 5) {
                        // Aún en el buffer de validación (evita ruidos de auto/sacudidas únicas)
                    } else if (consecutiveStepsCount == 5) {
                        // Confirmado caminar rítmico: sumamos los 5 del buffer de golpe
                        registrarPasos(5)
                    } else {
                        // Caminar regular en curso: sumamos 1 a 1
                        registrarPasos(1)
                    }
                    lastStepTime = now
                } else if (delay < MIN_STEP_DELAY_MS) {
                    // Demasiado rápido (posible vibración del motor de auto o rebote). No se cuenta
                } else {
                    // Primer paso después de una pausa
                    lastStepTime = now
                    consecutiveStepsCount = 1
                }
            }
        } else {
            wasAboveThreshold = false
        }
    }

    private fun registrarPasos(cantidad: Int) {
        // Verificar si cambió el día antes de incrementar
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val diaGuardado = repository.getPhysicalCurrentDay()

        if (diaGuardado != hoyStr) {
            inicializarPasos()
        }

        stepsToday += cantidad
        repository.savePhysicalStepsToday(stepsToday)

        actualizarNotificacion()
        evaluarNotificacionRacha()

        // Enviar Broadcast para actualizar UI
        val broadcastIntent = Intent("pa.ac.pa.miprimeraapp.STEP_UPDATE")
        broadcastIntent.putExtra("steps", stepsToday)
        sendBroadcast(broadcastIntent)
    }

    /**
     * Evalúa si los pasos acumulados hoy superan el récord del día de ayer
     * y despacha una notificación emergente única.
     */
    private fun evaluarNotificacionRacha() {
        if (stepsYesterday > 0 && stepsToday > stepsYesterday) {
            if (!repository.getStreakNotificationSentToday()) {
                repository.saveStreakNotificationSentToday(true)
                enviarNotificacionRachaSuperada()
            }
        }
    }

    private fun enviarNotificacionRachaSuperada() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔥 ¡Racha Superada!")
            .setContentText("Has superado tu marca de ayer de ${stepsYesterday.toInt()} pasos. ¡Sigue así!")
            .setSmallIcon(android.R.drawable.star_on)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(STREAK_NOTIFICATION_ID, notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun crearCanalDeNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Canal de Pedometer Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("PedometerService", "onDestroy: Deteniendo servicio y removiendo sensores")
        sensorManager.unregisterListener(this)
        // Detener ciclo de comprobación de medicamentos
        handler.removeCallbacks(medicationCheckRunnable)
    }

    /**
     * Revisa si hay medicamentos cuya hora de toma programada ya pasó para el día de hoy
     * y el usuario aún no la ha marcado como realizada. Lanza notificaciones push.
     */
    private fun verificarMedicamentosOmitidos() {
        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // 1. Verificar cambio de día y limpiar notificaciones enviadas hoy
        val sharedPrefs = getSharedPreferences("SaludAppPrefs", Context.MODE_PRIVATE)
        val diaGuardado = sharedPrefs.getString("meds_dia_verificado", "")
        if (diaGuardado != hoyStr) {
            sharedPrefs.edit()
                .putString("meds_dia_verificado", hoyStr)
                .putStringSet("meds_notificados_hoy", emptySet())
                .apply()
        }

        val notifiedMeds = sharedPrefs.getStringSet("meds_notificados_hoy", emptySet())?.toMutableSet() ?: mutableSetOf()
        val medications = repository.getMedications()
        val takenSlots = repository.getTakenSlotsToday()
        
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        for (med in medications) {
            // Verificar si el tratamiento está activo (no expirado)
            if (med.durationDays > 0) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                try {
                    val dateReg = sdf.parse(med.dateRegistered)
                    if (dateReg != null) {
                        val calLimit = Calendar.getInstance()
                        calLimit.time = dateReg
                        calLimit.add(Calendar.DAY_OF_YEAR, med.durationDays)
                        if (Date().after(calLimit.time)) {
                            continue // Tratamiento terminado
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Mapear slots según frecuencia
            val slots = mutableListOf<Pair<String, Int>>() // Par: label de hora, valor numérico de hora
            when (med.frequency) {
                "Cada 4 horas" -> {
                    slots.add(Pair("08:00 AM", 8))
                    slots.add(Pair("12:00 PM", 12))
                    slots.add(Pair("04:00 PM", 16))
                    slots.add(Pair("08:00 PM", 20))
                    slots.add(Pair("12:00 AM", 24))
                    slots.add(Pair("04:00 AM", 4))
                }
                "Cada 6 horas" -> {
                    slots.add(Pair("06:00 AM", 6))
                    slots.add(Pair("12:00 PM", 12))
                    slots.add(Pair("06:00 PM", 18))
                    slots.add(Pair("12:00 AM", 24))
                }
                "Cada 8 horas", "Cada 8 horas (3 veces)" -> {
                    slots.add(Pair("08:00 AM", 8))
                    slots.add(Pair("04:00 PM", 16))
                    slots.add(Pair("12:00 AM", 24))
                }
                "Cada 12 horas" -> {
                    slots.add(Pair("08:00 AM", 8))
                    slots.add(Pair("08:00 PM", 20))
                }
                "Una vez al día (Mañana)" -> {
                    slots.add(Pair("09:00 AM", 9))
                }
                "Antes de dormir (Noche)" -> {
                    slots.add(Pair("10:00 PM", 22))
                }
                "Un día sí, un día no" -> {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    try {
                        val dateReg = sdf.parse(med.dateRegistered)
                        if (dateReg != null) {
                            val diffTime = Date().time - dateReg.time
                            val diffDays = (diffTime / (1000 * 60 * 60 * 24)).toInt()
                            if (diffDays % 2 == 0) {
                                slots.add(Pair("09:00 AM", 9))
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            for (slot in slots) {
                val label = slot.first
                val hour = slot.second
                val slotKey = "${med.id}_$label"

                // Si la hora ya pasó (con 15 min de tolerancia)
                if (currentHour > hour || (currentHour == hour && currentMinute >= 15)) {
                    // Y no ha marcado que se la ha tomado
                    if (!takenSlots.contains(slotKey)) {
                        // Y no ha sido notificado aún hoy
                        if (!notifiedMeds.contains(slotKey)) {
                            // Enviar notificación recordatoria
                            enviarNotificacionMedicamentoOmitido(med.name, label, med.doseQty, med.doseType)
                            
                            // Marcar como notificado
                            notifiedMeds.add(slotKey)
                            sharedPrefs.edit().putStringSet("meds_notificados_hoy", notifiedMeds).apply()
                        }
                    }
                }
            }
        }
    }

    /**
     * Dispara una notificación de alta importancia indicando la dosis y medicina omitida.
     */
    private fun enviarNotificacionMedicamentoOmitido(medName: String, timeLabel: String, doseQty: Int, doseType: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "MedicationAlertChannel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Medicación",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // Intención al presionar la notificación (redirige a MedicamentoActivity)
        val intent = Intent(this, pa.ac.pa.miprimeraapp.ui.medication.MedicamentoActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Medicamento pendiente: $medName")
            .setContentText("Ya pasó la hora ($timeLabel) para tomar tu dosis de $doseQty $doseType.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
