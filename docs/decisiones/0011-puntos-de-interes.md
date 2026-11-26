# 0011. Puntos de interés: tipos, formato y presentación

Fecha: 2026-11-26. Estado: Aceptada.

## Contexto

El paso 2 del mapa añade los lugares útiles en un apagón. Ver `docs/investigacion/08-puntos-de-interes.md`.

## Alternativas consideradas

Detalladas en la investigación: base de datos Room, capa dentro del PMTiles o archivo aparte leído a
memoria; conjuntos de iconos; mostrar solo el mapa o también una lista.

## Decisión

1. **Tipos (lista cerrada, primera versión):**

   | Tipo | Etiquetas de OpenStreetMap |
   |---|---|
   | `pharmacy` | `amenity=pharmacy` |
   | `health_centre` | `amenity=hospital`, `amenity=clinic`, `amenity=doctors`, o `healthcare=hospital`, `clinic`, `doctor` o `centre`; nunca `amenity=veterinary` |
   | `drinking_water` | `amenity=drinking_water`, `amenity=water_point` |
   | `assembly_point` | `emergency=assembly_point` |
   | `defibrillator` | `emergency=defibrillator` |
   | `police` | `amenity=police` |
   | `fire_station` | `amenity=fire_station` |

   Se excluye `amenity=shelter` (marquesinas). Todo lo que no encaje se descarta. Una farmacia lleva
   también `healthcare=pharmacy`: se clasifica como farmacia, no como centro sanitario.

   Corrección del 2026-11-26: la primera versión decía `healthcare=*` para los centros sanitarios.
   Con los datos de Irun eso incluía fisioterapeutas, dentistas, podólogos, un laboratorio y un
   hospital veterinario, que no sirven en una emergencia. La lista de arriba es la correcta.

2. **Archivo `places.json` en cada paquete,** junto a `map.pmtiles`: GeoJSON (RFC 7946) con solo
   puntos en WGS84 y seis decimales. Propiedades:
   - obligatorias: `category` (uno de los tipos), `source` (`openstreetmap` u `official`) y `source_id`;
   - opcionales: `name`, `address`, `opening_hours` (el texto de OpenStreetMap, tal cual) y `phone`.

   La app **ignora los tipos que no conoce**, para que el generador pueda añadir tipos nuevos sin
   romper versiones antiguas de la app. Los datos oficiales de la decisión 0007 usarán este mismo
   formato en un archivo aparte, con `source` igual a `official`.

3. **Generación:** un módulo nuevo en Kotlin, `tools:package-generator`, que convierte la salida de
   `osmium` en `places.json` usando el código de `core:package-format`. Los lugares dibujados como
   contorno se reducen a un punto. Es la primera pieza del generador de la decisión 0006, y ya la usa
   `scripts/install-test-package.sh`.

4. **En el móvil:** el archivo se lee entero a memoria con `kotlinx-serialization-json` al abrir el
   mapa. **No se usa Room para datos de solo lectura del paquete.** Es una excepción a la decisión 0008,
   que reserva Room para los datos que la app escribe (mensajes, datos personales).

5. **En el mapa:** una capa propia con iconos de Maki (CC0) y uno propio para punto de encuentro, en
   colores de alto contraste por tipo. Se quitan las fuentes del mapa base para no duplicarlas. Al tocar
   un punto se abre una ficha con nombre (o tipo, si no tiene nombre), dirección, horario, teléfono
   (que abre el marcador, sin llamar) y el origen del dato.

6. **Lista accesible:** todos los puntos agrupados por tipo, con filtros por tipo. Es la alternativa
   al mapa para quien usa un lector de pantalla. Tocar un elemento lo centra en el mapa.

7. **Sin ubicación en este paso:** la lista no se ordena por distancia. "Dónde estoy" y "el más
   cercano" tendrán su propia decisión.

8. **Pruebas:** la lectura y validación del archivo, con tests de Kotlin puro, incluidas pruebas
   basadas en propiedades con Kotest, porque son datos externos; la conversión del generador, con
   tests propios; la lista y la ficha, con tests de Compose; el mapa, a mano en el móvil.

## Consecuencias

- `places.json` es una base de datos derivada de OpenStreetMap: se publica bajo ODbL, con atribución
  visible. El generador es público, así que cualquiera puede rehacer el archivo.
- La app debe dejar claro que los datos de OpenStreetMap pueden estar incompletos (por ejemplo, solo
  3 desfibriladores en Irun) y no sustituyen a la información oficial.
- Dependencias nuevas: `kotlinx-serialization-json` 1.13.0 y su complemento de compilación; Kotest
  para pruebas basadas en propiedades; iconos de Maki. Herramienta nueva de desarrollo: `osmium-tool`.
- Quedan fuera de este paso: farmacias de guardia, ubicación del usuario y datos oficiales del ayuntamiento.
