# Guía para desarrollar VialApp

> Si recién llegas al proyecto, parte por acá. La app creció harto: tiene exámenes, aprendizaje, gamificación y hasta un Pelao con escopeta. La idea es entender dónde está cada cosa antes de tocarla y romper cinco pantallas sin querer, jajaj.

## 1. ¿Qué es VialApp?

VialApp ayuda a preparar las licencias de conducir de Chile, con rutas para Clase B y Clase C, exámenes, correcciones, estadísticas, estudio, simulación y juegos. **No es solo una app de preguntas**: queremos que el alumno sepa qué practicar y por qué.

La arquitectura actual tiene dos repositorios:

- **Android:** [lastvyandy/VialApp](https://github.com/lastvyandy/VialApp), rama `master`, Kotlin + Jetpack Compose.
- **Servidor:** [DevAustral/VialApp_backend](https://github.com/DevAustral/VialApp_backend), rama `main`, Python + FastAPI.

Ambos comparten usuarios, resultados y progreso mediante la API. **No hay que inventar otra base de datos para una pantalla nueva**.

## 2. Antes de empezar

1. Clona el repositorio Android y ábrelo en Android Studio.
2. Usa una instalación de JDK compatible con la configuración Gradle (CI utiliza **Java 21**).
3. Instala **Android SDK 36**. El módulo `app` tiene `minSdk = 26`.
4. Sincroniza Gradle y ejecuta la app en un emulador o teléfono.
5. Para compilar y ejecutar las pruebas desde la raíz:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

En macOS o Linux: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

En GitHub Actions, el workflow `.github/workflows/android-tests.yml` también ejecuta simulaciones de **Pelao Doom** y **Vial Rally** sin interfaz gráfica. Si el botón «Jugar» funciona, pero el juego se cae después de 30 segundos, no basta con decir «compiló, así que está listo».

## 3. Dónde está cada cosa

| Carpeta / archivo | Para qué sirve |
| --- | --- |
| `MainActivity.kt` | Entrada Android, sesión inicial y eventos físicos del mando |
| `ui/navigation/VialAppNavigation.kt` | Destinos y recorrido entre pantallas |
| `data/api/VialApi.kt` | Contrato HTTP del backend; **no** guarda la lógica del negocio |
| `data/api/ApiClent.kt` | Configuración Retrofit/OkHttp, URL del backend y cookies (el nombre conserva una errata histórica) |
| `data/session/` | Sesión, token y lecturas autenticadas |
| `ui/home/` | Inicio, prioridad de estudio y acceso al camino a la licencia |
| `ui/practice/` | Recomendaciones, diagnóstico y entrenamiento personalizado |
| `ui/exams/` | Catálogo, categorías, historial y disponibilidad de exámenes |
| `ui/exam/` | Resolución de un examen, resultado y corrección |
| `ui/learning/` | Camino a la licencia, capítulos y actividades |
| `ui/library/` | Biblioteca y contenidos disponibles sin conexión |
| `ui/progress/` | Estadísticas, últimos resultados y preparación |
| `ui/games/` | Menú y minijuegos |
| `ui/games/doom/` | Motor, pantalla, cinemáticas y gráficos de Pelao Doom |
| `ui/games/rally/` | Física, pistas y renderizado 3D de Vial Rally |
| `data/games/GameProgressStore.kt` | Progreso local y sincronización de juegos, XP y misiones |
| `ui/theme/` | Tema claro/oscuro y colores |
| `engagement/` | Rachas, recordatorios y widget de Android |
| `ui/career/`, `ui/rewards/`, `ui/gamification/` | Garaje, coleccionables y recompensas |

### Dos ejemplos de flujo

**Examen:**

```text
Inicio / Exámenes → ExamsScreen
                 → ExamScreen → API de intentos y respuestas
                 → ExamResultScreen → CorrectionScreen → Progreso
```

No inicies otro intento por accidente cuando uno ya está abierto. El backend decide qué examen puede abrir cada cuenta: que aparezca en el catálogo **no** significa que esté desbloqueado.

**Camino a la licencia:**

```text
Inicio → HomeJourneyCard → LearningPathScreen
       → LearningChapterScreen → LearningActivityScreen
       → backend de aprendizaje → avance persistido
```

Clase C incluye contenidos generales y etapas específicas de motocicleta; si una etapa no aparece, comprueba la **versión desplegada del backend**, no solo el APK.

## 4. Reglas del proyecto que conviene respetar

- **La UI presenta; el backend autoriza.** Nunca desbloquees Premium, exámenes o recompensas basándote solamente en que un botón quedó habilitado.
- **Una pantalla no debería depender de internet para mostrar lo que ya conoce.** Usa estados de carga razonables, errores claros y reintentos.
- **El tema oscuro también existe, po.** No pongas un fondo blanco fijo suponiendo que siempre se verá bien.
- **Clase B y Clase C tienen la misma importancia.** No presentes la ruta de moto como una versión «menos completa» de la de auto.
- **Las respuestas correctas no viven en las pantallas.** Las preguntas del examen real se manejan según las reglas del servidor; los juegos pueden usar su lógica y datos específicos.
- **No dupliques motores de juego o sistemas de progreso** para resolver un fallo pequeño. Separa lógica y renderizado cuando sea posible.
- **No registres tokens, contraseñas, cookies ni datos personales** en Logcat, commits o capturas.
- **Respeta los permisos y licencias de recursos gráficos, audio y textos.**
- Si modificas la API, revisa ambos repositorios y prueba la compatibilidad entre una app actualizada y un servidor todavía sin desplegar.

## 5. Juegos: un aviso especial

**Pelao Doom** separa `DoomEngine.kt` (reglas, enemigos, colisiones, puertas y avance) de `DoomView.kt` (dibujo retro), `DoomScreen.kt` (pantalla y controles) y `DoomStory.kt` (narrativa). El motor usa mayormente Kotlin puro; las vistas y el audio dependen de Android.

**Vial Rally** separa `RallyEngine.kt` (carrera y física) de `RallyRenderer.kt` / `RallyWorld3D.kt` (OpenGL y geometría). Ojo: algo que se ve «más bonito» no sirve si después corre a cinco FPS. Los cambios de renderizado necesitan prueba en **dispositivo real** además de CI.

## 6. Cómo comentar sin llenar el proyecto de ruido

Los comentarios son **en español y con sentido**, como alguien explicándole el código a un compañero:

```kotlin
// Acá traemos los exámenes asignados por páginas.
// Si el servidor no responde, igual dejamos que el alumno vea lo que ya cargó.
```

```kotlin
// Ojo: que un examen aparezca en el catálogo no significa que se pueda rendir.
// Eso lo valida el backend, porque si no sería demasiado fácil hacerse el vivo jajaj.
```

No escribas `// sumamos uno` encima de `contador++`. Comenta **por qué** se hace algo, qué puede fallar y qué parte debes mantener sincronizada.

El humor va en lugares seguros: navegación, juegos, decisiones de diseño. **No** en temas de seguridad, accesibilidad, datos de alumnos, errores graves ni documentación legal.

## 7. Antes de enviar un pull request

- [ ] El cambio tiene un objetivo claro y un commit entendible.
- [ ] No agregaste datos personales, claves ni archivos grandes sin necesidad.
- [ ] La app compila y pasaron los tests locales o CI.
- [ ] Revisaste **modo claro y oscuro**.
- [ ] Probaste navegación, volver atrás, carga y estados sin conexión cuando corresponda.
- [ ] Si modificaste un juego, probaste también los controles y el rendimiento.
- [ ] Si modificaste contenido pedagógico, revisaste la fuente y la clase de licencia.
- [ ] Si tocaste endpoints, dejaste documentado si hay que desplegar el backend.

## 8. Qué hacer si te pierdes

Busca primero la pantalla en `ui/`, sigue su llamada a `VialApi.kt` y revisa el modelo correspondiente en `data/api/`. Si algo depende de progreso, busca `GameProgressStore` o los servicios del backend.

**Regla de oro:** antes de reescribir algo porque «se ve raro», entiende qué hace y por qué existe. Las escaleras de Pelao Doom ya nos enseñaron esa lección a la mala, jajaj.
