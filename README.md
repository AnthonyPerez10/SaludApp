# SaludApp 🏥✨

SaludApp es una aplicación nativa para Android diseñada bajo un paradigma **Offline-First / Local-First** para el monitoreo, seguimiento y control personal de indicadores de salud clave. 

La aplicación combina una interfaz de usuario moderna basada en **Material Design** con un robusto motor de almacenamiento local encriptado, garantizando que el usuario tenga el control absoluto y privado de sus registros médicos en cumplimiento con la **Ley N° 81 de Protección de Datos Personales de la República de Panamá**.

---

## 🚀 Módulos y Características

### 🔒 Registro y Autenticación Segura
*   **Acceso Biométrico Integrado:** Inicio de sesión rápido y seguro mediante huella dactilar o reconocimiento facial utilizando `BiometricPrompt` (API nativa de seguridad).
*   **Restablecimiento de Credenciales:** Mecanismo de recuperación local basado en la verificación de fecha de nacimiento y validación cruzada.
*   **Cumplimiento Legal (Ley N° 81):** Pantalla dedicada al consentimiento previo, informado e inequívoco sobre el procesamiento 100% local de datos sensibles.

### 💧 Control de Hidratación Inteligente
*   **Cálculo Personalizado:** Determina el objetivo diario de agua en mililitros basado en el peso, género y nivel de actividad física del usuario.
*   **Componentes Visuales Personalizados:** Vista interactiva de ola de agua (`WaterWaveView`) para representar dinámicamente el progreso de consumo.

### 🚶‍♂️ Actividad Física (Podómetro de Fondo)
*   **Servicio de Fondo (`PedometerService`):** Registro continuo de pasos utilizando el sensor de reconocimiento de actividad física de Android.
*   **Estadísticas en Tiempo Real:** Cálculo automático de calorías quemadas y racha de días consecutivos cumpliendo la meta.

### 💊 Gestión de Medicamentos e Inventario
*   **Planificador Diario:** Registro de dosis, frecuencias y duraciones de tratamientos.
*   **Monitoreo de Inventario:** Control de unidades restantes en caja y advertencias automáticas cuando el inventario es bajo.

### 🩸 Monitores de Glucosa y Presión Arterial
*   **Clasificación Automática:** Evaluación de registros de presión arterial según estándares médicos (Normal, Elevada, Hipertensión Grado 1/2 y Crisis).
*   **Historiales Interactivos:** Tablas ordenadas para visualización rápida y opción de eliminación de registros individuales.

### ⚖️ Control de Peso e IMC
*   **Cálculo de IMC Automático:** Evaluación en tiempo real del Índice de Masa Corporal y visualización de la clasificación correspondiente.

---

## 🛠️ Arquitectura y Almacenamiento Seguro

SaludApp implementa un diseño desacoplado y orientado al rendimiento, separando la lógica de interfaz del motor de almacenamiento mediante el **Patrón Repositorio**.

```
📦 pa.ac.pa.miprimeraapp
 ┣ 📂 data
 ┃ ┣ 📜 Models.kt                      # Clases de Datos (Medication, RegistroPeso, etc.)
 ┃ ┣ 📜 SaludAppDbHelper.kt            # Manejo de SQLite (Tablas y estructura)
 ┃ ┣ 📜 SecureDatabaseManager.kt       # Inicialización segura de SQLCipher
 ┃ ┣ 📜 SaludAppRepository.kt          # Interfaz del Repositorio de datos
 ┃ ┗ 📜 SaludAppRepositoryImpl.kt      # Implementación central (SQLCipher + EncryptedPrefs)
 ┣ 📂 services
 ┃ ┣ 📜 BiometricHelper.kt             # Integración con Biometric API
 ┃ ┗ 📜 PedometerService.kt            # Servicio de fondo del podómetro
 ┣ 📂 sharedpreferences
 ┃ ┗ 📜 SharedPreferencesManager.kt     # Wrapper de compatibilidad
 ┗ 📂 ui
   ┣ 📂 custom                         # Vistas animadas personalizadas (WaterWaveView)
   ┣ 📂 glucose                        # Pantallas del módulo de Glucosa
   ┣ 📂 hydration                      # Pantallas del módulo de Hidratación
   ┣ 📂 medication                     # Pantallas del módulo de Medicamentos
   ┣ 📂 menu                           # Splash, Login, Registro, Consentimiento y Menú
   ┣ 📂 physical                       # Pantallas del módulo de Podómetro
   ┣ 📂 pressure                       # Pantallas del módulo de Presión Arterial
   ┗ 📂 weight                         # Pantallas del módulo de Peso/IMC
```

### 🔐 Pila de Ciberseguridad

1.  **Cifrado de Base de Datos (SQLCipher):** Toda la información de históricos médicos se almacena localmente encriptada usando SQLCipher con algoritmos AES de 256 bits.
2.  **Cifrado de Preferencias (EncryptedSharedPreferences):** La clave criptográfica aleatoria de la base de datos y los datos del perfil de usuario se almacenan en un almacén cifrado seguro gestionado por Android Keystore (cifrado por hardware).
3.  **Migración Transparente:** La aplicación migra de forma automática los datos anteriores de SharedPreferences inseguras al nuevo motor cifrado seguro en el primer arranque.
4.  **Prevención de Extracción (`allowBackup="false"`):** Desactivación explícita de respaldos del sistema para evitar que los datos sensibles o las claves Keystore sean copiados fuera del dispositivo a nubes públicas.
5.  **Ofuscación y Minificación (ProGuard/R8):** Habilitada en la compilación de Release para mitigar ataques de ingeniería inversa sobre el código fuente de la app.

---

## 🚀 Requisitos de Compilación e Instalación

*   **IDE Recomendada:** Android Studio Ladybug (o superior).
*   **Min SDK:** API 24 (Android 7.0 Nougat).
*   **Target SDK:** API 36 (Android 16).
*   **JDK Compatible:** Java 11 / Java 17 / Java 21 (Recomendado).
*   **Gradle Build System:** Gradle 9.3.1 (Kotlin DSL).

### Pasos para Ejecutar Localmente

1.  Clona este repositorio:
    ```bash
    git clone https://github.com/AnthonyPerez10/SaludApp.git
    ```
2.  Abre el proyecto en Android Studio.
3.  Configura el SDK de compilación y JDK en `Settings > Build, Execution, Deployment > Build Tools > Gradle` seleccionando JDK 21.
4.  Compila y ejecuta la aplicación en un Emulador o Dispositivo Físico compatible.
