# Licencias de terceros

Todo código, dato o recurso ajeno que use el proyecto aparece aquí antes de incorporarse.

## Código distribuido con la app

Estas librerías forman parte del APK que recibe el usuario.

| Componente | Versión | Licencia | Uso |
|---|---|---|---|
| Jetpack Compose (ui, material3, tooling-preview), mediante el BOM | 2026.11.01 | Apache-2.0 | Interfaz |
| AndroidX Activity Compose | 1.13.0 | Apache-2.0 | Punto de entrada de la interfaz |
| AndroidX Core KTX | 1.19.1 | Apache-2.0 | Utilidades de Android |
| AndroidX Lifecycle (Runtime Compose, ViewModel Compose y ViewModel Navigation 3) | 2.11.1 | Apache-2.0 | Ciclo de vida y estado de las pantallas |
| AndroidX Navigation 3 (runtime y ui) | 1.2.1 | Apache-2.0 | Navegación entre pantallas |
| MapLibre Native para Android | 13.7.1 | BSD-2-Clause | Dibujo del mapa |
| MapLibre Java (geojson y turf), incluida por MapLibre | 6.0.1 | MIT | Geometrías del mapa |
| MapLibre Gestures, incluida por MapLibre | 0.0.4 | BSD-2-Clause | Gestos sobre el mapa |
| OkHttp, incluida por MapLibre | 4.12.0 | Apache-2.0 | La usa MapLibre internamente; la app tiene la red desactivada para el mapa |
| Timber, incluida por MapLibre | 5.0.1 | Apache-2.0 | Registros internos de MapLibre |
| kotlinx.coroutines, incluida por AndroidX | 1.10.2 | Apache-2.0 | Tareas en segundo plano |
| kotlinx-serialization-json | 1.13.0 | Apache-2.0 | Leer el archivo de lugares de cada paquete |
| Biblioteca estándar de Kotlin | 2.4.21 | Apache-2.0 | Lenguaje |

## Recursos distribuidos con la app

| Recurso | Origen | Licencia | Cómo se cumple |
|---|---|---|---|
| Estilos del mapa (`feature/map/src/main/assets/map/style/`) | Generados con `@protomaps/basemaps` 5.7.2 | BSD-3-Clause | Pendiente: aviso de copyright en una pantalla de licencias dentro de la app antes de la primera publicación |
| Fuentes Noto Sans Regular, Medium e Italic (solo tres rangos de caracteres) | `protomaps/basemaps-assets`, commit `028c18f` | SIL Open Font License 1.1 | El texto de la licencia va junto a las fuentes (`assets/map/fonts/OFL.txt`) |
| Iconos del mapa v4 | `protomaps/basemaps-assets`, derivados de `tangrams/icons` | MIT | El texto de la licencia va junto a los iconos (`assets/map/sprites/LICENSE-tangrams-icons.md`) |
| Iconos de los lugares (farmacia, hospital, agua potable, desfibrilador, policía y bomberos) | Maki de Mapbox, commit `28e2a36` | CC0-1.0 (dominio público) | No exige atribución; se cita igualmente. El icono de punto de encuentro es propio del proyecto. |

## Herramientas de compilación y pruebas

No se distribuyen con la app, así que su licencia no condiciona la del proyecto.

| Componente | Versión | Licencia | Uso |
|---|---|---|---|
| Gradle | 9.9.2 | Apache-2.0 | Sistema de compilación |
| Android Gradle Plugin | 9.5.3 | Apache-2.0 | Compilación Android |
| Kotlin Gradle Plugin y compilador de Compose | 2.4.21 | Apache-2.0 | Compilación Kotlin |
| JUnit 4 | 4.13.2 | EPL-1.0 | Tests de Android |
| JUnit 5 (Jupiter), mediante el BOM | 6.1.3 | EPL-2.0 | Tests de los módulos de Kotlin puro |
| kotlin-test | 2.4.21 | Apache-2.0 | Tests |
| kotlinx-coroutines-test | 1.10.2 | Apache-2.0 | Tests de corrutinas |
| Turbine | 1.2.1 | Apache-2.0 | Tests de flujos de datos |
| Kotest (solo `kotest-property`) | 6.2.7 | Apache-2.0 | Pruebas basadas en propiedades del archivo de lugares |
| Robolectric | 4.18.1 | MIT | Tests de Android sin emulador |
| AndroidX Test (core, ext-junit) y Compose UI Test | 1.7.0, 1.3.0 y BOM | Apache-2.0 | Tests |
| Kover | 0.9.11 | Apache-2.0 | Cobertura de tests |
| ktlint | 1.8.0 | MIT | Formato del código |
| detekt | 1.23.8 | Apache-2.0 | Reglas de código limpio |
| `@protomaps/basemaps` (Node) | 5.7.2 | BSD-3-Clause | Generar los estilos del mapa (`tools/map-style`) |
| pmtiles (línea de comandos) | 1.31.2 | BSD-3-Clause | Recortar paquetes de prueba (`scripts/install-test-package.sh`) |
| osmium-tool | 1.19.1 | GPL-3.0 | Recortar y filtrar los extractos de OpenStreetMap para el generador de paquetes |
| actions/checkout | 7.0.1 | MIT | Integración continua en GitHub |
| actions/setup-java | 6.3.0 | MIT | Integración continua en GitHub |
| actions/upload-artifact | 7.0.2 | MIT | Integración continua en GitHub |
| actions/cache (restore y save) | 6.1.0 | MIT | Caché de las dependencias descargadas en la integración continua |
| gradle/actions (solo wrapper-validation) | 6.4.1 | MIT | Comprobar que el lanzador de Gradle es el oficial |

## Datos

| Datos | Licencia | Cómo se cumple |
|---|---|---|
| Teselas de Protomaps derivadas de OpenStreetMap, en los paquetes municipales | ODbL 1.0 (obra producida) | Atribución "© OpenStreetMap (ODbL) · Protomaps" siempre visible sobre el mapa. Los paquetes no van dentro del APK: se instalan aparte. |
| Lugares (`places.json`) extraídos de OpenStreetMap con los extractos de Geofabrik | ODbL 1.0 (base de datos derivada) | Misma atribución visible, y cada ficha indica que el dato viene de OpenStreetMap. El generador es público, así que cualquiera puede rehacer el archivo, como exige la licencia. |

## Pendiente antes de la primera publicación

- Pantalla de licencias dentro de la app con los avisos que exigen las licencias BSD, MIT y OFL de lo
  que se distribuye con ella.

## Compatibilidad con la licencia del proyecto

El proyecto es GPL-3.0-or-later (decisión 0001). Son compatibles el código GPL-3, LGPL, Apache-2.0,
MIT, BSD y de dominio público. Antes de incorporar código GPL-2.0 hay que comprobar si es "or later":
el GPL-2.0 sin esa cláusula no se puede combinar con GPL-3. Las fuentes bajo la SIL Open Font License
se pueden distribuir con software de cualquier licencia, siempre que no se vendan por separado.
