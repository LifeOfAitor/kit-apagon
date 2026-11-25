# Investigación: mapas y puntos de interés sin conexión

Fecha: 2026-11-25. Estado: primera revisión.

## Pregunta

¿Cómo mostrar un mapa del municipio y sus puntos de interés (farmacias, fuentes de agua, centros
de salud, puntos de encuentro) sin red, gratis y cumpliendo las licencias?

## Piezas revisadas

| Pieza | Licencia | Papel |
|---|---|---|
| MapLibre Native para Android | BSD-2-Clause | Motor de mapas. Admite archivos PMTiles desde la versión 11.7.0 (prefijo `pmtiles://`). |
| PMTiles (Protomaps) | Código BSD-3, diseño de mapa CC0, datos ODbL | Formato de un solo archivo con todo el mapa. La herramienta `pmtiles extract` recorta una zona por coordenadas o por un polígono GeoJSON. |
| Datos de OpenStreetMap | ODbL 1.0 | Calles, edificios y puntos de interés. |
| Organic Maps | Apache-2.0 | Referencia de cómo optimizar mapas sin conexión en móviles modestos. |

Detalle importante: MapLibre no usa caché ni descargas sin conexión con PMTiles. Por eso el archivo
del municipio se distribuye como un paquete local completo, que encaja con el principio 6.

## Obligaciones de la licencia ODbL (OpenStreetMap)

1. **Atribución visible junto al mapa**, que nombre OpenStreetMap e indique la licencia ODbL.
   El mapa también se usa sin conexión, así que la atribución tiene que estar dentro de la app,
   no en una web.
2. **Al distribuir una copia de los datos** (nuestro paquete local) hay que incluir la atribución y
   el texto de la ODbL o un enlace a ella.
3. **Si alguien lo pide, hay que darle los datos**, incluidas nuestras modificaciones. Si publicamos
   el script que genera el paquete, cumplimos esto de forma natural.

## Puntos de interés: etiquetas de OpenStreetMap candidatas

- Farmacias: `amenity=pharmacy`
- Centros de salud y hospitales: `amenity=clinic`, `amenity=doctors`, `amenity=hospital`, `healthcare=*`
- Agua potable: `amenity=drinking_water`
- Puntos de encuentro: `emergency=assembly_point`
- Desfibriladores: `emergency=defibrillator`

Pendiente: comprobar cuántos de estos puntos hay realmente en OpenStreetMap para el municipio
elegido. Los puntos de encuentro oficiales suelen faltar, y aquí es donde el ayuntamiento puede
aportar sus datos. Si los datos del ayuntamiento son abiertos, hay que revisar su licencia antes
de mezclarlos con OpenStreetMap.

## Conclusión

MapLibre + un archivo PMTiles del municipio + una base de datos local de puntos de interés
extraída de OpenStreetMap. Todo gratuito y con licencias compatibles con GPL-3. El paquete de un
municipio debería ocupar unos pocos MB; hay que medirlo con el municipio real.

## Fuentes

- https://maplibre.org/maplibre-native/android/examples/data/PMTiles/
- https://docs.protomaps.com/basemaps/downloads
- https://docs.protomaps.com/guide/getting-started
- https://osmfoundation.org/wiki/Attribution
- https://osmfoundation.org/wiki/Licence_and_Legal_FAQ
- https://wiki.openstreetmap.org/wiki/Attribution
- https://scottish-tech-army.github.io/Soundscape-Android/developers/offline-maps.html (ejemplo real de app Android con PMTiles)
