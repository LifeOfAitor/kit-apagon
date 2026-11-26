# Investigación: paquetes por municipio y datos oficiales de los ayuntamientos

Fecha: 2026-11-26. Estado: primera revisión.

## Pregunta

¿Cómo se generan, se alojan gratis y se descargan de forma fiable los paquetes de mapa y puntos de
interés de cualquier municipio de España, y cómo se incorporan los datos oficiales que aporten los
ayuntamientos? Las decisiones 0006 y 0007 se investigan juntas porque forman un único sistema.

## Requisitos de partida

1. Más de 8.000 municipios en España, con la posibilidad de añadir otros países.
2. Coste cero (principio 3).
3. Debe aguantar picos de descarga: el momento de máxima demanda es justo antes de una crisis anunciada.
4. Un paquete descargado tiene que estar completo y no haber sido manipulado.
5. Un apagón no respeta los límites municipales: el paquete incluye un margen alrededor del municipio.
6. Un dato marcado como oficial tiene que poder demostrar que lo es.

## Datos base

| Dato | Fuente | Licencia | Notas |
|---|---|---|---|
| Mapa base | Compilación diaria de Protomaps (formato PMTiles) | ODbL, como obra producida a partir de OpenStreetMap | Se conservan las compilaciones de la última semana y las de los lunes del último mes. Protomaps pide no extraer de forma masiva de su servidor: hay que extraer España una sola vez por actualización y recortar después en local. |
| Puntos de interés | Extracto de España de Geofabrik (`spain-latest.osm.pbf`) | ODbL | Unos 1,4 GB. Gratuito y actualizado a diario. |
| Límites municipales | IGN, Base de Datos de Divisiones Administrativas de España | CC BY 4.0, atribución "CC-BY 4.0 ign.es" | Fuente oficial. Precisión de escala 1:25.000; algunos tramos son provisionales. |
| Códigos y nombres de municipios | INE, relación de municipios a 1 de enero de cada año | Reutilizable citando la fuente | Código de 5 dígitos: 2 de provincia y 3 de municipio. Los códigos de municipios desaparecidos no se reutilizan, así que sirven como identificador estable del paquete. |

Tamaño de referencia: un extracto de Berlín con todos los niveles de zoom ocupa 68 MB, y limitado al
zoom 6, 813 kB. Cada nivel de zoom adicional duplica aproximadamente el tamaño. Un municipio pequeño
ocupará pocos MB y una gran ciudad varias decenas. Hay que medirlo con municipios reales antes de
fijar el zoom máximo.

## Alojamiento gratuito: opciones revisadas

| Opción | Límites documentados | Valoración |
|---|---|---|
| GitHub Releases | Archivos de menos de 2 GiB, hasta 1.000 archivos por publicación. Según su documentación, sin límite de tamaño total ni de ancho de banda. | La mejor opción para empezar. Riesgo: las condiciones de uso de GitHub permiten limitar un consumo de ancho de banda excesivo. No debe ser la única fuente. |
| Cloudflare Pages (plan gratuito) | Hasta 20.000 archivos, máximo 25 MiB por archivo. Tráfico de archivos estáticos ilimitado. | Buena réplica para los paquetes pequeños. Las grandes ciudades superan los 25 MiB. Requiere crear una cuenta gratuita. |
| Cloudflare R2 | 10 GB gratis al mes y salida de datos gratuita. Se cobra lo que pase de ese límite. | Descartada por ahora: un exceso genera cargos, lo que choca con el principio 3. |
| GitHub Pages | Sitio de 1 GB como máximo y 100 GB de tráfico al mes como límite orientativo. | Descartada: demasiado pequeña. |
| Servidor propio o API | - | Descartada: cuesta dinero, necesita mantenimiento y es un punto único de fallo (ver decisión 0007). |

Referencia real: Organic Maps sirve sus mapas desde servidores cedidos gratuitamente por un proveedor
y desde réplicas mantenidas por voluntarios. Es un modelo que este proyecto puede seguir más adelante.

## Idea clave: réplicas verificadas por huella

Si la app comprueba la huella SHA-256 de cada archivo y la firma del índice, **no importa quién aloje
el archivo**. Se puede descargar de GitHub, de Cloudflare, del servidor de un ayuntamiento o del móvil
de un vecino, y la app sabe si es el archivo correcto. Esto permite:

- tener varias réplicas y probarlas en orden si una falla o va lenta;
- que un ayuntamiento ofrezca una réplica en su propio servidor;
- en el futuro, pasar paquetes de un móvil a otro sin internet.

## Datos oficiales de los ayuntamientos

Situación encontrada:

- Varios ayuntamientos publican capas de emergencia en datos abiertos (por ejemplo, Vigo: centrales
  de protección civil en GeoJSON con licencia CC BY; Málaga y Alcobendas: desfibriladores).
- No se ha encontrado en datos.gob.es ningún conjunto llamado "puntos de encuentro". Probablemente
  la mayoría de los ayuntamientos no los publican todavía.
- Málaga publica algunas capas en GeoJSON con el sistema de coordenadas EPSG:25830. El estándar
  GeoJSON (RFC 7946) exige WGS84. Es un ejemplo real de por qué los datos se validan y se convierten
  automáticamente antes de usarlos.
