# Investigación: arquitectura de la app y librerías base

Fecha: 2026-11-26. Estado: primera revisión.

## Pregunta

¿Cómo organizamos el código y qué librerías usamos para que la app sea fiable, fácil de probar,
ligera en un móvil de 2 GB de RAM, accesible y compatible con la licencia GPL-3?

## Criterios de selección

1. **Estable:** versión 1.0 o superior y mantenida activamente. Nada en fase alfa o beta en el código
   que llega al usuario.
2. **Licencia compatible con GPL-3** (Apache-2.0, MIT, BSD), comprobada en el repositorio. Las
   herramientas que solo se usan para compilar o probar no se distribuyen con la app, así que su
   licencia no afecta (por ejemplo, JUnit 5 es EPL-2.0).
3. **Sin Google Play Services** (principio 8).
4. **Pocas dependencias.** Cada librería es código que puede fallar, que hay que actualizar y que
   ocupa espacio. Si la plataforma lo resuelve bien, no se añade librería.

## Interfaz: Jetpack Compose o Views

- Según las pruebas de rendimiento de Google, Compose 1.9 o superior **iguala a Views en fluidez**
  al desplazarse. En versiones anteriores había una pequeña desventaja.
- Compose no viene precargado en el sistema como Views, así que el arranque en frío puede ser algo más
  lento. Se compensa con los perfiles de referencia (Baseline Profiles), que precompilan el código
  crítico; Google informa de mejoras de alrededor del 25-30 % en el arranque.
- Compose tiene comprobaciones automáticas de accesibilidad en los tests (Accessibility Test
  Framework, desde Compose 1.8). Limitación: requieren API 34 y no funcionan con Robolectric, así que
  se ejecutan en el emulador.
- Google recomienda medir el rendimiento solo con compilaciones de publicación y en dispositivos reales.

Conclusión: **Compose**, con perfiles de referencia y R8, midiendo el arranque en el Galaxy A20.

## Mapa dentro de Compose

| Opción | Licencia | Estado | Valoración |
|---|---|---|---|
| MapLibre Native para Android directamente, dentro de `AndroidView` | BSD-2 | Estable, versión 13.8.0 (2026-11-26) | Elegida. Es la librería oficial. |
| MapLibre Compose (envoltorio para Compose Multiplatform) | BSD-3 | Versión 0.20.0, todavía no 1.0 | Descartada por ahora: no es estable. Se puede revisar cuando llegue a 1.0, porque facilitaría iOS. |

La librería nativa de MapLibre es la parte más pesada de la app. Hay que distribuir un APK por
arquitectura de procesador (ABI) y medir el tamaño real. Pendiente: comprobar la arquitectura del
Galaxy A20 con `adb shell getprop ro.product.cpu.abilist`.

## Organización del código

- **Arquitectura recomendada por Google:** capa de interfaz (pantallas y ViewModel), capa de dominio
  opcional y capa de datos (repositorios). Flujo de datos en una sola dirección: el estado baja y los
  eventos suben. Es lo que usa la app de referencia de Google, Now in Android (Apache-2.0).
- **Módulos de Gradle:** separar el código en módulos obliga a respetar las dependencias entre
  capas, acelera la compilación y permite probar cada parte por separado.
- **Kotlin puro donde se pueda:** la lógica que no necesita Android (protocolo de la malla, formato de
  los paquetes, validación de datos, reglas de la guía) va en módulos de Kotlin puro. Tienen tres ventajas:
  - sus tests se ejecutan en segundos, sin emulador;
  - el generador de paquetes (decisión 0006) puede reutilizar el mismo código del formato;
  - el día que se haga la versión de iOS, esos módulos pasan a Kotlin Multiplatform casi sin cambios.

## Inyección de dependencias

| Opción | Valoración |
|---|---|
| Hilt (Dagger) | Recomendación de Google, comprobada al compilar. Añade generación de código, más tiempo de compilación y bastante "magia" que hay que aprender. |
| Koin | Sencilla, pero los errores aparecen al ejecutar, no al compilar. Para una app que no puede fallar, es un mal intercambio. |
| Manual (constructores y un contenedor de la aplicación) | Sin librerías, comprobada al compilar, explícita y fácil de leer. Google la documenta como opción válida para apps pequeñas y medianas. |

Conclusión: **inyección manual** al principio. Si el contenedor crece demasiado, se migra a Hilt con
una decisión nueva. La migración es sencilla porque ambas usan inyección por constructor.

## Almacenamiento local

- **Room** (AndroidX, Apache-2.0) para puntos de interés, mensajes y datos personales. Es una capa
  sobre SQLite, comprueba las consultas al compilar y ya es compatible con Kotlin Multiplatform.
- **DataStore** (AndroidX) para preferencias sencillas.
- **Búsquedas por cercanía:** un paquete municipal tiene como mucho unos miles de puntos. Basta con
  un índice por latitud y longitud y calcular la distancia en Kotlin. No hace falta una base de datos espacial.
