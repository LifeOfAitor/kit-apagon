# 0010. Primer mapa sin conexión: MapLibre, estilo y recursos

Fecha: 2026-11-26. Estado: Aceptada.

## Contexto

La decisión 0004 eligió MapLibre y PMTiles, y la 0006 el formato de los paquetes por municipio. Falta
concretar cómo se integra el mapa en la app. Ver `docs/investigacion/07-mapa-sin-conexion.md`.

## Alternativas consideradas

1. **Versión de MapLibre:** la 13.8.0 salió hoy; la 13.7.1 lleva seis semanas publicada. Las
   versiones `-globe` son preliminares y quedan descartadas.
2. **Estilo:** escribir uno propio desde cero (control total, pero mucho trabajo y errores) o generar
   el de Protomaps, que está pensado para sus propias teselas.
3. **Recursos del estilo:** descargarlos de internet (falla sin red) o incluirlos en la app.
4. **Atribución:** el botón de información de MapLibre (abre enlaces, poco visible) o un texto propio
   siempre visible sobre el mapa.
5. **Navegación:** Navigation Compose o Navigation 3.

## Decisión

1. **MapLibre Native para Android 13.7.1**, variante OpenGL ES (`android-sdk`), que es la más
   compatible con móviles antiguos. Se pasará a la 13.8.x cuando lleve al menos dos semanas publicada.
   Regla general a partir de ahora: al añadir o actualizar una librería que llega al usuario, se elige
   la última versión estable con al menos dos semanas desde su publicación.
2. **Permisos:** se eliminan del manifiesto final los permisos de ubicación que añade MapLibre. Se
   volverán a añadir, con su propia decisión, cuando exista una función que use la ubicación.
3. **Estilo:** generado con `@protomaps/basemaps` 5.7.2, en versión clara y oscura según el tema del
   sistema, con nombres locales. Los estilos generados se guardan en el repositorio, y el script que
   los genera, con versiones fijadas, en `tools/map-style/`.
4. **Recursos dentro de la app:** fuentes Noto Sans (solo los tres rangos de caracteres necesarios) e
   iconos de Protomaps, cargados con `asset://`. Ningún recurso del mapa se pide a internet.
5. **Atribución:** un texto propio, siempre visible sobre el mapa: "© OpenStreetMap (ODbL) ·
   Protomaps". Se desactivan el logotipo y el botón de atribución de MapLibre.
6. **Un APK por arquitectura:** arm64-v8a y armeabi-v7a para móviles, y x86_64 solo para el
   emulador. Se medirá si conviene guardar la librería nativa comprimida dentro del APK (menos
   descarga) o sin comprimir (menos espacio en el móvil).
7. **Dónde vive el paquete:** en el almacenamiento privado de la app,
   `files/municipal-packages/<identificador>/map.pmtiles`. Hasta que exista la descarga (paso 3 del
   mapa), el paquete de prueba se instala con un script de desarrollo y `adb`, solo en versiones de depuración.
8. **Navegación:** Navigation 3, porque está diseñada para Compose y su pila de pantallas se prueba
   como una lista normal.
9. **Pruebas:** la lógica, con tests unitarios; el dibujado del mapa, con un test instrumentado en el
   emulador local. Ejecutarlo también en GitHub se decide en la 0009.

## Consecuencias

- La app pasa de 0,9 MB a unos 10-14 MB por APK. Es el precio de un mapa vectorial sin conexión, y
  queda medido en `docs/mediciones.md`.
- Node pasa a ser una herramienta de desarrollo opcional, solo para regenerar el estilo.
- Se registran en `docs/licencias-de-terceros.md`: MapLibre (BSD-2), sus dependencias, el estilo de
  Protomaps (BSD-3), las fuentes Noto Sans (OFL-1.1) y los iconos (MIT).
- Nombres en alfabetos distintos del latino no se mostrarán en el mapa.
- Navigation 3 es más nueva y tiene menos ejemplos publicados que Navigation Compose.
