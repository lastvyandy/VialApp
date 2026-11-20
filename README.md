# VialApp

[![Android build and unit tests](https://github.com/lastvyandy/VialApp/actions/workflows/android-tests.yml/badge.svg?branch=master)](https://github.com/lastvyandy/VialApp/actions/workflows/android-tests.yml)

**VialApp** es una aplicación Android orientada a la preparación teórica de las licencias de conducir **Clase B y Clase C en Chile**. Combina contenidos educativos, exámenes de práctica, seguimiento del aprendizaje y actividades interactivas para ayudar a los estudiantes a identificar sus dificultades y avanzar de forma progresiva.

Este repositorio contiene el **cliente Android**. La API y las reglas de negocio del servidor se desarrollan por separado en [VialApp Backend](https://github.com/DevAustral/VialApp_backend).

## Funcionalidades

- **Rutas de aprendizaje:** capítulos y actividades organizados por clase de licencia, con contenido específico para motocicletas en Clase C.
- **Exámenes de práctica:** catálogo por categorías, intentos cronometrados, resultados, correcciones e historial.
- **Práctica personalizada:** recomendaciones de estudio a partir de los resultados y errores registrados.
- **Biblioteca vial:** materiales de consulta y recursos de repaso, incluidos contenidos locales.
- **Progreso:** estadísticas, niveles, experiencia, rachas y medallas.
- **Simulador vial:** escenarios interactivos para practicar decisiones de tránsito.
- **Minijuegos:** actividades educativas y juegos como *Pelao Doom* y *Vial Rally*.
- **Personalización:** perfil, mascotas, coleccionables, garaje, tema claro/oscuro y widget de estudio.

Algunas funcionalidades requieren conexión al servidor, autenticación o acceso Premium. La disponibilidad de exámenes y beneficios se valida desde el backend.

## Tecnologías

| Componente | Tecnología |
| --- | --- |
| Plataforma | Android (API 26 o superior) |
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose y Material 3 |
| Navegación | Navigation Compose |
| Comunicación HTTP | Retrofit y OkHttp |
| Serialización | Gson |
| Carga de imágenes | Coil |
| Compras en Android | Google Play Billing |
| Backend | Python, FastAPI y API REST |
| Compilación y pruebas | Gradle y GitHub Actions |

Los minijuegos tienen implementaciones propias. Por ejemplo, *Pelao Doom* utiliza un motor de juego y un renderizador retro, mientras que *Vial Rally* emplea renderizado OpenGL ES.

## Requisitos de desarrollo

Para compilar el proyecto se necesita:

- **Android Studio** con soporte para Kotlin y Jetpack Compose.
- **JDK 21** para ejecutar Gradle (versión configurada para el daemon del proyecto).
- **Android SDK Platform 36**.
- Un emulador o dispositivo Android con **Android 8.0 (API 26)** o posterior.
- Conexión a internet para descargar dependencias y utilizar las funciones que consultan la API.

El proyecto incluye **Gradle Wrapper**, por lo que no es necesario instalar Gradle manualmente.

## Instalación y ejecución

**1. Clonar el repositorio**

```bash
git clone https://github.com/lastvyandy/VialApp.git
cd VialApp
```

**2. Abrir el proyecto**

En Android Studio, selecciona **Open** y elige la carpeta del repositorio. Espera a que finalice la sincronización de Gradle y confirma que esté instalado Android SDK 36.

**3. Ejecutar la aplicación**

Selecciona un dispositivo o emulador y ejecuta la configuración `app` desde Android Studio.

También puedes generar una APK de depuración desde una terminal:

**Windows (PowerShell):**

```powershell
.\gradlew.bat :app:assembleDebug
```

**macOS / Linux:**

```bash
./gradlew :app:assembleDebug
```

La APK generada se encuentra normalmente en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

> **API:** El cliente utiliza actualmente `https://api.vialapp.cl/api/v1/`, definida en `app/src/main/java/cl/vialapp/app/data/api/ApiClent.kt`. Si necesitas un servidor local o de pruebas, revisa esa configuración. Iniciar el backend localmente no cambia automáticamente la dirección que utiliza Android.

## Estructura del proyecto

El código principal está en `app/src/main/java/cl/vialapp/app/`.

```text
app/
└── src/main/
    ├── java/cl/vialapp/app/
    │   ├── MainActivity.kt         # Punto de entrada de Android
    │   ├── data/
    │   │   ├── api/                # Cliente HTTP y modelos de la API
    │   │   ├── session/            # Gestión de sesión
    │   │   ├── settings/           # Preferencias de estudio
    │   │   └── games/              # Progreso local de los juegos
    │   ├── engagement/             # Rachas, recordatorios y widget
    │   └── ui/
    │       ├── navigation/         # Navegación entre pantallas
    │       ├── home/               # Pantalla de inicio
    │       ├── practice/           # Práctica y recomendaciones
    │       ├── exams/              # Catálogo e historial
    │       ├── exam/               # Realización y corrección
    │       ├── learning/           # Ruta de aprendizaje
    │       ├── library/            # Biblioteca vial
    │       ├── progress/           # Estadísticas y resultados
    │       ├── simulator/          # Simulador de tránsito
    │       ├── games/              # Minijuegos, Doom y Rally
    │       ├── career/             # Garaje y vehículos
    │       ├── profile/            # Perfil y preferencias
    │       └── theme/              # Tema y estilos
    └── res/                        # Recursos de Android
```

Para una descripción más detallada de los componentes y sus responsabilidades, consulta la [guía de desarrollo para colaboradores](docs/GUIA_PARA_COLABORADORES.md).

## Arquitectura y backend

VialApp distingue entre la aplicación Android y los servicios del servidor:

```text
                 Aplicación Android
              (Kotlin / Jetpack Compose)
                          |
                  Retrofit / HTTPS
                          |
                  API REST de VialApp
                  (Python / FastAPI)
                          |
             Usuarios, exámenes y progreso
```

En términos generales:

- **Android** muestra las pantallas, gestiona la interacción y mantiene ciertos datos locales.
- **La API** expone las operaciones de autenticación, aprendizaje, exámenes, progreso y gamificación.
- **El backend** valida permisos, intentos de examen, compras y reglas que no deben depender exclusivamente del dispositivo.

Si una modificación introduce o cambia un endpoint, debe revisarse la compatibilidad entre ambos repositorios. El backend puede requerir un despliegue independiente para que una funcionalidad nueva esté disponible.

## Pruebas

Para ejecutar las pruebas unitarias y compilar la APK de depuración:

**Windows:**

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

**macOS / Linux:**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

El workflow [Android build and unit tests](.github/workflows/android-tests.yml) de GitHub Actions incluye:

- Compilación Android y pruebas unitarias.
- Simulación automatizada del motor de *Pelao Doom*.
- Simulación automatizada del motor de *Vial Rally*.

Las simulaciones comprueban lógica de juego sin necesidad de renderizar la interfaz. Para cambios gráficos, controles o rendimiento es recomendable probar también en un dispositivo físico.

## Colaboración

Las contribuciones al proyecto deben procurar cambios acotados, explicables y verificables.

1. Crea una rama para el cambio que quieras desarrollar.
2. Respeta la estructura existente y reutiliza los componentes disponibles.
3. Verifica que las pantallas funcionen tanto en modo claro como oscuro.
4. Ejecuta las pruebas correspondientes antes de enviar los cambios.
5. Envía un *pull request* con una descripción de lo modificado y de las pruebas realizadas.

Antes de trabajar en autenticación, exámenes, suscripciones o sincronización, revisa qué validaciones pertenecen al backend. Evita incluir credenciales, tokens, datos personales o secretos en el repositorio.

**Documentación adicional:**

- [Guía para colaboradores](docs/GUIA_PARA_COLABORADORES.md)
- [Configuración de Google Sign-In y Google Play](ANDROID_GOOGLE_PLAY.md)
- [Backend de VialApp](https://github.com/DevAustral/VialApp_backend)

## Estado del proyecto

VialApp se encuentra en desarrollo activo. Algunas funcionalidades pueden evolucionar y ciertos servicios dependen de la versión del backend desplegada. Este proyecto tiene fines de preparación y práctica educativa; no sustituye los materiales oficiales ni los requisitos establecidos por las autoridades competentes.
