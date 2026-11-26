# Kit Apagón: normas del proyecto

Aplicación móvil que funciona sin conexión para la próxima caída de la red eléctrica:
información local en caché, mensajería en malla por Bluetooth y guía sin conexión.
Es una herramienta de emergencia: la fiabilidad está por encima de la velocidad de desarrollo.

## Principios innegociables

1. **La app no puede fallar.** Todo el código de lógica tiene tests unitarios. Nada se integra sin tests en verde.
2. **Investigar antes de decidir.** Toda decisión técnica relevante se documenta en `docs/decisiones/`
   (un archivo por decisión: contexto, alternativas consideradas, decisión y consecuencias).
3. **Coste cero.** Solo herramientas, servicios y datos gratuitos. Cualquier gasto lo decide el propietario.
4. **Nada ilegal.** Antes de reutilizar código, datos o textos se comprueba su licencia y se anota en
   `docs/licencias-de-terceros.md`. Sin datos personales fuera del dispositivo.
5. **Poco a poco.** Incrementos pequeños, cada uno verificado antes de pasar al siguiente.
6. **Sin conexión primero.** Todo lo esencial funciona sin red una vez instalada la app y descargado
   el paquete local. La red solo se usa para descargar o actualizar ese paquete.
7. **Móviles modestos.** El dispositivo de referencia es un Android de gama baja con 2 GB de RAM y la
   batería al 50 %. Sin animaciones costosas y con un modo de ahorro real. Cualquier función que use
   radio (Bluetooth, GPS, Wi-Fi) justifica su consumo, y el consumo se mide, no se supone.
8. **Privacidad total.** Sin cuentas, sin rastreo y sin analítica ni informes de errores de terceros.
   Los datos personales (plan familiar, ficha médica, etc.) se quedan solo en el dispositivo.
   Lo esencial no depende de los servicios de Google Play.
9. **Información fiable y con fuente.** Cada contenido de seguridad (guías, listas de comprobación)
   cita su fuente oficial en `docs/FUENTES.md`, con fecha de revisión. La app nunca sustituye al 112
   ni a los avisos oficiales, y lo dice de forma visible.

## Código limpio

- Nombres descriptivos y completos. Sin abreviaturas crípticas (`messageRepository`, no `msgRepo`).
- Funciones pequeñas con una sola responsabilidad.
- Sin código muerto, sin código comentado, sin TODO sin issue asociada.
- Los comentarios explican el porqué, nunca el qué.
- Identificadores y comentarios del código en inglés; documentación y textos de la interfaz en español.
- Sin emojis en el código, en los commits ni en la documentación.

## Stack

- Kotlin nativo, Android primero. iOS en una fase posterior.
- Licencia GPL-3.0-or-later y repositorio público (decisión 0001).
- Android 8.0 (API 26) como mínimo (decisión 0002).
- Malla Bluetooth compatible con el protocolo de BitChat, solo la parte sin internet (decisión 0003).
- Mapas con MapLibre y PMTiles; cada usuario descarga el paquete de su municipio. Cobertura: toda
  España, sin impedir otros países (decisión 0004).
- Guía propia con fuentes oficiales; Kiwix más adelante (decisión 0005).
- Un paquete por municipio (código INE, margen de 3 km, zoom máximo 14), generado cada mes en GitHub
  Actions, con índice firmado con Ed25519 y huellas SHA-256 que permiten usar réplicas (decisión 0006).
- Datos oficiales de los ayuntamientos en GeoJSON o CSV, publicados en su dominio oficial, validados
  automáticamente y guardados en una capa separada de OpenStreetMap (decisión 0007).
- Arquitectura en capas con módulos de Gradle; la lógica sin dependencias de Android va en módulos de
  Kotlin puro. Compose, MapLibre Native dentro de `AndroidView`, inyección manual, Room, DataStore,
  WorkManager, OkHttp, kotlinx.serialization y ACRA (decisión 0008).
- Solo versiones estables, fijadas en `gradle/libs.versions.toml`; nada en alfa o beta en el código que
  llega al usuario. Toda librería nueva se registra en `docs/licencias-de-terceros.md`.
- Calidad: JUnit 5 y kotlin.test en Kotlin puro, JUnit 4 con Robolectric donde Android lo exija,
  Turbine, Kotest para pruebas basadas en propiedades, objetos falsos antes que MockK, Kover, ktlint,
  Android Lint con avisos como errores y detekt 1.23.8 como herramienta independiente.
- Dispositivo de pruebas disponible: un Samsung Galaxy A51 con crDroid (Android 16, 4 GB de RAM). No es
  de gama baja ni cubre Android 8 a 11 (ver `docs/mediciones.md` y la decisión pendiente 0009).
  El emulador no sirve para probar Bluetooth.
- El resto de decisiones, y las pendientes, están en `docs/decisiones/README.md`.

## Compilar y comprobar

- `JAVA_HOME` debe apuntar a un JDK 21; en este Mac, el de Android Studio:
  `/Applications/Android Studio.app/Contents/jbr/Contents/Home`.
- `./gradlew verifyAll` ejecuta todas las comprobaciones obligatorias. Un cambio no está terminado
  hasta que esta tarea pasa completa.
- Los tests de Robolectric se ejecutan en Android 8 (API 26) y en la versión objetivo.
- Los avisos del compilador de Kotlin y de Android Lint son errores. No se desactiva una regla para
  que pase un cambio: se corrige el código, o se justifica la excepción por escrito junto a la propia regla.

## Flujo de trabajo con Git y GitHub

- La rama `main` siempre compila y pasa los tests. Nunca se trabaja directamente sobre ella.
  Durante la fase de investigación, sin código, los commits de documentación fueron directamente a
  `main`. Esa excepción terminó con el esqueleto del proyecto (rama `feature/esqueleto-proyecto`).
- Cada cambio va en una rama propia (`feature/...`, `fix/...`, `docs/...`) y entra en `main` mediante pull request.
- Los commits siguen Conventional Commits en español: `feat: añadir mapa sin conexión`,
  `fix: ...`, `test: ...`, `docs: ...`, `refactor: ...`, `chore: ...`.
- Un commit hace una sola cosa. Mensajes en imperativo y sin emojis.
- La integración continua (GitHub Actions) compila, pasa el linter y ejecuta los tests en cada pull request.
