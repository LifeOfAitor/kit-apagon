# 0013. Paquetes de toda España: límites, firma, publicación y descarga

Fecha: 2026-11-26. Estado: Propuesta.

Sustituye dos puntos de la decisión 0006: la fuente de los límites municipales y el algoritmo de
firma del índice. El resto de la 0006 sigue vigente.

## Contexto

El paso 3 del mapa construye lo que permite que cualquier persona descargue el paquete de su
municipio. Ver `docs/investigacion/09-paquetes-de-toda-espana.md`.

## Alternativas consideradas

1. **Límites:** IGN (lo previsto en la 0006) u OpenStreetMap. OpenStreetMap trae el código INE y el
   nombre oficial en el mismo extracto que ya se usa para los lugares.
2. **Firma:** Ed25519 (lo previsto en la 0006, pero solo existe de serie desde Android 13), Ed25519
   con una librería externa, o ECDSA P-256 (de serie desde Android 3 y en Java).
3. **Dónde se ejecuta el generador:** en el repositorio de la app, publicando en otro repositorio (hace
   falta un token personal con permisos sobre el segundo), o en el propio repositorio de datos (le
   basta con su token automático).

## Decisión

1. **Municipios:** relaciones de OpenStreetMap con `boundary=administrative`, `admin_level=8` y un
   `ine:municipio` de cinco cifras. Se descarta todo lo demás. El generador compara la lista con la
   relación oficial del INE y avisa de los municipios que falten.
2. **Firma:** ECDSA con la curva P-256 y SHA-256 (`SHA256withECDSA`). La clave privada solo existe en
   los secretos del repositorio de datos; la clave pública va dentro de la app.
3. **Índice** (`index.json`, con su firma en `index.json.sig`): versión del formato, fecha de los datos y,
   por municipio, identificador, nombre, provincia, rectángulo con margen, punto central y, por cada
   archivo, nombre, tamaño, huella SHA-256 y direcciones de descarga (réplicas).
4. **Generador**, un trabajo de GitHub Actions al mes y bajo demanda:
   - descarga una vez los extractos de Geofabrik de España y de Canarias, y un recorte de Protomaps que
     cubra España con margen para los municipios fronterizos;
   - extrae los límites y los lugares de toda España de una sola pasada;
   - provincia a provincia: recorta el mapa de cada municipio, reparte los lugares por rectángulo,
     elimina los lugares duplicados (un mismo sitio como punto y como edificio), publica y borra;
   - al final publica el índice firmado. Si cualquier paso falla, no se publica el índice y la app
     sigue viendo la versión anterior.
5. **Repositorio de datos** `kit-apagon-datos`, público, creado por el propietario: contiene el flujo
   de publicación y los datos (ODbL). Una publicación por provincia y mes, y una publicación fija con el
   índice. Se conservan las dos últimas versiones.
6. **En la app:**
   - si no hay ningún paquete, la pantalla del mapa ofrece elegir municipio (necesita red una vez);
   - el índice se descarga y se comprueba su firma antes de usarlo; la búsqueda ignora tildes y mayúsculas;
   - la descarga usa WorkManager y OkHttp: se reanuda si se corta, comprueba tamaño y huella, y solo
     entonces sustituye al paquete anterior;
   - se pueden tener varios municipios; el que se muestra se guarda con DataStore;
   - las actualizaciones se buscan cuando la persona lo pide, sin tareas en segundo plano que gasten
     batería o datos.
7. **Por partes, cada una con su pull request:**
   - 3a: el generador completo, probado en local con el País Vasco, y la firma;
   - 3b: la publicación automática en el repositorio de datos;
   - 3c: la elección y descarga del municipio en la app.

## Consecuencias

- Una sola fuente de datos geográficos (OpenStreetMap y Protomaps) con una sola licencia (ODbL).
- Sin librerías nuevas para la firma.
- El propietario tiene que crear el repositorio de datos y guardar la clave privada en sus secretos;
  se le guiará paso a paso en la parte 3b.
- Cada versión mensual ocupa del orden de 10 a 15 GB en GitHub Releases. Si GitHub lo limitara, la
  verificación por huella permite añadir réplicas sin cambiar la app.
- La política de privacidad tendrá que explicar que GitHub ve la dirección IP de quien descarga.
