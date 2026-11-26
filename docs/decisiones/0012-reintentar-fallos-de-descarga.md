# 0012. Repetir las comprobaciones solo cuando falla una descarga

Fecha: 2026-11-26. Estado: Aceptada.

## Contexto

Las comprobaciones de GitHub Actions descargan las librerías del proyecto de Maven Central. Los
servidores de GitHub comparten direcciones con miles de proyectos, y Maven Central les responde a
veces **429 (demasiadas peticiones)**. El 2026-11-26 pasó tres veces:

1. En la pull request #2, al descargar el entorno nativo de Robolectric (165 MB).
2. De nuevo en la pull request #2, porque la caché no servía cuando cambiaban los archivos de compilación.
   Se corrigió cambiando a `actions/cache` (commit `e1a54c1`).
3. En `main` tras fusionar la pull request #3: una librería nueva (`kotlinx-serialization-json`) no
   estaba en la caché de `main`, porque GitHub no deja a `main` usar la caché de las ramas de las PR.

En los tres casos el código estaba bien y bastó con relanzar a mano. Pero un rojo que casi siempre es
"la tienda otra vez" acostumbra a ignorar el rojo, y el día que sea un fallo real puede pasar desapercibido.

## Alternativas consideradas

1. **Un servidor propio que guarde copia de Maven Central.** Es lo que recomiendan el blog de Gradle
   ("Good Neighbors", agosto de 2025) y Sonatype. Cuesta dinero y mantenimiento: choca con el principio 3.
2. **Una copia pública de Maven Central** (por ejemplo, la de Google). Ni Gradle ni Sonatype la
   mencionan y no se ha podido confirmar que sea oficial. Habría que añadir la verificación de huellas
   de Gradle para no fiarse a ciegas, con un archivo de huellas que actualizar en cada cambio de librería.
3. **Relanzar a mano.** Gratis, pero deja `main` en rojo hasta que alguien lo hace.
4. **Separar en un paso propio la descarga de todas las librerías.** Descartada al diseñarla: en un
   proyecto Android las librerías se piden en muchos momentos distintos de la compilación, y forzar
   esa descarga por adelantado es frágil.
5. **Repetir la ejecución solo cuando el fallo es de descarga.** Un script lee el mensaje de error y
   solo repite si es un problema de red; cualquier otro fallo termina en el acto.

## Decisión

Alternativa 5, con `scripts/run-with-download-retries.sh`:

- Ejecuta la orden indicada (en la integración continua, `./gradlew verifyAll`) y guarda su salida.
- Si termina bien, termina.
- Si falla y la salida contiene un error de red conocido (código 429, "Too Many Requests", códigos
  502, 503 o 504, tiempo de espera agotado o conexión cortada), espera y la repite: 2 minutos antes
  del segundo intento y 4 antes del tercero. Como mucho, tres intentos.
- Si falla por cualquier otro motivo (un test, la compilación, Lint, detekt), termina en el acto con
  ese mismo error. **Un fallo del código nunca se repite.**

Sonatype advierte de que reintentar sin control empeora la saturación. Por eso son pocos reintentos,
cada vez más espaciados, y solo ante errores de red.

## Consecuencias

- En el peor caso de saturación, una ejecución tarda unos 6 minutos más.
- Si Maven Central falla durante mucho tiempo, la ejecución sigue fallando, y hay que relanzarla más tarde.
- El script tiene sus propios tests, que forman parte de `verifyAll`.
- Si los fallos de descarga continúan a pesar de esto, se reconsiderará la alternativa 2 con
  verificación de huellas, en una decisión nueva.
