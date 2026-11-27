# Investigación: publicación mensual de los paquetes en GitHub

Fecha: 2026-11-26. Estado: cuarta revisión, con los rechazos de Geofabrik y la prueba en GitHub Actions
(apartados 10 y 11, 2026-11-27).

## Pregunta

La decisión 0013 prevé publicar cada mes, en el repositorio de datos `kit-apagon-datos`, los dos
archivos de cada municipio en una publicación de GitHub por provincia. Antes de programarlo hay que
comprobar que cabe en los límites de GitHub Actions y de GitHub Releases. También hay que
comprobar cómo encuentra la app el índice, de dónde salen los datos de Canarias, Ceuta y Melilla, y
contra qué lista se comprueba que no falta ningún municipio.

## 1. Límites de GitHub

Según la documentación oficial (ver fuentes):

| Límite | Valor |
|---|---|
| Peticiones con el token automático de Actions (`GITHUB_TOKEN`) | 1.000 por hora y repositorio |
| Peticiones que crean contenido (límite secundario) | 80 por minuto y 500 por hora |
| Duración de un trabajo | 6 horas |
| Ejecutor estándar en repositorios públicos | 4 CPU, 16 GB de RAM, 14 GB de disco; gratuito y sin límite de uso |
| Archivos por publicación | 1.000 |
| Tamaño de cada archivo | menos de 2 GiB |
| Tamaño total de una publicación y ancho de banda | sin límite |
| Trabajos programados en repositorios públicos | se desactivan tras 60 días sin actividad en el repositorio |

**Consecuencia: la publicación prevista en la 0013 no cabe.** Con unos 8.130 municipios y dos archivos
por municipio, cada mes habría que subir más de 16.000 archivos. Cada subida es una petición que crea
contenido: a 500 por hora serían más de 32 horas, y aun con el límite de 1.000 por hora, más de 16. Un
trabajo dura como mucho 6 horas. Juntar los dos archivos de cada municipio en uno seguiría
necesitando más de 8 horas.

## 2. Descargas parciales de una publicación

Prueba con un archivo público de GitHub Releases (`go-pmtiles_1.31.2_Linux_x86_64.tar.gz`, 17,4 MB),
pidiendo solo una parte con la cabecera `Range`:

- GitHub redirige (302) a `release-assets.githubusercontent.com`, y ese servidor responde
  **206 Partial Content** con `Accept-Ranges: bytes` y la cabecera `Content-Range` correcta.
- Los 100 bytes pedidos a partir del byte 1.000.000 son idénticos a los mismos bytes del archivo
  completo.
- Una petición abierta (`bytes=17444000-`), la que se usa para reanudar una descarga cortada, también
  responde 206.

Conclusión: la app puede descargar solo su municipio de un archivo grande, sin bajar el resto.

## 3. Un archivo por provincia

Si los paquetes de cada provincia van juntos en un solo archivo y el índice indica dónde empieza cada
uno, cada mes se suben unos 55 archivos en lugar de 16.000.

- **Formato:** tar sin comprimir. Es un estándar: cualquiera puede extraer los paquetes con `tar -xf`,
  y añade solo 512 bytes de cabecera por archivo. Sin comprimir, cada paquete ocupa un trozo continuo
  del tar, que la app descarga tal cual. Los mapas ya van comprimidos por dentro, así que comprimir el
  tar ahorraría poco y rompería las descargas parciales.
- **Tamaño:** con la media medida (unos 3,2 MB por municipio, investigación 09), la provincia con más
  municipios, Burgos (371), ocuparía unos 1,2 GB. Si una provincia se acercara a los 2 GiB, habría que
  repartirla en varios archivos.
- **Para cada persona no cambia nada:** descarga los mismos bytes que si el archivo estuviera suelto.
- **Riesgo:** un servidor que no admitiera peticiones parciales enviaría el archivo entero, más de
  1 GB. La app debe exigir la respuesta 206 y cancelar en cualquier otro caso. Lo mismo para las
  réplicas que se añadan.

## 4. Una dirección fija para el índice

- GitHub ofrece `releases/latest/download/NOMBRE` para la publicación marcada como la más reciente
  ("latest"). Las publicaciones en borrador y las preliminares ("prerelease") no pueden marcarse así.
- Si las publicaciones de las provincias son preliminares y solo la del índice es normal, la dirección
  fija siempre lleva al último índice publicado.
