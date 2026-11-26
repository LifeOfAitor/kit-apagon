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

### 2026-11-26: memoria del mapa en el emulador de 2 GB

Emulador Pixel 9 con Android 16 (arm64), limitado a 2 GB de memoria y con pantalla de gama baja
(720 x 1600 a 320 ppp, ajustada con `adb shell wm size` y `wm density`). Versión de publicación, mapa
de Irun, tres pasadas por variante: vista inicial y después de cinco dobles toques sobre el centro
de Irun (nivel de calle). Script de medición en el historial de la sesión; se repetirá en una
herramienta del proyecto si estas mediciones se vuelven habituales.

| Variante | PSS vista inicial | PSS a nivel de calle | Memoria nativa a nivel de calle |
|---|---|---|---|
| Configuración actual | 96-132 MB | 117-135 MB | 94-95 MB |
| Sin precarga de zooms vecinos (`prefetchZoomDelta = 0`) | 115-128 MB | 125-126 MB | 85-86 MB |
| Sin caché de teselas (`tileCacheEnabled = false`) | 114-125 MB | 113-123 MB | 81-84 MB |

Conclusiones:

- **El emulador no contabiliza la memoria gráfica** (aparece 0 MB), que en el A51 era la parte más
  grande (145 MB). Sus cifras no son comparables con las de un móvil real.
- Con 2 GB y el mapa abierto, el sistema sigue en estado "normal" y la app no se cierra por falta de
  memoria. El mapa se dibuja correctamente en la pantalla de gama baja, también en modo claro.
- Las dos opciones de MapLibre reducen la memoria nativa alrededor de un 10 %, dentro de la variación
  entre pasadas. Desactivar la caché obliga además a volver a leer y procesar teselas, con más gasto
  de procesador y batería. **No se adopta ninguna** hasta medir la memoria gráfica en un móvil real
  de gama baja; eso forma parte de la decisión pendiente 0009.
- Se adopta sin coste: el mapa libera sus cachés cuando el sistema avisa de que queda poca memoria
  o cuando deja de verse (`onLowMemory` de MapLibre), algo que antes no se hacía.
- Una primera medición sin precarga se descartó porque la compilación había fallado y se midió, sin
  saberlo, el APK anterior. El script de medición ahora solo se ejecuta si la compilación termina bien.

## 2026-11-26: lugares en el mapa (decisión 0011)

Galaxy A51, versión de publicación, paquete de Irun con 273 lugares (`places.json`, 53 KB). Tres
pasadas: vista inicial y tras cinco dobles toques sobre el centro de Irun.

| Medida | Resultado | Antes de los lugares |
|---|---|---|
| Tamaño del APK, arm64-v8a | 14,4 MB | 14,2 MB |
| Tamaño del APK, armeabi-v7a | 11,1 MB | 11,0 MB |
| Memoria, vista inicial | 223-229 MB (PSS), 137 MB gráficos | 230 MB |
| Memoria, a nivel de calle | 241 MB (PSS), 136 MB gráficos, 80 MB nativa | - |

Los lugares apenas añaden memoria. El riesgo de memoria del mapa sigue siendo el mismo que antes.

Comprobaciones manuales en el móvil, sin red:

- Los lugares se dibujan con su icono y color. Cuando se solapan gana el más importante; las fuentes
  solo aparecen a partir del zoom 13.
- Los marcadores no tapan los nombres de pueblos, regiones y países (Irun, Oiartzun, Hendaye). En
  una primera versión los tapaban; un segundo intento los amontonaba unos sobre otros. La solución
  final coloca la capa de lugares justo encima de los nombres de barrios.
- Al tocar una farmacia el mapa se centra en ella y se abre su ficha con tipo y fuente del dato.
- Desde la lista, elegir el hospital del Bidasoa lo abre en el mapa con su ficha y su teléfono.
- La tecla atrás cierra primero la ficha y después vuelve a la pantalla de inicio. En la primera
  versión salía directamente al inicio.

Problemas de los datos de OpenStreetMap vistos en la prueba (no de la app):

- Una farmacia tiene como nombre su dirección ("Pintor Berrueta, 1").
- El hospital del Bidasoa aparece dos veces: como punto y como edificio. El generador debería
  eliminar estos duplicados; queda pendiente para el paso 3.
