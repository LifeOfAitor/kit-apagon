# 0006. Generación y distribución de los paquetes por municipio

Fecha: 2026-11-26. Estado: Aceptada.

## Contexto

Cada usuario descarga el paquete de su municipio (mapa y puntos de interés) mientras tiene red.
Hay que cubrir más de 8.000 municipios sin coste, aguantar picos de descarga y garantizar que lo
descargado está completo y no ha sido manipulado. Ver
`docs/investigacion/05-paquetes-municipales-y-datos-oficiales.md`.

## Alternativas consideradas

### Cómo se generan

1. **Manualmente, municipio a municipio.** Inviable con más de 8.000 municipios.
2. **En el móvil, pidiendo los datos a servicios públicos (Overpass, servidor de Protomaps).**
   Depende de servicios de terceros que no están pensados para eso y que fallarían justo en el pico
   de demanda. Protomaps pide expresamente no hacerlo.
3. **Automáticamente en GitHub Actions, una vez al mes.** Gratis en repositorios públicos,
   reproducible y con tests. Elegida.

### Cómo se trocea el mapa

1. **Un paquete por provincia.** Pocos archivos, pero de cientos de MB: demasiado para un móvil modesto con datos móviles.
2. **Una cuadrícula fija de teselas.** Sin duplicados y válida para cualquier país, pero obliga a
   combinar varios archivos en el móvil, y MapLibre trabaja mejor con un solo archivo por fuente.
   Se descarta por ahora; puede revisarse si el tamaño total se dispara.
3. **Un paquete por municipio con un margen alrededor.** Fácil de entender para el usuario y
   resuelve el caso de la farmacia del pueblo de al lado. Elegida.

### Dónde se alojan

Ver la tabla de la investigación. GitHub Releases como origen principal; réplicas adicionales más adelante.

## Decisión

1. **Identificador del paquete:** código de país ISO 3166 más el código oficial del municipio. En
   España, `ES-` seguido del código INE de 5 dígitos. Así el esquema sirve para otros países.
2. **Contenido de cada paquete:**
   - mapa en PMTiles del municipio más un margen de 3 km alrededor, con zoom máximo 14. Según las
     mediciones, un municipio típico ocupa entre 1 y 5 MB y Madrid, el caso más grande, 17 MB;
   - puntos de interés de OpenStreetMap;
   - capa oficial del ayuntamiento, si existe, en un archivo separado (decisión 0007);
   - metadatos: nombre, fecha de los datos, fuentes y licencias.
3. **Índice firmado:** un archivo índice lista cada paquete con su nombre, provincia, rectángulo
   geográfico, punto central, versión, tamaño, huella SHA-256 y direcciones de descarga (réplicas).
   El índice va firmado con Ed25519. La clave privada vive solo en los secretos de GitHub Actions y la
   app incluye la clave pública.
4. **Generador:** un programa en el repositorio, con tests, que se ejecuta cada mes en GitHub Actions:
   descarga los datos base una sola vez, recorta cada municipio y publica.
   - Si un paso falla, no se publica nada y se conserva la versión anterior.
   - Se procesa provincia a provincia para no agotar el disco del ejecutor.
5. **Alojamiento:** un repositorio aparte, solo para datos, con una publicación de GitHub Releases por
   provincia y versión. La provincia con más municipios (Burgos, con unos 370) queda lejos del
   límite de 1.000 archivos. Se conservan solo las dos últimas versiones.
6. **En el móvil:**
   - Al instalar, la app pide el municipio por nombre, con búsqueda. Opcionalmente lo sugiere a partir
     de la ubicación, calculándolo en el móvil con los rectángulos y puntos centrales del índice, sin
     enviar la ubicación a ningún sitio.
   - Permite guardar varios municipios.
   - La descarga se puede reanudar, se comprueba la huella y solo entonces sustituye al paquete
     anterior. Nunca se queda el usuario sin datos por una descarga a medias.
   - La app muestra la fecha de los datos y avisa cuando hay una versión nueva.

## Consecuencias

- Coste cero mientras GitHub mantenga sus condiciones. La verificación por huella permite añadir
  réplicas (Cloudflare Pages, servidores de ayuntamientos) sin cambiar la app.
- El generador es una pieza de software más del proyecto, con sus propios tests y revisión.
- Atribuciones obligatorias en la app: OpenStreetMap (ODbL), IGN (CC BY 4.0), INE y cada ayuntamiento que aporte datos.
- Medido: tamaños por municipio, zoom y margen en `docs/investigacion/05-paquetes-municipales-y-datos-oficiales.md`.
  Con zoom 14 todos los paquetes caben en el límite de 25 MiB de Cloudflare Pages, y el extracto de
  España (menos de 1,7 GB) cabe en el disco de un ejecutor de GitHub Actions.
- Pendiente de comprobar en el prototipo: que el zoom 14 ampliado se lee bien en el Galaxy A20. Si no,
  se sube a zoom 15 (Madrid pasaría a 42 MB y dejaría de caber en Cloudflare Pages).
- Pendiente de comprobar: verificación de Ed25519 en API 26, de forma nativa o con una librería.
