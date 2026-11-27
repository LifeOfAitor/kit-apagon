# 0015. Reintentos de la publicación mensual cuando Geofabrik rechaza la descarga

Fecha: 2026-11-27. Estado: Aceptada.

Completa la decisión 0014. El resto de la 0014 sigue vigente.

## Contexto

Desde el 20 de noviembre, Geofabrik rechaza las conexiones de parte de los ejecutores de GitHub
Actions, según la dirección que le toque a cada uno. Las dos primeras pruebas con Gipuzkoa en el
repositorio de datos fallaron así; la tercera, en otro ejecutor, funcionó (apartados 10 y 11 de la
investigación 10). Repetir dentro del mismo ejecutor no sirve: si su dirección está rechazada, lo
sigue estando. Hace falta otro ejecutor, y eso solo ocurre en otro trabajo o en otra ejecución.

## Alternativas consideradas

1. **Guardar una copia de los extractos** en la caché de GitHub Actions (10 GB gratis por
   repositorio; borra lo que lleva 7 días sin usarse). Un flujo aparte la renovaría en los días previos
   a la publicación y el trabajo mensual la usaría si Geofabrik rechaza la descarga.
   - A favor: una sola fuente de datos y sin depender de la suerte del día de la publicación.
   - En contra: otro flujo y una caché que mantener, datos de OpenStreetMap de hasta una semana antes y
     varias descargas al mes en lugar de una.
2. **Usar la réplica de OpenStreetMap Francia** cuando Geofabrik falle.
   - En contra: otro contorno para España (1,72 GB frente a 1,51), Canarias con días de retraso y sin
     MD5 para comprobar la descarga. Los paquetes podrían cambiar de un mes a otro según la fuente que
     respondiera.
3. **Repetir el trabajo entero el mismo día**, hasta unas cuantas veces.
   - A favor: datos del día.
   - En contra: los intentos seguidos pueden caer en el mismo rango de direcciones, y repetir un trabajo
     desde otro flujo añade permisos y piezas.
4. **Reintentar en días sucesivos.** El trabajo programado se ejecuta del día 2 al 8 de cada mes. Antes
   de nada comprueba si ese mes ya tiene una versión publicada y, si la tiene, termina sin hacer nada.
   - A favor: cada día es otro ejecutor y casi siempre otra dirección. Los datos siguen siendo del día de
     la publicación, solo hay una descarga correcta al mes y no hay nada que guardar ni mantener.
   - En contra: la publicación puede retrasarse unos días, y cada intento rechazado avisa al propietario
     de un fallo.

## Decisión

Se elige la alternativa 4.

1. **Calendario:** el trabajo programado se ejecuta los días 2 a 8 de cada mes a las 03:00 UTC.
2. **Comprobación previa:** un trabajo ligero, sin descargar datos ni compilar, mira si hay una versión
   publicada con fecha de ese mes. Si la hay, el trabajo de generación no se ejecuta. La comprobación
   es un script del repositorio de la app con sus tests.
3. **Ejecuciones manuales:** no hacen la comprobación previa, porque se lanzan a propósito (pruebas o
   una versión extra tras corregir un error).
4. **Si fallan los siete días**, ese mes no se publica y los móviles siguen con la versión anterior,
   que sigue siendo válida. El propietario recibe el aviso de cada fallo y puede lanzarlo a mano.
5. **Dentro de cada intento** se mantienen las repeticiones de `curl`, también ante conexiones
   rechazadas, para los cortes breves.

## Consecuencias

- Ningún cambio en los datos ni en la app.
- Unas líneas más en el flujo y un script pequeño con sus tests.
- El README del repositorio de datos debe explicar que un aviso de fallo entre los días 2 y 8 no
  requiere hacer nada si el día siguiente funciona.
- Si algún día Geofabrik rechazara a todos los ejecutores de GitHub, ninguna de estas alternativas,
  salvo la 2, bastaría. En ese caso habría que replantearlo con datos de ese momento.
