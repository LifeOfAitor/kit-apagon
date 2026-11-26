# Datos de referencia del generador

## clave-publica-del-indice.txt

Clave pública con la que se comprueba la firma de `index.json` (decisiones 0013 y 0014): ECDSA con la
curva P-256, codificada en Base64 (X.509 SubjectPublicKeyInfo). El trabajo mensual la usa para
comprobar la firma antes de publicar, y la app la llevará dentro para comprobar cada índice que
descargue.

- Creada el 2026-11-26 por el propietario con `new-signing-key`.
- Huella SHA-256 de la clave en DER:
  `c6ba729eea175554ab2bc361d886df7a43feff206ac9effbcfedaad30f397036`.
- La clave privada solo existe en el secreto `KIT_APAGON_INDEX_SIGNING_KEY` del repositorio
  `kit-apagon-datos` y en el gestor de contraseñas del propietario. Nunca se guarda en un repositorio.

### Si la clave privada se pierde o se filtra

1. Crear una pareja nueva con `new-signing-key` y guardar la privada en el secreto y en el gestor de
   contraseñas.
2. Sustituir este archivo por la clave pública nueva y actualizar la huella aquí y en su test.
3. Publicar una versión de la app con la clave nueva. Las versiones anteriores de la app rechazarán los
   índices nuevos hasta que se actualicen, así que conviene avisarlo con antelación si es posible.

## municipios-ine-2026.csv

Relación oficial de municipios a 1 de enero de 2026 del Instituto Nacional de Estadística: 8.132
municipios, con su código de cinco cifras y su nombre. El trabajo mensual la compara con los límites
de OpenStreetMap para saber si falta algún municipio (decisión 0014). No llega a la app.

- Origen: `https://www.ine.es/daco/daco42/codmun/26codmun.xlsx` ("Municipios por provincia con sus
  códigos"), descargado el 2026-11-26.
- Licencia: Creative Commons Reconocimiento 4.0 (CC BY 4.0).
- Atribución: elaboración propia con datos extraídos del sitio web del INE: www.ine.es.

### Actualización anual

El INE publica la relación nueva a principios de cada año. Para sustituirla:

1. Descargar el archivo del año, por ejemplo `27codmun.xlsx`, de la página "Relación de municipios y
   sus códigos por provincias" del INE.
2. Convertirlo con el generador:

   ```bash
   ./gradlew -q :tools:package-generator:installDist
   tools/package-generator/build/install/package-generator/bin/package-generator official-municipalities --spreadsheet 27codmun.xlsx --output tools/package-generator/data/municipios-ine-2027.csv
   ```

3. Comprobar que el número de municipios coincide con el que publica el INE, borrar el archivo del
   año anterior, actualizar este documento y cambiar el nombre del archivo en el trabajo mensual.