- Una publicación creada como borrador no es visible. Si se suben `index.json` e `index.json.sig` al
  borrador y se publica al final, el cambio de versión es instantáneo: la app nunca ve un índice a
  medias ni un índice con la firma de otro mes.
- GitHub ordena las publicaciones por la fecha del commit al que apuntan. Si todas apuntaran al
  mismo commit, el orden sería ambiguo. Un commit por ejecución (con el resumen de la versión) lo
  evita, y además cuenta como actividad: así GitHub no desactiva el trabajo programado a los 60 días.

## 5. Fuentes de los datos

- **Geofabrik:** el extracto de España (1,4 GB) incluye Ceuta y Melilla, que aparecen como
  subregiones. Canarias va aparte, en `africa/canary-islands` (57,9 MB). Ambos tienen licencia ODbL 1.0.
- **Protomaps:** guarda las compilaciones diarias durante una semana, con licencia ODbL (obra
  producida). Desaconseja enlazar sus archivos directamente desde una aplicación y recomienda
  copiarlos a un almacenamiento propio. Es lo que se hace: el mapa se recorta una vez al mes y se
  publica en el repositorio de datos; la app nunca se conecta a Protomaps.
- **Recorte por provincia:** recortar cada provincia con el rectángulo de `region-area` evita
  descargar el mar, Marruecos y Francia más allá de los márgenes. Además, solo hay en disco el mapa
  de una provincia a la vez. Todas las provincias deben usar la misma compilación.

## 6. Lista oficial de municipios

- El INE publica cada año la relación de municipios a 1 de enero. La de 2026 es
  `26codmun.xlsx` ("Municipios por provincia con sus códigos"), solo en formato XLSX.
- Licencia: Creative Commons Reconocimiento 4.0 (CC BY 4.0). Si los datos se transforman, hay que citar
  "Elaboración propia con datos extraídos del sitio web del INE: www.ine.es".
- La lista solo sirve para comprobar que no falta ningún municipio; no llega a la app. Basta con
  convertirla a CSV una vez al año y guardarla en el repositorio.

## 7. Herramientas en el ejecutor

- osmium-tool en Ubuntu 24.04: versión 1.16.0. Las mediciones locales usaron la 1.19.1; la primera
  ejecución de prueba debe confirmar que los resultados coinciden.
- pmtiles: el binario oficial para Linux de la versión 1.31.2, descargado de su publicación en GitHub
  y comprobado con una huella SHA-256 fijada en el flujo.

## 8. Estimación de disco y tiempo

- Disco: extractos (1,5 GB), exportaciones de osmium (menos de 1 GB), mapa de una provincia (menos de
  1 GB) y su tar (hasta 1,5 GB). Menos de 5 GB de los 14 disponibles.
- Tiempo: en local, generar una provincia tarda unos 3 s y recortar su mapa unos 15 s. Falta medir la
  velocidad de subida desde GitHub Actions. La estimación es de una a dos horas, dentro del límite de
  6; hay que medirla en la primera ejecución completa.

## 9. Resultados de la parte 3b-1 en local

Con `scripts/build-region-packages.sh` y el País Vasco, ya con un tar por provincia:

| Provincia | Municipios | Tar |
|---|---|---|
| Álava (01) | 51 | `ES-01.tar`, 194,7 MB |
| Gipuzkoa (20) | 88 | `ES-20.tar`, 256,1 MB |
| Bizkaia (48) | 113 | `ES-48.tar`, 359,3 MB |

- El tar solo añade unos 0,1 MB por provincia: cabeceras de 512 bytes y relleno.
- La relación del INE convertida por el generador tiene 8.132 municipios, la misma cifra que una
  lectura independiente del XLSX con Python. Comparada con OpenStreetMap en el País Vasco: 252
  municipios oficiales, ninguno sin límite y ninguno con un código desconocido.
- Comprobaciones con herramientas distintas del generador:
  - el `tar` del sistema lista los 504 archivos;
  - openssl acepta la firma hecha con una clave creada por el comando `new-signing-key`;
  - un script de Python lee cada archivo en su posición del tar y comprueba tamaño y huella.
- El proceso completo sigue tardando menos de 30 segundos.

## 10. Rechazos de Geofabrik desde GitHub Actions

