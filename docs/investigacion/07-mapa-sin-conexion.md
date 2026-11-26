# Investigación: primer mapa sin conexión en la app

Fecha: 2026-11-26. Estado: primera revisión.

## Pregunta

¿Qué hace falta exactamente para mostrar el mapa de un municipio con MapLibre, sin red, en un móvil
modesto, respetando la privacidad y las licencias?

## MapLibre Native para Android

Comprobado en Maven Central y en las publicaciones del repositorio `maplibre/maplibre-native`:

| Versión | Publicada | Tipo |
|---|---|---|
| 13.8.0 | 2026-11-26 (hoy) | Estable |
| 13.7.1 | 2026-10-15 | Estable |
| 2026.11.2-globe y 2026.10.19-globe | Octubre y noviembre de 2026 | **Preliminares**, aunque al ordenarlas por número parecen las más nuevas |

Cada versión se publica en tres variantes: `android-sdk` (OpenGL ES, la predeterminada),
`android-sdk-opengl` (igual) y `android-sdk-vulkan`.

Contenido del paquete 13.8.0, comprobado abriendo el archivo:

- **Versión mínima de Android:** 6.0 (API 23). Compatible con nuestro mínimo, Android 8.
- **Permisos que añade a la app sin pedirlo:** `INTERNET`, `ACCESS_NETWORK_STATE`,
  `ACCESS_WIFI_STATE`, `ACCESS_COARSE_LOCATION` y `ACCESS_FINE_LOCATION`. Los de ubicación chocan con
  el principio 8 mientras la app no use la ubicación.
- **Librería nativa por arquitectura (sin comprimir):** arm64-v8a 12,5 MB, armeabi-v7a 9,2 MB,
  x86 12,8 MB y x86_64 13,0 MB. Con las cuatro, la app pasaría de 0,9 MB a unos 47 MB.
- **Dependencias:** OkHttp 4.12, Timber, AndroidX Fragment y Annotation, y sus librerías de geometría
  y gestos. Ninguna de Google Play Services. MapLibre no incluye telemetría.

## Estilo del mapa

- **Generador:** paquete `@protomaps/basemaps` 5.7.2 (BSD-3-Clause), compatible con las compilaciones
  de Protomaps que medimos (esquema 4). Prueba realizada: genera los estilos claro y oscuro con 71
  capas y unos 65 KB cada uno.
- **Nombres de lugar:** con el idioma `es`, el estilo muestra primero la traducción al castellano
  (`name:es`) y solo si no existe el nombre local, aunque el nombre local ya esté en alfabeto latino.
  En Irun salía "Irún", "Oyárzun" y "Hendaya" en lugar de "Irun", "Oiartzun" y "Hendaye".
  Corrección del 2026-11-26: la primera versión de este documento afirmaba lo contrario, sin haberlo
  comprobado en un móvil. El generador del proyecto (`tools/map-style/generate-styles.mjs`) invierte
  ese orden solo para nombres en alfabeto latino, como pide la decisión 0010, y un test lo vigila.
- **Ejecutarlo:** la orden de línea de comandos del paquete necesita una herramienta extra (`tsx`).
  Basta con un script de Node de diez líneas que llama a su interfaz en JavaScript. Node solo hace
  falta para regenerar el estilo, nunca para compilar la app.

## Recursos sin conexión del estilo

Un estilo normal descarga fuentes e iconos de internet. Sin red, las etiquetas no aparecerían. Hay que
incluirlos en la app (repositorio `protomaps/basemaps-assets`):

| Recurso | Licencia | Tamaño |
|---|---|---|
| Fuentes Noto Sans Regular, Medium e Italic, solo los rangos 0-255, 256-511 y 8192-8447 | SIL Open Font License 1.1 | Unos 800 KB |
| Iconos v4, claro y oscuro, a 1x y 2x | MIT (derivados de tangrams/icons) | Unos 90 KB |

Esos rangos cubren el castellano, el catalán, el euskera y el gallego, más la puntuación tipográfica.
Un nombre con caracteres fuera de ellos (por ejemplo, en alfabeto árabe) no se mostraría.
MapLibre carga estos recursos desde la propia app con direcciones `asset://`.

## Cómo probar el mapa

- MapLibre dibuja con la tarjeta gráfica mediante código nativo. Robolectric no puede cargarlo, así
  que el mapa en sí solo se prueba en un emulador o en un móvil (tests instrumentados).
- La lógica alrededor del mapa (preparar el estilo, localizar el paquete instalado, decidir qué
  mostrar) sí se prueba con tests unitarios normales.
- Hay un emulador local (Pixel 9, Android 16, arm64) configurado con 2 GB de RAM: sirve como
  aproximación al móvil de referencia del principio 7. Lleva Google Play Services, así que no prueba
  que la app funcione sin ellos.

## Navegación entre pantallas

| Librería | Última estable | Valoración |
|---|---|---|
| Navigation Compose | 2.10.2 | Muy usada, pero nació para el sistema de vistas antiguo y se adaptó a Compose. |
| Navigation 3 | 1.2.2 | Diseñada para Compose. La pila de pantallas es una lista que controla la app, así que se prueba sin trucos. |

## Fuentes

- https://repo1.maven.org/maven2/org/maplibre/gl/android-sdk/ (versiones, `.pom` y `.aar` de 13.7.1 y 13.8.0)
- https://github.com/maplibre/maplibre-native/releases
- https://www.npmjs.com/package/@protomaps/basemaps (versión 5.7.2, licencia y código fuente)
- https://github.com/protomaps/basemaps-assets (README, licencias y tamaños de fuentes e iconos)
- https://dl.google.com/android/maven2/androidx/navigation/ y https://dl.google.com/android/maven2/androidx/navigation3/
