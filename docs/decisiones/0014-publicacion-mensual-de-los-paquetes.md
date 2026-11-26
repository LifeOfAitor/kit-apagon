# 0014. Publicación mensual de los paquetes

Fecha: 2026-11-26. Estado: Propuesta.

Sustituye la forma de publicar de la decisión 0013 (un archivo por municipio en una publicación por
provincia) y completa cómo descarga la app. El resto de la 0013 sigue vigente.

## Contexto

La parte 3b sube cada mes los paquetes de toda España al repositorio de datos `kit-apagon-datos`. La
investigación 10 muestra que subir un archivo por municipio no cabe en los límites de GitHub: más de
16.000 subidas al mes, a un máximo de 500 a 1.000 por hora, en trabajos de 6 horas como mucho.

## Alternativas consideradas

1. **Un archivo por municipio, repartiendo las subidas en varios días.** Mantiene el formato, pero una
   versión tardaría días en publicarse y cualquier fallo a medias es difícil de recuperar.
2. **Otro alojamiento** (almacenamiento en la nube, Internet Archive). Más trabajo de mantenimiento,
   cuentas nuevas y, en algunos casos, coste o límites de espacio gratuito por debajo de 26 GB.
3. **Un archivo por provincia y descargas parciales.** Unas 55 subidas al mes. GitHub admite descargas
   parciales (comprobado) y cada persona descarga los mismos bytes que antes.

## Decisión

Se elige la alternativa 3.

1. **Un tar sin comprimir por provincia** (`ES-20.tar`). Si una provincia pasa de 1,5 GB, se reparte
   en varios (`ES-09-1.tar`, `ES-09-2.tar`). Dentro van los dos archivos de cada municipio, con los
   nombres de siempre.
2. **El índice indica, por cada archivo de un paquete, dónde está dentro del tar:** dirección del tar,
   posición del primer byte, tamaño y huella SHA-256. El formato del índice todavía no se ha
   publicado, así que se amplía sin cambiar su versión.
3. **La app descarga solo su trozo** con una petición parcial (`Range`). Si el servidor no responde
   206, cancela, para no descargar el tar entero por error. Si la descarga se corta, la reanuda desde
   el último byte recibido.
4. **Publicaciones en GitHub:**
   - una por provincia y mes, marcada como preliminar (`2026-12-01-ES-20`);
   - una por mes con el índice (`2026-12-01`), creada como borrador con `index.json` e
     `index.json.sig` y publicada como la más reciente al final de todo;
   - la app usa siempre `https://github.com/LifeOfAitor/kit-apagon-datos/releases/latest/download/index.json`.
5. **Cada ejecución hace un commit** en el repositorio de datos con el resumen de la versión: deja
   registro, ordena las publicaciones y evita que GitHub desactive el trabajo programado.
6. **Se conservan las dos últimas versiones.** Las anteriores se borran (publicación y etiqueta)
   después de publicar la nueva. Si una ejecución falla, la versión anterior sigue intacta; al
   repetirla, se borran primero los restos de la versión fallida.
7. **Mapa:** se recorta provincia a provincia de la compilación de Protomaps del día, con el
   rectángulo de `region-area`, y la misma compilación para todas las provincias.
8. **Comprobación con el INE:** la relación oficial de municipios se guarda en CSV en el repositorio
   de la app y se actualiza una vez al año. Si faltan municipios, la versión se publica igualmente y
   el informe del mes los lista. Si falta más del 1 %, no se publica.
9. **Antes de publicar, la firma se comprueba con la clave pública que lleva la app.** Si la clave
   privada del secreto no corresponde a ella, no se publica nada.
10. **Dónde vive cada cosa:**
    - la lógica (qué publicaciones borrar, informe, tar) va en el generador, con tests;
    - el script del mes va en el repositorio de la app y se prueba con un `gh` falso;
    - el repositorio de datos solo tiene el flujo, que descarga el repositorio de la app en un commit
      fijado, y su README;
    - las herramientas van fijadas: osmium-tool de Ubuntu y pmtiles 1.31.2 con su huella SHA-256.
11. **Cuándo se ejecuta:** el día 2 de cada mes y bajo demanda. Bajo demanda se puede probar con
    algunas provincias sin publicar el índice.
12. **Por partes:**
    - 3b-1, en el repositorio de la app: tar y posiciones en el índice, comparación con el INE,
      planificación de publicaciones, comando para crear la clave de firma y script mensual con sus
      tests;
    - 3b-2, con el propietario: crear el repositorio de datos y el secreto, una ejecución de prueba con
      Gipuzkoa y después la primera versión completa.

## Consecuencias

- Unas 55 subidas al mes, muy por debajo de los límites.
- Quien quiera los datos sin la app puede descargar el tar de su provincia y extraerlo con `tar`.
- Las réplicas que se añadan deben admitir descargas parciales.
- En GitHub, las publicaciones de las provincias aparecen como preliminares; la nota de cada una
  explica por qué.
- Queda por medir en la primera ejecución completa: el tiempo total, la velocidad de subida, que
  osmium 1.16.0 de Ubuntu da los mismos resultados que la 1.19.1 local y que la dirección fija lleva
  al índice nuevo.
- La parte 3c debe comprobar con un servidor de prueba que OkHttp mantiene la petición parcial tras
  la redirección de GitHub.
