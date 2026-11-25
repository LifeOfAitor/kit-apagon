# Investigación: plataforma Android y requisitos legales

Fecha: 2026-11-25. Estado: primera revisión.

## Versión mínima de Android

- Android 8.0 (API 26) o superior cubre aproximadamente el 96 % de los dispositivos Android del
  mundo, según datos de StatCounter de abril de 2026. Es una fuente secundaria: hay que confirmarlo
  con el gráfico de distribución de Android Studio al crear el proyecto.
- BitChat para Android usa también API 26 como mínimo, "para un soporte de Bluetooth LE correcto".

## Permisos de Bluetooth (cambian según la versión)

- **Android 8 a 11:** escanear Bluetooth exige el permiso de ubicación precisa y tener la ubicación
  activada en el sistema, aunque la app no use la ubicación. Hay que explicarlo bien al usuario.
- **Android 12 o superior:** permisos propios `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE` y
  `BLUETOOTH_CONNECT`. Con el indicador `neverForLocation` en `BLUETOOTH_SCAN` no hace falta el
  permiso de ubicación, siempre que la app nunca deduzca la ubicación a partir del escaneo.
- **Android 14 o superior:** el servicio en primer plano que mantiene la malla debe declararse con el
  tipo `connectedDevice` y el permiso `FOREGROUND_SERVICE_CONNECTED_DEVICE`.

Pendiente: revisar los cambios de comportamiento de Android 15, 16 y 17 en la documentación oficial
antes de escribir el servicio de malla.

## Medir la batería (principio 7)

- `adb shell dumpsys batterystats` y Battery Historian: gratuitos y funcionan en cualquier móvil.
- El perfilador de energía de Android Studio da datos precisos, pero solo en algunos móviles
  (Pixel recientes).
- Prueba de referencia propuesta: una hora de uso con la malla activa en un móvil de gama baja,
  midiendo cuánto porcentaje de batería consume.

## Distribución sin coste

| Canal | Coste | Comentario |
|---|---|---|
| GitHub Releases (APK directo) | Gratis | Suficiente para la demo. |
| F-Droid | Gratis | Exige código abierto y compilación reproducible. Encaja con el proyecto. |
| Google Play | 25 dólares, pago único | Es un gasto: lo decide el propietario más adelante. |

## Requisitos legales

- **Protección de datos (RGPD):** si los datos personales (plan familiar, ficha médica) nunca salen
  del dispositivo y el desarrollador no tiene acceso a ellos, el tratamiento lo hace el propio
  usuario. Aun así, la ficha médica es una categoría especial de datos: debe guardarse cifrada en
  el dispositivo. Google Play y F-Droid piden de todos modos una política de privacidad clara.
- **Mensajes en malla:** pasan por los móviles de otros vecinos. Los mensajes privados van cifrados
  de extremo a extremo. Los mensajes públicos los puede leer cualquiera, y la app debe decirlo.
- **Accesibilidad:** el Real Decreto 1112/2018 obliga a que las apps del sector público cumplan la
  norma UNE-EN 301549, que se basa en WCAG 2.1 nivel AA. Si el ayuntamiento adopta o distribuye la
  app, le aplicará. Conviene cumplirla desde el principio: es mucho más barato que adaptarla después.
- **Aviso sobre el 112:** principio 9, visible en la app.

## Fuentes

- https://developer.android.com/develop/connectivity/bluetooth/bt-permissions
- https://developer.android.com/develop/connectivity/bluetooth/ble/background
- https://composables.com/android-distribution-chart
- https://boe.es/buscar/pdf/2018/BOE-A-2018-12699-consolidado.pdf
- https://administracionelectronica.gob.es/comunidades/accesibilidad/documentacion
