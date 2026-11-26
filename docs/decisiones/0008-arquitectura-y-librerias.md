# 0008. Arquitectura de la app y librerías base

Fecha: 2026-11-26. Estado: Aceptada.

## Contexto

Antes de crear el proyecto Android hay que fijar cómo se organiza el código y qué librerías se usan.
Criterios: estabilidad, licencia compatible con GPL-3, sin Google Play Services, pocas dependencias,
rendimiento en un móvil de 2 GB y accesibilidad. Ver `docs/investigacion/06-arquitectura-y-librerias.md`.

## Alternativas consideradas

Detalladas en la investigación: Compose o Views; MapLibre directo o MapLibre Compose; Hilt, Koin o
inyección manual; detekt integrado en Gradle o como herramienta independiente.

## Decisión

### Organización

Arquitectura en capas recomendada por Google (interfaz, dominio y datos), con flujo de datos en una
sola dirección y estado expuesto por los ViewModel con `StateFlow`.

Módulos iniciales (los nombres en inglés, como el resto del código):

| Módulo | Tipo | Contenido |
|---|---|---|
| `app` | Android | Punto de entrada, navegación y contenedor de dependencias. |
| `core:model` | Kotlin puro | Modelos compartidos (municipio, punto de interés, mensaje). |
| `core:package-format` | Kotlin puro | Formato de los paquetes municipales e índice firmado. Lo comparten la app y el generador. |
| `core:data` | Android | Repositorios, Room, DataStore y descargas. |
| `core:ui` | Android | Tema, componentes comunes y accesibilidad. |
| `feature:map` | Android | Mapa y puntos de interés. |
| `feature:guide` | Android | Guía y listas de comprobación. |

Los módulos `core:mesh-protocol` y `feature:mesh` se crean cuando se empiece la malla, y el generador
cuando se empiece la decisión 0006. No se crean módulos vacíos por adelantado.

### Librerías

- **Interfaz:** Jetpack Compose con Material 3, perfiles de referencia y R8.
- **Mapa:** MapLibre Native para Android, dentro de `AndroidView`.
- **Inyección de dependencias:** manual, con un contenedor de la aplicación.
- **Almacenamiento:** Room y DataStore.
- **Descargas:** WorkManager y OkHttp.
- **JSON:** kotlinx.serialization.
- **Errores:** ACRA, guardando en el móvil y enviando solo si el usuario quiere.
- **Asincronía:** corrutinas y Flow.

### Calidad

- **Tests:** JUnit 5 y kotlin.test en Kotlin puro; JUnit 4 con Robolectric donde Android lo exija;
  kotlinx-coroutines-test y Turbine; Kotest para pruebas basadas en propiedades; objetos falsos
  escritos a mano antes que MockK.
- **Cobertura:** Kover, con un umbral mínimo en los módulos de Kotlin puro que la integración continua
  exige. El valor exacto se fija con el primer código real.
- **Análisis estático:** ktlint, Android Lint con los avisos tratados como errores, y detekt 1.23.8
  como herramienta independiente.
- **Accesibilidad:** comprobaciones automáticas en los tests de interfaz que se ejecutan en el emulador.
- **Script propio:** rechaza emojis en el código, los commits y la documentación.

### Versiones

Siempre versiones estables, fijadas en el catálogo de versiones de Gradle (`gradle/libs.versions.toml`).
Las actualizaciones se hacen de una en una, en su propia rama y con todos los tests en verde.

## Consecuencias

- Todo lo elegido es gratuito, estable y compatible con GPL-3. La licencia de cada librería se
  registra en `docs/licencias-de-terceros.md` al incorporarla.
- La lógica importante queda en Kotlin puro: tests rápidos, código reutilizable por el generador y
  camino abierto a iOS con Kotlin Multiplatform.
- La inyección manual es más código a mano, pero sin errores ocultos que aparezcan al ejecutar.
- detekt queda fuera de la compilación hasta que haya una versión 2.0 estable.
- Pendiente: el tamaño del APK con MapLibre y el tiempo de arranque, medidos en el móvil de pruebas (corrección del 2026-11-26: el móvil de pruebas es un Galaxy A51, no un A20).
- Pendiente: el cifrado de la ficha médica tendrá su propia decisión cuando se diseñe esa función.
