# Kit Apagón

Aplicación Android que funciona sin conexión para la próxima caída de la red eléctrica: información
local de cada municipio, mensajería en malla por Bluetooth entre vecinos y una guía con fuentes
oficiales.

Estado: esqueleto inicial. La app solo muestra la pantalla de inicio con el aviso del 112.

La app no sustituye al 112 ni a los avisos oficiales.

## Documentación

- [Normas del proyecto](CLAUDE.md)
- [Decisiones técnicas](docs/decisiones/README.md)
- [Investigación](docs/investigacion/)
- [Fuentes de los contenidos de seguridad](docs/FUENTES.md)
- [Licencias de terceros](docs/licencias-de-terceros.md)

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

## Licencia

GPL-3.0-or-later. Ver [LICENSE](LICENSE) y la [decisión 0001](docs/decisiones/0001-licencia-gpl-3.md).
