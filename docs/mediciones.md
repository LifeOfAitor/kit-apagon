# Mediciones en dispositivos reales

Registro de lo medido en móviles reales, para seguir la evolución del rendimiento a medida que la
app crece (principio 7: el consumo se mide, no se supone). Las mediciones de la versión de
depuración no cuentan: siempre es más lenta y pesada que la de publicación.

## Dispositivos

| Dispositivo | Sistema | Memoria | Arquitectura | Observaciones |
|---|---|---|---|---|
| Samsung Galaxy A51 (SM-A515F) | crDroid, derivado de LineageOS, sobre Android 16 (API 36) | 4 GB | arm64-v8a | No es el sistema de Samsung: cierra menos las apps en segundo plano. No representa a un móvil de gama baja ni sirve para probar los permisos de Android 8 a 11. |

## 2026-11-26: esqueleto inicial (pantalla de inicio)

Versión de publicación (R8 activado), firmada solo para la prueba con la clave de depuración local.

| Medida | Resultado | Cómo se midió |
|---|---|---|
| Tamaño del APK | 0,9 MB | `app-release-unsigned.apk` |
| Arranque en frío | 459-483 ms (5 arranques) | `adb shell am start -W`, valor `TotalTime`, cerrando la app antes con `am force-stop` |
| Memoria | 31 MB (PSS) | `adb shell dumpsys meminfo`, con la pantalla de inicio abierta |

Comprobaciones manuales:

- La pantalla de inicio se ve correctamente en modo oscuro, con fondo negro.
- El botón "Llamar al 112" abre el marcador con el 112 escrito y no inicia la llamada. Se salió del
  marcador con la tecla atrás y el registro de llamadas del sistema quedó vacío.

Referencia: la versión de depuración tardó 2,9 s en arrancar y usó 75 MB.
