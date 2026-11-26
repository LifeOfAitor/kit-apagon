# Cómo colaborar

Antes de nada, lee las [normas del proyecto](CLAUDE.md): son de obligado cumplimiento. Las decisiones
técnicas tomadas y sus motivos están en [docs/decisiones](docs/decisiones/README.md).

## Requisitos para compilar

- JDK 21. Sirve el que incluye Android Studio.
- Android SDK. Las plataformas que falten las descarga Gradle automáticamente.
- Android Studio, solo si se quiere abrir el proyecto en el editor. Tiene que ser compatible con
  Android Gradle Plugin 9.5.

En macOS, con Android Studio instalado:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

## Comandos

| Comando | Qué hace |
|---|---|
| `./gradlew verifyAll` | Todas las comprobaciones obligatorias antes de integrar un cambio: formato, código limpio, emojis, Lint, tests y compilación de publicación. |
| `./gradlew :app:assembleDebug` | Compila el APK de depuración en `app/build/outputs/apk/debug/`. |
| `./gradlew ktlintFormat` | Corrige el formato del código automáticamente. |
| `./gradlew :app:testDebugUnitTest :core:ui:testDebugUnitTest` | Ejecuta los tests. Cada test se ejecuta en Android 8 y en Android 16. |

## Probar el mapa en un móvil o emulador

Hasta que la app pueda descargar paquetes por sí misma, el paquete de prueba se instala con:

```bash
scripts/install-test-package.sh
```

Recorta el mapa de Irun (`ES-20045`) de la última compilación de Protomaps, extrae sus lugares del
extracto del País Vasco de Geofabrik (se guarda una semana en `~/.cache/kit-apagon/osm`), los convierte
con el generador de paquetes y lo instala todo en la versión de depuración de la app conectada por
cable. Necesita `pmtiles` y `osmium` (`brew install pmtiles osmium-tool`) y `python3`.
Para otro municipio: `scripts/install-test-package.sh IDENTIFICADOR oeste,sur,este,norte REGION`,
donde `REGION` es la ruta de Geofabrik, por ejemplo `europe/spain/madrid`.

## Regenerar el estilo del mapa

Solo hace falta si se cambia de versión de Protomaps o se modifica el estilo. Necesita Node:

```bash
cd tools/map-style && npm ci && npm run generate && ./download-assets.sh
```

Los iconos de los lugares se regeneran con `tools/map-style/download-place-icons.sh`.

## Problemas conocidos

- Si una tarea falla con `Failed to exec spawn helper` o `Cannot run program`, el proceso de Gradle en
  segundo plano se ha quedado en mal estado. Se arregla con `./gradlew --stop` y repitiendo la orden.

## Proponer un cambio

1. Crea una rama desde `main` (`feature/...`, `fix/...` o `docs/...`).
2. Haz commits pequeños con mensajes en español según Conventional Commits, sin emojis.
3. Ejecuta `./gradlew verifyAll` hasta que pase completo.
4. Abre una pull request contra `main`. Las comprobaciones automáticas se ejecutan en ella y deben
   pasar antes de integrarla.
