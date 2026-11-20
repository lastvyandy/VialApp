# VialApp

Aplicación Android para preparar licencias de conducir en Chile: exámenes, aprendizaje para Clase B y Clase C, seguimiento de progreso, biblioteca, simulación y minijuegos.

**Tecnologías:** Kotlin, Jetpack Compose, Retrofit y backend Python/FastAPI compartido.

### ¿Vienes a ayudar a desarrollar?

Empieza por la [**Guía para colaboradores**](docs/GUIA_PARA_COLABORADORES.md). Ahí está la estructura de carpetas, cómo compilar, cómo se conectan la app y el backend, qué tener presente al modificar exámenes y juegos, y los criterios de comentarios del equipo.

### Ejecutar y probar

Abre el proyecto con Android Studio e instala Android SDK 36. Para ejecutar pruebas y generar una compilación de depuración:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

En Linux/macOS usa `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

El código de servidor está en [VialApp_backend](https://github.com/DevAustral/VialApp_backend). Los endpoints y las políticas de acceso a exámenes son responsabilidad del backend.

> La idea es que estudiar para la licencia sea más claro, práctico y entretenido. Sí, hasta hay un Don Pelao peleando por los servidores. Bienvenidos, jajaj.