- **Cifrado de datos sensibles:** la librería `androidx.security:security-crypto` está **obsoleta**
  desde la versión 1.1.0. La alternativa habitual es Tink (Google, Apache-2.0) con claves en el
  almacén seguro del sistema (Android Keystore). Riesgo detectado: Google retiró la librería anterior en
  parte por comportamientos distintos del almacén seguro según fabricante y versión. Si la clave se
  invalida, los datos cifrados se pierden. Esto afecta a la ficha médica y merece su propia decisión
  cuando se diseñe esa función.

## Red y tareas en segundo plano

- **Descargas de paquetes:** WorkManager (AndroidX) para que la descarga sobreviva aunque se cierre
  la app. Funciona sin Google Play Services. Para la descarga en sí, OkHttp (Square, Apache-2.0), que
  permite reanudar descargas con peticiones parciales.
- **JSON (índice y GeoJSON):** kotlinx.serialization (JetBrains, Apache-2.0). Funciona en Kotlin puro
  y en Multiplatform, sin reflexión.
- **Malla:** servicio en primer plano de tipo `connectedDevice` (investigación 04).

## Errores en producción sin rastreo (principio 8)

Prohibido usar servicios como Firebase Crashlytics. Alternativa: **ACRA** (Apache-2.0). Guarda el
informe de error en el móvil y, solo si el usuario quiere, lo envía por correo o lo comparte. Sin
servidor y sin enviar nada automáticamente.

## Calidad del código y tests

| Herramienta | Licencia | Última versión estable | Papel |
|---|---|---|---|
| JUnit 5 + kotlin.test | EPL-2.0 / Apache-2.0 | Activa | Tests de los módulos de Kotlin puro. Solo se usa en pruebas, no se distribuye. |
| JUnit 4 | EPL-1.0 | Estable | Solo donde Android lo exige (Robolectric y tests en el emulador). |
| kotlinx-coroutines-test y Turbine | Apache-2.0 | Activa | Probar corrutinas y flujos de datos. |
| Robolectric | MIT | 4.18.1 (2026-11-18) | Tests de código Android sin emulador. |
| Kotest (pruebas basadas en propiedades) | Apache-2.0 | Activa | Genera miles de entradas aleatorias. Ideal para el decodificador de paquetes de la malla, que recibe datos de desconocidos. |
| MockK | Apache-2.0 | Activa | Solo cuando no se pueda usar un objeto falso escrito a mano. Se prefieren los falsos: los tests quedan más claros. |
| Kover | Apache-2.0 | 0.9.11 (2026-09-29) | Mide la cobertura de tests y puede hacer fallar la integración continua si baja de un umbral. |
| ktlint | MIT | 1.8.0 (2025-11-14) | Formato del código uniforme y automático. |
| Android Lint | Apache-2.0 | Incluida en Android | Errores típicos de Android, accesibilidad básica y rendimiento. |
| detekt | Apache-2.0 | **1.23.8 (2025-02-21)**; la 2.0 sigue en alfa | Reglas de código limpio: longitud de funciones, complejidad, nombres y números mágicos. |

Problema con detekt: su última versión estable es de febrero de 2025 y no sigue bien las versiones
nuevas de Kotlin (la actual es la 2.4.21). Hay quejas públicas de proyectos que no pueden actualizar
Kotlin por culpa de detekt. La 2.0 está en alfa. Opción propuesta: ejecutarlo como herramienta de
línea de comandos independiente, sin integrarlo en la compilación de Gradle. Así no condiciona la
versión de Kotlin del proyecto y, si falla, solo afecta a la comprobación, nunca a la app. Cuando
salga la 2.0 estable, se integra.

Comprobación propia adicional: un script en la integración continua que rechace emojis en el código,
los commits y la documentación (norma del proyecto).

## Fuentes

- https://developer.android.com/develop/ui/compose/performance
- https://developer.android.com/develop/ui/compose/migrate/compare-metrics
- https://developer.android.com/develop/ui/compose/accessibility/testing
- https://maplibre.org/maplibre-compose/
- https://github.com/maplibre/maplibre-native (versiones publicadas)
- https://github.com/android/nowinandroid
- https://developer.android.com/training/dependency-injection/manual
- https://developer.android.com/reference/androidx/security/crypto/package-summary
- https://android.googlesource.com/platform/frameworks/support/+/3724241aab8a48dbfd5cc147869350fd6511bb97%5E%21
- https://detekt.dev/changelog-2.0.0/
- https://slack-chats.kotlinlang.org/t/32887194/
- Licencias y versiones comprobadas con la API de GitHub el 2026-11-26: maplibre-native, maplibre-compose,
  detekt, ktlint, kotlinx-kover, mockk, turbine, robolectric, tink-java, acra, okhttp,
  kotlinx.serialization, junit-framework, kotest, nowinandroid.
