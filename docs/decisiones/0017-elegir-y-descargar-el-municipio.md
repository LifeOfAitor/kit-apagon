# 0017. Elegir y descargar el municipio en la app

Fecha: 2026-11-28. Estado: Propuesta.

Completa las decisiones 0013 y 0014 para la parte 3c. Sustituye el formato del índice de la 0014, que
ninguna versión de la app ha llegado a leer.

## Contexto

La app tiene que permitir elegir el municipio, descargar su paquete y mantenerlo al día sin gastar datos
ni batería de más (principios 6 y 7). La investigación 11 midió que el índice actual ocupa 5,8 MB y que
GitHub no lo comprime, y repasó cómo descargar en segundo plano de Android 8 a 16.

## Alternativas consideradas

1. **Índice:** como está (5,8 MB); comprimido con gzip (1,5 MB); más compacto y con gzip (1,2 MB); o en
   dos niveles con gzip (0,11 MB la primera vez y 0,09 MB cada búsqueda de actualizaciones).
2. **Descarga:** con WorkManager, con trabajos de transferencia iniciados por el usuario (solo desde
   Android 14 y sin librería de Jetpack), o mientras la pantalla está abierta (se pierde si la app se
   cierra).

## Decisión

1. **Índice en dos niveles, versión 2 del formato:**
   - `municipios.json.gz`, firmado en `municipios.json.gz.sig`: fecha de los datos, plantilla de las
     direcciones y, por municipio, código, nombre oficial, nombres alternativos y provincia. Por
     provincia lleva su nombre y el tamaño y la huella SHA-256 de su archivo de detalles;
   - `provincia-ES-NN.json.gz`, uno por provincia: rectángulo, punto central y tar de cada municipio,
     con la posición, el tamaño y la huella de cada archivo;
   - la firma y las huellas se calculan sobre los archivos comprimidos, tal como se descargan, para
     comprobarlos antes de descomprimir. La app limita lo que acepta al descomprimir;
   - se publican en la publicación del índice de cada mes, en lugar de `index.json`. La dirección fija
     pasa a ser `releases/latest/download/municipios.json.gz`.
2. **Código:**
   - la lógica que no depende de Android (formatos, búsqueda, descarga con OkHttp, comprobación e
     instalación) va en módulos de Kotlin puro, probados con un servidor de prueba (MockWebServer);
   - las pantallas y el trabajo de WorkManager van en un módulo nuevo, `feature:packages`.
3. **Descarga** con WorkManager:
   - trabajo urgente que, sin cuota de urgencia, pasa a ser normal; un trabajo por paquete y solo con
     conexión;
   - en Android 11 y anteriores se ejecuta como servicio en primer plano, con una notificación de
     progreso. Desde Android 14 se declara de tipo `dataSync`;
   - no se pide el permiso de notificaciones: el progreso se ve en la propia app.
4. **Cada archivo** se pide con una petición parcial:
   - la app exige la respuesta 206 con el rango exacto;
   - si la descarga se corta, se reanuda desde lo ya recibido;
   - si una dirección falla, prueba la siguiente réplica;
   - tras comprobar tamaño y huella, el paquete nuevo sustituye al anterior de una sola vez, y nunca
     queda uno a medias.
5. **Cada paquete instalado guarda sus datos:** nombre, provincia, fecha de los datos y rectángulo.
6. **Pantallas:**
   - si no hay ningún paquete, el mapa ofrece elegir municipio;
   - la búsqueda ignora tildes y mayúsculas, encuentra también los nombres alternativos y muestra la
     provincia junto a cada resultado;
   - antes de descargar se ven el tamaño y una línea sobre privacidad: "Al descargar, GitHub verá la
     dirección IP de tu móvil";
   - se pueden tener varios municipios; el que se muestra se guarda con DataStore, y cada uno se puede
     borrar.
7. **Fecha de los datos** (acordado el 2026-11-27):
   - el mapa y la lista de municipios muestran la fecha de los datos de cada paquete, por ejemplo
     "Datos del 27 de noviembre de 2026";
   - "Buscar actualizaciones" descarga solo la lista de nombres y, si hay una versión más reciente,
     propone actualizar. Nunca se actualiza solo.
8. **Clave pública:** la app lleva la clave de `tools/package-generator/data/clave-publica-del-indice.txt`,
   copiada al compilar, para que no haya dos copias que puedan divergir.
9. **Librerías nuevas:** OkHttp 5.5.1, WorkManager 2.12.0, DataStore 1.3.0 y, solo en tests,
   MockWebServer 3 5.5.1. Todas tienen licencia Apache-2.0.
10. **Por partes, cada una con su pull request:**
    - 3c-1: el formato de índice nuevo en el generador y en la publicación;
    - 3c-2: la descarga y la instalación en un módulo de Kotlin puro, con sus tests;
    - 3c-3: elegir y descargar el municipio en la app;
    - 3c-4: varios municipios, borrar, fecha de los datos y búsqueda de actualizaciones.

## Consecuencias

- La primera descarga del índice pasa de 5,8 MB a unos 0,11 MB, y cada búsqueda de actualizaciones, a
  unos 0,09 MB.
- El generador y la publicación cambian de formato. La primera versión con el formato nuevo sustituye a
  la actual, que nadie usa todavía.
- La app pasa a tener una notificación en Android 11 y anteriores mientras descarga, y un permiso de
  servicio en primer plano de tipo `dataSync`.
- La política de privacidad, pendiente, tendrá que explicar que GitHub ve la dirección IP al descargar.
