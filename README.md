# SaludApp (Proyecto Universitario) 🏥

SaludApp es una aplicación nativa para Android diseñada para el monitoreo y seguimiento personal de indicadores de salud clave. El proyecto destaca por la implementación de una navegación fluida entre múltiples *Activities* y el uso de componentes modernos de interfaz de usuario para ofrecer una experiencia intuitiva, limpia y profesional.

---

## 🚀 Características Clave

*   **Splash Screen Animado:** Pantalla de bienvenida con identidad visual que optimiza los tiempos de carga iniciales.
*   **Dashboard Centralizado:** Menú principal intuitivo basado en tarjetas para un acceso rápido a los diferentes módulos de control.
*   **Módulo de Control de Peso:** Interfaz optimizada para el registro, control y seguimiento de la masa corporal.
*   **Monitor de Presión Arterial:** Formulario especializado con validaciones en tiempo real para capturar y visualizar datos de presión sistólica y diastólica.
*   **UI/UX Moderna:** Interfaz limpia y minimalista que sigue los lineamientos de Material Design.

---

## 🛠️ Stack Tecnológico y Conceptos Aplicados

*   **Lenguaje:** Kotlin / Java *(Especifica el que uses)*
*   **Diseño de Interfaz:** `ConstraintLayout` para layouts complejos y planos (optimizando el rendimiento), `CardView` y componentes nativos de Material Design.
*   **Navegación Avanzada:** Gestión del flujo de la aplicación mediante `Intents` explícitos y paso de parámetros entre pantallas (*Bundles*).
*   **Ciclo de Vida de Android:** Control y persistencia temporal del estado de las *Activities* durante la navegación y cambios de configuración.
*   **Validación de Datos Robusta:** Control estricto de tipos de entrada de datos (`EditText` numéricos, manejo de excepciones y prevención de campos vacíos).
*   **Entre otras opciones, revisa y clona el repositorio para ver el avance de SaludApp**

---

## ⚙️ Requisitos y Arquitectura

*   **Min SDK:** API 24 (Android 7.0 Nougat) o superior.
*   **Target SDK:** API 34 (Android 14).
*   **Herramienta de Construcción:** Gradle (Kotlin/Groovy DSL).

```text
📦 saludapp
 ┣ 📂 ui
 ┃ ┣ 📜 MainActivity.kt      # Dashboard Principal
 ┃ ┣ 📜 PesoActivity.kt      # Registro de masa corporal
 ┃ ┗ 📜 PresionActivity.kt   # Registro de presión arterial
 ┣ 📂 utils
 ┃ ┗ 📜 Validator.kt         # Lógica de validación de entradas
 ┗ 📜 SplashActivity.kt      # Pantalla de carga inicial
