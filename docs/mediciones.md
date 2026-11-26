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

## 2026-11-26: primer mapa sin conexión (decisión 0010)

Versión de publicación (R8 activado), APK de arm64-v8a firmado solo para la prueba con la clave de
depuración local. Paquete de Irun (`ES-20045`, margen de 3 km, zoom máximo 14, 3,8 MB) instalado con
`scripts/install-test-package.sh`. El móvil no tenía ninguna red activa durante la prueba
(`dumpsys connectivity`: `Active default network: none`).

| Medida | Resultado | Comparación con el esqueleto |
|---|---|---|
| Tamaño del APK, arm64-v8a | 14,2 MB | 0,9 MB antes: casi todo es la librería nativa de MapLibre |
| Tamaño del APK, armeabi-v7a | 11,0 MB | - |
| Arranque en frío hasta la pantalla de inicio | 513-553 ms (5 arranques) | 459-483 ms antes |
| Memoria en la pantalla de inicio | 34 MB (PSS) | 31 MB antes |
| Memoria con el mapa de Irun abierto | **230 MB (PSS)**: 145 MB gráficos, 60 MB nativa, 4 MB Java | - |

Comprobaciones manuales en el móvil:

- El mapa se dibuja sin red. El registro de MapLibre confirma que no tiene conexión
  (`Mbgl-ConnectivityReceiver: connected - false`) y no aparece ningún intento de acceso a internet.
- Los nombres salen en su forma oficial local: Irun, Hendaye, Oiartzun, Biriatou, Lesaka.
- Al acercarse a nivel de calle, el zoom 14 ampliado se lee bien: nombres de calles, barrios,
  edificios y lugares con icono. Queda resuelto el punto pendiente de la decisión 0006.
- La tecla atrás vuelve del mapa a la pantalla de inicio sin cerrar la app.
- MapLibre registra errores al pedir rangos de caracteres no incluidos (árabe, 1536-1791; tifinagh,
  11520-11775; selectores de variación, 65024-65279). Son nombres de lugares en otros alfabetos que no
  se muestran, como prevé la decisión 0010. No afecta al resto del mapa.

### Riesgo detectado: memoria con el mapa abierto

230 MB es mucho para el móvil de referencia del principio 7 (2 GB de RAM). La mayor parte (145 MB) es
memoria gráfica, que depende de la resolución de la pantalla: el A51 tiene 1080 x 2400, y un móvil
de gama baja suele tener menos. Antes de la demo hay que:

1. Medirlo en el emulador configurado con 2 GB de RAM y una pantalla de gama baja.
2. Revisar qué opciones de MapLibre reducen la memoria (caché de teselas, precarga de zooms vecinos,
   tipo de superficie de dibujo) y medir el efecto de cada una por separado.
