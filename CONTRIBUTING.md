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

## Problemas conocidos

- Si una tarea falla con `Failed to exec spawn helper` o `Cannot run program`, el proceso de Gradle en
  segundo plano se ha quedado en mal estado. Se arregla con `./gradlew --stop` y repitiendo la orden.

## Proponer un cambio

1. Crea una rama desde `main` (`feature/...`, `fix/...` o `docs/...`).
2. Haz commits pequeños con mensajes en español según Conventional Commits, sin emojis.
3. Ejecuta `./gradlew verifyAll` hasta que pase completo.
4. Abre una pull request contra `main`. Las comprobaciones automáticas se ejecutan en ella y deben
   pasar antes de integrarla.
