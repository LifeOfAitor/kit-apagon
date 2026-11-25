# 0004. Mapas sin conexión con MapLibre y PMTiles

Fecha: 2026-11-25. Estado: Propuesta.

## Contexto

La app debe mostrar el mapa del municipio y sus puntos de interés sin red (principio 6), en móviles
modestos (principio 7) y sin coste (principio 3). Ver `docs/investigacion/02-mapas-y-puntos-de-interes.md`.

## Alternativas consideradas

1. **MapLibre Native + un archivo PMTiles por municipio.** Código abierto (BSD-2), sin claves ni
   servidores, mapa en un solo archivo de pocos MB.
2. **Google Maps SDK.** Necesita Google Play Services y clave de API, y su uso sin conexión es
   limitado. Contradice los principios 3, 6 y 8.
3. **osmdroid con teselas en imagen.** Funciona, pero las teselas en imagen ocupan mucho más que las vectoriales.
4. **Solo una lista de puntos, sin mapa.** Muy ligero, pero mucho menos útil para orientarse.
   Se mantendrá como vista alternativa para móviles con muy poco espacio y por accesibilidad.

## Decisión

MapLibre Native para Android con un archivo PMTiles del municipio, más una base de datos local de
puntos de interés generada a partir de OpenStreetMap con un script publicado en el repositorio.

## Consecuencias

- Atribución "© OpenStreetMap" y licencia ODbL visibles en la pantalla del mapa.
- El script que genera el paquete local forma parte del proyecto y tiene sus propios tests.
- Habrá que medir el tamaño real del paquete y la memoria usada en un móvil de 2 GB.