2026-11-27: las dos primeras ejecuciones de prueba en el repositorio de datos fallaron al descargar el
extracto de España. `download.geofabrik.de` rechazó la conexión del ejecutor dos veces en menos de un
segundo, en dos ejecutores distintos. No se publicó nada.

- Otros proyectos describen lo mismo desde el 20 de noviembre: Geofabrik rechaza las conexiones de
  parte de los ejecutores de GitHub, a veces casi la mitad, mientras otros descargan con normalidad.
  Geofabrik no ha publicado nada sobre la causa.
- Repetir dentro del mismo ejecutor no ayuda cuando su dirección está rechazada. Además, `curl` no
  repite por defecto una conexión rechazada: hace falta `--retry-connrefused`.
- Fallo propio encontrado: el script siguió dos pasos después de la descarga fallida, porque el
  extracto se pedía dentro de `$(...)`, donde `set -e` no detiene la función. Terminó parado en osmium,
  pero por casualidad. Corregido: cada paso de la descarga comprueba su resultado, la descarga se
  guarda con otro nombre hasta que su MD5 es correcto y un test reproduce el rechazo.
- Cómo seguir publicando cuando Geofabrik rechaza: decisión 0015, reintentar en días sucesivos.

## 11. Prueba con Gipuzkoa en GitHub Actions

2026-11-27, flujo del repositorio de datos con el generador del commit `4e62fcd`, Gipuzkoa y sin
publicar el índice. Los dos primeros intentos fallaron porque Geofabrik rechazó la descarga (apartado
10); el tercero, en otro ejecutor, funcionó.

| Paso | Tiempo |
|---|---|
| Instalar herramientas y compilar el generador | 2 min |
| Descargar España (1,5 GB) y exportar límites y lugares | 4 min 20 s |
| Canarias | 22 s |
| Comparar con el INE | 6 s |
| Recortar el mapa de la provincia de la compilación de Protomaps | 15 s |
| Generar los 88 paquetes | 12 s |
| Crear la publicación y subir `ES-20.tar` (256 MB) | 15 s |
| Total | 7 min 35 s |

- El generador firmó el índice comprobándolo con la clave pública del repositorio de la app: el
  secreto del repositorio de datos es el correcto.
- INE: 88 municipios oficiales, ninguno sin límite y ninguno con un código desconocido.
- Comprobación como la hará la app, desde fuera de GitHub Actions:
  - openssl acepta la firma del índice descargado del borrador;
  - los archivos de Irun, Donostia y Eskoriatza, pedidos con descargas parciales a la dirección
    pública del tar, llegan con la respuesta 206, el rango exacto y la huella del índice.
- Estimación para toda España: unos 5 minutos de descarga y exportación y, por provincia, entre medio
  minuto y unos pocos minutos. Del orden de una a dos horas, dentro del límite de 6.
- **Tamaño del índice:** 63 KB para 88 paquetes, unos 720 bytes por paquete. Para toda España serían
  unos 5,8 MB, más que un paquete típico; con gzip, unos 1,5 MB. Hay que decidir en la parte 3c cómo lo
  descarga la app (comprimido, por provincia u otra forma) para no gastar datos de más.

## Fuentes

- https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api
- https://docs.github.com/en/rest/releases/releases (`make_latest`, publicación más reciente)
- https://docs.github.com/en/rest/releases/assets
- https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases
- https://docs.github.com/en/actions/reference/runners/github-hosted-runners
- https://docs.github.com/en/actions/reference/limits
- https://docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-workflow-runs/disabling-and-enabling-a-workflow
- https://download.geofabrik.de/europe/spain.html y https://download.geofabrik.de/africa/canary-islands.html
- https://docs.protomaps.com/basemaps/downloads
- https://www.ine.es/dyngs/INEbase/es/operacion.htm?c=Estadistica_C&cid=1254736177031&menu=ultiDatos&idp=1254734710990
- https://www.ine.es/dyngs/AYU/index.htm?cid=125 (aviso legal del INE)
- https://packages.ubuntu.com/noble/osmium-tool
- Prueba propia de descargas parciales con `curl -r`, 2026-11-26.
- https://github.com/gsueur/osm-geoparquet/pull/201 y https://github.com/gsueur/osm-geoparquet/pull/205
- https://github.com/patricebender/resupply-karoo/pull/160
