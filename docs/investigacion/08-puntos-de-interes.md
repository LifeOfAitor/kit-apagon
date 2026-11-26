# Investigación: puntos de interés del municipio

Fecha: 2026-11-26. Estado: primera revisión.

## Pregunta

¿Qué lugares útiles en un apagón hay realmente en OpenStreetMap, cómo se llevan al paquete del
municipio, cómo se guardan en el móvil y cómo se muestran de forma accesible y sin red?

## Datos reales: Irun

Fuente: extracto del País Vasco de Geofabrik del 2026-11-24 (81 MB, huella MD5 comprobada), recortado
con `osmium` a la zona del paquete de Irun (municipio más 3 km, incluye Hendaye y Biriatou, en
Francia).

| Tipo | Cantidad | Con nombre | Con horario |
|---|---|---|---|
| Agua potable (`amenity=drinking_water`) | 213 | 3 | 0 |
| Centros sanitarios (`amenity=hospital/clinic/doctors`, `healthcare=*`) | 36 | 35 | 10 |
| Farmacias (`amenity=pharmacy`) | 35 | 35 | 3 |
| Policía y bomberos | 8 | 8 | 0 |
| Desfibriladores (`emergency=defibrillator`) | 3 | 0 | 0 |
| **Puntos de encuentro (`emergency=assembly_point`)** | **0** | - | - |

Además, solo 39 de los 295 lugares útiles tienen dirección, y 22 tienen teléfono.

Hallazgos:

1. **No hay ningún punto de encuentro.** Confirma lo visto en la investigación 05: es justo el dato
   que tienen que aportar los ayuntamientos (decisión 0007).
2. **Muchos desfibriladores faltan con casi total seguridad:** solo hay 3 en una ciudad de 60.000
   habitantes, todos sin nombre. Los datos de OpenStreetMap son útiles, pero incompletos, y la app
   debe decir de dónde viene cada dato.
3. **`amenity=shelter` no sirve:** en Irun son marquesinas de autobús (7) o refugios sin tipo (4), no
   refugios de emergencia. Se excluye.
4. **El filtrado deja ruido:** `osmium` conserva puertas y cámaras que forman parte del contorno de
   otros elementos. El generador debe clasificar de forma estricta y descartar lo demás.
5. **Hay lugares dibujados como contorno** (25 de 322, por ejemplo hospitales): hay que reducirlos a
   un punto.
6. **El horario casi nunca está** y, cuando está, usa el formato de OpenStreetMap
   (`Mo-Fr 09:00-14:00`). Las farmacias de guardia no están en OpenStreetMap: esa información la dan
   los colegios de farmacéuticos y queda fuera de este paso.

## Otros hallazgos sobre las fuentes

- **Los servidores públicos de consultas de OpenStreetMap (Overpass) fallaron los dos** el
  2026-11-26: el principal estaba saturado y pedía no usarlo de forma automatizada; el alternativo
  devolvió una página de error. Ni la app ni nuestras herramientas deben depender de ellos.
- **Geofabrik publica Canarias dentro de África** (`africa/canary-islands`), no en el extracto de
  España. El generador de la decisión 0006 tendrá que procesar los dos.

## Lo que ya muestra el mapa base

La capa `pois` del estilo de Protomaps dibuja comercios, transporte, colegios, Correos y otros
lugares que ayudan a orientarse, pero de nuestros tipos **solo dibuja las fuentes de agua**: ni
farmacias, ni centros sanitarios, ni desfibriladores, ni policía, ni bomberos. Hace falta una capa
propia, y conviene quitar las fuentes del mapa base para que no aparezcan dos veces.

## Tamaño y almacenamiento en el móvil

Los 295 lugares útiles de Irun, con solo los campos necesarios, ocupan **54 KB (5 KB comprimidos)**.
Una gran ciudad tendrá varios miles, como mucho unos pocos megabytes.

| Opción | Valoración |
|---|---|
| Base de datos Room, como preveía la decisión 0008 | Permite búsquedas complejas, pero añade migraciones, código generado y escrituras en disco para unos datos que la app solo lee. |
| Dentro del archivo PMTiles, como otra capa | Un solo archivo, pero las teselas solo permiten consultar lo que se está viendo: no sirve para hacer una lista completa. |
| **Archivo aparte en el paquete, leído entero a memoria** | Sencillo, rápido para cientos o miles de puntos, fácil de probar y de validar. |

## Iconos

| Conjunto | Licencia | Tipos que cubre |
|---|---|---|
| **Maki (Mapbox)** | CC0-1.0, dominio público | Farmacia, hospital, médico, agua potable, desfibrilador, policía y bomberos. Falta punto de encuentro. |
| Temaki | Varias | Pocos de nuestros tipos |
| Material Symbols (Google) | Apache-2.0 | Genéricos, menos reconocibles como lugares |

## Cómo dibujarlos y tocarlos

Comprobado en la API de MapLibre 13.7.1: `GeoJsonSource` con los puntos, `SymbolLayer` con un icono
por tipo (`Style.addImage`), y `addOnMapClickListener` más `queryRenderedFeatures` para saber qué se ha
tocado. Limitación importante: **el contenido del mapa no es accesible para TalkBack**, el lector de
pantalla. Para una persona ciega, la única forma de llegar a una farmacia es una lista.

## Herramientas

- `osmium-tool` 1.19.1 (GPL-3.0): recorta y filtra los extractos. Se usa solo para generar paquetes,
  no se distribuye con la app.
- `kotlinx-serialization-json` 1.13.0 (Apache-2.0), publicada el 2026-11-11: lectura del archivo de
  puntos en Kotlin puro, compartida por la app y el generador.

## Fuentes

- https://download.geofabrik.de/europe/spain.html y https://download.geofabrik.de/europe/spain/pais-vasco/
- https://osmcode.org/osmium-tool/
- https://wiki.openstreetmap.org/wiki/Key:emergency y https://wiki.openstreetmap.org/wiki/Key:amenity
- https://github.com/mapbox/maki (licencia CC0-1.0 y lista de iconos)
- https://github.com/rapideditor/temaki
- https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/
- API de MapLibre Native para Android 13.7.1, comprobada en el `.aar` publicado
