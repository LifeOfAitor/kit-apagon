# Investigación: elegir y descargar el municipio en la app

Fecha: 2026-11-28. Estado: primera revisión.

## Pregunta

La decisión 0013 fijó cómo descarga la app su municipio: elegirlo con una búsqueda que ignora tildes y
mayúsculas, comprobar la firma del índice, descargar con WorkManager y OkHttp, comprobar tamaño y
huella, y actualizar solo cuando la persona lo pide. La 0014 añadió las descargas parciales de un tar
por provincia. Antes de programar quedaban cuatro puntos abiertos:

- cómo descargar el índice sin gastar datos de más;
- cómo hacer la descarga en segundo plano de Android 8 a 16;
- qué librerías usar;
- cómo mostrar la fecha de los datos.

## 1. Tamaño del índice

Medido con el índice publicado el 2026-11-27 (8.129 paquetes):

| Forma | Primera descarga | Cada búsqueda de actualizaciones |
|---|---|---|
| A. Como se publica ahora | 5,8 MB | 5,8 MB |
| A con gzip | 1,5 MB | 1,5 MB |
| B. Más compacto (la dirección del tar una vez por paquete, coordenadas con 5 decimales) y con gzip | 1,2 MB | 1,2 MB |
| C. Dos niveles con gzip: una lista firmada con los nombres y otro archivo con los detalles de cada provincia | 0,11 MB | 0,09 MB |

- El 70 % del índice actual son los datos de los archivos: dirección, posición, tamaño y huella de cada
  uno. Para buscar un municipio solo hacen falta los nombres.
- En la forma C, la lista de nombres ocupa 0,54 MB sin comprimir y 0,09 MB con gzip. El archivo de una
  provincia ocupa una mediana de 0,02 MB con gzip y, el mayor, el de Burgos, 0,05 MB.
- GitHub sirve los archivos tal cual: con `Accept-Encoding: gzip` sigue enviando los 5,8 MB sin
  comprimir. La compresión hay que hacerla al publicar, y la app tiene que descomprimir.
- En la forma C, la firma protege la lista, y la lista lleva la huella de cada archivo de provincia: con
  una sola firma queda todo protegido.
- Añadir más adelante campos opcionales, como el punto central para ordenar por cercanía, no obliga a
  cambiar la versión del formato, porque el lector ignora lo que no conoce.

## 2. Descarga en segundo plano

Según la documentación de Android (fuentes al final):

- **WorkManager con trabajo urgente** (`setExpedited`):
  - en Android 12 y posteriores es un trabajo prioritario del sistema;
  - en versiones anteriores, WorkManager puede ejecutarlo como servicio en primer plano. Por eso el
    trabajador debe implementar `getForegroundInfo`, porque si no "puede provocar cierres
    inesperados";
  - con `OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST`, si el sistema no le da cuota de
    urgencia, se ejecuta como un trabajo normal en lugar de perderse.
- **Android 14 y posteriores:** los trabajos largos deben declarar el tipo de servicio en primer plano.
  Para descargas es `dataSync`, con el permiso `FOREGROUND_SERVICE_DATA_SYNC`. El servicio
  `SystemForegroundService` de WorkManager se declara en el manifiesto con ese tipo.
- **Notificaciones:** no hace falta el permiso `POST_NOTIFICATIONS` para iniciar un servicio en primer
  plano. Si la persona no lo concede, el aviso del servicio sigue apareciendo en el gestor de tareas.
- **Alternativa nueva de Google** (trabajos de transferencia iniciados por el usuario): existe desde
  Android 14, no tiene librería de Jetpack y está pensada para transferencias largas. Para trabajo de
  red corto que se puede interrumpir, la propia página recomienda seguir con WorkManager.
- **Tiempo de las descargas:** un paquete típico ocupa 3 MB y el mayor, el de Madrid, 18,6 MB. Con una
  conexión lenta de 1 Mbit/s son unos 25 segundos y unos 2 minutos y medio.

## 3. Librerías

Versiones estables más recientes con al menos dos semanas publicadas (decisión 0010), comprobadas el
2026-11-28 en Maven Central y en el repositorio de Google:

| Librería | Versión | Publicada | Licencia |
|---|---|---|---|
| OkHttp | 5.5.1 | 2026-10-22 | Apache-2.0 |
| MockWebServer 3 (solo tests) | 5.5.1 | 2026-10-22 | Apache-2.0 |
| WorkManager (`work-runtime`) | 2.12.0 | 2026-09-23 | Apache-2.0 |
| DataStore (`datastore-preferences`) | 1.3.0 | 2026-10-21 | Apache-2.0 |

- WorkManager 2.12.0 lista un módulo `work-analytics` ("Analytics and debugging info about workers"),
  pero solo como versión recomendada de la familia, igual que `work-testing`. No es una dependencia de
  `work-runtime`. Hay que confirmarlo en el árbol de dependencias al añadirla, porque la app no puede
  llevar analítica (principio 8).
- OkHttp no añade `Accept-Encoding: gzip` a una petición con `Range`. En la parte 3c hay que comprobar
  con un servidor de prueba que mantiene la cabecera `Range` al seguir la redirección de GitHub.

## 4. Fecha de los datos

- Cada versión tiene la fecha en que se generó (`data_version`). Los datos de OpenStreetMap son de ese
  mismo día, o de hasta un día antes.
- Acordado con el propietario el 2026-11-27: la app muestra la fecha de los datos de cada paquete y,
  cuando hay conexión y una versión más reciente, sugiere actualizar sin hacerlo nunca por su cuenta.
- Los paquetes instalados no guardan hoy la fecha de sus datos: hay que guardarla al instalarlos.

## 5. Otros detalles

- Muchos municipios comparten nombre en distintas provincias, así que la búsqueda debe mostrar la
  provincia junto a cada resultado. La relación del INE guardada no tiene los nombres de las
  provincias; la hoja de cada provincia del XLSX sí los tiene, en su segunda fila.
- Privacidad: GitHub ve la dirección IP de quien descarga. La app debe decirlo antes de la primera
  descarga, en una línea, además de en la política de privacidad.
- La app ya tiene el permiso de Internet, que añade la librería del mapa, MapLibre.

## Fuentes

- https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
- https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running
- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://developer.android.com/develop/ui/views/notifications/notification-permission
- https://developer.android.com/develop/background-work/background-tasks/data-transfer-options
- https://developer.android.com/develop/background-work/background-tasks/uidt
- Metadatos de Maven Central y del repositorio Maven de Google, consultados el 2026-11-28.
- Mediciones propias con el índice publicado el 2026-11-27 y una petición con `Accept-Encoding: gzip`.
