# Datos de Kit Apagón

Paquetes municipales de la app [Kit Apagón](https://github.com/LifeOfAitor/kit-apagon): el mapa y los
lugares útiles de cada municipio de España, para usarlos sin conexión durante un apagón. Se generan y
se publican solos cada mes con el flujo `.github/workflows/publicar.yml`.

## Cómo los usa la app

1. Descarga `index.json` y su firma, `index.json.sig`, de la última publicación:
   `https://github.com/LifeOfAitor/kit-apagon-datos/releases/latest/download/index.json`.
2. Comprueba la firma con la clave pública que lleva dentro. Si no es válida, no usa el índice.
3. Descarga solo los archivos de su municipio, con una petición parcial al tar de su provincia, y
   comprueba su tamaño y su huella SHA-256 antes de instalarlos.

## Publicaciones

- Una por mes con el índice, con la fecha de los datos como nombre (por ejemplo, `2026-12-02`). Es la
  versión vigente y lleva el informe de la comparación con la relación oficial de municipios del INE.
- Una por provincia y mes con su tar (por ejemplo, `2026-12-02-ES-20`). Se marcan como preliminares a
  propósito, para que GitHub nunca las tome por la versión vigente.
- Se conservan las dos últimas versiones, para que un móvil que leyó el índice anterior pueda terminar
  su descarga.

Quien quiera los datos sin la app puede descargar el tar de su provincia y extraerlo con `tar -xf`.

## Licencias

- Mapas y lugares: © colaboradores de OpenStreetMap y Protomaps, con licencia
  [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/). Cualquiera puede rehacerlos con el
  generador público de Kit Apagón.
- Relación de municipios usada en el informe: elaboración propia con datos extraídos del sitio web del
  INE (www.ine.es), con licencia CC BY 4.0.

## Privacidad

GitHub ve la dirección IP de quien descarga los archivos y qué archivo pide. La app no envía nada más.

## Mantenimiento

- El flujo usa el generador de `LifeOfAitor/kit-apagon` en el commit fijado en `GENERATOR_COMMIT`. Para
  actualizarlo, se cambia ese valor por el commit nuevo de `main`, una vez fusionado allí.
- La clave privada del índice está en el secreto `KIT_APAGON_INDEX_SIGNING_KEY`. Si se pierde o se
  filtra, hay que crear una nueva y publicar una versión de la app con su clave pública.
- Para probar sin que los móviles lo vean: Actions, "Publicar los paquetes", "Run workflow", con unas
  pocas provincias y sin marcar "Publicar el índice". El índice queda en una publicación en borrador.