- La licencia habitual es CC BY 4.0. Incorporar esos datos a OpenStreetMap exige una autorización
  expresa del ayuntamiento: Málaga, Barcelona y Madrid la dieron en su día. Mantenerlos en una capa
  separada, sin mezclarlos con OpenStreetMap, evita ese problema.
- La mayoría de los municipios españoles son muy pequeños y no tienen técnicos de sistemas de
  información geográfica. El formato tiene que poder rellenarse con una hoja de cálculo.

## Mediciones reales (2026-11-26)

Medido con `pmtiles extract --dry-run` (pmtiles 1.31.2) sobre la compilación de Protomaps del
2026-11-25 (planeta completo: 140 GB). El modo de prueba solo descarga los índices, no el mapa.
Límites municipales obtenidos de OpenStreetMap a través de Nominatim, solo para medir.
El margen se ha aproximado ampliando el rectángulo del municipio.

| Municipio | Tipo | Exacto, zoom 15 | Exacto, zoom 14 | Exacto, zoom 13 | Margen 3 km, zoom 15 | Margen 3 km, zoom 14 |
|---|---|---|---|---|---|---|
| Illán de Vacas (Toledo) | Uno de los más pequeños de España | 0,9 MB | 0,9 MB | 0,9 MB | 1,2 MB | 1,0 MB |
| Hondarribia (Gipuzkoa) | Pequeño, costero | 1,9 MB | 1,5 MB | 1,2 MB | 4,9 MB | 3,2 MB |
| Irun (Gipuzkoa) | Mediano | 3,4 MB | 2,6 MB | 2,2 MB | 5,9 MB | 3,8 MB |
| Tineo (Asturias) | Rural muy extenso (540 km²) | 3,6 MB | 2,7 MB | 2,1 MB | 6,6 MB | 4,4 MB |
| Madrid | La ciudad más grande | 25 MB | 11 MB | 5,4 MB | 42 MB | 17 MB |

Otras medidas:

- Margen de 5 km en Irun: 8,1 MB (zoom 15) y 4,9 MB (zoom 14).
- Hay un coste fijo de unos 0,7 MB por paquete: las teselas de poco zoom, que cubren mucha superficie.
  Empezar en el zoom 10 deja Illán de Vacas en 0,3 MB, a cambio de no poder alejar la vista más allá de la comarca.
- Total aproximado, sin márgenes: rectángulo de la Península y Baleares (incluye zonas de Portugal,
  Francia y Marruecos, así que es una cota superior) 1,7 GB en zoom 14 y 3,4 GB en zoom 15;
  Canarias 63 MB en zoom 15.

Conclusiones de la medición:

1. Con margen de 3 km y zoom máximo 14, un municipio típico ocupa entre 1 y 5 MB, y Madrid 17 MB.
   Es razonable incluso con datos móviles.
2. El zoom 15 casi triplica el tamaño en las ciudades. MapLibre puede ampliar las teselas del zoom 14
   para ver la calle de cerca, pero hay que comprobar en el móvil de pruebas que el resultado se lee bien.
3. Con zoom 14, todos los paquetes caben en el límite de 25 MiB de Cloudflare Pages, lo que permite usarlo como réplica.
4. El extracto de España (menos de 1,7 GB en zoom 14) cabe con holgura en el disco de un ejecutor de GitHub Actions.
5. El coste fijo de 0,7 MB por paquete es irrelevante para el usuario. Se mantiene el zoom mínimo en 0
   para poder alejar la vista; se puede revisar si el total alojado crece demasiado.

## Riesgos y puntos a medir

1. Medido: ver la sección anterior.
2. Espacio en disco y tiempo de los ejecutores gratuitos de GitHub Actions al generar toda España.
   Probablemente haya que procesar provincia a provincia.
3. Compatibilidad de la verificación de firmas Ed25519 con Android 8 (API 26).
4. Que la base de datos de OpenStreetMap tenga pocos puntos de encuentro (`emergency=assembly_point`) en España.

## Fuentes

- https://docs.protomaps.com/basemaps/downloads
- https://docs.protomaps.com/guide/getting-started
- https://docs.protomaps.com/pmtiles/cli
- https://pyviz-tutorial.readthedocs.io/en/latest/protomaps/extract.html (tamaños del extracto de Berlín)
- https://download.geofabrik.de/europe.html
- https://datos.gob.es/es/catalogo/e00125901-spaignllm
- https://centrodedescargas.cnig.es/CentroDescargas/index.jsp
- https://ine.es/daco/daco42/codmun/codmun_anual.htm
- https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases
- https://developers.cloudflare.com/pages/platform/limits
- https://developers.cloudflare.com/r2/pricing
- https://docs.github.com/en/pages/getting-started-with-github-pages/github-pages-limits
- https://cdn.organicmaps.app/
- https://datos.gob.es/eu/catalogo/l01360577-centrales-de-proteccion-civil-de-vigo1.csv
- https://datosabiertos.malaga.eu/en_GB/dataset/centros-asistenciales/resource/5bec4017-4f14-468a-89b0-58e6f8cdff10
- https://wiki.openstreetmap.org/wiki/ES:Import/ODbL_Compatibility
- https://www.rfc-editor.org/rfc/rfc7946
