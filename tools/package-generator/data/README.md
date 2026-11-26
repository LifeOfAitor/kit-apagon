# Datos de referencia del generador

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
