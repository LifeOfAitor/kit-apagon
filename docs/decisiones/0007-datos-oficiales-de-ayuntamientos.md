# 0007. Formato abierto para los datos oficiales de los ayuntamientos

Fecha: 2026-11-26. Estado: Propuesta.

## Contexto

Los ayuntamientos pueden aportar puntos de encuentro y otros lugares importantes. En una emergencia,
un dato falso marcado como oficial es peligroso, así que lo oficial tiene que poder demostrarse. La
mayoría de los municipios son pequeños y no tienen técnicos de información geográfica. Ver
`docs/investigacion/05-paquetes-municipales-y-datos-oficiales.md`.

## Alternativas consideradas

1. **Una API propia donde los ayuntamientos escriben datos** (propuesta inicial del propietario).
   Necesita un servidor encendido permanentemente, con coste y mantenimiento. Obliga a gestionar
   cuentas y permisos de escritura, y es un punto único de fallo. Además, la app no necesita consultar
   nada en directo: necesita un archivo descargado de antemano. Descartada.
2. **Un formato propio de texto (`localidad:x:y:dato`).** Ambiguo (no fija el orden de latitud y
   longitud ni el sistema de coordenadas), imposible de validar y no lo exporta ninguna herramienta.
   Descartada.
3. **Que los ayuntamientos editen directamente OpenStreetMap.** Beneficia a todas las apps de mapas,
   pero exige aprender a editar OpenStreetMap y seguir sus normas de importación, y no distingue lo
   oficial de lo aportado por la comunidad. Se recomendará como vía complementaria, no como única.
4. **Archivos estándar publicados por el propio ayuntamiento en su dominio oficial, recogidos por el
   generador.** Sin servidores nuevos y con una prueba sencilla de origen: el dominio del ayuntamiento.
   Elegida.

## Decisión

1. **Formatos aceptados:**
   - GeoJSON (RFC 7946, coordenadas WGS84) para quien tenga herramientas de mapas;
   - CSV con columnas fijas (incluidas `latitud` y `longitud`) para quien solo tenga una hoja de cálculo.
2. **Campos:** tipo de una lista cerrada (punto de encuentro, farmacia, centro de salud, agua potable,
   desfibrilador, refugio, punto de carga), nombre, dirección y fecha de actualización obligatorios;
   horario, accesibilidad y observaciones opcionales. Se publica un esquema JSON y una plantilla CSV con ejemplos.
3. **Origen verificable:** el repositorio mantiene un registro que asocia cada código de municipio con
   la dirección del archivo en el dominio oficial del ayuntamiento, su licencia y un contacto. Se añade
   una entrada al registro solo tras confirmar la petición con el ayuntamiento por un canal oficial.
4. **Validación automática:** el generador descarga el archivo, comprueba el esquema, convierte las
   coordenadas si hace falta y descarta los puntos fuera del municipio o de su margen. Si el archivo no
   es válido, se mantiene la última versión válida y se abre una incidencia automáticamente.
5. **Capa separada:** los datos oficiales se guardan aparte de los de OpenStreetMap. Se evita así
   mezclar licencias (CC BY 4.0 y ODbL) y la app puede mostrarlos como "Oficial: Ayuntamiento de X, fecha".
6. **Conflictos:** si un punto oficial y uno de OpenStreetMap parecen el mismo, se muestran los dos;
   prima el oficial en la lista.

## Consecuencias

- Coste cero también para el ayuntamiento: le basta con publicar un archivo en su web.
- Hay que escribir una guía corta para ayuntamientos, con la plantilla y el proceso de alta.
- Cada ayuntamiento que aporte datos aparece citado en la app, como exige la licencia CC BY 4.0.
- Incorporar esos datos además a OpenStreetMap exigiría una autorización expresa del ayuntamiento;
  queda como opción para cada ayuntamiento, no como requisito.
