# Investigación: paquetes de toda España y su descarga en la app

Fecha: 2026-11-26. Estado: primera revisión.

## Pregunta

La decisión 0006 fijó la arquitectura: un paquete por municipio, generado cada mes en GitHub Actions,
con un índice firmado y descargado por la app. Antes de programarlo hay que comprobar los detalles que
pueden fallar: de dónde salen los límites de los municipios, qué firma funciona en Android 8, cuánto
tarda y cuánto ocupa generarlo todo, y cómo descarga la app sin quedarse a medias.

## 1. Límites, nombres y códigos de los municipios

Prueba con el extracto del País Vasco de Geofabrik (relaciones `boundary=administrative`,
`admin_level=8`):

| Provincia | Municipios con `ine:municipio` en OpenStreetMap | Cifra oficial |
|---|---|---|
| Álava (01) | 51 | 51 |
| Gipuzkoa (20) | 88 | 88 |
| Bizkaia (48) | 113 | 113 |

- Ningún código repetido ni mal formado.
- Bizkaia tiene 113 porque Usansolo (48916) se separó de Galdakao. La primera estimación de esta
  investigación decía 112, una cifra desactualizada; OpenStreetMap estaba al día.
- Aparecen también límites que hay que descartar: mancomunidades y parzonerías sin código, municipios
  franceses y trozos de municipios de provincias vecinas que entran en el extracto.

Conclusión: los límites, el nombre oficial y el código INE pueden salir del mismo extracto de
OpenStreetMap que ya se usa para los lugares, sin una segunda fuente. La decisión 0006 preveía los
límites del IGN; con OpenStreetMap hay una sola fuente, una sola licencia (ODbL) y un solo proceso.
Como control de calidad, el generador debe comparar los códigos con la relación oficial del INE y
avisar de los que falten.

## 2. Firma del índice en Android 8

Tabla de algoritmos de `java.security.Signature` en la documentación oficial de Android:

| Algoritmo | Disponible desde |
|---|---|
| Ed25519 (previsto en la decisión 0006) | API 33 (Android 13) |
| SHA256withECDSA | API 11 |

Ed25519 no está disponible de serie en Android 8 a 12. ECDSA con la curva P-256 y SHA-256 sí, y
también en Java, donde se ejecuta el generador. No necesita ninguna librería.

## 3. Recorte de los mapas

- Recortar el País Vasco de la compilación mundial de Protomaps (zoom máximo 14): 83 MB en 13 segundos.
- Recortar después Irun de ese archivo local: **0,02 a 0,05 segundos**, y el resultado es idéntico
  byte a byte en cada ejecución.
- Lección: el Irun recortado del archivo regional tenía 119 teselas en lugar de 133, porque el
  recorte regional terminaba en la longitud -1,72 y el margen de Irun llega hasta -1,69, en Francia.
  **El archivo del que se recortan los municipios debe cubrir los márgenes de los municipios
  fronterizos** con Francia, Portugal, Andorra y Marruecos.

## 4. Recursos de GitHub Actions

- Datos de entrada: España de Geofabrik (unos 1,4 GB), Canarias (que Geofabrik publica en África) y el
  recorte de Protomaps de España con margen (como mucho 1,7 GB más Canarias).
- Salida estimada: más de 8.000 paquetes de 1 a 5 MB, Madrid 17 MB; del orden de 10 a 15 GB por versión.
- El disco libre de un ejecutor gratuito no da para tenerlo todo a la vez: hay que generar, publicar y
  borrar provincia a provincia.
- GitHub Releases admite 1.000 archivos por publicación: la provincia con más municipios, Burgos (371),
  necesita 742 con dos archivos por municipio.

## 5. Descarga en la app

- El índice se descarga de una dirección estable de GitHub (`releases/latest/download/...`), se
  comprueba su firma y se busca el municipio por nombre, sin tildes ni mayúsculas.
- La descarga de cada archivo se hace con WorkManager y OkHttp (decisión 0008): sobrevive a que se
  cierre la app, se puede reanudar con peticiones parciales, se comprueban tamaño y huella, y solo
  entonces sustituye al paquete anterior.
- Privacidad: GitHub ve la dirección IP de quien descarga y qué archivo pide. Es inevitable al
  descargar de internet y hay que decirlo en la política de privacidad. La app no envía nada más.

## Fuentes

- https://developer.android.com/reference/java/security/Signature (tabla de algoritmos)
- https://download.geofabrik.de/europe/spain.html
- https://ine.es/daco/daco42/codmun/codmun_anual.htm
- https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases
- https://docs.protomaps.com/pmtiles/cli
- Mediciones propias con `osmium` 1.19.1 y `pmtiles` 1.31.2, compilación de Protomaps del 2026-11-26.
